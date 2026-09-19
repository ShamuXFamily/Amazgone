package com.cikup.amazgone.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.cikup.amazgone.account.presentation.AccountDestination
import com.cikup.amazgone.account.presentation.AccountRoute
import com.cikup.amazgone.cart.presentation.CartRoute
import com.cikup.amazgone.catalog.presentation.detail.ProductDetailRoute
import com.cikup.amazgone.catalog.presentation.categories.CategoriesRoute
import com.cikup.amazgone.catalog.presentation.flash.FlashSaleRoute
import com.cikup.amazgone.catalog.presentation.home.HomeDestination
import com.cikup.amazgone.catalog.presentation.home.HomeRoute
import com.cikup.amazgone.catalog.presentation.search.SearchRoute
import com.cikup.amazgone.core.designsystem.motion.LocalReduceMotion
import com.cikup.amazgone.core.designsystem.motion.MotionTokens
import com.cikup.amazgone.orders.presentation.checkout.CheckoutRoute
import com.cikup.amazgone.orders.presentation.detail.OrderDetailRoute
import com.cikup.amazgone.orders.presentation.list.OrdersRoute
import com.cikup.amazgone.stores.presentation.StoreRoute
import com.cikup.amazgone.stores.presentation.StoresRoute
import com.cikup.amazgone.wallet.presentation.WalletRoute
import com.cikup.amazgone.wishlist.presentation.WishlistRoute

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    SharedTransitionLayout(modifier) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(
                navController = navController,
                startDestination = Route.Home,
                enterTransition = { fadeThroughEnter(reduceMotion) },
                exitTransition = { fadeThroughExit() },
                popEnterTransition = { fadeThroughEnter(reduceMotion) },
                popExitTransition = { fadeThroughExit() },
            ) {
                shopGraph(navController)
                accountGraph(navController)
                gamesGraph(navController)
            }
        }
    }
}

private fun NavHostController.openProduct(productId: String, origin: String) = navigate(Route.ProductDetail(productId, origin))

/** Re-opening the store you came from pops back to it instead of stacking a copy. */
private fun NavHostController.openStore(storeId: String) {
    if (!popBackStack(Route.Store(storeId), inclusive = false)) navigate(Route.Store(storeId))
}

private fun NavGraphBuilder.shopGraph(nav: NavHostController) {
    composable<Route.Home> {
        Animated {
            HomeRoute(
                onNavigate = { destination ->
                    when (destination) {
                        HomeDestination.SEARCH -> nav.navigate(Route.Search())
                        HomeDestination.CATEGORIES -> nav.navigate(Route.Categories)
                        HomeDestination.WALLET -> nav.navigate(Route.Wallet)
                        HomeDestination.SPIN -> nav.navigate(Route.SpinWheel)
                        HomeDestination.SCRATCH -> nav.navigate(Route.ScratchCard)
                        HomeDestination.ORDERS -> nav.navigate(Route.Orders)
                        HomeDestination.FLASH_SALE -> nav.navigate(Route.FlashSale)
                        HomeDestination.STORES -> nav.navigate(Route.Stores)
                    }
                },
                onOpenProduct = nav::openProduct,
                onOpenStore = nav::openStore,
            )
        }
    }
    composable<Route.Search> { entry ->
        val route = entry.toRoute<Route.Search>()
        Animated { SearchRoute(onBack = { nav.popBackStack() }, onOpenProduct = nav::openProduct, category = route.category) }
    }
    composable<Route.FlashSale> { Animated { FlashSaleRoute(onBack = { nav.popBackStack() }, onOpenProduct = nav::openProduct) } }
    composable<Route.Stores> { Animated { StoresRoute(onBack = { nav.popBackStack() }, onOpenStore = nav::openStore) } }
    composable<Route.Store> { entry ->
        val route = entry.toRoute<Route.Store>()
        Animated { StoreRoute(route.storeId, onBack = { nav.popBackStack() }, onOpenProduct = nav::openProduct) }
    }
    composable<Route.Categories> {
        Animated { CategoriesRoute(onBack = { nav.popBackStack() }, onOpenCategory = { nav.navigate(Route.Search(it)) }) }
    }
    composable<Route.ProductDetail>(
        // plain cross-fade so the shared image/title morph carries the motion
        enterTransition = { fadeIn(tween(MotionTokens.DURATION_MEDIUM_MS)) },
        exitTransition = { fadeOut(tween(MotionTokens.DURATION_MEDIUM_MS)) },
        popEnterTransition = { fadeIn(tween(MotionTokens.DURATION_MEDIUM_MS)) },
        popExitTransition = { fadeOut(tween(MotionTokens.DURATION_MEDIUM_MS)) },
    ) { entry ->
        val route = entry.toRoute<Route.ProductDetail>()
        Animated {
            ProductDetailRoute(route.productId, route.origin, onBack = { nav.popBackStack() }, onOpenProduct = nav::openProduct, onOpenStore = nav::openStore)
        }
    }
    composable<Route.Cart> {
        Animated {
            CartRoute(
                onOpenProduct = nav::openProduct,
                onCheckout = { nav.navigate(Route.Checkout) },
                onBrowse = { nav.navigateToTopLevel(TopLevelDestination.HOME) },
                onPlayGames = { nav.navigateToTopLevel(TopLevelDestination.GAMES) },
            )
        }
    }
    composable<Route.Checkout> {
        Animated {
            CheckoutRoute(
                onClose = { nav.popBackStack() },
                onOpenOrder = { id -> nav.navigate(Route.OrderDetail(id)) { popUpTo(Route.Cart) } },
                onGoHome = { nav.navigateToTopLevel(TopLevelDestination.HOME) },
            )
        }
    }
}

private fun NavGraphBuilder.accountGraph(nav: NavHostController) {
    composable<Route.Account> {
        Animated {
            AccountRoute(
                onNavigate = { destination ->
                    nav.navigate(
                        when (destination) {
                            AccountDestination.ORDERS -> Route.Orders
                            AccountDestination.WISHLIST -> Route.Wishlist
                            AccountDestination.WALLET -> Route.Wallet
                            AccountDestination.LEADERBOARD -> Route.Leaderboard
                            AccountDestination.ACHIEVEMENTS -> Route.Achievements
                            AccountDestination.SEARCH -> Route.Search()
                            AccountDestination.SPIN -> Route.SpinWheel
                            AccountDestination.CATEGORIES -> Route.Categories
                            AccountDestination.GAMES -> return@AccountRoute nav.navigateToTopLevel(TopLevelDestination.GAMES)
                        },
                    )
                },
            )
        }
    }
    composable<Route.Orders> { Animated { OrdersRoute(onBack = { nav.popBackStack() }, onOpenOrder = { nav.navigate(Route.OrderDetail(it)) }) } }
    composable<Route.OrderDetail> { entry ->
        val route = entry.toRoute<Route.OrderDetail>()
        Animated { OrderDetailRoute(route.orderId, onBack = { nav.popBackStack() }, onOpenProduct = { nav.openProduct(it, "order") }) }
    }
    composable<Route.Wishlist> { Animated { WishlistRoute(onBack = { nav.popBackStack() }, onOpenProduct = nav::openProduct) } }
    composable<Route.Wallet> { Animated { WalletRoute(onBack = { nav.popBackStack() }) } }
}

/** Exposes this destination's AnimatedVisibilityScope for shared-element modifiers below it. */
@Composable
internal fun AnimatedContentScope.Animated(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this, content = content)
}
