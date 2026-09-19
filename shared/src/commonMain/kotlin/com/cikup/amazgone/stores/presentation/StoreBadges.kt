package com.cikup.amazgone.stores.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.store_kind_amazgone
import amazgone.shared.generated.resources.store_kind_brand
import amazgone.shared.generated.resources.store_kind_digital
import amazgone.shared.generated.resources.store_kind_official
import amazgone.shared.generated.resources.store_verified
import amazgone.shared.generated.resources.store_visit
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.stores.domain.model.Store
import com.cikup.amazgone.stores.domain.model.StoreKind
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Monogram "logo" (real logos are trademarks): a solid colour per official store, brand orange for
 * Amazgone, a stable pastel per name for everyone else.
 */
@Composable
fun StoreLogo(store: Store, size: Dp, modifier: Modifier = Modifier, ring: Boolean = false) {
    val ext = AmazgoneTheme.extended
    val index = store.id.sumOf { it.code } % ext.tileTints.size
    val (background, content) = when (store.kind) {
        StoreKind.OFFICIAL -> ext.tileIcons[index] to ext.tileTints[index]
        StoreKind.AMAZGONE -> ext.cta to ext.onCta
        else -> ext.tileTints[index] to ext.tileIcons[index]
    }
    Surface(
        color = background,
        contentColor = content,
        shape = CircleShape,
        border = if (ring) BorderStroke(size / RING_FRACTION, MaterialTheme.colorScheme.surfaceContainerLowest) else null,
        modifier = modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(monogram(store.name), fontWeight = FontWeight.Bold, fontSize = fontSizeFor(size))
        }
    }
}

@Composable
private fun fontSizeFor(size: Dp): TextUnit = MaterialTheme.typography.titleMedium.fontSize * (size / AmazgoneDimens.iconLg)

/** "Apple Official" → "A", "Calvin Klein Official" → "CK", "Green Man Gaming" → "GM". */
internal fun monogram(name: String): String {
    val words = name.removeSuffix(" Official").split(' ', '-', '&').filter { it.isNotBlank() }
    return words.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
}

/** Blue check for sellers the shop vouches for (official brands and Amazgone itself). */
@Composable
fun VerifiedIcon(size: Dp = AmazgoneDimens.iconSm, modifier: Modifier = Modifier) {
    Icon(Icons.Rounded.Verified, stringResource(Res.string.store_verified), tint = AmazgoneTheme.extended.verified, modifier = modifier.size(size))
}

fun storeKindLabel(kind: StoreKind): StringResource = when (kind) {
    StoreKind.OFFICIAL -> Res.string.store_kind_official
    StoreKind.BRAND -> Res.string.store_kind_brand
    StoreKind.DIGITAL -> Res.string.store_kind_digital
    StoreKind.AMAZGONE -> Res.string.store_kind_amazgone
}

/** Small pill: "✓ Official store", "Digital game store", … */
@Composable
fun StoreKindChip(store: Store, modifier: Modifier = Modifier) {
    val tint = if (store.isVerified) AmazgoneTheme.extended.verified else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color = tint.copy(alpha = CHIP_ALPHA),
        contentColor = tint,
        shape = CircleShape,
        modifier = modifier,
    ) {
        Row(Modifier.padding(horizontal = AmazgoneDimens.spaceSm, vertical = AmazgoneDimens.spaceXs / 2), verticalAlignment = Alignment.CenterVertically) {
            if (store.isVerified) VerifiedIcon(AmazgoneDimens.iconSm * 0.85f, Modifier.padding(end = AmazgoneDimens.spaceXs))
            Text(stringResource(storeKindLabel(store.kind)), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

private const val RING_FRACTION = 16
private const val CHIP_ALPHA = 0.12f

/** Amazon's "Visit the Apple Store" link above a product title. */
@Composable
fun StoreLink(store: Store, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(MaterialTheme.shapes.small).clickable(onClick = onClick).padding(vertical = AmazgoneDimens.spaceXs / 2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs),
    ) {
        StoreLogo(store, AmazgoneDimens.iconMd)
        Text(
            stringResource(Res.string.store_visit, store.name),
            style = MaterialTheme.typography.labelLarge,
            color = AmazgoneTheme.extended.verified,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (store.isVerified) VerifiedIcon()
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = AmazgoneTheme.extended.verified, modifier = Modifier.size(AmazgoneDimens.iconSm))
    }
}
