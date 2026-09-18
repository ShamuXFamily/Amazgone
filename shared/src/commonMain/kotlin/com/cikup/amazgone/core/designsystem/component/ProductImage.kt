package com.cikup.amazgone.core.designsystem.component

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.image_placeholder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.cikup.amazgone.catalog.domain.model.CatalogSourceId
import com.cikup.amazgone.core.designsystem.motion.shimmer
import com.cikup.amazgone.core.designsystem.theme.AmazgoneDimens
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource

private enum class ImageStage { REMOTE, BUNDLED, FAILED }

/**
 * Offline-first product image: remote URL (served from Coil's disk cache when offline) →
 * bundled seed thumbnail → neutral placeholder. Shimmers while loading.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun ProductImage(
    url: String?,
    productId: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
) {
    var stage by remember(url, productId) { mutableStateOf(if (url.isNullOrBlank()) ImageStage.BUNDLED else ImageStage.REMOTE) }
    var loading by remember(url, productId) { mutableStateOf(true) }
    val bundled = remember(productId) { bundledThumbnailPath(productId)?.let { Res.getUri(it) } }
    val model = when (stage) {
        ImageStage.REMOTE -> url
        ImageStage.BUNDLED -> bundled
        ImageStage.FAILED -> null
    }
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest),
        contentAlignment = Alignment.Center,
    ) {
        if (model == null) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = contentDescription ?: stringResource(Res.string.image_placeholder),
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(AmazgoneDimens.iconLg),
            )
        } else {
            if (loading) Box(Modifier.fillMaxSize().shimmer())
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
                onSuccess = { loading = false },
                onError = {
                    stage = if (stage == ImageStage.REMOTE && bundled != null) ImageStage.BUNDLED else ImageStage.FAILED
                    loading = stage != ImageStage.FAILED
                },
            )
        }
    }
}

/** Seed thumbnails bundled by tools/seed/build_seed.py, named "{source}_{rawId}.{ext}". */
private fun bundledThumbnailPath(productId: String): String? {
    val source = CatalogSourceId.fromProductId(productId) ?: return null
    val rawId = productId.substringAfter(':')
    val extension = when (source) {
        CatalogSourceId.DUMMY_JSON -> "webp"
        CatalogSourceId.CHEAP_SHARK -> "jpg"
        CatalogSourceId.AMAZGONE -> "webp"
    }
    return "files/seed/thumbs/${source.key}_$rawId.$extension"
}
