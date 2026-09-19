package com.cikup.amazgone.presentation

import com.cikup.amazgone.cart.domain.usecase.AddToCartUseCase
import com.cikup.amazgone.cart.domain.usecase.ObserveCartUseCase
import com.cikup.amazgone.catalog.data.mapper.CatalogBatch
import com.cikup.amazgone.catalog.data.repository.CatalogRepositoryImpl
import com.cikup.amazgone.orders.data.OrderRepositoryImpl
import com.cikup.amazgone.orders.domain.model.OrderStage
import com.cikup.amazgone.orders.domain.model.ShippingAddress
import com.cikup.amazgone.orders.domain.usecase.PlaceOrderUseCase
import com.cikup.amazgone.orders.presentation.detail.OrderDetailIntent
import com.cikup.amazgone.orders.presentation.detail.OrderDetailViewModel
import com.cikup.amazgone.reviews.domain.model.ReviewProblem
import com.cikup.amazgone.reviews.domain.usecase.ObserveProductReviewsUseCase
import com.cikup.amazgone.testing.TestGraph
import com.cikup.amazgone.testing.eventually
import com.cikup.amazgone.testing.mainForViewModels
import com.cikup.amazgone.testing.product
import com.cikup.amazgone.core.domain.DomainResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.parameter.parametersOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrderReviewTest {
    private lateinit var graph: TestGraph
    private val charger = product("7", price = 20.0).copy(brand = "Apple")

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

    private suspend fun placeOrder(): String {
        graph.grantGuestWallet()
        graph.get<CatalogRepositoryImpl>().save(CatalogBatch(listOf(charger), emptyList()))
        graph.get<AddToCartUseCase>()(charger.id)
        val summary = graph.get<ObserveCartUseCase>()().first { !it.isEmpty }
        val order = graph.get<PlaceOrderUseCase>()(summary, ShippingAddress("Bob", "1 Main", "Jakarta", "10110", "Indonesia"))
        return (order as DomainResult.Success).value.id
    }

    @Test
    fun reviewUnlocksOnlyAfterTheParcelIsReceived() = runTest {
        val orderId = placeOrder()
        val vm = graph.koin.get<OrderDetailViewModel> { parametersOf(orderId) }
        assertEquals(OrderStage.PLACED, vm.state.eventually { it.order != null }.stage)

        vm.onIntent(OrderDetailIntent.WriteReview(charger.id))
        assertNull(vm.state.value.reviewDraft) // not delivered yet: no review sheet

        graph.get<OrderRepositoryImpl>().markConfirmed(orderId) // server accepted it
        assertTrue(vm.state.eventually { it.stage == OrderStage.CONFIRMED }.canConfirmReceived)
        vm.onIntent(OrderDetailIntent.ConfirmReceived)
        vm.state.eventually { it.stage == OrderStage.DELIVERED }

        vm.onIntent(OrderDetailIntent.WriteReview(charger.id))
        vm.state.eventually { it.reviewDraft != null }
        vm.onIntent(OrderDetailIntent.SubmitReview)
        assertEquals(setOf(ReviewProblem.NO_RATING, ReviewProblem.COMMENT_TOO_SHORT), vm.state.eventually { it.reviewDraft?.problems?.isNotEmpty() == true }.reviewDraft?.problems)

        vm.onIntent(OrderDetailIntent.SetRating(5))
        vm.onIntent(OrderDetailIntent.SetComment("Charges fast"))
        vm.onIntent(OrderDetailIntent.SubmitReview)
        val mine = vm.state.eventually { it.reviewDraft == null && charger.id in it.myReviews }.myReviews.getValue(charger.id)
        assertEquals(5, mine.rating)
        assertTrue(mine.isPending)

        val shown = graph.get<ObserveProductReviewsUseCase>()(charger.id).first()
        assertEquals(listOf("Charges fast"), shown.map { it.comment })
    }
}
