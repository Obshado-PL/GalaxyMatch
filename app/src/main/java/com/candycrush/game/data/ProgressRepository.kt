package com.candycrush.game.data

import com.candycrush.game.model.PlayerProgress
import kotlinx.coroutines.flow.Flow

/**
 * Repository for player progress.
 *
 * This is a thin wrapper around ProgressDataStore that provides
 * a clean API for the rest of the app. Having this layer makes
 * it easy to swap the data source later (e.g., to a server-based
 * storage for cloud saves).
 */
class ProgressRepository(private val dataStore: ProgressDataStore) {

    /**
     * Save the player's result for a level.
     * Only updates if the new result is better than the existing one.
     */
    suspend fun saveProgress(levelNumber: Int, stars: Int, score: Int) {
        dataStore.saveProgress(levelNumber, stars, score)
    }

    /**
     * Get the player's progress as a Flow.
     * The Flow emits new values whenever progress is updated.
     */
    fun getProgress(): Flow<PlayerProgress> {
        return dataStore.getProgress()
    }
}
