package com.cikup.amazgone.reviews.domain.model

/** A shopper's review of a product they received (distinct from the catalog's built-in reviews). */
data class ProductReview(
    val productId: String,
    val authorName: String,
    val rating: Int,
    val comment: String,
    val createdAt: Long,
    val orderId: String,
    /** Written on this account. */
    val isMine: Boolean,
    /** Saved locally, not yet on the server. */
    val isPending: Boolean,
)

enum class ReviewProblem { NO_RATING, COMMENT_TOO_SHORT, COMMENT_TOO_LONG }

/** Mirrors firestore.rules (rating 1–5, comment ≤ MAX_COMMENT chars). */
object ReviewRules {
    const val MIN_COMMENT = 3
    const val MAX_COMMENT = 1_000

    fun problems(rating: Int, comment: String): Set<ReviewProblem> = buildSet {
        if (rating !in 1..5) add(ReviewProblem.NO_RATING)
        val length = comment.trim().length
        if (length < MIN_COMMENT) add(ReviewProblem.COMMENT_TOO_SHORT)
        if (length > MAX_COMMENT) add(ReviewProblem.COMMENT_TOO_LONG)
    }
}
