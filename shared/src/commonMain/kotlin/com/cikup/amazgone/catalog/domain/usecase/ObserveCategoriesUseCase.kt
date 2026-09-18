package com.cikup.amazgone.catalog.domain.usecase

import com.cikup.amazgone.catalog.domain.model.Category
import com.cikup.amazgone.catalog.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow

class ObserveCategoriesUseCase(private val repository: CatalogRepository) {
    operator fun invoke(): Flow<List<Category>> = repository.observeCategories()
}
