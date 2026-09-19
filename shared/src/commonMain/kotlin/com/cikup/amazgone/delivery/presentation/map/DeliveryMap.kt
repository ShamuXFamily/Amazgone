package com.cikup.amazgone.delivery.presentation.map

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import com.cikup.amazgone.core.designsystem.theme.AmazgoneTheme
import com.cikup.amazgone.delivery.domain.model.GeoMath
import com.cikup.amazgone.delivery.domain.model.GeoPoint

/** One parcel on the map: route from [from] to the shared home, and how far along the courier is. */
data class MapRoute(val from: GeoPoint, val progress: Double, val emoji: String)

/**
 * OpenStreetMap map (standard OSM tiles, dimmed in dark mode) framed around every route,
 * with the travelled part solid, the rest dashed, and each courier bobbing at its live position.
 * Tiles are cached by Coil; offline the routes still draw on the plain background.
 */
@Composable
fun DeliveryMap(routes: List<MapRoute>, home: GeoPoint, modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background.luminance() < DARK_THRESHOLD
    val ext = AmazgoneTheme.extended
    val lines = remember(routes.map { it.from }, home) { MapProjection.unwrappedRoutes(routes.map { it.from to home }, home) }
    BoxWithConstraints(modifier.clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainer)) {
        val width = maxWidth.value.toDouble()
        val height = maxHeight.value.toDouble()
        val frame = remember(lines, width, height) { MapProjection.frame(lines.flatten() + home, width, height) }
        fun local(p: GeoPoint): Offset = MapProjection.worldPoint(p, frame.zoom).let { (x, y) -> Offset((x - frame.left).toFloat(), (y - frame.top).toFloat()) }

        Tiles(frame, width, height, dark)
        val cta = ext.cta
        val track = if (dark) ext.onBrandNavy else ext.brandNavy
        Canvas(Modifier.fillMaxSize()) {
            val scale = density
            lines.forEachIndexed { index, line ->
                val points = line.map { local(it) * scale }
                val done = (routes[index].progress * (points.size - 1)).toInt()
                drawPath(points.toPath(), track.copy(alpha = ROUTE_ALPHA), style = Stroke(ROUTE_WIDTH.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH.toPx(), DASH.toPx()))))
                if (done > 0) drawPath(points.take(done + 1).toPath(), cta, style = Stroke(ROUTE_WIDTH.toPx(), cap = StrokeCap.Round))
                drawCircle(track, radius = PIN.toPx() / 2, center = points.first())
                drawCircle(ext.onBrandNavy, radius = PIN.toPx() / 4, center = points.first())
            }
            val homePx = local(home) * scale
            drawCircle(cta.copy(alpha = HALO_ALPHA), radius = PIN.toPx() * 1.4f, center = homePx)
            drawCircle(cta, radius = PIN.toPx() / 1.6f, center = homePx)
        }
        routes.forEachIndexed { index, route ->
            val at = GeoMath.interpolate(route.from, home, route.progress)
            // Use the unwrapped longitude of the drawn line so the courier sits on it.
            val drawn = lines[index].let { it[(route.progress * (it.size - 1)).toInt().coerceIn(0, it.lastIndex)] }
            CourierMarker(route.emoji, local(GeoPoint(at.lat, drawn.lon)))
        }
        Attribution(Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private fun Tiles(frame: MapFrame, width: Double, height: Double, dark: Boolean) {
    // Standard OpenStreetMap tiles (the open-source map); dimmed and inverted for dark mode.
    val filter = if (dark) DARK_TILES else null
    MapProjection.tiles(frame, width, height).forEach { (x, y) ->
        AsyncImage(
            model = "https://tile.openstreetmap.org/${frame.zoom}/${MapProjection.wrapX(x, frame.zoom)}/$y.png",
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            colorFilter = filter,
            modifier = Modifier
                .offset((x * MapProjection.TILE - frame.left).dp, (y * MapProjection.TILE - frame.top).dp)
                // A tile can be bigger than the map view: let it overflow from its top-left corner
                // instead of being squeezed (size) or centred (requiredSize).
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .size(MapProjection.TILE.dp),
        )
    }
}

/** Inverts lightness and dims, so the light OSM style reads as a night map. */
private val DARK_TILES = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            -0.75f, 0f, 0f, 0f, 210f,
            0f, -0.75f, 0f, 0f, 210f,
            0f, 0f, -0.7f, 0f, 215f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

/** The courier emoji on a white disc, bobbing gently so the map feels alive. */
@Composable
private fun CourierMarker(emoji: String, at: Offset) {
    val bob by if (LocalReduceMotion.current) {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    } else {
        rememberInfiniteTransition(label = "bob").animateFloat(
            -BOB, BOB, infiniteRepeatable(tween(MotionTokens.DURATION_EXTRA_LONG_MS), RepeatMode.Reverse), label = "y",
        )
    }
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = AmazgoneDimens.spaceXs,
        modifier = Modifier.offset((at.x - MARKER.value / 2).dp, (at.y - MARKER.value / 2 + bob).dp).size(MARKER),
    ) {
        Box(contentAlignment = Alignment.Center) { Text(emoji, style = MaterialTheme.typography.titleLarge) }
    }
}

/** Required credit for OpenStreetMap data and tiles. */
@Composable
private fun Attribution(modifier: Modifier) {
    Text(
        "© OpenStreetMap contributors",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(AmazgoneDimens.spaceXs).background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = CREDIT_ALPHA), MaterialTheme.shapes.extraSmall)
            .padding(horizontal = AmazgoneDimens.spaceXs),
    )
}

private fun List<Offset>.toPath() = Path().also { path ->
    firstOrNull()?.let { path.moveTo(it.x, it.y) }
    drop(1).forEach { path.lineTo(it.x, it.y) }
}

private val ROUTE_WIDTH = AmazgoneDimens.spaceXs
private val DASH = AmazgoneDimens.spaceSm
private val PIN = AmazgoneDimens.spaceMd
private val MARKER: Dp = AmazgoneDimens.iconLg * 0.8f
private const val BOB = 3f
private const val ROUTE_ALPHA = 0.55f
private const val HALO_ALPHA = 0.25f
private const val CREDIT_ALPHA = 0.8f
private const val DARK_THRESHOLD = 0.5f
