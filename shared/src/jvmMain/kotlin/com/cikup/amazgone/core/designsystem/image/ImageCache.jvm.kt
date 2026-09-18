package com.cikup.amazgone.core.designsystem.image

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toPath

actual fun imageCacheDirectory(context: PlatformContext): Path =
    "${System.getProperty("java.io.tmpdir")}/amazgone/image_cache".toPath()
