package com.galaxymatch.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.galaxymatch.game.model.GemType
import com.galaxymatch.game.model.StatisticsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.statisticsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "player_statistics"
)

/**
 * Handles reading and writing aggregate gameplay statistics.
 *
 * Stores cumulative counters (total games, gems, combos, etc.) that are
 * incremented after each level attempt. Uses a separate DataStore from
 * player progress and settings.
 */
class StatisticsDataStore(private val context: Context) {

    companion object {
        private val TOTAL_GAMES_KEY = intPreferencesKey("total_games")
        private val TOTAL_GEMS_KEY = intPreferencesKey("total_gems")
        private val BEST_COMBO_KEY = intPreferencesKey("best_combo")
        private val TOTAL_SCORE_KEY = longPreferencesKey("total_score")
        private val SPECIAL_GEMS_KEY = intPreferencesKey("special_gems")
        private val POWERUPS_USED_KEY = intPreferencesKey("powerups_used")

        /** Get the preference key for a specific gem color's match count. */
        private fun colorKey(gemType: GemType) =
            intPreferencesKey("color_${gemType.name.lowercase()}_count")
    }

    /**
     * Increment statistics after a level attempt (win or lose).
     * All values are additive except bestCombo which uses max.
     */
    suspend fun incrementStats(
        gemsMatched: Int,
        comboReached: Int,
        scoreGained: Int,
        gemColorCounts: Map<GemType, Int>,
        specialGemsCreated: Int,
        powerUpsUsed: Int
    ) {
        context.statisticsDataStore.edit { prefs ->
            prefs[TOTAL_GAMES_KEY] = (prefs[TOTAL_GAMES_KEY] ?: 0) + 1
            prefs[TOTAL_GEMS_KEY] = (prefs[TOTAL_GEMS_KEY] ?: 0) + gemsMatched
            prefs[TOTAL_SCORE_KEY] = (prefs[TOTAL_SCORE_KEY] ?: 0L) + scoreGained
            prefs[SPECIAL_GEMS_KEY] = (prefs[SPECIAL_GEMS_KEY] ?: 0) + specialGemsCreated
            prefs[POWERUPS_USED_KEY] = (prefs[POWERUPS_USED_KEY] ?: 0) + powerUpsUsed

            // Best combo: only update if this game's max was higher
            val existingBest = prefs[BEST_COMBO_KEY] ?: 0
            if (comboReached > existingBest) {
                prefs[BEST_COMBO_KEY] = comboReached
            }

            // Per-color gem counts
            for ((gemType, count) in gemColorCounts) {
                val key = colorKey(gemType)
                prefs[key] = (prefs[key] ?: 0) + count
            }
        }
    }

    /** Get statistics as a reactive Flow. */
    fun getStatistics(): Flow<StatisticsState> {
        return context.statisticsDataStore.data.map { prefs ->
            val colorCounts = mutableMapOf<GemType, Int>()
            for (gemType in GemType.entries) {
                val count = prefs[colorKey(gemType)] ?: 0
                if (count > 0) colorCounts[gemType] = count
            }

            StatisticsState(
                totalGamesPlayed = prefs[TOTAL_GAMES_KEY] ?: 0,
                totalGemsMatched = prefs[TOTAL_GEMS_KEY] ?: 0,
                bestCombo = prefs[BEST_COMBO_KEY] ?: 0,
                totalScore = prefs[TOTAL_SCORE_KEY] ?: 0L,
                specialGemsCreated = prefs[SPECIAL_GEMS_KEY] ?: 0,
                powerUpsUsed = prefs[POWERUPS_USED_KEY] ?: 0,
                gemColorCounts = colorCounts
            )
        }
    }

    /** Clear all statistics (used when resetting progress). */
    suspend fun clearAll() {
        context.statisticsDataStore.edit { it.clear() }
    }
}
