package com.cikup.amazgone.orders.presentation.detail

import com.cikup.amazgone.core.analytics.Analytics
import com.cikup.amazgone.cart.domain.usecase.AddToCartUseCase
import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.core.presentation.mvi.MviViewModel
import com.cikup.amazgone.games.domain.usecase.TickerUseCase
import com.cikup.amazgone.orders.domain.usecase.ConfirmReceivedUseCase
import com.cikup.amazgone.orders.domain.usecase.ObserveOrderUseCase
import com.cikup.amazgone.reviews.domain.model.ReviewRules
import com.cikup.amazgone.reviews.domain.usecase.ObserveMyReviewsUseCase
import com.cikup.amazgone.reviews.domain.usecase.SubmitReviewUseCase

class OrderDetailViewModel(
    private val orderId: String,
    observeOrder: ObserveOrderUseCase,
    observeMyReviews: ObserveMyReviewsUseCase,
    ticker: TickerUseCase,
    private val confirmReceived: ConfirmReceivedUseCase,
    private val submitReview: SubmitReviewUseCase,
    private val addToCart: AddToCartUseCase,
    private val analytics: Analytics,
) : MviViewModel<OrderDetailState, OrderDetailIntent, OrderDetailEffect>(OrderDetailState()) {

    init {
        observeOrder(orderId).observe { setState { copy(order = it, isLoading = false) } }
        observeMyReviews().observe { setState { copy(myReviews = it) } }
        ticker(TICK_MS).observe { setState { copy(now = it) } }
    }

    override fun handleIntent(intent: OrderDetailIntent) {
        when (intent) {
            OrderDetailIntent.Back -> sendEffect(OrderDetailEffect.NavigateBack)
            is OrderDetailIntent.OpenProduct -> sendEffect(OrderDetailEffect.NavigateToProduct(intent.productId))
            OrderDetailIntent.ConfirmReceived -> launchSafely {
                if (confirmReceived(orderId)) {
                    analytics.orderReceived(orderId)
                    sendEffect(OrderDetailEffect.ReceivedConfirmed)
                }
            }
            is OrderDetailIntent.BuyAgain -> launchSafely {
                if (addToCart(intent.productId) is DomainResult.Success) sendEffect(OrderDetailEffect.AddedToCart)
            }
            is OrderDetailIntent.WriteReview -> openReview(intent.productId)
            is OrderDetailIntent.SetRating -> updateDraft { copy(rating = intent.rating, problems = problems - REVIEW_RATING_PROBLEMS) }
            is OrderDetailIntent.SetComment -> updateDraft { copy(comment = intent.comment.take(ReviewRules.MAX_COMMENT), problems = emptySet()) }
            OrderDetailIntent.SubmitReview -> submit()
            OrderDetailIntent.DismissReview -> setState { copy(reviewDraft = null) }
        }
    }

    /** Opens the sheet for an item of a delivered order, pre-filled when editing an earlier review. */
    private fun openReview(productId: String) {
        val item = currentState.order?.items?.firstOrNull { it.productId == productId } ?: return
        if (!currentState.canReview(item)) return
        val existing = currentState.myReviews[productId]
        setState {
            copy(reviewDraft = ReviewDraft(item, existing?.rating ?: 0, existing?.comment.orEmpty(), isEditing = existing != null))
        }
    }

    private fun submit() {
        val draft = currentState.reviewDraft ?: return
        if (draft.isSubmitting) return
        val problems = ReviewRules.problems(draft.rating, draft.comment)
        if (problems.isNotEmpty()) return updateDraft { copy(problems = problems) }
        updateDraft { copy(isSubmitting = true) }
        launchSafely {
            when (submitReview(orderId, draft.item.productId, draft.rating, draft.comment)) {
                is DomainResult.Success -> {
                    analytics.reviewPosted(draft.item.productId, draft.rating, draft.isEditing)
                    setState { copy(reviewDraft = null) }
                    sendEffect(OrderDetailEffect.ReviewPosted)
                }
                is DomainResult.Failure -> updateDraft { copy(isSubmitting = false) }
            }
        }
    }

    private fun updateDraft(change: ReviewDraft.() -> ReviewDraft) = setState { copy(reviewDraft = reviewDraft?.change()) }

    override fun onError(throwable: Throwable) = updateDraft { copy(isSubmitting = false) }

    private companion object {
        const val TICK_MS = 60_000L
        val REVIEW_RATING_PROBLEMS = setOf(com.cikup.amazgone.reviews.domain.model.ReviewProblem.NO_RATING)
    }
}
