package com.galaxymatch.game.data

import com.galaxymatch.game.engine.LevelGenerator
import com.galaxymatch.game.model.DailyChallengeState
import com.galaxymatch.game.model.LevelConfig
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository for the daily challenge feature.
 *
 * Wraps the DataStore and adds level generation logic.
 * Each day gets a unique, deterministic level based on the date.
 *
 * Day-of-week difficulty scaling:
 * - Monday: Easy (7×7, 5 colors, 25 moves)
 * - ...gradual increase...
 * - Sunday: Hard (9×9, 6 colors, 12 moves)
 */
class DailyChallengeRepository(
    private val dataStore: DailyChallengeDataStore,
    private val levelGenerator: LevelGenerator = LevelGenerator()
) {

    /**
     * Get the current daily challenge state (streak, completion, etc.)
     */
    fun getState(): Flow<DailyChallengeState> = dataStore.getState()

    /**
     * Generate today's daily challenge level.
     *
     * Uses the current date as a seed for deterministic generation.
     * The same date always produces the same level worldwide.
     * Day of week controls difficulty (Monday=easy, Sunday=hard).
     */
    fun generateTodayLevel(): LevelConfig {
        val today = LocalDate.now()
        val seed = today.toEpochDay().toInt()

        // Day of week: Monday=1, Sunday=7
        val dayOfWeek = today.dayOfWeek.value

        // Scale difficulty based on day of week
        val boardSize = when {
            dayOfWeek <= 2 -> 7  // Mon-Tue: Small board
            dayOfWeek <= 5 -> 8  // Wed-Fri: Medium board
            else -> 9            // Sat-Sun: Large board
        }
        val gemTypes = if (dayOfWeek <= 4) 5 else 6
        val maxMoves = when {
            dayOfWeek <= 2 -> 25  // Mon-Tue: Generous
            dayOfWeek <= 4 -> 20  // Wed-Thu: Standard
            dayOfWeek <= 6 -> 16  // Fri-Sat: Tight
            else -> 12            // Sun: Hard
        }

        // Use the generator to create a base level, then customize
        val baseLevel = levelGenerator.generate(seed.coerceAtLeast(21))

        return baseLevel.copy(
            levelNumber = -1, // Sentinel value for daily challenge
            rows = boardSize,
            cols = boardSize,
            maxMoves = maxMoves,
            availableGemTypes = gemTypes,
            targetScore = 5000 + dayOfWeek * 1000,
            twoStarScore = 8000 + dayOfWeek * 1500,
            threeStarScore = 12000 + dayOfWeek * 2000,
            description = "Daily Challenge — ${today.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}"
        )
    }

    /**
     * Mark today's challenge as completed.
     */
    suspend fun markCompleted(score: Int) {
        dataStore.markCompleted(score)
    }

    /**
     * Clear all daily challenge data (for progress reset).
     */
    suspend fun clearAll() {
        dataStore.clearAll()
    }
}
