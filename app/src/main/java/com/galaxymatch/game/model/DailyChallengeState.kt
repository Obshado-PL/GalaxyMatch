package com.galaxymatch.game.model

/**
 * State of the player's daily challenge progress.
 *
 * Tracks whether today's challenge has been completed, the current streak
 * (consecutive days played), and the best daily score.
 *
 * @param lastPlayedDate ISO date string (yyyy-MM-dd) of when the last daily was completed
 * @param currentStreak How many consecutive days the player has completed the daily
 * @param bestDailyScore Highest score ever achieved on a daily challenge
 * @param todayCompleted Whether today's challenge has already been completed
 * @param totalDailiesCompleted Total number of daily challenges completed all-time
 */
data class DailyChallengeState(
    val lastPlayedDate: String = "",
    val currentStreak: Int = 0,
    val bestDailyScore: Int = 0,
    val todayCompleted: Boolean = false,
    val totalDailiesCompleted: Int = 0
)
