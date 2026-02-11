package com.candycrush.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.candycrush.game.model.PlayerProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property to create a single DataStore instance for the app
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "player_progress"
)

/**
 * Handles reading and writing player progress to persistent storage.
 *
 * Uses Android's DataStore (Preferences) which is a modern replacement for
 * SharedPreferences. It stores simple key-value pairs:
 * - "highest_unlocked_level" -> Int
 * - "level_3_stars" -> Int (stars earned on level 3)
 * - "level_3_score" -> Int (best score on level 3)
 *
 * DataStore is asynchronous and uses Kotlin coroutines + Flow, making it
 * safe to use without blocking the main thread.
 */
class ProgressDataStore(private val context: Context) {

    companion object {
        private val HIGHEST_LEVEL_KEY = intPreferencesKey("highest_unlocked_level")
        private const val MAX_LEVELS = 50 // Maximum number of levels to scan when loading
    }

    /**
     * Save the player's result for a level.
     * Only updates if the new result is better than the existing one.
     */
    suspend fun saveProgress(levelNumber: Int, stars: Int, score: Int) {
        context.dataStore.edit { prefs ->
            val starsKey = intPreferencesKey("level_${levelNumber}_stars")
            val scoreKey = intPreferencesKey("level_${levelNumber}_score")

            // Only save if better than existing record
            val existingStars = prefs[starsKey] ?: 0
            if (stars > existingStars) {
                prefs[starsKey] = stars
            }

            val existingScore = prefs[scoreKey] ?: 0
            if (score > existingScore) {
                prefs[scoreKey] = score
            }

            // Unlock the next level if the player earned at least 1 star
            val currentHighest = prefs[HIGHEST_LEVEL_KEY] ?: 1
            if (stars >= 1 && levelNumber + 1 > currentHighest) {
                prefs[HIGHEST_LEVEL_KEY] = levelNumber + 1
            }
        }
    }

    /**
     * Get the player's progress as a Flow.
     *
     * A Flow is like a stream of data — it automatically emits new values
     * whenever the underlying data changes. The UI observes this Flow
     * and updates automatically when progress is saved.
     */
    fun getProgress(): Flow<PlayerProgress> {
        return context.dataStore.data.map { prefs ->
            val highest = prefs[HIGHEST_LEVEL_KEY] ?: 1
            val levelStars = mutableMapOf<Int, Int>()
            val levelScores = mutableMapOf<Int, Int>()

            for (i in 1..MAX_LEVELS) {
                prefs[intPreferencesKey("level_${i}_stars")]?.let { levelStars[i] = it }
                prefs[intPreferencesKey("level_${i}_score")]?.let { levelScores[i] = it }
            }

            PlayerProgress(
                highestUnlockedLevel = highest,
                levelStars = levelStars,
                levelScores = levelScores
            )
        }
    }
}
