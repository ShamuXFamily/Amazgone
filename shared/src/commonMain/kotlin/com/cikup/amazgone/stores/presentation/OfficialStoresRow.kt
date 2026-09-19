package com.cikup.amazgone.stores.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.stores_official_row
import amazgone.shared.generated.resources.stores_see_all
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.stores.domain.model.StoreSummary
import org.jetbrains.compose.resources.stringResource

/** Round store logos with a verified tick, like brand "stories"; tap one to open its store. */
@Composable
fun OfficialStoresRow(stores: List<StoreSummary>, onOpenStore: (String) -> Unit, onSeeAll: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.stores_official_row), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = onSeeAll) { Text(stringResource(Res.string.stores_see_all), color = AmazgoneTheme.extended.cta) }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
            itemsIndexed(stores, key = { _, s -> s.store.id }) { index, summary ->
                Column(
                    Modifier.width(TILE_WIDTH).staggeredEnter(index).clickable { onOpenStore(summary.store.id) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs),
                ) {
                    Box {
                        StoreLogo(summary.store, AmazgoneDimens.iconLg * LOGO_SCALE)
                        if (summary.store.isVerified) VerifiedIcon(modifier = Modifier.align(Alignment.BottomEnd))
                    }
                    Text(
                        summary.store.name.removeSuffix(" Official"),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = AmazgoneDimens.spaceXs),
                    )
                }
            }
        }
    }
}

private val TILE_WIDTH = AmazgoneDimens.iconLg * 1.5f
private const val LOGO_SCALE = 1.25f
