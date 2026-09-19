package com.cikup.amazgone.stores.presentation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.store_products
import amazgone.shared.generated.resources.stores_section_amazgone
import amazgone.shared.generated.resources.stores_section_brand
import amazgone.shared.generated.resources.stores_section_digital
import amazgone.shared.generated.resources.stores_section_official
import amazgone.shared.generated.resources.stores_title
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.core.designsystem.component.BackTopBar
import com.cikup.amazgone.core.designsystem.component.appCardColors
import com.cikup.amazgone.core.designsystem.motion.pressScale
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.presentation.format.Formatters
import com.cikup.amazgone.stores.domain.model.StoreKind
import com.cikup.amazgone.stores.domain.model.StoreSummary
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StoresRoute(onBack: () -> Unit, onOpenStore: (String) -> Unit, viewModel: StoresViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                StoresEffect.NavigateBack -> onBack()
                is StoresEffect.NavigateToStore -> onOpenStore(effect.storeId)
            }
        }
    }
    StoresScreen(state, viewModel::onIntent)
}

@Composable
fun StoresScreen(state: StoresState, onIntent: (StoresIntent) -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BackTopBar(stringResource(Res.string.stores_title), { onIntent(StoresIntent.Back) }) },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(AmazgoneDimens.productCardMinWidth),
            contentPadding = PaddingValues(
                start = AmazgoneDimens.spaceLg,
                end = AmazgoneDimens.spaceLg,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + AmazgoneDimens.iconXl,
            ),
            horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
            verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
            modifier = Modifier.fillMaxSize(),
        ) {
            state.sections.forEach { (kind, stores) ->
                item(key = "title-$kind", span = { GridItemSpan(maxLineSpan) }) {
                    Text(stringResource(sectionTitle(kind)), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = AmazgoneDimens.spaceSm))
                }
                itemsIndexed(stores, key = { _, s -> s.store.id }) { index, summary ->
                    StoreTile(summary, { onIntent(StoresIntent.OpenStore(summary.store.id)) }, Modifier.staggeredEnter(index))
                }
            }
        }
    }
}

@Composable
private fun StoreTile(summary: StoreSummary, onClick: () -> Unit, modifier: Modifier) {
    val interaction = remember { MutableInteractionSource() }
    ElevatedCard(onClick = onClick, colors = appCardColors(), interactionSource = interaction, modifier = modifier.pressScale(interaction)) {
        Column(Modifier.padding(AmazgoneDimens.spaceMd), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            StoreLogo(summary.store, AmazgoneDimens.iconLg)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(summary.store.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (summary.store.isVerified) VerifiedIcon(modifier = Modifier.padding(start = AmazgoneDimens.spaceXs))
            }
            Text(
                listOfNotNull(stringResource(Res.string.store_products, summary.productCount), summary.topCategory?.let(Formatters::categoryLabel)).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun sectionTitle(kind: StoreKind) = when (kind) {
    StoreKind.OFFICIAL -> Res.string.stores_section_official
    StoreKind.AMAZGONE -> Res.string.stores_section_amazgone
    StoreKind.DIGITAL -> Res.string.stores_section_digital
    StoreKind.BRAND -> Res.string.stores_section_brand
}
