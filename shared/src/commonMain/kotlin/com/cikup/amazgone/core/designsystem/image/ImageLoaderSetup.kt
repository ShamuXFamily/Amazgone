package com.cikup.amazgone.core.designsystem.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import okio.Path

/** Where Coil persists images so previously seen products still render offline. */
expect fun imageCacheDirectory(context: PlatformContext): Path

private const val DISK_CACHE_BYTES = 250L * 1024 * 1024
private const val MEMORY_CACHE_FRACTION = 0.2

fun buildImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
    .crossfade(MotionTokens.DURATION_MEDIUM_MS)
    .memoryCache { MemoryCache.Builder().maxSizePercent(context, MEMORY_CACHE_FRACTION).build() }
    .diskCache {
        DiskCache.Builder()
            .directory(imageCacheDirectory(context))
            .maxSizeBytes(DISK_CACHE_BYTES)
            .build()
    }
    .build()
