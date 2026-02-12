package com.galaxymatch.game.model

/**
 * State of timed challenge best scores.
 *
 * Each difficulty tier has its own best score record.
 */
data class TimedChallengeState(
    val bestScoreEasy: Int = 0,
    val bestScoreMedium: Int = 0,
    val bestScoreHard: Int = 0,
    val totalTimedGames: Int = 0
)

/**
 * Timed challenge difficulty tiers.
 *
 * Each tier has a different starting time, making harder difficulties
 * require more efficient play and combo-chaining for time bonuses.
 *
 * @param seconds Starting countdown time in seconds
 * @param label Display name for the difficulty
 */
enum class TimedDifficulty(val seconds: Int, val label: String) {
    Easy(120, "Easy"),
    Medium(90, "Medium"),
    Hard(60, "Hard")
}
