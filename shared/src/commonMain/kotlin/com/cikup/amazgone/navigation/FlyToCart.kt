package com.cikup.amazgone.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.lerp
import com.cikup.amazgone.core.designsystem.component.ProductImage
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import kotlin.math.roundToInt

data class CartFlight(val id: Long, val productId: String, val imageUrl: String, val from: Rect)

/** Coordinates fly-to-cart animations between screens and the cart tab (all in root coordinates). */
@Stable
class FlyToCartState {
    var cartTarget by mutableStateOf<Rect?>(null)
    val flights = mutableStateListOf<CartFlight>()
    /** Increments when a flight lands; the cart badge pops on change. */
    var arrivals by mutableIntStateOf(0)
        private set
    private var nextId = 0L

    fun launch(productId: String, imageUrl: String, from: Rect) {
        flights += CartFlight(nextId++, productId, imageUrl, from)
    }

    fun land(flight: CartFlight) {
        flights.remove(flight)
        arrivals++
    }
}

val LocalFlyToCart = staticCompositionLocalOf { FlyToCartState() }

private const val ARC_LIFT_FRACTION = 0.35f
private const val END_SCALE = 0.25f
private const val SPIN_DEGREES = 20f

@Composable
fun FlyToCartOverlay(state: FlyToCartState, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        state.flights.forEach { flight ->
            key(flight.id) { Flight(flight, state) }
        }
    }
}

@Composable
private fun Flight(flight: CartFlight, state: FlyToCartState) {
    val target = state.cartTarget
    val reduceMotion = LocalReduceMotion.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(flight.id) {
        if (target != null && !reduceMotion) {
            progress.animateTo(1f, tween(MotionTokens.DURATION_EXTRA_LONG_MS, easing = MotionTokens.EmphasizedEasing))
        }
        state.land(flight)
    }
    if (target == null || reduceMotion) return
    val sizePx = with(LocalDensity.current) { AmazgoneDimens.iconXl.toPx() }
    val start = flight.from.center
    val end = target.center
    val control = Offset((start.x + end.x) / 2, minOf(start.y, end.y) - (end.y - start.y).let { if (it < 0) -it else it } * ARC_LIFT_FRACTION)
    Box(
        Modifier
            .offset {
                val point = quadraticBezier(start, control, end, progress.value)
                IntOffset((point.x - sizePx / 2).roundToInt(), (point.y - sizePx / 2).roundToInt())
            }
            .size(AmazgoneDimens.iconXl)
            .graphicsLayer {
                val t = progress.value
                val scale = lerp(1f, END_SCALE, t)
                scaleX = scale
                scaleY = scale
                rotationZ = SPIN_DEGREES * t
                alpha = 1f - (t - FADE_START).coerceAtLeast(0f) / (1f - FADE_START)
            }
            .clip(CircleShape),
    ) {
        ProductImage(flight.imageUrl, flight.productId, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

private const val FADE_START = 0.8f

internal fun quadraticBezier(p0: Offset, p1: Offset, p2: Offset, t: Float): Offset {
    val u = 1 - t
    return Offset(
        u * u * p0.x + 2 * u * t * p1.x + t * t * p2.x,
        u * u * p0.y + 2 * u * t * p1.y + t * t * p2.y,
    )
}
