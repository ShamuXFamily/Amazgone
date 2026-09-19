package com.cikup.amazgone.core.designsystem.component

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.app_name
import amazgone.shared.generated.resources.brand_logo_dark
import amazgone.shared.generated.resources.brand_logo_light
import amazgone.shared.generated.resources.brand_wordmark_dark
import amazgone.shared.generated.resources.brand_wordmark_light
import amazgone.shared.generated.resources.brand_tagline
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val WORDMARK_HEIGHT = 30.dp
private val LOGO_HEIGHT = 52.dp
private const val DARK_BACKGROUND_LUMINANCE = 0.5f

/** "amazgone" with the smile arrow, for compact headers. Reveals left to right, like the smile being drawn. */
@Composable
fun BrandWordmark(modifier: Modifier = Modifier) {
    BrandImage(
        light = Res.drawable.brand_wordmark_light,
        dark = Res.drawable.brand_wordmark_dark,
        description = stringResource(Res.string.app_name),
        modifier = modifier.height(WORDMARK_HEIGHT),
    )
}

/** The full logo with its "Your order is somewhere…" tagline. */
@Composable
fun BrandLogo(modifier: Modifier = Modifier) {
    BrandImage(
        light = Res.drawable.brand_logo_light,
        dark = Res.drawable.brand_logo_dark,
        description = "${stringResource(Res.string.app_name)} · ${stringResource(Res.string.brand_tagline)}",
        modifier = modifier.height(LOGO_HEIGHT),
    )
}

/**
 * Picks the artwork by the background actually on screen (the in-app theme can differ from the
 * system's), so the black "amaz" never disappears on a dark surface.
 */
@Composable
private fun BrandImage(light: DrawableResource, dark: DrawableResource, description: String, modifier: Modifier) {
    val onDark = MaterialTheme.colorScheme.background.luminance() < DARK_BACKGROUND_LUMINANCE
    val reduceMotion = LocalReduceMotion.current
    val reveal = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduceMotion) reveal.animateTo(1f, tween(MotionTokens.DURATION_EXTRA_LONG_MS, easing = MotionTokens.EmphasizedEasing))
    }
    Image(
        painter = painterResource(if (onDark) dark else light),
        contentDescription = description,
        contentScale = ContentScale.FillHeight,
        modifier = modifier.drawWithContent {
            clipRect(right = size.width * reveal.value) { this@drawWithContent.drawContent() }
        },
    )
}
