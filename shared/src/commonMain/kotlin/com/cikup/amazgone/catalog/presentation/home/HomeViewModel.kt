package com.cikup.amazgone.catalog.presentation.home

import com.cikup.amazgone.catalog.domain.usecase.ObserveFlashSaleUseCase
import com.cikup.amazgone.catalog.domain.usecase.ObserveHomeFeedUseCase
import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.core.sync.domain.usecase.ObserveIsSyncingUseCase
import com.cikup.amazgone.core.sync.domain.usecase.ObserveSyncStatusUseCase
import com.cikup.amazgone.core.sync.domain.usecase.RequestSyncUseCase
import com.cikup.amazgone.games.domain.usecase.TickerUseCase
import com.cikup.amazgone.stores.domain.model.StoreKind
import com.cikup.amazgone.stores.domain.usecase.ObserveStoresUseCase
import com.cikup.amazgone.wishlist.domain.usecase.ObserveWishlistIdsUseCase
import com.cikup.amazgone.wishlist.domain.usecase.ToggleWishlistUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.shareIn

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    observeHomeFeed: ObserveHomeFeedUseCase,
    observeFlashSale: ObserveFlashSaleUseCase,
    observeSyncStatus: ObserveSyncStatusUseCase,
    observeIsSyncing: ObserveIsSyncingUseCase,
    observeWishlistIds: ObserveWishlistIdsUseCase,
    ticker: TickerUseCase,
    private val requestSync: RequestSyncUseCase,
    private val toggleWishlist: ToggleWishlistUseCase,
    observeStores: ObserveStoresUseCase,
) : MviViewModel<HomeState, HomeIntent, HomeEffect>(HomeState()) {

    private val selectedCategory = MutableStateFlow<String?>(null)

    init {
        selectedCategory
            .flatMapLatest { observeHomeFeed(it) }
            .observe { feed ->
                setState {
                    copy(
                        isLoading = false,
                        deals = feed.deals,
                        topRated = feed.topRated,
                        newArrivals = feed.newArrivals,
                        categories = feed.categories,
                        products = feed.products,
                    )
                }
            }
        val ticks = ticker().shareIn(vmScope, SharingStarted.WhileSubscribed(), replay = 1)
        ticks.observe { setState { copy(now = it) } }
        observeFlashSale(ticks, FLASH_ITEMS).observe { setState { copy(flashSale = it) } }
        observeStores().observe { sections -> setState { copy(officialStores = sections[StoreKind.OFFICIAL].orEmpty().take(OFFICIAL_STORES)) } }
        observeWishlistIds().observe { setState { copy(savedIds = it) } }
        observeSyncStatus().observe { setState { copy(syncStatus = it) } }
        observeIsSyncing().observe { setState { copy(isRefreshing = it) } }
    }

    override fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.SelectCategory -> {
                val next = intent.slug.takeIf { it != currentState.selectedCategory }
                selectedCategory.value = next
                setState { copy(selectedCategory = next) }
            }
            HomeIntent.Refresh -> requestSync(force = true)
            is HomeIntent.Open -> sendEffect(HomeEffect.Navigate(intent.destination))
            is HomeIntent.OpenProduct -> sendEffect(HomeEffect.NavigateToProduct(intent.productId, intent.origin))
            is HomeIntent.OpenStore -> sendEffect(HomeEffect.NavigateToStore(intent.storeId))
            is HomeIntent.ToggleSaved -> launchSafely { toggleWishlist(intent.productId) }
        }
    }

    private companion object {
        const val FLASH_ITEMS = 8
        const val OFFICIAL_STORES = 12
    }
}
