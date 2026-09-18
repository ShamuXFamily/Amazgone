package com.cikup.amazgone.core.designsystem.image

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toOkioPath

actual fun imageCacheDirectory(context: PlatformContext): Path = context.cacheDir.resolve("image_cache").toOkioPath()
