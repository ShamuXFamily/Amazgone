package com.cikup.amazgone.reviews

import com.cikup.amazgone.reviews.domain.model.ReviewProblem
import com.cikup.amazgone.reviews.domain.model.ReviewRules
import kotlin.test.Test
import kotlin.test.assertEquals

class ReviewRulesTest {
    @Test
    fun needsAStarRatingAndAShortComment() {
        assertEquals(setOf(ReviewProblem.NO_RATING, ReviewProblem.COMMENT_TOO_SHORT), ReviewRules.problems(0, " "))
        assertEquals(emptySet(), ReviewRules.problems(5, "Great charger"))
        assertEquals(setOf(ReviewProblem.COMMENT_TOO_LONG), ReviewRules.problems(4, "x".repeat(ReviewRules.MAX_COMMENT + 1)))
        assertEquals(setOf(ReviewProblem.NO_RATING), ReviewRules.problems(6, "Great charger"))
    }
}
