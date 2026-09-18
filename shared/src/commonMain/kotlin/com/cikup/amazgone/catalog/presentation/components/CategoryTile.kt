package com.cikup.amazgone.catalog.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme

private const val SELECTED_SCALE = 1.08f

/** Square icon tile used as a filter (flash sale, categories). Selection springs scale + tint. */
@Composable
fun CategoryTile(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cta = AmazgoneTheme.extended.cta
    val container by animateColorAsState(
        if (selected) cta.copy(alpha = SELECTED_TINT_ALPHA) else MaterialTheme.colorScheme.surfaceContainerLowest,
        label = "tileBg",
    )
    val iconTint by animateColorAsState(if (selected) cta else MaterialTheme.colorScheme.onSurfaceVariant, label = "tileIcon")
    val scale by animateFloatAsState(if (selected) SELECTED_SCALE else 1f, MotionTokens.bouncy(), label = "tileScale")
    Column(
        modifier.width(AmazgoneDimens.iconXl * 0.72f).graphicsLayer { scaleX = scale; scaleY = scale },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs),
    ) {
        Surface(
            onClick = onClick,
            color = container,
            shape = MaterialTheme.shapes.medium,
            border = if (selected) BorderStroke(AmazgoneDimens.spaceXs / 4, cta) else null,
            shadowElevation = if (selected) AmazgoneDimens.spaceXs else AmazgoneDimens.none,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.padding(AmazgoneDimens.spaceMd).size(AmazgoneDimens.iconMd))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (selected) cta else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val SELECTED_TINT_ALPHA = 0.12f
