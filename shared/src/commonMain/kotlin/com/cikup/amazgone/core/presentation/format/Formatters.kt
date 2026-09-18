package com.cikup.amazgone.core.presentation.format

import kotlin.math.abs
import kotlin.math.roundToInt

/** Locale-independent display formatting shared by every screen. */
object Formatters {
    private const val GROUP = 3
    private const val THOUSAND = 1_000L
    private const val TENTHS = 10

    /** 1234567 → "1,234,567". */
    fun coins(amount: Long): String {
        val digits = abs(amount).toString()
        val grouped = digits.reversed().chunked(GROUP).joinToString(",").reversed()
        return if (amount < 0) "-$grouped" else grouped
    }

    /** 950 → "950", 12_345 → "12.3K" (for compact badges). */
    fun compactCount(value: Long): String = when {
        value < THOUSAND -> value.toString()
        else -> {
            val tenths = (value * TENTHS / THOUSAND.toDouble()).roundToInt()
            val whole = tenths / TENTHS
            val fraction = tenths % TENTHS
            if (fraction == 0) "${whole}K" else "$whole.${fraction}K"
        }
    }

    /** 4.56 → "4.6". */
    fun rating(value: Double): String {
        val tenths = (value * TENTHS).roundToInt()
        return "${tenths / TENTHS}.${tenths % TENTHS}"
    }

    /** "home-decoration" → "Home Decoration". */
    fun categoryLabel(slug: String): String =
        slug.split('-', '_', ' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercaseChar() } }
}
