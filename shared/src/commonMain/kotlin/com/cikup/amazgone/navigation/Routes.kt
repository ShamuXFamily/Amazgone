package com.cikup.amazgone.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation destinations. Arguments live as constructor properties. */
sealed interface Route {
    @Serializable data object Home : Route
    @Serializable data object Games : Route
    @Serializable data object Cart : Route
    @Serializable data object Account : Route
    /** [category] pre-selects a department filter (from the categories screen). */
    @Serializable data class Search(val category: String? = null) : Route
    /** [origin] namespaces shared-element keys so the image morphs from the card that was tapped. */
    @Serializable data class ProductDetail(val productId: String, val origin: String) : Route
    @Serializable data object Checkout : Route
    @Serializable data object Orders : Route
    @Serializable data class OrderDetail(val orderId: String) : Route
    @Serializable data object Wishlist : Route
    @Serializable data object Wallet : Route
    @Serializable data object Leaderboard : Route
    @Serializable data object Achievements : Route
    @Serializable data object SpinWheel : Route
    @Serializable data object ScratchCard : Route
    @Serializable data object LightningDeal : Route
    @Serializable data object FlashSale : Route
    @Serializable data object Categories : Route
}
