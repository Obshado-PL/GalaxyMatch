package com.galaxymatch.game.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for the app.
 *
 * Each object/class here represents a screen (destination) in the app.
 * Using @Serializable with Navigation Compose gives us type-safe
 * argument passing between screens — no more string-based routes!
 *
 * Screen flow:
 *   Splash → LevelMap → Game(levelNumber) → Results(levelNumber, score, stars, won)
 *                ↑                                         |
 *                └─────────────────────────────────────────┘
 */

/** The splash/loading screen shown when the app first opens */
@Serializable
object SplashRoute

/** The level map where the player selects which level to play */
@Serializable
object LevelMapRoute

/** The main game screen where the match-3 gameplay happens */
@Serializable
data class GameRoute(val levelNumber: Int)

/** The settings screen for sound toggles and progress reset */
@Serializable
object SettingsRoute

/** The results screen shown after completing or failing a level */
@Serializable
data class ResultsRoute(
    val levelNumber: Int,
    val score: Int,
    val stars: Int,
    val won: Boolean,
    /** Objective status text (e.g., "All ice broken!" or "Red: 20/25"). Empty = score-only level. */
    val objectiveText: String = ""
)
