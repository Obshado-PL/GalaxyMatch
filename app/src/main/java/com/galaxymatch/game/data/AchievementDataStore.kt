package com.galaxymatch.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore persistence for achievement unlock state.
 *
 * Each achievement is stored as a boolean key: "achievement_{id}" → true.
 * Only unlocked achievements have entries (keeps storage sparse).
 */
private val Context.achievementDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "achievements"
)

class AchievementDataStore(private val context: Context) {

    /**
     * Get the set of unlocked achievement IDs.
     */
    fun getUnlockedIds(): Flow<Set<String>> {
        return context.achievementDataStore.data.map { prefs ->
            prefs.asMap()
                .filter { (key, value) -> key.name.startsWith("ach_") && value == true }
                .map { it.key.name.removePrefix("ach_") }
                .toSet()
        }
    }

    /**
     * Mark an achievement as unlocked.
     */
    suspend fun unlockAchievement(id: String) {
        context.achievementDataStore.edit { prefs ->
            prefs[booleanPreferencesKey("ach_$id")] = true
        }
    }

    /**
     * Clear all achievement data (used on progress reset).
     */
    suspend fun clearAll() {
        context.achievementDataStore.edit { it.clear() }
    }
}
