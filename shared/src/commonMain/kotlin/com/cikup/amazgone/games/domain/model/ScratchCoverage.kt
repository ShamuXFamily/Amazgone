package com.cikup.amazgone.games.domain.model

/**
 * Tracks how much of a scratch card has been uncovered on a coarse grid (pure, testable).
 * Points are normalized (0..1); each touch clears cells within [brushRadius].
 */
class ScratchCoverage(private val columns: Int = 16, private val rows: Int = 10, private val brushRadius: Float = 0.07f) {
    private val cleared = BooleanArray(columns * rows)

    val fraction: Float get() = cleared.count { it }.toFloat() / cleared.size

    fun scratch(x: Float, y: Float) {
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                val cx = (col + HALF) / columns
                val cy = (row + HALF) / rows
                val dx = cx - x
                val dy = cy - y
                if (dx * dx + dy * dy <= brushRadius * brushRadius) cleared[row * columns + col] = true
            }
        }
    }

    companion object {
        const val REVEAL_THRESHOLD = 0.6f
        private const val HALF = 0.5f
    }
}
