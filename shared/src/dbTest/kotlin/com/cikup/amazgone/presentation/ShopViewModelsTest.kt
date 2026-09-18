package com.cikup.amazgone.presentation

import com.cikup.amazgone.cart.domain.usecase.AddToCartUseCase
import com.cikup.amazgone.cart.presentation.CartEffect
import com.cikup.amazgone.cart.presentation.CartIntent
import com.cikup.amazgone.cart.presentation.CartViewModel
import com.cikup.amazgone.catalog.data.mapper.CatalogBatch
import com.cikup.amazgone.catalog.data.repository.CatalogRepositoryImpl
import com.cikup.amazgone.catalog.domain.model.SortOrder
import com.cikup.amazgone.catalog.presentation.detail.ProductDetailEffect
import com.cikup.amazgone.catalog.presentation.detail.ProductDetailIntent
import com.cikup.amazgone.catalog.presentation.detail.ProductDetailViewModel
import com.cikup.amazgone.catalog.presentation.home.HomeEffect
import com.cikup.amazgone.catalog.presentation.home.HomeIntent
import com.cikup.amazgone.catalog.presentation.home.HomeViewModel
import com.cikup.amazgone.catalog.presentation.search.SearchIntent
import com.cikup.amazgone.catalog.presentation.search.SearchViewModel
import com.cikup.amazgone.testing.TestGraph
import com.cikup.amazgone.testing.eventually
import com.cikup.amazgone.testing.product
import com.cikup.amazgone.wishlist.presentation.WishlistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import com.cikup.amazgone.testing.mainForViewModels
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.parameter.parametersOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ShopViewModelsTest {
    private lateinit var graph: TestGraph
    private val phone = product("1", price = 100.0, original = 120.0, title = "Galaxy Phone", category = "smartphones")
    private val case = product("2", price = 5.0, title = "Phone Case", category = "smartphones", rating = 3.0)
    private val lamp = product("3", price = 20.0, title = "Desk Lamp", category = "lighting")

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

    private suspend fun seed() = graph.get<CatalogRepositoryImpl>().save(CatalogBatch(listOf(phone, case, lamp), emptyList()))

    @Test
    fun homeLoadsFeedAndFiltersByCategory() = runTest {
        seed()
        val vm = graph.get<HomeViewModel>()
        val loaded = vm.state.eventually { !it.isLoading && it.products.size == 3 }
        assertEquals(listOf(phone.id), loaded.deals.map { it.id })

        vm.onIntent(HomeIntent.SelectCategory("lighting"))
        assertEquals(listOf(lamp.id), vm.state.eventually { it.products.size == 1 }.products.map { it.id })
        vm.onIntent(HomeIntent.SelectCategory("lighting")) // tapping again clears the filter
        vm.state.eventually { it.selectedCategory == null && it.products.size == 3 }

        vm.onIntent(HomeIntent.OpenProduct(lamp.id, "grid"))
        assertEquals(HomeEffect.NavigateToProduct(lamp.id, "grid"), vm.effects.first())
    }

    @Test
    fun searchUsesLocalIndexFiltersAndSort() = runTest {
        seed()
        val vm = graph.koin.get<SearchViewModel> { parametersOf(null) }
        vm.onIntent(SearchIntent.QueryChanged("phone"))
        val results = vm.state.eventually { it.query == "phone" && it.results.size == 2 }.results
        assertTrue(results.all { "Phone" in it.title })

        vm.onIntent(SearchIntent.ChangeSort(SortOrder.PRICE_LOW_TO_HIGH))
        assertEquals(listOf(case.id, phone.id), vm.state.eventually { it.sort == SortOrder.PRICE_LOW_TO_HIGH && it.results.firstOrNull()?.id == case.id }.results.map { it.id })

        vm.onIntent(SearchIntent.ClearQuery)
        vm.onIntent(SearchIntent.SelectCategory("lighting"))
        vm.state.eventually { it.results.map { p -> p.id } == listOf(lamp.id) }
    }

    @Test
    fun detailAddsToCartFliesAndTogglesWishlist() = runTest {
        seed()
        val vm = graph.koin.get<ProductDetailViewModel> { parametersOf(phone.id, "grid") }
        vm.state.eventually { it.product != null && it.recommendations.isNotEmpty() }

        vm.onIntent(ProductDetailIntent.AddToCart)
        assertIs<ProductDetailEffect.FlyToCart>(vm.effects.first())
        vm.state.eventually { it.quantityInCart == 1 }

        vm.onIntent(ProductDetailIntent.ToggleWishlist)
        vm.state.eventually { it.isSaved }
        val wishlist = graph.get<WishlistViewModel>()
        assertEquals(listOf(phone.id), wishlist.state.eventually { it.products.isNotEmpty() }.products.map { it.id })
    }

    @Test
    fun unknownProductShowsNotFound() = runTest {
        val vm = graph.koin.get<ProductDetailViewModel> { parametersOf("dummyjson:missing", "grid") }
        assertTrue(vm.state.eventually { !it.isLoading }.notFound)
    }

    @Test
    fun cartChangesQuantityRemovesWithUndoAndStartsCheckout() = runTest {
        seed()
        graph.get<AddToCartUseCase>()(phone.id)
        val vm = graph.get<CartViewModel>()
        vm.state.eventually { it.summary.itemCount == 1 }

        vm.onIntent(CartIntent.ChangeQuantity(phone.id, 3))
        assertEquals(3_000, vm.state.eventually { it.summary.itemCount == 3 }.summary.subtotalCoins)

        vm.onIntent(CartIntent.Checkout)
        assertEquals(CartEffect.NavigateToCheckout, vm.effects.first())

        vm.onIntent(CartIntent.Remove(phone.id))
        val undo = vm.effects.first() as CartEffect.ShowUndo
        vm.state.eventually { it.summary.isEmpty }
        vm.onIntent(CartIntent.Undo(undo.entry))
        vm.state.eventually { it.summary.itemCount == 3 }
    }
}
