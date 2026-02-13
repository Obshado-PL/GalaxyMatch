package com.galaxymatch.game.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.galaxymatch.game.ui.game.GameScreen
import com.galaxymatch.game.ui.levelmap.LevelMapScreen
import com.galaxymatch.game.ui.results.ResultsScreen
import com.galaxymatch.game.ui.settings.SettingsScreen
import com.galaxymatch.game.ui.splash.SplashScreen
import com.galaxymatch.game.ui.statistics.StatisticsScreen
import com.galaxymatch.game.ui.dailychallenge.DailyChallengeScreen
import com.galaxymatch.game.ui.achievements.AchievementsScreen
import com.galaxymatch.game.ui.timedchallenge.TimedChallengeScreen
import com.galaxymatch.game.ui.help.HelpScreen

/** Duration for all screen transitions (400ms with smooth deceleration) */
private const val NAV_ANIM_DURATION = 400

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
 * - LevelMap → Game: zoom effect (scale out map + scale in game) — dramatic "entering level" feel
 * - Game → Results: zoom effect (scale out game + scale in results) — dramatic end moment
 * - Pop transitions stay as slides for natural "going back" feel
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
        // Fades in from splash. Zooms out (scale 1.0→0.85 + fade) when entering a game level
        // for a dramatic "diving into the level" effect. Default slide for other transitions.
        composable<LevelMapRoute>(
            enterTransition = {
                fadeIn(animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                scaleOut(
                    targetScale = 0.85f,
                    animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing))
            }
        ) {
            LevelMapScreen(
                onLevelSelected = { levelNumber ->
                    navController.navigate(GameRoute(levelNumber))
                },
                onSettingsClicked = {
                    navController.navigate(SettingsRoute)
                },
                onStatsClicked = {
                    navController.navigate(StatisticsRoute)
                },
                onDailyChallengeClicked = {
                    navController.navigate(DailyChallengeRoute)
                },
                onAchievementsClicked = {
                    navController.navigate(AchievementsRoute)
                },
                onTimedChallengeClicked = {
                    navController.navigate(TimedChallengeRoute)
                },
                onHelpClicked = {
                    navController.navigate(HelpRoute)
                }
            )
        }

        // ===== Daily Challenge Screen =====
        composable<DailyChallengeRoute> {
            DailyChallengeScreen(
                onPlayChallenge = {
                    // levelNumber = -1 is the sentinel for daily challenge
                    navController.navigate(GameRoute(-1))
                },
                onBack = {
                    navController.popBackStack(LevelMapRoute, inclusive = false)
                }
            )
        }

        // ===== Achievements Screen =====
        composable<AchievementsRoute> {
            AchievementsScreen(
                onBack = {
                    navController.popBackStack(LevelMapRoute, inclusive = false)
                }
            )
        }

        // ===== Timed Challenge Screen =====
        composable<TimedChallengeRoute> {
            TimedChallengeScreen(
                onStartTimed = { difficultyOrdinal ->
                    // levelNumber = -(100 + difficultyOrdinal) is the sentinel for timed mode
                    navController.navigate(GameRoute(-(100 + difficultyOrdinal)))
                },
                onBack = {
                    navController.popBackStack(LevelMapRoute, inclusive = false)
                }
            )
        }

        // ===== Help Screen =====
        composable<HelpRoute> {
            HelpScreen(
                onBack = {
                    navController.popBackStack(LevelMapRoute, inclusive = false)
                }
            )
        }

        // ===== Statistics Screen =====
        composable<StatisticsRoute> {
            StatisticsScreen(
                onBackToMap = {
                    navController.popBackStack(LevelMapRoute, inclusive = false)
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
        // Enters with zoom-in (scale 0.9→1.0 + fade) for a dramatic "entering the level" effect.
        // Exits to Results with zoom-out (scale 1.0→0.85 + fade) for a dramatic end moment.
        // Pop back to map uses default slide for natural feel.
        composable<GameRoute>(
            enterTransition = {
                scaleIn(
                    initialScale = 0.9f,
                    animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                scaleOut(
                    targetScale = 0.85f,
                    animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing))
            }
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<GameRoute>()
            GameScreen(
                levelNumber = route.levelNumber,
                onGameEnd = { score, stars, won, objectiveText, isNewHighScore ->
                    navController.navigate(
                        ResultsRoute(
                            levelNumber = route.levelNumber,
                            score = score,
                            stars = stars,
                            won = won,
                            objectiveText = objectiveText,
                            isNewHighScore = isNewHighScore
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
        // Zooms in (scale 0.85→1.0 + fade) from Game for a dramatic reveal moment.
        composable<ResultsRoute>(
            enterTransition = {
                scaleIn(
                    initialScale = 0.85f,
                    animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION, easing = FastOutSlowInEasing))
            }
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<ResultsRoute>()
            ResultsScreen(
                levelNumber = route.levelNumber,
                score = route.score,
                stars = route.stars,
                won = route.won,
                objectiveText = route.objectiveText,
                isNewHighScore = route.isNewHighScore,
                onPlayAgain = {
                    navController.navigate(GameRoute(route.levelNumber)) {
                        popUpTo(LevelMapRoute)
                    }
                },
                onNextLevel = {
                    // Guard: only navigate to next level for normal levels (positive IDs).
                    // Special modes use negative sentinels (-1 = daily, -100+ = timed).
                    if (route.levelNumber > 0) {
                        navController.navigate(GameRoute(route.levelNumber + 1)) {
                            popUpTo(LevelMapRoute)
                        }
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
