package com.galaxymatch.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.galaxymatch.game.model.TimedChallengeState
import com.galaxymatch.game.model.TimedDifficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore persistence for timed challenge best scores.
 */
private val Context.timedChallengeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "timed_challenge"
)

class TimedChallengeDataStore(private val context: Context) {

    companion object {
        private val BEST_EASY = intPreferencesKey("best_easy")
        private val BEST_MEDIUM = intPreferencesKey("best_medium")
        private val BEST_HARD = intPreferencesKey("best_hard")
        private val TOTAL_GAMES = intPreferencesKey("total_timed_games")
    }

    /**
     * Get the current timed challenge state.
     */
    fun getState(): Flow<TimedChallengeState> {
        return context.timedChallengeDataStore.data.map { prefs ->
            TimedChallengeState(
                bestScoreEasy = prefs[BEST_EASY] ?: 0,
                bestScoreMedium = prefs[BEST_MEDIUM] ?: 0,
                bestScoreHard = prefs[BEST_HARD] ?: 0,
                totalTimedGames = prefs[TOTAL_GAMES] ?: 0
            )
        }
    }

    /**
     * Save a new best score for a difficulty tier.
     * Only updates if the new score is higher than the existing best.
     */
    suspend fun saveBestScore(difficulty: TimedDifficulty, score: Int) {
        context.timedChallengeDataStore.edit { prefs ->
            val key = when (difficulty) {
                TimedDifficulty.Easy -> BEST_EASY
                TimedDifficulty.Medium -> BEST_MEDIUM
                TimedDifficulty.Hard -> BEST_HARD
            }
            val currentBest = prefs[key] ?: 0
            if (score > currentBest) {
                prefs[key] = score
            }
            prefs[TOTAL_GAMES] = (prefs[TOTAL_GAMES] ?: 0) + 1
        }
    }

    /**
     * Clear all timed challenge data (used on progress reset).
     */
    suspend fun clearAll() {
        context.timedChallengeDataStore.edit { it.clear() }
    }
}
