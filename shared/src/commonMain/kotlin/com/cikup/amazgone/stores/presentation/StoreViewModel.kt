package com.cikup.amazgone.stores.presentation

import com.cikup.amazgone.catalog.domain.model.SortOrder
import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.stores.domain.usecase.ObserveStorePageUseCase
import kotlinx.coroutines.flow.MutableStateFlow

class StoreViewModel(
    storeId: String,
    observeStorePage: ObserveStorePageUseCase,
) : MviViewModel<StoreState, StoreIntent, StoreEffect>(StoreState(storeId)) {

    private val category = MutableStateFlow<String?>(null)
    private val sort = MutableStateFlow(SortOrder.RELEVANCE)

    init {
        observeStorePage(storeId, category, sort).observe { page ->
            setState { copy(isLoading = false, summary = page.summary, categories = page.categories, products = page.products) }
        }
    }

    override fun handleIntent(intent: StoreIntent) {
        when (intent) {
            is StoreIntent.SelectCategory -> {
                val next = intent.slug.takeIf { it != currentState.selectedCategory }
                category.value = next
                setState { copy(selectedCategory = next) }
            }
            is StoreIntent.ChangeSort -> {
                sort.value = intent.sort
                setState { copy(sort = intent.sort) }
            }
            is StoreIntent.OpenProduct -> sendEffect(StoreEffect.NavigateToProduct(intent.productId, STORE_ORIGIN))
            StoreIntent.Back -> sendEffect(StoreEffect.NavigateBack)
        }
    }
}
