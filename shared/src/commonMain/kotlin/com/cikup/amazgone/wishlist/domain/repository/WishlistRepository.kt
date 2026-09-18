package com.cikup.amazgone.wishlist.domain.repository

import kotlinx.coroutines.flow.Flow

interface WishlistRepository {
    fun observeIds(): Flow<List<String>>
    suspend fun setSaved(productId: String, saved: Boolean)
    suspend fun isSaved(productId: String): Boolean
}
