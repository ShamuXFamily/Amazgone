package com.cikup.amazgone.reviews.domain.repository

import com.cikup.amazgone.reviews.domain.model.ProductReview
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    /** Shopper reviews for a product, the current user's first. */
    fun observeForProduct(productId: String): Flow<List<ProductReview>>

    /** The current user's reviews keyed by product id. */
    fun observeMine(): Flow<Map<String, ProductReview>>

    /** Saves locally (pending) and queues the upload; editing replaces the previous review. */
    suspend fun saveMine(productId: String, orderId: String, rating: Int, comment: String)

    /** Pulls other shoppers' reviews for a product into the local cache (no-op offline). */
    suspend fun refresh(productId: String)
}
