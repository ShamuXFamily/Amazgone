package com.cikup.amazgone.navigation

import amazgone.shared.generated.resources.Res
import amazgone.shared.generated.resources.nav_menu
import amazgone.shared.generated.resources.nav_cart
import amazgone.shared.generated.resources.nav_games
import amazgone.shared.generated.resources.nav_home
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource

/** Destinations shown in the adaptive navigation bar/rail. */
enum class TopLevelDestination(
    val route: Route,
    val label: StringResource,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(Route.Home, Res.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    GAMES(Route.Games, Res.string.nav_games, Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports),
    CART(Route.Cart, Res.string.nav_cart, Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
    ACCOUNT(Route.Account, Res.string.nav_menu, Icons.Filled.Menu, Icons.Outlined.Menu),
}
