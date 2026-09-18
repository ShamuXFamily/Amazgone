package com.cikup.amazgone.core.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.cikup.amazgone.core.designsystem.motion.pressScale
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens

/** Pastel icon tile + caption (quick actions row). Springs down while pressed. */
@Composable
fun QuickActionTile(icon: ImageVector, label: String, tint: Color, iconColor: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier.width(AmazgoneDimens.iconXl * 0.8f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs),
    ) {
        Surface(
            onClick = onClick,
            interactionSource = interaction,
            color = tint,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.pressScale(interaction),
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.padding(AmazgoneDimens.spaceMd).size(AmazgoneDimens.iconMd + AmazgoneDimens.spaceXs))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2)
    }
}
