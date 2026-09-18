package com.cikup.amazgone.settings.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.settings_appearance
import amazgone.shared.generated.resources.theme_dark
import amazgone.shared.generated.resources.theme_light
import amazgone.shared.generated.resources.theme_system
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.motion.pressScale
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.settings.domain.model.ThemeMode
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Appearance card wired to the stored preference. */
@Composable
fun ThemePickerRoute(modifier: Modifier = Modifier, viewModel: ThemeViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ThemePicker(state.mode, onSelect = { viewModel.onIntent(ThemeIntent.Select(it)) }, modifier)
}

private data class ThemeOption(val mode: ThemeMode, val label: StringResource, val icon: ImageVector)

private val OPTIONS = listOf(
    ThemeOption(ThemeMode.LIGHT, Res.string.theme_light, Icons.Outlined.LightMode),
    ThemeOption(ThemeMode.DARK, Res.string.theme_dark, Icons.Outlined.DarkMode),
    ThemeOption(ThemeMode.SYSTEM, Res.string.theme_system, Icons.Outlined.PhoneIphone),
)

@Composable
fun ThemePicker(selected: ThemeMode, onSelect: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.large, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(AmazgoneDimens.spaceLg), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
            Text(stringResource(Res.string.settings_appearance), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                OPTIONS.forEach { option ->
                    ThemeTile(option, option.mode == selected, { onSelect(option.mode) }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ThemeTile(option: ThemeOption, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val cta = AmazgoneTheme.extended.cta
    val border by animateColorAsState(if (selected) cta else MaterialTheme.colorScheme.outlineVariant, label = "border")
    val width by animateDpAsState(if (selected) AmazgoneDimens.spaceXs / 2 else AmazgoneDimens.spaceXs / 4, MotionTokens.bouncy(), label = "width")
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(width, border),
        modifier = modifier
            .pressScale(interaction)
            .selectable(selected = selected, interactionSource = interaction, indication = null, role = Role.RadioButton, onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            }),
    ) {
        Column(Modifier.padding(AmazgoneDimens.spaceSm), horizontalAlignment = Alignment.CenterHorizontally) {
            MiniPreview(option.mode, Modifier.fillMaxWidth().aspectRatio(PREVIEW_RATIO))
            Row(Modifier.padding(top = AmazgoneDimens.spaceSm), verticalAlignment = Alignment.CenterVertically) {
                Icon(option.icon, contentDescription = null, tint = if (selected) cta else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(AmazgoneDimens.iconSm))
                Text(
                    stringResource(option.label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) cta else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = AmazgoneDimens.spaceXs),
                )
            }
        }
    }
}

/** A tiny sketch of a screen in that theme; System shows half light, half dark. */
@Composable
private fun MiniPreview(mode: ThemeMode, modifier: Modifier) {
    val cta = AmazgoneTheme.extended.cta
    val light = PreviewPalette(Color.White, LIGHT_LINE)
    val dark = PreviewPalette(DARK_BG, DARK_LINE)
    Canvas(modifier) {
        val radius = CornerRadius(size.minDimension * CORNER)
        fun sketch(p: PreviewPalette, left: Float, width: Float) {
            drawRect(p.background, Offset(left, 0f), Size(width, size.height))
            val pad = size.width * PAD
            listOf(LINE_TOP to LINE_WIDE, LINE_MID to LINE_NARROW).forEach { (y, w) ->
                drawRoundRect(p.line, Offset(pad, size.height * y), Size((size.width - pad * 2) * w, size.height * LINE_H), radius)
            }
            drawRoundRect(cta, Offset(pad, size.height * BUTTON_Y), Size(size.width - pad * 2, size.height * BUTTON_H), radius)
        }
        when (mode) {
            ThemeMode.LIGHT -> sketch(light, 0f, size.width)
            ThemeMode.DARK -> sketch(dark, 0f, size.width)
            ThemeMode.SYSTEM -> {
                sketch(light, 0f, size.width)
                clipRect(left = size.width / 2) { sketch(dark, size.width / 2, size.width / 2) }
            }
        }
    }
}

private data class PreviewPalette(val background: Color, val line: Color)

// Preview-only sketch colours: they must look light/dark regardless of the current theme.
private val DARK_BG = Color(0xFF1E1B3A)
private val DARK_LINE = Color(0xFF3F3B63)
private val LIGHT_LINE = Color(0xFFE3E3EA)
private const val PREVIEW_RATIO = 1.3f
private const val CORNER = 0.05f
private const val PAD = 0.12f
private const val LINE_TOP = 0.16f
private const val LINE_MID = 0.34f
private const val LINE_H = 0.1f
private const val LINE_WIDE = 1f
private const val LINE_NARROW = 0.6f
private const val BUTTON_Y = 0.66f
private const val BUTTON_H = 0.18f
