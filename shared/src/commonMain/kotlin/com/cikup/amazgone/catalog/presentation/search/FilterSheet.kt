package com.cikup.amazgone.catalog.presentation.search

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.action_apply
import amazgone.shared.generated.resources.action_reset
import amazgone.shared.generated.resources.filter_in_stock
import amazgone.shared.generated.resources.filter_price
import amazgone.shared.generated.resources.filter_price_range
import amazgone.shared.generated.resources.filter_rating
import amazgone.shared.generated.resources.filter_rating_any
import amazgone.shared.generated.resources.filter_rating_value
import amazgone.shared.generated.resources.search_filters
import amazgone.shared.generated.resources.sort_discount
import amazgone.shared.generated.resources.sort_price_high
import amazgone.shared.generated.resources.sort_price_low
import amazgone.shared.generated.resources.sort_rating
import amazgone.shared.generated.resources.sort_relevance
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cikup.amazgone.catalog.domain.model.SearchFilters
import com.cikup.amazgone.catalog.domain.model.SortOrder
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.presentation.format.Formatters
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToLong

private const val MAX_PRICE_COINS = 50_000f
private const val PRICE_STEPS = 49
private val RATING_OPTIONS = listOf(0, 2, 3, 4)

/** Spring-driven M3 bottom sheet; edits are local until Apply. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(initial: SearchFilters, onApply: (SearchFilters) -> Unit, onDismiss: () -> Unit) {
    var price by remember {
        mutableStateOf((initial.minPriceCoins?.toFloat() ?: 0f)..(initial.maxPriceCoins?.toFloat() ?: MAX_PRICE_COINS))
    }
    var minRating by remember { mutableStateOf(initial.minRating?.toInt() ?: 0) }
    var inStock by remember { mutableStateOf(initial.inStockOnly) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.padding(horizontal = AmazgoneDimens.spaceXl).padding(bottom = AmazgoneDimens.spaceXl),
            verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceLg),
        ) {
            Text(stringResource(Res.string.search_filters), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(Res.string.filter_price), style = MaterialTheme.typography.titleSmall)
            RangeSlider(value = price, onValueChange = { price = it }, valueRange = 0f..MAX_PRICE_COINS, steps = PRICE_STEPS)
            Text(
                stringResource(
                    Res.string.filter_price_range,
                    Formatters.coins(price.start.roundToLong()),
                    Formatters.coins(price.endInclusive.roundToLong()),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(stringResource(Res.string.filter_rating), style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                RATING_OPTIONS.forEachIndexed { index, stars ->
                    SegmentedButton(
                        selected = minRating == stars,
                        onClick = { minRating = stars },
                        shape = SegmentedButtonDefaults.itemShape(index, RATING_OPTIONS.size),
                    ) { Text(if (stars == 0) stringResource(Res.string.filter_rating_any) else stringResource(Res.string.filter_rating_value, stars)) }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(Res.string.filter_in_stock), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = inStock, onCheckedChange = { inStock = it })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd)) {
                OutlinedButton(onClick = { onApply(initial.copy(minPriceCoins = null, maxPriceCoins = null, minRating = null, inStockOnly = false)) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.action_reset))
                }
                Button(
                    onClick = {
                        onApply(
                            initial.copy(
                                minPriceCoins = price.start.roundToLong().takeIf { it > 0 },
                                maxPriceCoins = price.endInclusive.roundToLong().takeIf { it < MAX_PRICE_COINS },
                                minRating = minRating.takeIf { it > 0 }?.toDouble(),
                                inStockOnly = inStock,
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(Res.string.action_apply)) }
            }
        }
    }
}

@Composable
fun sortLabel(sort: SortOrder): String = stringResource(
    when (sort) {
        SortOrder.RELEVANCE -> Res.string.sort_relevance
        SortOrder.PRICE_LOW_TO_HIGH -> Res.string.sort_price_low
        SortOrder.PRICE_HIGH_TO_LOW -> Res.string.sort_price_high
        SortOrder.TOP_RATED -> Res.string.sort_rating
        SortOrder.BIGGEST_DISCOUNT -> Res.string.sort_discount
    },
)
