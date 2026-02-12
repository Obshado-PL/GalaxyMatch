package com.galaxymatch.game.data

import com.galaxymatch.game.engine.LevelGenerator
import com.galaxymatch.game.model.LevelConfig

/**
 * Repository that provides level configurations.
 *
 * For levels 1-20: reads from the hardcoded LevelDataSource.
 * For levels 21+: generates procedurally via LevelGenerator.
 *
 * The generator is deterministic (seeded by level number) so the
 * same level always produces the same configuration.
 */
class LevelRepository(
    private val dataSource: LevelDataSource,
    private val levelGenerator: LevelGenerator = LevelGenerator()
) {

    /**
     * Get the configuration for a specific level.
     *
     * Levels 1-20 come from the handcrafted data source.
     * Levels 21+ are procedurally generated (infinite content!).
     *
     * @param levelNumber The level number (1-based)
     * @return The level config (never null for valid level numbers)
     */
    fun getLevel(levelNumber: Int): LevelConfig? {
        // First, try the handcrafted levels
        val hardcoded = dataSource.levels.find { it.levelNumber == levelNumber }
        if (hardcoded != null) return hardcoded

        // For levels beyond the handcrafted set, generate procedurally
        if (levelNumber > 0) {
            return levelGenerator.generate(levelNumber)
        }

        return null
    }

    /**
     * Get all handcrafted levels.
     * Used for the level map's initial display (levels 1-20).
     * Procedurally generated levels are fetched individually via getLevel().
     */
    fun getAllLevels(): List<LevelConfig> {
        return dataSource.levels
    }

    /**
     * Get the total number of handcrafted levels.
     * Beyond this, levels are generated procedurally.
     */
    fun getHandcraftedLevelCount(): Int {
        return dataSource.levels.size
    }

    /**
     * Generate level configs for a range of levels.
     * Used by the level map to display levels beyond the handcrafted set.
     *
     * @param fromLevel Start level number (inclusive)
     * @param toLevel End level number (inclusive)
     * @return List of level configs for the range
     */
    fun getLevelsInRange(fromLevel: Int, toLevel: Int): List<LevelConfig> {
        return (fromLevel..toLevel).mapNotNull { getLevel(it) }
    }
}
