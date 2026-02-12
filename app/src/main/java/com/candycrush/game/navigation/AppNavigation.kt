package com.candycrush.game.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.candycrush.game.ui.game.GameScreen
import com.candycrush.game.ui.levelmap.LevelMapScreen
import com.candycrush.game.ui.results.ResultsScreen
import com.candycrush.game.ui.settings.SettingsScreen
import com.candycrush.game.ui.splash.SplashScreen

/** Duration for all screen transitions (300ms with smooth deceleration) */
private const val NAV_ANIM_DURATION = 300

/**
 * The main navigation graph for the app.
 *
 * This defines all the screens and how the user can navigate between them.
 * It uses Navigation Compose with type-safe routes — each route is a
 * @Serializable class defined in Screens.kt.
 *
 * NavHost is the container that displays the current screen.
 * NavController is used to navigate between screens.
 *
 * === Screen Transitions ===
 * - Forward navigation: new screen slides in from the right
 * - Back navigation: current screen slides out to the right
 * - The underlying screen shifts slightly (1/3 width) for a parallax effect
 * - Splash → LevelMap: crossfade (smooth handoff)
 * - Game → Results: crossfade (dramatic game-ending moment)
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = SplashRoute,
        // === Global defaults ===
        // New screen slides in from the right (full width)
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing)
            )
        },
        // Current screen shifts slightly left when something pushes on top (parallax)
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing)
            )
        },
        // When popping, the revealed screen slides back in from the left
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing)
            )
        },
        // When popping, the top screen slides out to the right
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing)
            )
        }
    ) {
        // ===== Splash Screen =====
        // Shown briefly when the app opens, then auto-navigates to the level map.
        // Fades out instead of sliding (smooth handoff to level map).
        composable<SplashRoute>(
            enterTransition = { EnterTransition.None },
            exitTransition = {
                fadeOut(animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing))
            }
        ) {
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
        // Shows all levels with their star ratings, player taps a level to play.
        // Fades in from splash; uses default slide when returning from Game/Settings.
        composable<LevelMapRoute>(
            enterTransition = {
                fadeIn(animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing))
            }
        ) {
            LevelMapScreen(
                onLevelSelected = { levelNumber ->
                    navController.navigate(GameRoute(levelNumber))
                },
                onSettingsClicked = {
                    navController.navigate(SettingsRoute)
                }
            )
        }

        // ===== Settings Screen =====
        // Sound toggles and progress reset.
        // Uses global defaults: slides in from right, slides out to right.
        composable<SettingsRoute> {
            SettingsScreen(
                onBackToMap = {
                    navController.popBackStack(LevelMapRoute, inclusive = false)
                }
            )
        }

        // ===== Game Screen =====
        // The main match-3 gameplay screen.
        // Fades out when going to Results (dramatic moment); slides out on pop (back to map).
        composable<GameRoute>(
            exitTransition = {
                fadeOut(animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing))
            }
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<GameRoute>()
            GameScreen(
                levelNumber = route.levelNumber,
                onGameEnd = { score, stars, won, objectiveText ->
                    navController.navigate(
                        ResultsRoute(
                            levelNumber = route.levelNumber,
                            score = score,
                            stars = stars,
                            won = won,
                            objectiveText = objectiveText
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
        // Shows score, stars, and options to replay, go to next level, or return to map.
        // Fades in from Game (complements Game's fadeOut); slides out on forward navigation.
        composable<ResultsRoute>(
            enterTransition = {
                fadeIn(animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing))
            }
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<ResultsRoute>()
            ResultsScreen(
                levelNumber = route.levelNumber,
                score = route.score,
                stars = route.stars,
                won = route.won,
                objectiveText = route.objectiveText,
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
