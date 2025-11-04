package info.cemu.cemu.games

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import info.cemu.cemu.games.details.GameDetailsScreen
import info.cemu.cemu.games.list.GamesListScreen
import info.cemu.cemu.games.profile.GameProfileEditScreen
import info.cemu.cemu.nativeinterface.NativeGameTitles
import kotlinx.serialization.Serializable

@Serializable
object GameListRoute

private object GameListRoutes {
    @Serializable
    object GamesRoute

    @Serializable
    object GameDetailsRoute

    @Serializable
    object GameProfileEditRoute
}

private inline fun <reified T : Any> NavGraphBuilder.composableGameScreen(
    navController: NavController,
    noinline content: @Composable (AnimatedContentScope.(NativeGameTitles.Game) -> Unit),
) {
    composable<T> {
        val previousBackStackEntry = navController.previousBackStackEntry ?: return@composable
        val game = viewModel<GameViewModel>(previousBackStackEntry).game

        LaunchedEffect(game) {
            if (game == null) navController.popBackStack()
        }

        if (game == null) return@composable


        content(game)
    }
}

fun NavGraphBuilder.gamesNavigation(
    navController: NavHostController,
    startGame: (NativeGameTitles.Game) -> Unit,
    createShortcut: (NativeGameTitles.Game) -> Unit,
    gameListToolBarActions: @Composable (RowScope.() -> Unit),
) {
    navigation<GameListRoute>(startDestination = GameListRoutes.GamesRoute) {
        composable<GameListRoutes.GamesRoute> { backStackEntry ->
            val gameViewModel: GameViewModel = viewModel(backStackEntry)
            GamesListScreen(
                startGame = startGame,
                createShortcut = createShortcut,
                goToGameEditProfile = { game ->
                    gameViewModel.game = game
                    navController.navigate(GameListRoutes.GameProfileEditRoute)
                },
                goToGameDetails = { game ->
                    gameViewModel.game = game
                    navController.navigate(GameListRoutes.GameDetailsRoute)
                },
                toolbarActions = gameListToolBarActions
            )
        }
        composableGameScreen<GameListRoutes.GameDetailsRoute>(navController) { game ->
            GameDetailsScreen(
                game = game,
                navigateBack = { navController.popBackStack() },
            )
        }
        composableGameScreen<GameListRoutes.GameProfileEditRoute>(navController) { game ->
            GameProfileEditScreen(
                game = game,
                navigateBack = { navController.popBackStack() },
            )
        }
    }
}
