package com.cikup.amazgone.orders

import com.cikup.amazgone.account.data.remote.AuthRepositoryImpl
import com.cikup.amazgone.account.data.repository.ProfileRepositoryImpl
import com.cikup.amazgone.account.data.sync.ProfilePuller
import com.cikup.amazgone.account.domain.usecase.LoginUseCase
import com.cikup.amazgone.account.domain.usecase.LogoutUseCase
import com.cikup.amazgone.cart.data.coupon.CouponRepositoryImpl
import com.cikup.amazgone.cart.data.repository.CartRepositoryImpl
import com.cikup.amazgone.cart.domain.model.CartLine
import com.cikup.amazgone.cart.domain.model.CartCalculator
import com.cikup.amazgone.cart.domain.model.Coupon
import com.cikup.amazgone.cart.domain.model.CouponKind
import com.cikup.amazgone.core.common.PrintLogger
import com.cikup.amazgone.core.database.RoomTransactionRunner
import com.cikup.amazgone.core.database.inMemoryDatabase
import com.cikup.amazgone.core.domain.DomainError
import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.core.remote.FirebaseAuthApi
import com.cikup.amazgone.core.remote.FirebaseServices
import com.cikup.amazgone.core.storage.InMemorySecureStore
import com.cikup.amazgone.core.sync.data.RoomOutboxStore
import com.cikup.amazgone.core.sync.domain.PushResult
import com.cikup.amazgone.orders.data.OrderPlaceHandler
import com.cikup.amazgone.orders.data.OrderRepositoryImpl
import com.cikup.amazgone.orders.data.REJECT_INSUFFICIENT_COINS
import com.cikup.amazgone.orders.domain.model.OrderStatus
import com.cikup.amazgone.orders.domain.model.ShippingAddress
import com.cikup.amazgone.orders.domain.usecase.PlaceOrderUseCase
import com.cikup.amazgone.remote.FakeFirebase
import com.cikup.amazgone.remote.TEST_CONFIG
import com.cikup.amazgone.remote.error
import com.cikup.amazgone.remote.ok
import com.cikup.amazgone.remote.userDoc
import com.cikup.amazgone.testing.FakeClock
import com.cikup.amazgone.testing.SequentialIds
import com.cikup.amazgone.testing.product
import com.cikup.amazgone.wallet.data.WalletRepositoryImpl
import com.cikup.amazgone.wallet.domain.model.Currency
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private val ADDRESS = ShippingAddress("Bob Buyer", "1 Main St", "Jakarta", "12345", "Indonesia")
private const val TOKENS = """{"localId":"uid-1","idToken":"id-1","refreshToken":"r-1","expiresIn":"3600"}"""

class CheckoutFlowTest {
    private val db = inMemoryDatabase()
    private val clock = FakeClock()
    private val ids = SequentialIds()
    private val tx = RoomTransactionRunner(db)
    private val outbox = RoomOutboxStore(db.outboxDao(), clock)
    private val wallet = WalletRepositoryImpl(db.ledgerDao(), tx, clock, ids)
    private val cart = CartRepositoryImpl(db.cartDao(), outbox, tx, clock, ids)
    private val coupons = CouponRepositoryImpl(db.couponDao(), clock)
    private val orders = OrderRepositoryImpl(db.orderDao(), wallet, cart, coupons, outbox, tx, clock, ids)
    private val backend = FakeFirebase()
    private val auth = AuthRepositoryImpl(FirebaseAuthApi(backend.client, TEST_CONFIG), InMemorySecureStore(), clock, PrintLogger)
    private val firebase = FirebaseServices(FirebaseAuthApi(backend.client, TEST_CONFIG), backend.firestore())
    private val handler = OrderPlaceHandler(auth, firebase, orders)
    private val placeOrder = PlaceOrderUseCase(orders, wallet)
    private val phone = product("1", price = 100.0) // 1,000 coins

    @AfterTest
    fun tearDown() = db.close()

    private suspend fun signIn() {
        backend.onPathEnds("POST", "accounts:signInWithPassword", ok(TOKENS))
        auth.login("bob", "password1")
        backend.onPathEnds("POST", ":beginTransaction", ok("""{"transaction":"tx"}"""))
    }

    private suspend fun checkout(quantity: Int = 1, coupon: Coupon? = null) = run {
        wallet.run()
        cart.setQuantity(phone.id, quantity)
        coupon?.let { coupons.grant(it) }
        placeOrder(CartCalculator.summarize(listOf(CartLine(phone, quantity)), coupon), ADDRESS)
    }

    @Test
    fun placingAnOrderIsOneAtomicLocalStep() = runTest {
        val order = (checkout(quantity = 2) as DomainResult.Success).value

        assertEquals(OrderStatus.PENDING_SYNC, order.status)
        assertEquals(3_000L, wallet.balance(Currency.COINS))
        assertEquals(200L, wallet.balance(Currency.XP))
        assertTrue(cart.observeEntries().first().isEmpty())
        assertTrue(outbox.head(20).any { it.id == order.id && it.type == "order.place" })
    }

    @Test
    fun insufficientLocalBalanceIsRefusedBeforeAnythingIsWritten() = runTest {
        val failure = (checkout(quantity = 6) as DomainResult.Failure).error
        assertIs<DomainError.InsufficientCoins>(failure)
        assertEquals(6, cart.quantityOf(phone.id))
        assertEquals(5_000L, wallet.balance(Currency.COINS))
    }

    @Test
    fun concurrentCheckoutsCannotOverspendTheWallet() = runTest {
        wallet.run()
        val summary = CartCalculator.summarize(listOf(CartLine(phone, 3)), null) // 3,000 of 5,000 coins
        val results = listOf(
            async { placeOrder(summary, ADDRESS) },
            async { placeOrder(summary, ADDRESS) },
        ).awaitAll()

        assertEquals(1, results.count { it is DomainResult.Success })
        assertIs<DomainError.InsufficientCoins>((results.single { it is DomainResult.Failure } as DomainResult.Failure).error)
        assertEquals(2_000L, wallet.balance(Currency.COINS))
    }

    @Test
    fun signingInAsSomeoneElseAfterExpiryWipesTheOldUsersData() = runTest {
        signIn()
        checkout()
        backend.onPathEnds("POST", "/token", error(HttpStatusCode.BadRequest, "TOKEN_EXPIRED"))
        clock.advance(3_600_000)
        runCatching { auth.idToken() }
        assertEquals("bob", auth.expiredSession.value?.username)
        assertEquals(1, orders.observeOrders().first().size, "expiry alone keeps local data")

        backend.onPathEnds("POST", "accounts:signInWithPassword", ok(TOKENS.replace("uid-1", "uid-2")))
        LoginUseCase(auth, listOf(wallet, orders, outbox))("alice", "password1")

        assertTrue(orders.observeOrders().first().isEmpty())
        assertTrue(outbox.head(20).isEmpty(), "bob's unsynced order must never reach alice's account")
    }

    @Test
    fun serverConfirmationMarksOrderAndLedgerConfirmed() = runTest {
        signIn()
        backend.onPathEnds("GET", "users/uid-1", ok(userDoc("uid-1", coins = 5_000)))
        backend.onPathEnds("POST", ":commit", ok("{}"))
        val order = (checkout() as DomainResult.Success).value

        val result = handler.push(outbox.head(20).first { it.id == order.id })

        assertEquals(PushResult.Success, result)
        assertEquals(OrderStatus.CONFIRMED, orders.observeOrder(order.id).first()!!.status)
        assertEquals(0L, wallet.observePending(Currency.COINS).first())
    }

    @Test
    fun serverRejectionRefundsRestoresCartAndCoupon() = runTest {
        signIn()
        backend.onPathEnds("GET", "users/uid-1", ok(userDoc("uid-1", coins = 10)))
        val coupon = Coupon("SPIN10", CouponKind.PERCENT, 10)
        val order = (checkout(coupon = coupon) as DomainResult.Success).value
        assertEquals(4_100L, wallet.balance(Currency.COINS))
        val entry = outbox.head(20).first { it.id == order.id }

        val result = handler.push(entry)
        assertEquals(PushResult.Rejected(REJECT_INSUFFICIENT_COINS), result)
        handler.onRejected(entry, REJECT_INSUFFICIENT_COINS)

        assertEquals(OrderStatus.REJECTED, orders.observeOrder(order.id).first()!!.status)
        assertEquals(5_000L, wallet.balance(Currency.COINS))
        assertEquals(1, cart.quantityOf(phone.id))
        assertEquals(listOf("SPIN10"), coupons.observeAvailable(clock.now).first().map { it.code })
    }

    @Test
    fun alreadyCommittedOrderIsTreatedAsSuccess() = runTest {
        signIn()
        backend.onPathEnds("GET", "users/uid-1", ok(userDoc("uid-1", coins = 5_000)))
        backend.onPathEnds("POST", ":commit", error(HttpStatusCode.BadRequest, "FAILED_PRECONDITION"))
        val order = (checkout() as DomainResult.Success).value

        assertEquals(PushResult.Success, handler.push(outbox.head(20).first { it.id == order.id }))
        assertEquals(OrderStatus.CONFIRMED, orders.observeOrder(order.id).first()!!.status)
    }

    @Test
    fun outageKeepsTheOrderPendingForRetry() = runTest {
        signIn()
        backend.onPathEnds("GET", "users/uid-1", error(HttpStatusCode.ServiceUnavailable, "UNAVAILABLE"))
        val order = (checkout() as DomainResult.Success).value

        assertIs<PushResult.Retry>(handler.push(outbox.head(20).first { it.id == order.id }))
        assertEquals(OrderStatus.PENDING_SYNC, orders.observeOrder(order.id).first()!!.status)
        assertEquals(4_000L, wallet.balance(Currency.COINS))
    }

    @Test
    fun pulledProfileRebasesConfirmedButKeepsPendingRows() = runTest {
        signIn()
        checkout()
        backend.onPathEnds("GET", "users/uid-1", ok(userDoc("uid-1", coins = 9_000, xp = 300)))
        val profiles = ProfileRepositoryImpl(db.userProfileDao(), outbox, ids)

        ProfilePuller(auth, firebase, profiles, wallet, clock).pull(force = true)

        assertEquals(8_000L, wallet.balance(Currency.COINS), "server 9,000 minus the pending 1,000 order")
        assertEquals(400L, wallet.balance(Currency.XP))
        assertEquals("bob", profiles.current()?.username)
    }

    @Test
    fun logoutRequiresConfirmationWhilePendingThenWipesUserData() = runTest {
        signIn()
        checkout()
        val logout = LogoutUseCase(auth, outbox, listOf(wallet, orders, outbox))

        assertIs<DomainError.PendingChanges>((logout() as DomainResult.Failure).error)
        assertIs<DomainResult.Success<Unit>>(logout(force = true))

        assertEquals(5_000L, wallet.balance(Currency.COINS), "fresh guest starter")
        assertTrue(orders.observeOrders().first().isEmpty())
        assertTrue(outbox.head(20).isEmpty())
        assertEquals(null, auth.session.value)
    }
}
