package com.cikup.amazgone.catalog.presentation.detail

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.detail_highlights
import amazgone.shared.generated.resources.detail_not_affiliated
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cikup.amazgone.catalog.domain.model.CatalogSourceId
import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import org.jetbrains.compose.resources.stringResource

/** Amazon-style "About this item" bullets; each line cascades in. */
@Composable
fun Highlights(product: Product, modifier: Modifier = Modifier) {
    val lines = product.details.highlights
    if (lines.isEmpty()) return
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.padding(horizontal = AmazgoneDimens.spaceLg).fillMaxWidth(),
    ) {
        Column(Modifier.padding(AmazgoneDimens.spaceLg), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            Text(stringResource(Res.string.detail_highlights), style = MaterialTheme.typography.titleMedium)
            lines.forEachIndexed { index, line ->
                Row(Modifier.staggeredEnter(index), horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
                    Box(
                        Modifier.padding(top = AmazgoneDimens.spaceSm)
                            .size(AmazgoneDimens.spaceXs * 1.5f)
                            .background(AmazgoneTheme.extended.cta, CircleShape),
                    )
                    Text(line, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** Licence attribution for the photos (e.g. CC BY-SA) plus a no-affiliation note for curated items. */
@Composable
fun ImageCredit(product: Product, modifier: Modifier = Modifier) {
    val credit = product.details.imageCredit ?: return
    Column(modifier.padding(horizontal = AmazgoneDimens.spaceLg), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
            Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(AmazgoneDimens.iconSm))
            Text(credit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (product.source == CatalogSourceId.AMAZGONE) {
            Text(stringResource(Res.string.detail_not_affiliated), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
