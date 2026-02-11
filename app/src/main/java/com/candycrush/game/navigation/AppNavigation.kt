package com.candycrush.game.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.candycrush.game.ui.game.GameScreen
import com.candycrush.game.ui.levelmap.LevelMapScreen
import com.candycrush.game.ui.results.ResultsScreen
import com.candycrush.game.ui.splash.SplashScreen

/**
 * The main navigation graph for the app.
 *
 * This defines all the screens and how the user can navigate between them.
 * It uses Navigation Compose with type-safe routes — each route is a
 * @Serializable class defined in Screens.kt.
 *
 * NavHost is the container that displays the current screen.
 * NavController is used to navigate between screens.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = SplashRoute
    ) {
        // ===== Splash Screen =====
        // Shown briefly when the app opens, then auto-navigates to the level map
        composable<SplashRoute> {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate(LevelMapRoute) {
                        // Remove splash from the back stack so pressing back
                        // doesn't go back to the splash screen
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                }
            )
        }

        // ===== Level Map Screen =====
        // Shows all levels with their star ratings, player taps a level to play
        composable<LevelMapRoute> {
            LevelMapScreen(
                onLevelSelected = { levelNumber ->
                    navController.navigate(GameRoute(levelNumber))
                }
            )
        }

        // ===== Game Screen =====
        // The main match-3 gameplay screen
        composable<GameRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<GameRoute>()
            GameScreen(
                levelNumber = route.levelNumber,
                onGameEnd = { score, stars, won ->
                    navController.navigate(
                        ResultsRoute(
                            levelNumber = route.levelNumber,
                            score = score,
                            stars = stars,
                            won = won
                        )
                    ) {
                        // Pop back to level map so the back stack is clean
                        popUpTo(LevelMapRoute)
                    }
                },
                onBackToMap = {
                    navController.popBackStack(LevelMapRoute, inclusive = false)
                }
            )
        }

        // ===== Results Screen =====
        // Shows score, stars, and options to replay, go to next level, or return to map
        composable<ResultsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ResultsRoute>()
            ResultsScreen(
                levelNumber = route.levelNumber,
                score = route.score,
                stars = route.stars,
                won = route.won,
                onPlayAgain = {
                    navController.navigate(GameRoute(route.levelNumber)) {
                        popUpTo(LevelMapRoute)
                    }
                },
                onNextLevel = {
                    navController.navigate(GameRoute(route.levelNumber + 1)) {
                        popUpTo(LevelMapRoute)
                    }
                },
                onBackToMap = {
                    navController.navigate(LevelMapRoute) {
                        popUpTo(LevelMapRoute) { inclusive = true }
                    }
                }
            )
        }
    }
}
