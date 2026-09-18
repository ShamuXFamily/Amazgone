package com.cikup.amazgone.catalog.presentation.detail

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.action_back
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.cikup.amazgone.catalog.domain.model.Product
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.wishlist.presentation.HeartButton
import org.jetbrains.compose.resources.stringResource

/** True once the hero image has mostly scrolled away. */
@Composable
fun rememberIsCollapsed(listState: LazyListState): Boolean {
    val collapsed by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                (listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: Int.MAX_VALUE)
                    .let { listState.firstVisibleItemScrollOffset > it * COLLAPSE_FRACTION }
        }
    }
    return collapsed
}

/**
 * Two overlay states: floating back button + rating badge over the image, and a solid compact bar
 * (back · title · heart) that slides down once the image has scrolled away, so content never runs
 * under the status bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailOverlay(product: Product?, collapsed: Boolean, isSaved: Boolean, onIntent: (ProductDetailIntent) -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        AnimatedVisibility(!collapsed, enter = fadeIn() + scaleIn(MotionTokens.bouncy()), exit = fadeOut() + scaleOut()) {
            FilledTonalIconButton(
                onClick = { onIntent(ProductDetailIntent.Back) },
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier.statusBarsPadding().padding(AmazgoneDimens.spaceSm),
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(Res.string.action_back)) }
        }
        product?.rating?.let { rating ->
            AnimatedVisibility(!collapsed, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopEnd)) {
                RatingBadge(rating, Modifier.statusBarsPadding().padding(AmazgoneDimens.spaceMd))
            }
        }
        AnimatedVisibility(
            visible = collapsed && product != null,
            enter = slideInVertically(MotionTokens.snappy()) { -it } + fadeIn(),
            exit = slideOutVertically(MotionTokens.snappy()) { -it } + fadeOut(),
        ) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = AmazgoneDimens.spaceXs) {
                TopAppBar(
                    title = { Text(product?.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = { onIntent(ProductDetailIntent.Back) }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(Res.string.action_back))
                        }
                    },
                    actions = { HeartButton(isSaved, { onIntent(ProductDetailIntent.ToggleWishlist) }) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                )
            }
        }
    }
}

private const val COLLAPSE_FRACTION = 0.6f
