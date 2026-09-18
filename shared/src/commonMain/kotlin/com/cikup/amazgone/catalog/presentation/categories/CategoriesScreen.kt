package com.cikup.amazgone.catalog.presentation.categories

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.categories_title
import amazgone.shared.generated.resources.order_items_count
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.catalog.presentation.components.categoryIcon
import com.cikup.amazgone.core.designsystem.component.BackTopBar
import com.cikup.amazgone.core.designsystem.motion.pressScale
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.core.presentation.format.Formatters
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CategoriesRoute(onBack: () -> Unit, onOpenCategory: (String) -> Unit, viewModel: CategoriesViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CategoriesEffect.NavigateBack -> onBack()
                is CategoriesEffect.NavigateToCategory -> onOpenCategory(effect.slug)
            }
        }
    }
    val tints = AmazgoneTheme.extended.tileTints
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BackTopBar(stringResource(Res.string.categories_title), { viewModel.onIntent(CategoriesIntent.Back) }) },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(AmazgoneDimens.iconXl * 1.1f),
            contentPadding = PaddingValues(AmazgoneDimens.spaceLg),
            horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
            verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            itemsIndexed(state.categories, key = { _, c -> c.slug }) { index, category ->
                val interaction = remember { MutableInteractionSource() }
                Card(
                    onClick = { viewModel.onIntent(CategoriesIntent.Open(category.slug)) },
                    interactionSource = interaction,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    modifier = Modifier.pressScale(interaction).staggeredEnter(index),
                ) {
                    Column(Modifier.fillMaxWidth().padding(AmazgoneDimens.spaceMd), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceXs)) {
                        Surface(color = tints[index % tints.size], shape = MaterialTheme.shapes.large) {
                            Icon(categoryIcon(category.slug), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(AmazgoneDimens.spaceMd).size(AmazgoneDimens.iconMd))
                        }
                        Text(Formatters.categoryLabel(category.slug), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 2)
                        Text(stringResource(Res.string.order_items_count, category.productCount), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
