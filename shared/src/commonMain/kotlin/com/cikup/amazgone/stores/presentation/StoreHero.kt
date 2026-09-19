package com.cikup.amazgone.stores.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.store_products
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.stores.domain.model.StoreKind
import com.cikup.amazgone.stores.domain.model.StoreSummary
import org.jetbrains.compose.resources.stringResource

/** Gradient banner with a giant faint monogram; the logo pops in and the info card overlaps the banner. */
@Composable
fun StoreHero(summary: StoreSummary, modifier: Modifier = Modifier) {
    val store = summary.store
    val (top, bottom) = bannerColors(store.kind, store.id)
    Box(modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth().height(BANNER_HEIGHT)
                .background(Brush.linearGradient(listOf(top, bottom)), RoundedCornerShape(bottomStart = AmazgoneDimens.spaceXl, bottomEnd = AmazgoneDimens.spaceXl)),
        ) {
            Text(
                monogram(store.name),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize * WATERMARK_SCALE),
                fontWeight = FontWeight.Black,
                color = AmazgoneTheme.extended.onBrandNavy.copy(alpha = WATERMARK_ALPHA),
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = AmazgoneDimens.spaceLg),
            )
        }
        InfoCard(summary, Modifier.padding(start = AmazgoneDimens.spaceLg, end = AmazgoneDimens.spaceLg, top = BANNER_HEIGHT - CARD_OVERLAP))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoCard(summary: StoreSummary, modifier: Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val pop = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduceMotion) pop.animateTo(1f, MotionTokens.bouncy()) }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.large, shadowElevation = AmazgoneDimens.spaceXs, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(AmazgoneDimens.spaceLg), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
                StoreLogo(summary.store, LOGO_SIZE, Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }, ring = true)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(summary.store.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        if (summary.store.isVerified) VerifiedIcon(AmazgoneDimens.iconMd * 0.8f, Modifier.padding(start = AmazgoneDimens.spaceXs))
                    }
                    StoreKindChip(summary.store)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                Stat(Icons.Rounded.Inventory2, stringResource(Res.string.store_products, summary.productCount))
                summary.averageRating?.let { Stat(Icons.Rounded.Star, Formatters.rating(it), MaterialTheme.colorScheme.tertiary) }
                summary.topCategory?.let { Stat(Icons.Rounded.Category, Formatters.categoryLabel(it)) }
            }
        }
    }
}

@Composable
private fun Stat(icon: ImageVector, text: String, tint: Color = AmazgoneTheme.extended.cta) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(AmazgoneDimens.spaceSm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(AmazgoneDimens.iconSm))
            Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun bannerColors(kind: StoreKind, id: String): Pair<Color, Color> {
    val ext = AmazgoneTheme.extended
    val index = id.sumOf { it.code } % ext.tileIcons.size
    return when (kind) {
        StoreKind.AMAZGONE -> ext.cta to lerp(ext.cta, ext.brandNavy, ACCENT_MIX)
        else -> ext.tileIcons[index] to lerp(ext.tileIcons[index], ext.brandNavy, ACCENT_MIX)
    }
}

private val BANNER_HEIGHT = AmazgoneDimens.iconXl * 2.3f
private val CARD_OVERLAP = AmazgoneDimens.iconXl * 0.45f
private val LOGO_SIZE = AmazgoneDimens.iconLg * 1.35f
private const val WATERMARK_ALPHA = 0.10f
private const val WATERMARK_SCALE = 2.2f
private const val ACCENT_MIX = 0.35f
