package com.cikup.amazgone.games.domain.model

/**
 * Angles for a wheel whose segment 0 starts at the top pointer and segments run clockwise.
 * Rotating the wheel clockwise by R puts the segment containing angle (360 − R) under the pointer.
 */
object WheelGeometry {
    const val FULL_TURN = 360f

    fun segmentSweep(segments: Int): Float = FULL_TURN / segments

    /** Absolute rotation that lands the centre of [index] under the pointer after at least [extraTurns] turns. */
    fun targetRotation(current: Float, index: Int, segments: Int, extraTurns: Int): Float {
        val sweep = segmentSweep(segments)
        val segmentCentre = index * sweep + sweep / 2
        val base = current - (current % FULL_TURN) + extraTurns * FULL_TURN
        return base + (FULL_TURN - segmentCentre)
    }

    /** Which segment is under the pointer for a given rotation. */
    fun segmentAt(rotation: Float, segments: Int): Int {
        val normalized = ((FULL_TURN - (rotation % FULL_TURN)) % FULL_TURN + FULL_TURN) % FULL_TURN
        return (normalized / segmentSweep(segments)).toInt().coerceIn(0, segments - 1)
    }
}
