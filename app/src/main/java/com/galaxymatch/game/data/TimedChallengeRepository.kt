package com.galaxymatch.game.data

import com.galaxymatch.game.model.TimedChallengeState
import com.galaxymatch.game.model.TimedDifficulty
import kotlinx.coroutines.flow.Flow

/**
 * Repository for the timed challenge mode.
 * Thin wrapper around the DataStore.
 */
class TimedChallengeRepository(private val dataStore: TimedChallengeDataStore) {

    fun getState(): Flow<TimedChallengeState> = dataStore.getState()

    suspend fun saveBestScore(difficulty: TimedDifficulty, score: Int) {
        dataStore.saveBestScore(difficulty, score)
    }

    suspend fun clearAll() {
        dataStore.clearAll()
    }
}
