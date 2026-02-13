package com.galaxymatch.game.data

import com.galaxymatch.game.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Repository for the achievement system.
 *
 * Wraps the DataStore and provides evaluation logic to check
 * which achievements should be unlocked based on current statistics.
 */
class AchievementRepository(private val dataStore: AchievementDataStore) {

    /**
     * Get the set of unlocked achievement IDs.
     */
    fun getUnlockedIds(): Flow<Set<String>> = dataStore.getUnlockedIds()

    /**
     * Evaluate all achievements against current game data and unlock any that are met.
     *
     * @param stats The player's current statistics
     * @param progress The player's current level progress
     * @param dailyState The player's daily challenge state
     * @return List of newly unlocked achievement IDs (empty if none)
     */
    suspend fun checkAndUnlockAchievements(
        stats: StatisticsState,
        progress: PlayerProgress,
        dailyState: DailyChallengeState
    ): List<String> {
        val alreadyUnlocked = dataStore.getUnlockedIds().first()
        val newlyUnlocked = mutableListOf<String>()

        for (achievement in AchievementDefinitions.all) {
            if (achievement.id in alreadyUnlocked) continue

            val isMet = evaluateCriteria(achievement.criteria, stats, progress, dailyState)
            if (isMet) {
                dataStore.unlockAchievement(achievement.id)
                newlyUnlocked.add(achievement.id)
            }
        }

        return newlyUnlocked
    }

    /**
     * Evaluate whether a single achievement criteria is met.
     */
    private fun evaluateCriteria(
        criteria: AchievementCriteria,
        stats: StatisticsState,
        progress: PlayerProgress,
        dailyState: DailyChallengeState
    ): Boolean {
        return when (criteria) {
            is AchievementCriteria.GamesPlayed ->
                stats.totalGamesPlayed >= criteria.threshold
            is AchievementCriteria.GemsMatched ->
                stats.totalGemsMatched >= criteria.threshold
            is AchievementCriteria.ComboReached ->
                stats.bestCombo >= criteria.threshold
            is AchievementCriteria.TotalScore ->
                stats.totalScore >= criteria.threshold
            is AchievementCriteria.LevelsCompleted ->
                progress.levelStars.size >= criteria.threshold
            is AchievementCriteria.PerfectLevels ->
                progress.levelStars.count { it.value >= 3 } >= criteria.count
            is AchievementCriteria.SpecialGemsCreated ->
                stats.specialGemsCreated >= criteria.threshold
            is AchievementCriteria.PowerUpsUsed ->
                stats.powerUpsUsed >= criteria.threshold
            is AchievementCriteria.DailyChallengeStreak ->
                dailyState.currentStreak >= criteria.threshold
            is AchievementCriteria.DailyChallengesCompleted ->
                dailyState.totalDailiesCompleted >= criteria.threshold
        }
    }

    /**
     * Get the progress value for a specific criteria (for progress display).
     */
    fun getProgress(
        criteria: AchievementCriteria,
        stats: StatisticsState,
        progress: PlayerProgress,
        dailyState: DailyChallengeState
    ): Pair<Int, Int> {
        return when (criteria) {
            is AchievementCriteria.GamesPlayed ->
                stats.totalGamesPlayed to criteria.threshold
            is AchievementCriteria.GemsMatched ->
                stats.totalGemsMatched to criteria.threshold
            is AchievementCriteria.ComboReached ->
                stats.bestCombo to criteria.threshold
            is AchievementCriteria.TotalScore ->
                stats.totalScore.coerceAtMost(Int.MAX_VALUE.toLong()).toInt() to
                    criteria.threshold.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            is AchievementCriteria.LevelsCompleted ->
                progress.levelStars.size to criteria.threshold
            is AchievementCriteria.PerfectLevels ->
                progress.levelStars.count { it.value >= 3 } to criteria.count
            is AchievementCriteria.SpecialGemsCreated ->
                stats.specialGemsCreated to criteria.threshold
            is AchievementCriteria.PowerUpsUsed ->
                stats.powerUpsUsed to criteria.threshold
            is AchievementCriteria.DailyChallengeStreak ->
                dailyState.currentStreak to criteria.threshold
            is AchievementCriteria.DailyChallengesCompleted ->
                dailyState.totalDailiesCompleted to criteria.threshold
        }
    }

    /**
     * Clear all achievement data (for progress reset).
     */
    suspend fun clearAll() {
        dataStore.clearAll()
    }
}
