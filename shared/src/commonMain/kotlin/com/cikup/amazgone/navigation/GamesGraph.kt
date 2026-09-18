package com.cikup.amazgone.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.cikup.amazgone.games.presentation.deal.LightningDealRoute
import com.cikup.amazgone.games.presentation.hub.GameDestination
import com.cikup.amazgone.games.presentation.hub.GamesHubRoute
import com.cikup.amazgone.games.presentation.scratch.ScratchCardRoute
import com.cikup.amazgone.games.presentation.spin.SpinWheelRoute
import com.cikup.amazgone.progress.presentation.AchievementsRoute
import com.cikup.amazgone.progress.presentation.LeaderboardRoute

internal fun NavGraphBuilder.gamesGraph(nav: NavHostController) {
    composable<Route.Games> {
        Animated {
            GamesHubRoute(onNavigate = { destination ->
                nav.navigate(
                    when (destination) {
                        GameDestination.SPIN -> Route.SpinWheel
                        GameDestination.SCRATCH -> Route.ScratchCard
                        GameDestination.DEAL -> Route.LightningDeal
                        GameDestination.LEADERBOARD -> Route.Leaderboard
                        GameDestination.ACHIEVEMENTS -> Route.Achievements
                    },
                )
            })
        }
    }
    composable<Route.SpinWheel> { Animated { SpinWheelRoute(onBack = { nav.popBackStack() }) } }
    composable<Route.ScratchCard> { Animated { ScratchCardRoute(onBack = { nav.popBackStack() }) } }
    composable<Route.LightningDeal> {
        Animated { LightningDealRoute(onBack = { nav.popBackStack() }, onOpenProduct = { nav.navigate(Route.ProductDetail(it, "deal-screen")) }) }
    }
    composable<Route.Leaderboard> { Animated { LeaderboardRoute(onBack = { nav.popBackStack() }) } }
    composable<Route.Achievements> { Animated { AchievementsRoute(onBack = { nav.popBackStack() }) } }
}
