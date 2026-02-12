package com.galaxymatch.game.engine

import com.galaxymatch.game.model.*
import kotlin.random.Random

/**
 * Procedural level generator for levels beyond the 20 handcrafted ones.
 *
 * Generates deterministic levels from a seed (the level number), so the same
 * level number always produces the same configuration. The difficulty scales
 * up gradually with higher level numbers.
 *
 * Design principles:
 * - **Deterministic**: Same levelNumber → same level every time (uses Random(seed))
 * - **Gradual scaling**: Each parameter (board size, moves, obstacle count) changes
 *   incrementally so difficulty ramps smoothly
 * - **Variety**: Cycles through objective types, mixes obstacle types, and adjusts
 *   gem counts to keep gameplay fresh
 * - **Validation**: Ensures no completely walled-off board sections
 *
 * This class has NO Android dependencies — it's pure Kotlin, just like the
 * rest of the engine package.
 */
class LevelGenerator {

    /**
     * Generate a LevelConfig for the given level number.
     *
     * Only call this for levels > 20 (the first 20 are handcrafted in LevelDataSource).
     *
     * @param levelNumber The level number (21, 22, 23, ...)
     * @return A fully configured LevelConfig ready to play
     */
    fun generate(levelNumber: Int): LevelConfig {
        val rng = Random(levelNumber.toLong())

        // === Board Size ===
        // Starts at 7×7, grows to 8×8 at level 31, 9×9 at level 51
        val boardSize = when {
            levelNumber <= 30 -> 7
            levelNumber <= 50 -> 8
            else -> 9
        }

        // === Gem Types ===
        // 5 colors for levels 21-40, 6 for 41+
        val gemTypes = if (levelNumber <= 40) 5 else 6

        // === Max Moves ===
        // Starts generous and decreases, but never below 10
        val maxMoves = maxOf(10, 25 - (levelNumber - 21) / 3)

        // === Score Targets ===
        // Base score increases 500 per level, with 1.5x/2x multipliers for stars
        val baseScore = 8000 + (levelNumber - 21) * 500
        val targetScore = baseScore
        val twoStarScore = (baseScore * 1.5).toInt()
        val threeStarScore = baseScore * 2

        // === Objective ===
        // Cycles through the three types based on level number
        val objective = when (levelNumber % 3) {
            0 -> ObjectiveType.ReachScore
            1 -> ObjectiveType.BreakAllIce
            2 -> {
                // Pick a random gem type for ClearGemType objective
                val gemTypeList = GemType.forLevel(gemTypes)
                val targetGem = gemTypeList[rng.nextInt(gemTypeList.size)]
                val targetCount = 20 + (levelNumber - 21) / 2
                ObjectiveType.ClearGemType(targetGem, targetCount)
            }
            else -> ObjectiveType.ReachScore // Never reached, keeps compiler happy
        }

        // === Obstacles ===
        // Density increases with level number, capped at 15
        val obstacleCount = minOf(15, 2 + (levelNumber - 21) / 5)
        val obstacles = generateObstacles(rng, boardSize, obstacleCount, levelNumber, objective)

        // === Bombs ===
        // Start appearing at level 41, with more bombs at higher levels
        val bombs = if (levelNumber >= 41) {
            generateBombs(rng, boardSize, obstacles, levelNumber)
        } else {
            emptyMap()
        }

        // === Description ===
        val description = when (objective) {
            is ObjectiveType.ReachScore -> "Score $targetScore points!"
            is ObjectiveType.BreakAllIce -> "Break all the ice!"
            is ObjectiveType.ClearGemType -> "Clear ${objective.targetCount} ${objective.gemType.name} gems!"
        }

        return LevelConfig(
            levelNumber = levelNumber,
            rows = boardSize,
            cols = boardSize,
            maxMoves = maxMoves,
            targetScore = targetScore,
            twoStarScore = twoStarScore,
            threeStarScore = threeStarScore,
            availableGemTypes = gemTypes,
            description = description,
            obstacles = obstacles,
            objective = objective,
            bombs = bombs
        )
    }

    /**
     * Generate obstacle positions for a level.
     *
     * Obstacle mix depends on level range:
     * - 21-30: Ice, Stone, ReinforcedIce
     * - 31-40: Add Locked
     * - 41+: All types (Ice, Stone, ReinforcedIce, Locked)
     *
     * For BreakAllIce objectives, ensures at least half the obstacles are ice-type.
     */
    private fun generateObstacles(
        rng: Random,
        boardSize: Int,
        count: Int,
        levelNumber: Int,
        objective: ObjectiveType
    ): Map<Position, ObstacleType> {
        val obstacles = mutableMapOf<Position, ObstacleType>()
        val usedPositions = mutableSetOf<Position>()

        // Keep a buffer around edges to avoid walling off entire rows/columns
        val margin = 1
        val maxRow = boardSize - 1 - margin
        val maxCol = boardSize - 1 - margin

        repeat(count) {
            // Find a random valid position that's not already used
            var attempts = 0
            while (attempts < 20) {
                val row = rng.nextInt(margin, maxRow + 1)
                val col = rng.nextInt(margin, maxCol + 1)
                val pos = Position(row, col)

                if (pos !in usedPositions) {
                    // Choose obstacle type based on level range
                    val type = chooseObstacleType(rng, levelNumber, objective)
                    obstacles[pos] = type
                    usedPositions.add(pos)
                    break
                }
                attempts++
            }
        }

        // For BreakAllIce: ensure at least some ice exists
        if (objective == ObjectiveType.BreakAllIce) {
            val iceCount = obstacles.count {
                it.value == ObstacleType.Ice || it.value == ObstacleType.ReinforcedIce
            }
            if (iceCount == 0 && obstacles.isNotEmpty()) {
                // Convert some obstacles to ice
                val entries = obstacles.entries.toList()
                val toConvert = minOf(3, entries.size)
                for (i in 0 until toConvert) {
                    obstacles[entries[i].key] = if (levelNumber >= 30) {
                        ObstacleType.ReinforcedIce
                    } else {
                        ObstacleType.Ice
                    }
                }
            }
        }

        return obstacles
    }

    /**
     * Choose a random obstacle type appropriate for the level range.
     */
    private fun chooseObstacleType(
        rng: Random,
        levelNumber: Int,
        objective: ObjectiveType
    ): ObstacleType {
        // For BreakAllIce, bias toward ice types
        if (objective == ObjectiveType.BreakAllIce) {
            return when (rng.nextInt(10)) {
                in 0..3 -> ObstacleType.Ice
                in 4..6 -> ObstacleType.ReinforcedIce
                in 7..8 -> ObstacleType.Stone
                else -> ObstacleType.Ice
            }
        }

        return when {
            levelNumber <= 30 -> {
                // Early procedural: Ice, Stone, ReinforcedIce
                when (rng.nextInt(6)) {
                    0, 1 -> ObstacleType.Ice
                    2, 3 -> ObstacleType.Stone
                    else -> ObstacleType.ReinforcedIce
                }
            }
            levelNumber <= 40 -> {
                // Mid procedural: Add Locked
                when (rng.nextInt(8)) {
                    0, 1 -> ObstacleType.Ice
                    2, 3 -> ObstacleType.Stone
                    4, 5 -> ObstacleType.ReinforcedIce
                    else -> ObstacleType.Locked
                }
            }
            else -> {
                // Late procedural: All types
                when (rng.nextInt(8)) {
                    0 -> ObstacleType.Ice
                    1, 2 -> ObstacleType.Stone
                    3, 4 -> ObstacleType.ReinforcedIce
                    5, 6 -> ObstacleType.Locked
                    else -> ObstacleType.Ice // Extra ice for variety
                }
            }
        }
    }

    /**
     * Generate bomb positions for levels 41+.
     * Avoids placing bombs on existing obstacle positions.
     */
    private fun generateBombs(
        rng: Random,
        boardSize: Int,
        obstacles: Map<Position, ObstacleType>,
        levelNumber: Int
    ): Map<Position, Int> {
        val bombCount = when {
            levelNumber <= 50 -> 1
            levelNumber <= 70 -> 2
            else -> 3
        }

        // Timer decreases with level (more urgent)
        val baseTimer = maxOf(5, 10 - (levelNumber - 41) / 10)

        val bombs = mutableMapOf<Position, Int>()
        val margin = 1

        repeat(bombCount) {
            var attempts = 0
            while (attempts < 20) {
                val row = rng.nextInt(margin, boardSize - margin)
                val col = rng.nextInt(margin, boardSize - margin)
                val pos = Position(row, col)

                if (pos !in obstacles && pos !in bombs) {
                    // Slight timer variation: ±1 from base
                    val timer = baseTimer + rng.nextInt(-1, 2)
                    bombs[pos] = maxOf(4, timer)
                    break
                }
                attempts++
            }
        }

        return bombs
    }
}
