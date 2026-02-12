package com.galaxymatch.game.data

import com.galaxymatch.game.model.GemType
import com.galaxymatch.game.model.StatisticsState
import kotlinx.coroutines.flow.Flow

/**
 * Repository for aggregate gameplay statistics.
 * Thin wrapper around [StatisticsDataStore] following the same pattern
 * as [ProgressRepository] and [SettingsRepository].
 */
class StatisticsRepository(private val dataStore: StatisticsDataStore) {

    fun getStatistics(): Flow<StatisticsState> = dataStore.getStatistics()

    suspend fun incrementStats(
        gemsMatched: Int,
        comboReached: Int,
        scoreGained: Int,
        gemColorCounts: Map<GemType, Int>,
        specialGemsCreated: Int,
        powerUpsUsed: Int
    ) = dataStore.incrementStats(
        gemsMatched, comboReached, scoreGained,
        gemColorCounts, specialGemsCreated, powerUpsUsed
    )

    suspend fun clearAll() = dataStore.clearAll()
}
