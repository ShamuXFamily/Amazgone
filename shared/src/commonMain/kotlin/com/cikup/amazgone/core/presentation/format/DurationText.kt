package com.cikup.amazgone.core.presentation.format

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.duration_days
import amazgone.shared.generated.resources.duration_hours
import amazgone.shared.generated.resources.duration_minutes
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE
private const val DAY = 24 * HOUR

/** "23 days", "5 h", "12 min" — coarse on purpose, like a delivery estimate. */
@Composable
fun durationText(millis: Long): String {
    val safe = millis.coerceAtLeast(MINUTE)
    return when {
        safe >= DAY -> ((safe + DAY / 2) / DAY).toInt().let { pluralStringResource(Res.plurals.duration_days, it, it) }
        safe >= HOUR -> stringResource(Res.string.duration_hours, ((safe + HOUR / 2) / HOUR).toInt())
        else -> stringResource(Res.string.duration_minutes, (safe / MINUTE).toInt())
    }
}
