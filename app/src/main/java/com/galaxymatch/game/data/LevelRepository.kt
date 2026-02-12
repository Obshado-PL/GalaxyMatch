package com.galaxymatch.game.data

import com.galaxymatch.game.model.LevelConfig

/**
 * Repository that provides level configurations.
 *
 * Currently reads from a hardcoded data source (LevelDataSource),
 * but this abstraction makes it easy to switch to loading levels
 * from a file or server in the future.
 */
class LevelRepository(private val dataSource: LevelDataSource) {

    /**
     * Get the configuration for a specific level.
     * @param levelNumber The level number (1-based)
     * @return The level config, or null if the level doesn't exist
     */
    fun getLevel(levelNumber: Int): LevelConfig? {
        return dataSource.levels.find { it.levelNumber == levelNumber }
    }

    /**
     * Get all available levels.
     */
    fun getAllLevels(): List<LevelConfig> {
        return dataSource.levels
    }

    /**
     * Get the total number of levels in the game.
     */
    fun getTotalLevelCount(): Int {
        return dataSource.levels.size
    }
}
