package com.cikup.amazgone.presentation

import com.cikup.amazgone.cart.domain.usecase.ObserveCartUseCase
import com.cikup.amazgone.catalog.data.mapper.CatalogBatch
import com.cikup.amazgone.catalog.data.repository.CatalogRepositoryImpl
import com.cikup.amazgone.testing.TestGraph
import com.cikup.amazgone.testing.eventually
import com.cikup.amazgone.testing.mainForViewModels
import com.cikup.amazgone.testing.product
import com.cikup.amazgone.wishlist.domain.usecase.SaveToWishlistUseCase
import com.cikup.amazgone.wishlist.presentation.WishlistEffect
import com.cikup.amazgone.wishlist.presentation.WishlistFilter
import com.cikup.amazgone.wishlist.presentation.WishlistIntent
import com.cikup.amazgone.wishlist.presentation.WishlistSort
import com.cikup.amazgone.wishlist.presentation.WishlistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WishlistViewModelTest {
    private lateinit var graph: TestGraph
    private val phone = product("1", price = 100.0, original = 120.0) // on sale
    private val lamp = product("2", price = 20.0)
    private val soldOut = product("3", price = 50.0, stock = 0)

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

    private suspend fun seed() {
        graph.get<CatalogRepositoryImpl>().save(CatalogBatch(listOf(phone, lamp, soldOut), emptyList()))
        listOf(phone, lamp, soldOut).forEach { graph.get<SaveToWishlistUseCase>()(it.id) }
    }

    @Test
    fun summarisesFiltersAndSorts() = runTest {
        seed()
        val vm = graph.get<WishlistViewModel>()
        val state = vm.state.eventually { it.products.size == 3 }
        assertEquals(1, state.onSaleCount)
        assertEquals(phone.originalPriceCoins!! - phone.priceCoins, state.savingsCoins)
        assertEquals(2, state.availableCount) // sold-out item can't go to the cart

        vm.onIntent(WishlistIntent.SetFilter(WishlistFilter.ON_SALE))
        assertEquals(listOf(phone.id), vm.state.eventually { it.filter == WishlistFilter.ON_SALE }.visible.map { it.id })
        vm.onIntent(WishlistIntent.SetFilter(WishlistFilter.ALL))
        vm.onIntent(WishlistIntent.SetSort(WishlistSort.PRICE_LOW))
        assertEquals(listOf(lamp.id, soldOut.id, phone.id), vm.state.eventually { it.sort == WishlistSort.PRICE_LOW && it.filter == WishlistFilter.ALL }.visible.map { it.id })
    }

    @Test
    fun addAllPutsAvailableItemsInTheCartAndRemoveCanBeUndone() = runTest {
        seed()
        val vm = graph.get<WishlistViewModel>()
        vm.state.eventually { it.products.size == 3 }

        vm.onIntent(WishlistIntent.AddAllToCart)
        assertEquals(WishlistEffect.AddedToCart(2), vm.effects.first())
        assertEquals(setOf(phone.id, lamp.id), vm.state.eventually { it.inCart.size == 2 }.inCart)
        assertEquals(2, graph.get<ObserveCartUseCase>()().first { it.itemCount == 2 }.itemCount)

        vm.onIntent(WishlistIntent.Remove(lamp.id))
        val undo = vm.effects.first()
        assertIs<WishlistEffect.ShowUndo>(undo)
        vm.state.eventually { it.products.size == 2 }
        vm.onIntent(WishlistIntent.Undo(undo.productId))
        vm.state.eventually { it.products.size == 3 }
    }
}
