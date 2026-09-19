package com.cikup.amazgone.presentation

import com.cikup.amazgone.account.presentation.AccountIntent
import com.cikup.amazgone.account.presentation.AccountViewModel
import com.cikup.amazgone.account.presentation.AuthMode
import com.cikup.amazgone.cart.domain.usecase.AddToCartUseCase
import com.cikup.amazgone.catalog.data.mapper.CatalogBatch
import com.cikup.amazgone.catalog.data.repository.CatalogRepositoryImpl
import com.cikup.amazgone.core.domain.DomainError
import com.cikup.amazgone.core.domain.ValidationReason
import com.cikup.amazgone.core.sync.domain.SyncEngine
import com.cikup.amazgone.orders.domain.model.AddressValidator
import com.cikup.amazgone.orders.domain.model.OrderStatus
import com.cikup.amazgone.orders.presentation.checkout.CheckoutEffect
import com.cikup.amazgone.orders.presentation.checkout.CheckoutIntent
import com.cikup.amazgone.orders.presentation.checkout.CheckoutStep
import com.cikup.amazgone.orders.domain.model.DeliveryOption
import com.cikup.amazgone.orders.presentation.checkout.CheckoutViewModel
import com.cikup.amazgone.orders.presentation.list.OrdersViewModel
import com.cikup.amazgone.remote.ok
import com.cikup.amazgone.remote.userDoc
import com.cikup.amazgone.testing.TestGraph
import com.cikup.amazgone.testing.eventually
import com.cikup.amazgone.testing.product
import com.cikup.amazgone.wallet.presentation.WalletViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import com.cikup.amazgone.testing.mainForViewModels
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private const val TOKENS = """{"localId":"uid-1","idToken":"id-1","refreshToken":"r-1","expiresIn":"3600"}"""

class AccountCheckoutViewModelsTest {
    private lateinit var graph: TestGraph
    private val phone = product("1", price = 100.0)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainForViewModels)
        graph = TestGraph()
    }

    @AfterTest
    fun tearDown() {
        graph.close()
        Dispatchers.resetMain()
    }

    @Test
    fun registrationValidatesThenSignsInAndQueuesProfile() = runTest {
        graph.backend.onPathEnds("POST", "accounts:signUp", ok(TOKENS))
        val vm = graph.get<AccountViewModel>()
        vm.onIntent(AccountIntent.SwitchMode(AuthMode.REGISTER))
        vm.onIntent(AccountIntent.UsernameChanged("x"))
        vm.onIntent(AccountIntent.Submit)
        val invalid = vm.state.eventually { it.form.fieldErrors.isNotEmpty() }
        assertEquals(ValidationReason.TOO_SHORT, invalid.form.fieldErrors["username"]?.reason)
        assertEquals(1, invalid.form.errorPulse)

        vm.onIntent(AccountIntent.UsernameChanged("shopper"))
        vm.onIntent(AccountIntent.PasswordChanged("password1"))
        vm.onIntent(AccountIntent.ConfirmationChanged("password1"))
        vm.onIntent(AccountIntent.Submit)
        assertEquals("shopper", vm.state.eventually { it.session != null }.session?.username)
    }

    @Test
    fun signOutAsksFirstWhenChangesAreUnsynced() = runTest {
        graph.backend.onPathEnds("POST", "accounts:signInWithPassword", ok(TOKENS))
        graph.backend.onPathEnds("GET", "users/uid-1", ok(userDoc("uid-1", coins = 5_000)))
        val vm = graph.get<AccountViewModel>()
        vm.onIntent(AccountIntent.UsernameChanged("shopper"))
        vm.onIntent(AccountIntent.PasswordChanged("password1"))
        vm.onIntent(AccountIntent.Submit)
        vm.state.eventually { it.session != null }
        graph.get<CatalogRepositoryImpl>().save(CatalogBatch(listOf(phone), emptyList()))
        graph.get<AddToCartUseCase>()(phone.id) // creates an unsynced cart change

        vm.onIntent(AccountIntent.SignOut)
        vm.state.eventually { it.pendingSignOutCount != null }
        vm.onIntent(AccountIntent.ConfirmSignOut)
        vm.state.eventually { it.session == null && it.pendingSignOutCount == null && it.coins == 5_000L }
    }

    @Test
    fun checkoutWalksStepsAndPlacesAnOfflineOrder() = runTest {
        graph.grantGuestWallet()
        graph.get<CatalogRepositoryImpl>().save(CatalogBatch(listOf(phone), emptyList()))
        graph.get<AddToCartUseCase>()(phone.id)
        val vm = graph.get<CheckoutViewModel>()
        val first = vm.state.eventually { !it.summary.isEmpty && it.balance == 5_000L && it.addressLoaded }
        assertEquals(CheckoutStep.ADDRESS, first.step) // no earlier order to reuse an address from

        vm.onIntent(CheckoutIntent.Next)
        assertEquals(5, vm.state.eventually { it.invalidFields.isNotEmpty() }.invalidFields.size)
        fillAddress(vm)
        vm.onIntent(CheckoutIntent.Next)
        vm.state.eventually { it.step == CheckoutStep.REVIEW }
        vm.onIntent(CheckoutIntent.EditAddress)
        vm.state.eventually { it.step == CheckoutStep.ADDRESS }
        vm.onIntent(CheckoutIntent.Back) // back from editing returns to the review page, not out of checkout
        vm.state.eventually { it.step == CheckoutStep.REVIEW }

        vm.onIntent(CheckoutIntent.SelectDelivery(DeliveryOption.EXPRESS))
        val review = vm.state.eventually { it.delivery == DeliveryOption.EXPRESS }
        assertEquals(DeliveryOption.EXPRESS.feeCoins, review.deliveryFeeCoins)
        assertEquals(1_000L + DeliveryOption.EXPRESS.feeCoins, review.totalCoins)
        assertEquals(listOf(phone.store.id), review.shipments.map { it.store.id })

        vm.onIntent(CheckoutIntent.Pay)
        val order = vm.state.eventually { it.placedOrder != null }.placedOrder!!
        assertEquals(OrderStatus.PENDING_SYNC, order.status)
        assertEquals(DeliveryOption.EXPRESS, order.delivery)
        assertEquals(phone.store.name, order.items.single().storeName)
        vm.onIntent(CheckoutIntent.ViewOrder)
        assertEquals(CheckoutEffect.OpenOrder(order.id), vm.effects.first())

        assertEquals(1, graph.get<OrdersViewModel>().state.eventually { it.orders.isNotEmpty() }.orders.size)
        val expected = 5_000L - 1_000L - DeliveryOption.EXPRESS.feeCoins
        assertEquals(expected, graph.get<WalletViewModel>().state.eventually { it.summary.coins == expected }.summary.coins)

        // Next checkout reuses the last address and opens straight on the review page.
        graph.get<AddToCartUseCase>()(phone.id)
        val again = graph.koin.get<CheckoutViewModel>().state.eventually { it.addressLoaded && !it.summary.isEmpty }
        assertEquals(CheckoutStep.REVIEW, again.step)
        assertEquals("1 Main", again.address.line1)
    }

    private fun fillAddress(vm: CheckoutViewModel) = mapOf(
        AddressValidator.FIELD_NAME to "Bob", AddressValidator.FIELD_LINE1 to "1 Main", AddressValidator.FIELD_CITY to "Jakarta",
        AddressValidator.FIELD_POSTAL to "10110", AddressValidator.FIELD_COUNTRY to "Indonesia",
    ).forEach { (field, value) -> vm.onIntent(CheckoutIntent.FieldChanged(field, value)) }

    @Test
    fun checkoutRefusesWhenTheWalletIsShort() = runTest {
        graph.grantGuestWallet()
        graph.get<CatalogRepositoryImpl>().save(CatalogBatch(listOf(phone), emptyList()))
        graph.get<AddToCartUseCase>()(phone.id, quantity = 6)
        val vm = graph.get<CheckoutViewModel>()
        val state = vm.state.eventually { it.summary.itemCount == 6 && it.balance > 0 }
        assertEquals(false, state.canAfford)
        vm.onIntent(CheckoutIntent.Pay)
        assertIs<DomainError.Validation>(vm.state.eventually { it.error != null }.error) // address still missing
    }

    @Test
    fun syncEngineConfirmsQueuedOrderAfterSignIn() = runTest {
        graph.backend.onPathEnds("POST", "accounts:signInWithPassword", ok(TOKENS))
        graph.backend.onPathEnds("GET", "users/uid-1", ok(userDoc("uid-1", coins = 5_000)))
        graph.backend.onPathEnds("POST", ":commit", ok("{}"))
        graph.backend.onPathEnds("POST", ":runQuery", ok("[]"))
        graph.backend.on({ p -> p.method.value == "GET" && listOf("/orders", "/cart", "/wishlist", "/achievements").any { p.url.encodedPath.endsWith(it) } }, ok("{}"))
        graph.grantGuestWallet()
        graph.get<CatalogRepositoryImpl>().save(CatalogBatch(listOf(phone), emptyList()))
        graph.get<AddToCartUseCase>()(phone.id)
        val checkout = graph.get<CheckoutViewModel>()
        checkout.state.eventually { !it.summary.isEmpty }
        listOf("Bob", "1 Main", "Jakarta", "10110", "Indonesia").zip(
            listOf(AddressValidator.FIELD_NAME, AddressValidator.FIELD_LINE1, AddressValidator.FIELD_CITY, AddressValidator.FIELD_POSTAL, AddressValidator.FIELD_COUNTRY),
        ).forEach { (value, field) -> checkout.onIntent(CheckoutIntent.FieldChanged(field, value)) }
        checkout.onIntent(CheckoutIntent.Pay)
        val order = checkout.state.eventually { it.placedOrder != null }.placedOrder!!

        val account = graph.get<AccountViewModel>()
        account.onIntent(AccountIntent.UsernameChanged("shopper"))
        account.onIntent(AccountIntent.PasswordChanged("password1"))
        account.onIntent(AccountIntent.Submit)
        account.state.eventually { it.session != null }
        graph.get<SyncEngine>().sync()

        graph.get<OrdersViewModel>().state.eventually { s -> s.orders.any { it.id == order.id && it.status == OrderStatus.CONFIRMED } }
    }
}
