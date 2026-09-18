package com.cikup.amazgone.catalog.presentation.categories

import com.cikup.amazgone.catalog.domain.usecase.ObserveCategoriesUseCase
import com.cikup.amazgone.core.presentation.mvi.MviViewModel

class CategoriesViewModel(observeCategories: ObserveCategoriesUseCase) :
    MviViewModel<CategoriesState, CategoriesIntent, CategoriesEffect>(CategoriesState()) {
    init {
        observeCategories().observe { setState { copy(categories = it.sortedBy { c -> c.slug }) } }
    }

    override fun handleIntent(intent: CategoriesIntent) = when (intent) {
        is CategoriesIntent.Open -> sendEffect(CategoriesEffect.NavigateToCategory(intent.slug))
        CategoriesIntent.Back -> sendEffect(CategoriesEffect.NavigateBack)
    }
}
