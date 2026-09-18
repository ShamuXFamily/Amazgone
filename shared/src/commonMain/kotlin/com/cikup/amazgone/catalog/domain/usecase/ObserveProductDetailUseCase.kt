package com.cikup.amazgone.catalog.domain.usecase

import com.cikup.amazgone.catalog.domain.model.ProductWithReviews
import com.cikup.amazgone.catalog.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow

class ObserveProductDetailUseCase(private val repository: CatalogRepository) {
    operator fun invoke(productId: String): Flow<ProductWithReviews?> = repository.observeProduct(productId)
}
