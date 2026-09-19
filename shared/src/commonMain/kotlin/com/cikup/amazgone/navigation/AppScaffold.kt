package com.cikup.amazgone.navigation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.achievement_unlocked_toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cikup.amazgone.core.analytics.Analytics
import com.cikup.amazgone.core.notifications.DeepLinks
import org.koin.compose.koinInject
import com.cikup.amazgone.navigation.presentation.ShellEffect
import com.cikup.amazgone.navigation.presentation.ShellIntent
import com.cikup.amazgone.navigation.presentation.ShellViewModel
import com.cikup.amazgone.progress.presentation.label
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val RAIL_BREAKPOINT = 600.dp

/** Phones: content + navy floating bar. Tablets/iPad: M3 navigation rail. Hosts global overlays. */
@Composable
fun AppScaffold(
    navController: NavHostController = rememberNavController(),
    shell: ShellViewModel = koinViewModel(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    TrackScreens(currentDestination?.route)
    val onTab = TopLevelDestination.entries.firstOrNull { currentDestination.isOn(it) }
    // deeper screens (detail, checkout…) keep the tab they were opened from highlighted
    var lastTab by remember { mutableStateOf(TopLevelDestination.HOME) }
    if (onTab != null) lastTab = onTab
    val selected = lastTab
    val shellState by shell.state.collectAsStateWithLifecycle()
    val flyToCart = remember { FlyToCartState() }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(shell) {
        shell.effects.collect { effect ->
            when (effect) {
                is ShellEffect.AchievementsUnlocked -> effect.ids.forEach { id ->
                    snackbar.showSnackbar(getString(Res.string.achievement_unlocked_toast, getString(id.label().title)))
                }
                is ShellEffect.OpenLink -> navController.openLink(effect.link)
            }
        }
    }
    // taps on system notifications arrive through DeepLinks (iOS delegate / Android launch intent)
    val pendingLink by DeepLinks.pending.collectAsStateWithLifecycle()
    LaunchedEffect(pendingLink) {
        pendingLink?.let {
            navController.openLink(it)
            DeepLinks.consume()
        }
    }
    val onSelect: (TopLevelDestination) -> Unit = { destination ->
        // re-tapping the current tab returns to its root, like most shopping apps
        if (destination == selected) navController.popBackStack(destination.route, inclusive = false)
        else navController.navigateToTopLevel(destination)
    }

    CompositionLocalProvider(LocalFlyToCart provides flyToCart) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth >= RAIL_BREAKPOINT) {
                NavigationSuiteScaffold(
                    navigationSuiteItems = {
                        TopLevelDestination.entries.forEach { destination ->
                            item(
                                selected = destination == selected,
                                onClick = { onSelect(destination) },
                                icon = { Icon(if (destination == selected) destination.selectedIcon else destination.unselectedIcon, null) },
                                label = { Text(stringResource(destination.label)) },
                            )
                        }
                    },
                ) { AppNavHost(navController) }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).consumeWindowInsets(WindowInsets.navigationBars)) { AppNavHost(navController) }
                    FloatingNavBar(selected, shellState.cartCount, flyToCart, onSelect)
                }
            }
            FlyToCartOverlay(flyToCart)
            SnackbarHost(snackbar, Modifier.align(Alignment.TopCenter).safeContentPadding())
            NotificationBanner(
                banner = shellState.banner,
                onOpen = { shell.onIntent(ShellIntent.OpenBanner) },
                onDismiss = { shell.onIntent(ShellIntent.DismissBanner) },
                modifier = Modifier.align(Alignment.TopCenter),
            )
            AnimatedVisibility(shellState.levelUpTo != null, enter = fadeIn(), exit = fadeOut()) {
                shellState.levelUpTo?.let { LevelUpOverlay(it) { shell.onIntent(ShellIntent.DismissLevelUp) } }
            }
        }
    }
}

private fun NavDestination?.isOn(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.route::class) } == true

/** Single-top, state-restoring tab switch so each tab keeps its own back stack. */
internal fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** One screen_view per destination change, named after the route class ("ProductDetail", "Checkout"…). */
@Composable
private fun TrackScreens(route: String?, analytics: Analytics = koinInject()) {
    LaunchedEffect(route) {
        route?.let { analytics.screen(screenName(it)) }
    }
}

/** "com.cikup.amazgone.navigation.Route.ProductDetail/{productId}/{origin}" → "ProductDetail". */
internal fun screenName(route: String): String = route.substringBefore('/').substringBefore('?').substringAfterLast('.')
