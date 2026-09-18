package com.cikup.amazgone.catalog.presentation.categories

import com.cikup.amazgone.catalog.domain.model.Category
import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState

data class CategoriesState(val categories: List<Category> = emptyList()) : UiState

sealed interface CategoriesIntent : UiIntent {
    data class Open(val slug: String) : CategoriesIntent
    data object Back : CategoriesIntent
}

sealed interface CategoriesEffect : UiEffect {
    data class NavigateToCategory(val slug: String) : CategoriesEffect
    data object NavigateBack : CategoriesEffect
}
