package com.cikup.amazgone.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp

/** Lets one item of a horizontally padded list/grid extend edge to edge (e.g. a hero header). */
fun Modifier.fullBleed(horizontalPadding: Dp): Modifier = layout { measurable, constraints ->
    val extra = horizontalPadding.roundToPx() * 2
    val width = constraints.maxWidth + extra
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
}

/** Moves content up by [amount] AND gives back that space (unlike offset), for overlapping cards. */
fun Modifier.pullUp(amount: Dp): Modifier = layout { measurable, constraints ->
    val px = amount.roundToPx()
    val placeable = measurable.measure(constraints)
    layout(placeable.width, (placeable.height - px).coerceAtLeast(0)) { placeable.place(0, -px) }
}
