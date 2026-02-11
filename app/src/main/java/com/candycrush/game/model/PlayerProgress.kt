package com.candycrush.game.model

/**
 * Saved progress for the player across all levels.
 *
 * This data is persisted to disk using DataStore so it survives
 * app restarts. It tracks:
 * - Which levels the player has unlocked
 * - The best star rating for each completed level
 * - The highest score achieved on each level
 *
 * @param highestUnlockedLevel The highest level number the player can play
 *                             (new players start at 1, completing a level unlocks the next)
 * @param levelStars Map of levelNumber -> best star count (1, 2, or 3)
 * @param levelScores Map of levelNumber -> best score achieved
 */
data class PlayerProgress(
    val highestUnlockedLevel: Int = 1,
    val levelStars: Map<Int, Int> = emptyMap(),
    val levelScores: Map<Int, Int> = emptyMap()
)
