package com.cikup.amazgone.orders.presentation.detail

import com.cikup.amazgone.core.presentation.mvi.UiEffect
import com.cikup.amazgone.core.presentation.mvi.UiIntent
import com.cikup.amazgone.core.presentation.mvi.UiState
import com.cikup.amazgone.orders.domain.model.Order
import com.cikup.amazgone.orders.domain.model.OrderItem
import com.cikup.amazgone.orders.domain.model.OrderStage
import com.cikup.amazgone.orders.domain.model.canConfirmReceived
import com.cikup.amazgone.orders.domain.model.isItemDelivered
import com.cikup.amazgone.orders.domain.model.stageAt
import com.cikup.amazgone.reviews.domain.model.ProductReview
import com.cikup.amazgone.reviews.domain.model.ReviewProblem

/** The review being written in the bottom sheet (null = sheet closed). */
data class ReviewDraft(
    val item: OrderItem,
    val rating: Int = 0,
    val comment: String = "",
    val problems: Set<ReviewProblem> = emptySet(),
    val isEditing: Boolean = false,
    val isSubmitting: Boolean = false,
)

data class OrderDetailState(
    val order: Order? = null,
    val isLoading: Boolean = true,
    /** Clock for the tracker; refreshed every minute. */
    val now: Long = 0,
    val myReviews: Map<String, ProductReview> = emptyMap(),
    val reviewDraft: ReviewDraft? = null,
) : UiState {
    val stage: OrderStage? get() = order?.takeIf { now > 0 }?.stageAt(now)
    val canConfirmReceived: Boolean get() = now > 0 && order?.canConfirmReceived(now) == true
    fun canReview(item: OrderItem): Boolean = now > 0 && order?.isItemDelivered(item, now) == true
}

sealed interface OrderDetailIntent : UiIntent {
    data object Back : OrderDetailIntent
    data class OpenProduct(val productId: String) : OrderDetailIntent
    data object ConfirmReceived : OrderDetailIntent
    data class BuyAgain(val productId: String) : OrderDetailIntent
    data class WriteReview(val productId: String) : OrderDetailIntent
    data class SetRating(val rating: Int) : OrderDetailIntent
    data class SetComment(val comment: String) : OrderDetailIntent
    data object SubmitReview : OrderDetailIntent
    data object DismissReview : OrderDetailIntent
}

sealed interface OrderDetailEffect : UiEffect {
    data object NavigateBack : OrderDetailEffect
    data class NavigateToProduct(val productId: String) : OrderDetailEffect
    data object ReceivedConfirmed : OrderDetailEffect
    data object ReviewPosted : OrderDetailEffect
    data object AddedToCart : OrderDetailEffect
}
