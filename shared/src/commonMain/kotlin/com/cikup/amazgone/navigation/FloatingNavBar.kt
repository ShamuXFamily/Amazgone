package com.cikup.amazgone.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.cikup.amazgone.core.designsystem.component.RollingNumber
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import org.jetbrains.compose.resources.stringResource

private val BAR_CORNER = 28.dp
private const val ICON_POP = 1.3f
private const val CART_BUMP = 1.4f

/**
 * Navy bottom bar: the selected tab morphs into an orange pill (color + width springs, label
 * expands), its icon pops; the cart icon doubles as the fly-to-cart target and bumps on arrival.
 */
@Composable
fun FloatingNavBar(
    selected: TopLevelDestination?,
    cartCount: Int,
    flyToCart: FlyToCartState,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AmazgoneTheme.extended
    Surface(
        color = colors.brandNavy,
        contentColor = colors.onBrandNavy,
        shape = RoundedCornerShape(topStart = BAR_CORNER, topEnd = BAR_CORNER),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.navigationBarsPadding().padding(horizontal = AmazgoneDimens.spaceLg, vertical = AmazgoneDimens.spaceMd),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopLevelDestination.entries.forEach { destination ->
                NavPill(
                    destination = destination,
                    isSelected = destination == selected,
                    badge = if (destination == TopLevelDestination.CART) cartCount else 0,
                    flyToCart = flyToCart.takeIf { destination == TopLevelDestination.CART },
                    onClick = { onSelect(destination) },
                )
            }
        }
    }
}

@Composable
private fun NavPill(
    destination: TopLevelDestination,
    isSelected: Boolean,
    badge: Int,
    flyToCart: FlyToCartState?,
    onClick: () -> Unit,
) {
    val colors = AmazgoneTheme.extended
    val reduceMotion = LocalReduceMotion.current
    val haptics = LocalHapticFeedback.current
    val container by animateColorAsState(if (isSelected) colors.cta else colors.brandNavy, MotionTokens.snappy(), label = "pill")
    val tint by animateColorAsState(if (isSelected) colors.onCta else colors.onBrandNavyVariant, label = "pillTint")
    val pop = remember { Animatable(1f) }
    LaunchedEffect(isSelected) {
        if (isSelected && !reduceMotion) {
            pop.snapTo(ICON_POP)
            pop.animateTo(1f, MotionTokens.bouncy())
        }
    }
    val bump = remember { Animatable(1f) }
    val arrivals = flyToCart?.arrivals ?: 0
    LaunchedEffect(arrivals) {
        if (arrivals == 0 || reduceMotion) return@LaunchedEffect
        bump.snapTo(CART_BUMP)
        bump.animateTo(1f, MotionTokens.bouncy())
    }
    val label = stringResource(destination.label)
    Surface(
        color = container,
        contentColor = tint,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .heightIn(min = AmazgoneDimens.minTouchTarget)
            .selectable(
                selected = isSelected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onClick()
                },
            ),
    ) {
        Row(
            Modifier.padding(horizontal = AmazgoneDimens.spaceLg, vertical = AmazgoneDimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AmazgoneDimens.spaceSm),
        ) {
            BadgedBox(
                badge = { if (badge > 0) Badge { RollingNumber(badge) } },
                modifier = Modifier
                    .graphicsLayer {
                        val s = pop.value * bump.value
                        scaleX = s
                        scaleY = s
                    }
                    .then(if (flyToCart != null) Modifier.onGloballyPositioned { flyToCart.cartTarget = it.boundsInRoot() } else Modifier),
            ) {
                Icon(
                    if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                    contentDescription = if (isSelected) null else label,
                )
            }
            AnimatedVisibility(
                visible = isSelected,
                enter = expandHorizontally(MotionTokens.bouncy()) + fadeIn(),
                exit = shrinkHorizontally(MotionTokens.snappy()) + fadeOut(),
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}
