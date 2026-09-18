package com.cikup.amazgone.core.designsystem.component

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.navigation.LocalNavAnimatedVisibilityScope
import com.cikup.amazgone.navigation.LocalSharedTransitionScope

/** Keys shared between a product's list card and its detail screen. */
object SharedKeys {
    fun image(origin: String, productId: String) = "image/$origin/$productId"
    fun title(origin: String, productId: String) = "title/$origin/$productId"
}

/** Morphs this element into the same-keyed element on the next screen; no-op without a scope or with reduced motion. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedElementOrNone(key: String): Modifier {
    val shared = LocalSharedTransitionScope.current
    val animated = LocalNavAnimatedVisibilityScope.current
    if (shared == null || animated == null || LocalReduceMotion.current) return this
    return with(shared) { this@sharedElementOrNone.sharedElement(rememberSharedContentState(key), animated) }
}

/** Like [sharedElementOrNone] but lets text reflow between sizes. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedBoundsOrNone(key: String): Modifier {
    val shared = LocalSharedTransitionScope.current
    val animated = LocalNavAnimatedVisibilityScope.current
    if (shared == null || animated == null || LocalReduceMotion.current) return this
    return with(shared) {
        this@sharedBoundsOrNone.sharedBounds(
            rememberSharedContentState(key),
            animated,
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
        )
    }
}
