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

/** The statistics screen showing aggregate gameplay stats */
@Serializable
object StatisticsRoute

/** The daily challenge screen showing streak, stats, and play button */
@Serializable
object DailyChallengeRoute

/** The achievements screen showing all 30 achievements and progress */
@Serializable
object AchievementsRoute

/** The timed challenge selection screen with 3 difficulty tiers */
@Serializable
object TimedChallengeRoute

/** The help screen explaining game mechanics, obstacles, and power-ups */
@Serializable
object HelpRoute

/** The results screen shown after completing or failing a level */
@Serializable
data class ResultsRoute(
    val levelNumber: Int,
    val score: Int,
    val stars: Int,
    val won: Boolean,
    /** Objective status text (e.g., "All ice broken!" or "Red: 20/25"). Empty = score-only level. */
    val objectiveText: String = "",
    /** Whether this score is a new personal best for this level. */
    val isNewHighScore: Boolean = false
)
