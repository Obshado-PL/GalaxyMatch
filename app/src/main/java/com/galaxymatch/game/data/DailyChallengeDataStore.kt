package com.galaxymatch.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.galaxymatch.game.model.DailyChallengeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * DataStore persistence for daily challenge state.
 *
 * Stores the streak, best score, and completion status.
 * Automatically resets todayCompleted when the date changes.
 */
private val Context.dailyChallengeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "daily_challenge"
)

class DailyChallengeDataStore(private val context: Context) {

    companion object {
        private val LAST_PLAYED_DATE = stringPreferencesKey("last_played_date")
        private val CURRENT_STREAK = intPreferencesKey("current_streak")
        private val BEST_DAILY_SCORE = intPreferencesKey("best_daily_score")
        private val TOTAL_DAILIES_COMPLETED = intPreferencesKey("total_dailies_completed")
    }

    /**
     * Get the current daily challenge state.
     * Automatically detects date changes and resets todayCompleted.
     */
    fun getState(): Flow<DailyChallengeState> {
        val today = LocalDate.now().toString()
        return context.dailyChallengeDataStore.data.map { prefs ->
            val lastPlayed = prefs[LAST_PLAYED_DATE] ?: ""
            val streak = prefs[CURRENT_STREAK] ?: 0
            val bestScore = prefs[BEST_DAILY_SCORE] ?: 0
            val totalCompleted = prefs[TOTAL_DAILIES_COMPLETED] ?: 0

            // Check if today has been completed
            val todayCompleted = lastPlayed == today

            // Check if streak should be reset (missed a day)
            val activeStreak = if (lastPlayed.isNotEmpty()) {
                try {
                    val lastDate = LocalDate.parse(lastPlayed)
                    val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(lastDate, LocalDate.now())
                    if (daysBetween <= 1) streak else 0
                } catch (e: Exception) {
                    0
                }
            } else {
                0
            }

            DailyChallengeState(
                lastPlayedDate = lastPlayed,
                currentStreak = activeStreak,
                bestDailyScore = bestScore,
                todayCompleted = todayCompleted,
                totalDailiesCompleted = totalCompleted
            )
        }
    }

    /**
     * Mark today's challenge as completed and update streak/score.
     */
    suspend fun markCompleted(score: Int) {
        val today = LocalDate.now().toString()
        context.dailyChallengeDataStore.edit { prefs ->
            val lastPlayed = prefs[LAST_PLAYED_DATE] ?: ""
            val currentStreak = prefs[CURRENT_STREAK] ?: 0

            // Calculate new streak
            val newStreak = if (lastPlayed.isNotEmpty()) {
                try {
                    val lastDate = LocalDate.parse(lastPlayed)
                    val yesterday = LocalDate.now().minusDays(1)
                    if (lastDate == yesterday) currentStreak + 1 else 1
                } catch (e: Exception) {
                    1
                }
            } else {
                1
            }

            prefs[LAST_PLAYED_DATE] = today
            prefs[CURRENT_STREAK] = newStreak
            prefs[TOTAL_DAILIES_COMPLETED] = (prefs[TOTAL_DAILIES_COMPLETED] ?: 0) + 1

            // Update best score if this is a new high
            val currentBest = prefs[BEST_DAILY_SCORE] ?: 0
            if (score > currentBest) {
                prefs[BEST_DAILY_SCORE] = score
            }
        }
    }

    /**
     * Clear all daily challenge data (used on progress reset).
     */
    suspend fun clearAll() {
        context.dailyChallengeDataStore.edit { it.clear() }
    }
}
