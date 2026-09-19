package com.cikup.amazgone.core.analytics

import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.core.common.AppLogger
import kotlinx.coroutines.CancellationException

/**
 * Where events go. Implemented natively: Firebase Analytics on Android (androidMain) and iOS (Swift,
 * passed in at startup). Param values are String, Long or Double only.
 */
interface AnalyticsSink {
    fun logEvent(name: String, params: Map<String, Any>)
    fun setUserId(id: String?)
    fun setUserProperty(name: String, value: String?)
}

/** Used when Firebase isn't configured (and in tests): events go nowhere. */
object NoopAnalyticsSink : AnalyticsSink {
    override fun logEvent(name: String, params: Map<String, Any>) = Unit
    override fun setUserId(id: String?) = Unit
    override fun setUserProperty(name: String, value: String?) = Unit
}

/**
 * Typed events, named after Google Analytics 4 recommended events so Firebase's ecommerce and games
 * reports work out of the box. Prices are in coins and sent without a currency on purpose: this is
 * virtual money, so Firebase must not count it as real revenue. Analytics never breaks the app.
 */
class Analytics(private val sink: AnalyticsSink, private val logger: AppLogger) {

    fun screen(name: String) = log(Events.SCREEN_VIEW, Params.SCREEN_NAME to name, Params.SCREEN_CLASS to name)

    fun viewItem(product: Product) = log(Events.VIEW_ITEM, *product.params())

    fun addToCart(product: Product, quantity: Int) = log(Events.ADD_TO_CART, *product.params(), Params.QUANTITY to quantity.toLong())

    fun removeFromCart(product: Product, quantity: Int) = log(Events.REMOVE_FROM_CART, *product.params(), Params.QUANTITY to quantity.toLong())

    fun addToWishlist(productId: String) = log(Events.ADD_TO_WISHLIST, Params.ITEM_ID to productId)

    fun search(term: String) {
        if (term.isNotBlank()) log(Events.SEARCH, Params.SEARCH_TERM to term.trim().take(MAX_TEXT))
    }

    fun viewStore(storeId: String) = log(Events.SELECT_CONTENT, Params.CONTENT_TYPE to "store", Params.ITEM_ID to storeId)

    fun beginCheckout(totalCoins: Long, itemCount: Int) =
        log(Events.BEGIN_CHECKOUT, Params.VALUE to totalCoins, Params.ITEMS_COUNT to itemCount.toLong())

    fun addShippingInfo(tier: String) = log(Events.ADD_SHIPPING_INFO, Params.SHIPPING_TIER to tier)

    fun purchase(orderId: String, totalCoins: Long, itemCount: Int, shippingTier: String, coupon: String?, shipments: Int) {
        log(
            Events.PURCHASE,
            Params.TRANSACTION_ID to orderId,
            Params.VALUE to totalCoins,
            Params.ITEMS_COUNT to itemCount.toLong(),
            Params.SHIPPING_TIER to shippingTier,
            Params.SHIPMENTS to shipments.toLong(),
            *listOfNotNull(coupon?.let { Params.COUPON to it }).toTypedArray(),
        )
        spendCoins(totalCoins, "order")
    }

    fun spendCoins(coins: Long, itemName: String) =
        log(Events.SPEND_VIRTUAL_CURRENCY, Params.VIRTUAL_CURRENCY_NAME to COINS, Params.VALUE to coins, Params.ITEM_NAME to itemName)

    fun earnCoins(coins: Long, source: String) {
        if (coins > 0) log(Events.EARN_VIRTUAL_CURRENCY, Params.VIRTUAL_CURRENCY_NAME to COINS, Params.VALUE to coins, Params.SOURCE to source)
    }

    fun orderReceived(orderId: String) = log(Events.ORDER_RECEIVED, Params.TRANSACTION_ID to orderId)

    fun reviewPosted(productId: String, rating: Int, edited: Boolean) =
        log(Events.REVIEW_POSTED, Params.ITEM_ID to productId, Params.RATING to rating.toLong(), Params.EDITED to edited.toString())

    fun login(method: String = METHOD) = log(Events.LOGIN, Params.METHOD to method)

    fun signUp(method: String = METHOD) = log(Events.SIGN_UP, Params.METHOD to method)

    fun levelUp(level: Int) = log(Events.LEVEL_UP, Params.LEVEL to level.toLong())

    fun unlockAchievement(id: String) = log(Events.UNLOCK_ACHIEVEMENT, Params.ACHIEVEMENT_ID to id)

    fun themeChanged(mode: String) {
        log(Events.THEME_CHANGED, Params.THEME to mode)
        userProperty(Properties.THEME, mode)
    }

    /** Firebase uid (never the username) so events from one account can be tied together. */
    fun identify(uid: String?) = safely { sink.setUserId(uid) }

    fun userProperty(name: String, value: String?) = safely { sink.setUserProperty(name, value) }

    private fun log(name: String, vararg params: Pair<String, Any>) = safely { sink.logEvent(name, params.toMap()) }

    private inline fun safely(block: () -> Unit) {
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            logger.error(TAG, "Analytics call failed", t)
        }
    }

    private fun Product.params(): Array<Pair<String, Any>> = listOfNotNull(
        Params.ITEM_ID to id,
        Params.ITEM_NAME to title.take(MAX_TEXT),
        Params.ITEM_CATEGORY to categorySlug,
        brand?.let { Params.ITEM_BRAND to it.take(MAX_TEXT) },
        Params.STORE to store.id,
        Params.PRICE to priceCoins,
    ).toTypedArray()

    /** GA4 names (recommended events where one exists, snake_case custom otherwise). */
    object Events {
        const val SCREEN_VIEW = "screen_view"
        const val VIEW_ITEM = "view_item"
        const val ADD_TO_CART = "add_to_cart"
        const val REMOVE_FROM_CART = "remove_from_cart"
        const val ADD_TO_WISHLIST = "add_to_wishlist"
        const val SEARCH = "search"
        const val SELECT_CONTENT = "select_content"
        const val BEGIN_CHECKOUT = "begin_checkout"
        const val ADD_SHIPPING_INFO = "add_shipping_info"
        const val PURCHASE = "purchase"
        const val SPEND_VIRTUAL_CURRENCY = "spend_virtual_currency"
        const val EARN_VIRTUAL_CURRENCY = "earn_virtual_currency"
        const val LOGIN = "login"
        const val SIGN_UP = "sign_up"
        const val LEVEL_UP = "level_up"
        const val UNLOCK_ACHIEVEMENT = "unlock_achievement"
        const val ORDER_RECEIVED = "order_received"
        const val REVIEW_POSTED = "review_posted"
        const val THEME_CHANGED = "theme_changed"
    }

    object Params {
        const val SCREEN_NAME = "screen_name"
        const val SCREEN_CLASS = "screen_class"
        const val ITEM_ID = "item_id"
        const val ITEM_NAME = "item_name"
        const val ITEM_CATEGORY = "item_category"
        const val ITEM_BRAND = "item_brand"
        const val STORE = "store_id"
        const val PRICE = "price_coins"
        const val QUANTITY = "quantity"
        const val SEARCH_TERM = "search_term"
        const val CONTENT_TYPE = "content_type"
        const val VALUE = "value"
        const val ITEMS_COUNT = "items_count"
        const val SHIPPING_TIER = "shipping_tier"
        const val SHIPMENTS = "shipments"
        const val TRANSACTION_ID = "transaction_id"
        const val COUPON = "coupon"
        const val VIRTUAL_CURRENCY_NAME = "virtual_currency_name"
        const val SOURCE = "source"
        const val RATING = "rating"
        const val EDITED = "edited"
        const val METHOD = "method"
        const val LEVEL = "level"
        const val ACHIEVEMENT_ID = "achievement_id"
        const val THEME = "theme"
    }

    object Properties {
        const val THEME = "theme"
    }

    private companion object {
        const val TAG = "Analytics"
        const val COINS = "coins"
        const val METHOD = "username"
        const val MAX_TEXT = 100 // GA4 param value limit
    }
}
