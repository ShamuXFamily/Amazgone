package com.cikup.amazgone.core.designsystem.component

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.action_retry
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.cikup.amazgone.core.designsystem.motion.shimmer
import com.cikup.amazgone.core.designsystem.motion.staggeredEnter
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import org.jetbrains.compose.resources.stringResource

/** Friendly empty/error state with an optional action. */
@Composable
fun MessageState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(AmazgoneDimens.spaceXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceMd),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AmazgoneTheme.extended.cta,
            modifier = Modifier.size(AmazgoneDimens.iconXl).staggeredEnter(0),
        )
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.staggeredEnter(1))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.staggeredEnter(2),
        )
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = AmazgoneTheme.extended.cta, contentColor = AmazgoneTheme.extended.onCta),
                modifier = Modifier.staggeredEnter(3),
            ) { Text(actionLabel) }
        }
    }
}

@Composable
fun ErrorState(title: String, body: String, icon: ImageVector, onRetry: () -> Unit, modifier: Modifier = Modifier) =
    MessageState(icon, title, body, modifier, stringResource(Res.string.action_retry), onRetry)

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = AmazgoneDimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (actionLabel != null && onAction != null) TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

/** Skeleton shaped like [ProductCard] while the catalog loads. */
@Composable
fun ProductCardSkeleton(modifier: Modifier = Modifier) {
    ElevatedCard(modifier, colors = appCardColors()) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).shimmer())
        Column(Modifier.padding(AmazgoneDimens.spaceMd), verticalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm)) {
            SkeletonLine(fraction = 1f)
            SkeletonLine(fraction = SKELETON_SHORT)
            SkeletonLine(fraction = SKELETON_PRICE)
        }
    }
}

@Composable
fun SkeletonLine(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth(fraction)
            .height(AmazgoneDimens.spaceMd)
            .clip(MaterialTheme.shapes.extraSmall)
            .shimmer(),
    )
}

private const val SKELETON_SHORT = 0.6f
private const val SKELETON_PRICE = 0.4f
