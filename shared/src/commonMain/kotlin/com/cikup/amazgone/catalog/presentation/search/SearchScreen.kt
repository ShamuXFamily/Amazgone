package com.cikup.amazgone.catalog.presentation.search

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.action_back
import amazgone.shared.generated.resources.action_clear
import amazgone.shared.generated.resources.search_filters
import amazgone.shared.generated.resources.search_hint
import amazgone.shared.generated.resources.search_no_results_body
import amazgone.shared.generated.resources.search_no_results_title
import amazgone.shared.generated.resources.search_results_count
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cikup.amazgone.catalog.domain.model.SortOrder
import com.cikup.amazgone.core.designsystem.component.MessageState
import com.cikup.amazgone.core.designsystem.component.ProductCard
import com.cikup.amazgone.core.designsystem.component.ProductCardSkeleton
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.catalog.presentation.home.CategoryChips
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val SKELETONS = 6

@Composable
fun SearchRoute(
    onBack: () -> Unit,
    onOpenProduct: (productId: String, origin: String) -> Unit,
    category: String? = null,
    viewModel: SearchViewModel = koinViewModel(key = "search/$category") { parametersOf(category) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SearchEffect.NavigateBack -> onBack()
                is SearchEffect.NavigateToProduct -> onOpenProduct(effect.productId, effect.origin)
            }
        }
    }
    SearchScreen(state, viewModel::onIntent)
    if (state.isFilterSheetOpen) {
        FilterSheet(
            initial = state.filters,
            onApply = { viewModel.onIntent(SearchIntent.ApplyFilters(it)) },
            onDismiss = { viewModel.onIntent(SearchIntent.CloseFilters) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(state: SearchState, onIntent: (SearchIntent) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { onIntent(SearchIntent.Back) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(Res.string.action_back))
                    }
                },
                title = { SearchField(state.query, onIntent, autoFocus = state.filters.categorySlug == null) },
                actions = { FiltersButton(state.filters.activeCount) { onIntent(SearchIntent.OpenFilters) } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            SortRow(state.sort) { onIntent(SearchIntent.ChangeSort(it)) }
            CategoryChips(
                categories = state.categories,
                selected = state.filters.categorySlug,
                onSelect = { onIntent(SearchIntent.SelectCategory(it)) },
                modifier = Modifier.padding(horizontal = AmazgoneDimens.spaceLg),
            )
            Results(state, onIntent, bottomPadding = padding.calculateBottomPadding())
        }
    }
}

@Composable
private fun SearchField(query: String, onIntent: (SearchIntent) -> Unit, autoFocus: Boolean) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (autoFocus) focus.requestFocus() }
    TextField(
        value = query,
        onValueChange = { onIntent(SearchIntent.QueryChanged(it)) },
        placeholder = { Text(stringResource(Res.string.search_hint), maxLines = 1) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        trailingIcon = {
            AnimatedVisibility(query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                IconButton(onClick = { onIntent(SearchIntent.ClearQuery) }) {
                    Icon(Icons.Rounded.Close, stringResource(Res.string.action_clear))
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
    )
}

@Composable
private fun FiltersButton(activeCount: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        BadgedBox(badge = { if (activeCount > 0) Badge { Text(activeCount.toString()) } }) {
            Icon(Icons.Rounded.Tune, stringResource(Res.string.search_filters))
        }
    }
}

@Composable
private fun SortRow(selected: SortOrder, onSelect: (SortOrder) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = AmazgoneDimens.spaceLg),
        horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm),
    ) {
        items(SortOrder.entries) { sort ->
            FilterChip(selected = sort == selected, onClick = { onSelect(sort) }, label = { Text(sortLabel(sort)) })
        }
    }
}

private enum class ResultsMode { LOADING, EMPTY, RESULTS }

@Composable
private fun Results(state: SearchState, onIntent: (SearchIntent) -> Unit, bottomPadding: Dp) {
    val mode = when {
        state.isLoading -> ResultsMode.LOADING
        state.showNoResults -> ResultsMode.EMPTY
        else -> ResultsMode.RESULTS
    }
    AnimatedContent(
        targetState = mode,
        transitionSpec = { fadeIn(MotionTokens.snappy()).togetherWith(fadeOut(MotionTokens.snappy())) },
        label = "searchResults",
    ) { current ->
        when (current) {
            ResultsMode.EMPTY -> MessageState(
                Icons.Outlined.SearchOff,
                stringResource(Res.string.search_no_results_title),
                stringResource(Res.string.search_no_results_body),
            )
            ResultsMode.LOADING, ResultsMode.RESULTS -> ResultsGrid(state, current == ResultsMode.LOADING, onIntent, bottomPadding)
        }
    }
}

@Composable
private fun ResultsGrid(state: SearchState, loading: Boolean, onIntent: (SearchIntent) -> Unit, bottomPadding: Dp) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(AmazgoneDimens.productCardMinWidth),
        contentPadding = PaddingValues(
            start = AmazgoneDimens.spaceLg,
            end = AmazgoneDimens.spaceLg,
            top = AmazgoneDimens.spaceMd,
            bottom = bottomPadding + AmazgoneDimens.spaceXl,
        ),
        horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (loading) {
            items(count = SKELETONS) { ProductCardSkeleton() }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(Res.string.search_results_count, state.results.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            itemsIndexed(state.results, key = { _, p -> p.id }) { index, product ->
                ProductCard(
                    product = product,
                    origin = SEARCH_ORIGIN,
                    onClick = { onIntent(SearchIntent.OpenProduct(product.id)) },
                    modifier = Modifier.animateItem().staggeredEnter(index),
                )
            }
        }
    }
}
