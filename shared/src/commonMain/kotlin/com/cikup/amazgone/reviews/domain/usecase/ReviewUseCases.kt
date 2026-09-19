package com.cikup.amazgone.reviews.domain.usecase

import com.cikup.amazgone.core.common.TimeProvider
import com.cikup.amazgone.core.domain.DomainError
import com.cikup.amazgone.core.domain.DomainResult
import com.cikup.amazgone.core.domain.ValidationReason
import com.cikup.amazgone.orders.domain.model.isItemDelivered
import com.cikup.amazgone.orders.domain.repository.OrderRepository
import com.cikup.amazgone.reviews.domain.model.ProductReview
import com.cikup.amazgone.reviews.domain.model.ReviewProblem
import com.cikup.amazgone.reviews.domain.model.ReviewRules
import com.cikup.amazgone.reviews.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ObserveProductReviewsUseCase(private val reviews: ReviewRepository) {
    operator fun invoke(productId: String): Flow<List<ProductReview>> = reviews.observeForProduct(productId)
}

class ObserveMyReviewsUseCase(private val reviews: ReviewRepository) {
    operator fun invoke(): Flow<Map<String, ProductReview>> = reviews.observeMine()
}

class RefreshProductReviewsUseCase(private val reviews: ReviewRepository) {
    suspend operator fun invoke(productId: String) = reviews.refresh(productId)
}

/** Only for items of a delivered order you placed ("Verified purchase"). */
class SubmitReviewUseCase(
    private val reviews: ReviewRepository,
    private val orders: OrderRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(orderId: String, productId: String, rating: Int, comment: String): DomainResult<Unit> {
        ReviewRules.problems(rating, comment).firstOrNull()?.let { problem ->
            val reason = when (problem) {
                ReviewProblem.NO_RATING -> ValidationReason.EMPTY
                ReviewProblem.COMMENT_TOO_SHORT -> ValidationReason.TOO_SHORT
                ReviewProblem.COMMENT_TOO_LONG -> ValidationReason.TOO_LONG
            }
            return DomainResult.Failure(DomainError.Validation(problem.name, reason))
        }
        val order = orders.observeOrder(orderId).first() ?: return DomainResult.Failure(DomainError.NotFound)
        val item = order.items.firstOrNull { it.productId == productId } ?: return DomainResult.Failure(DomainError.NotFound)
        if (!order.isItemDelivered(item, time.nowMillis())) return DomainResult.Failure(DomainError.NotFound)
        reviews.saveMine(productId, orderId, rating, comment.trim())
        return DomainResult.Success(Unit)
    }
}
