package com.candycrush.game.data

import com.candycrush.game.model.LevelConfig

/**
 * Hardcoded level definitions for the game.
 *
 * All levels are defined here as a list. To add a new level,
 * simply add a new LevelConfig to the list.
 *
 * Difficulty is controlled by:
 * - availableCandyTypes: 4 colors = easy (more matches), 6 colors = hard (fewer matches)
 * - maxMoves: Fewer moves = harder
 * - targetScore: Higher target = harder
 * - Board size: Larger boards have more room but can be harder to control
 */
object LevelDataSource {

    val levels: List<LevelConfig> = listOf(
        // ===== TUTORIAL LEVELS (1-3): Small board, few colors, generous moves =====
        LevelConfig(
            levelNumber = 1,
            rows = 7, cols = 7,
            maxMoves = 30,
            targetScore = 800,
            twoStarScore = 1500,
            threeStarScore = 2500,
            availableCandyTypes = 4,
            description = "Match 3 candies to score!"
        ),
        LevelConfig(
            levelNumber = 2,
            rows = 7, cols = 7,
            maxMoves = 25,
            targetScore = 1200,
            twoStarScore = 2000,
            threeStarScore = 3500,
            availableCandyTypes = 4,
            description = "Try matching 4 for a special candy!"
        ),
        LevelConfig(
            levelNumber = 3,
            rows = 7, cols = 7,
            maxMoves = 22,
            targetScore = 1500,
            twoStarScore = 2500,
            threeStarScore = 4000,
            availableCandyTypes = 4,
            description = "Match 5 to create a Color Bomb!"
        ),

        // ===== EASY LEVELS (4-8): Standard board, 5 colors =====
        LevelConfig(
            levelNumber = 4,
            rows = 8, cols = 8,
            maxMoves = 25,
            targetScore = 2000,
            twoStarScore = 3500,
            threeStarScore = 5000,
            availableCandyTypes = 5
        ),
        LevelConfig(
            levelNumber = 5,
            rows = 8, cols = 8,
            maxMoves = 22,
            targetScore = 2500,
            twoStarScore = 4000,
            threeStarScore = 6000,
            availableCandyTypes = 5
        ),
        LevelConfig(
            levelNumber = 6,
            rows = 8, cols = 8,
            maxMoves = 20,
            targetScore = 3000,
            twoStarScore = 4500,
            threeStarScore = 6500,
            availableCandyTypes = 5
        ),
        LevelConfig(
            levelNumber = 7,
            rows = 8, cols = 8,
            maxMoves = 20,
            targetScore = 3500,
            twoStarScore = 5000,
            threeStarScore = 7000,
            availableCandyTypes = 5
        ),
        LevelConfig(
            levelNumber = 8,
            rows = 8, cols = 8,
            maxMoves = 18,
            targetScore = 4000,
            twoStarScore = 6000,
            threeStarScore = 8000,
            availableCandyTypes = 5
        ),

        // ===== MEDIUM LEVELS (9-15): Tighter moves, higher targets =====
        LevelConfig(
            levelNumber = 9,
            rows = 8, cols = 8,
            maxMoves = 18,
            targetScore = 4500,
            twoStarScore = 6500,
            threeStarScore = 9000,
            availableCandyTypes = 5
        ),
        LevelConfig(
            levelNumber = 10,
            rows = 8, cols = 8,
            maxMoves = 16,
            targetScore = 5000,
            twoStarScore = 7000,
            threeStarScore = 10000,
            availableCandyTypes = 5
        ),
        LevelConfig(
            levelNumber = 11,
            rows = 9, cols = 9,
            maxMoves = 20,
            targetScore = 5500,
            twoStarScore = 8000,
            threeStarScore = 11000,
            availableCandyTypes = 5
        ),
        LevelConfig(
            levelNumber = 12,
            rows = 9, cols = 9,
            maxMoves = 18,
            targetScore = 6000,
            twoStarScore = 8500,
            threeStarScore = 12000,
            availableCandyTypes = 5
        ),
        LevelConfig(
            levelNumber = 13,
            rows = 8, cols = 8,
            maxMoves = 16,
            targetScore = 6000,
            twoStarScore = 9000,
            threeStarScore = 12000,
            availableCandyTypes = 6
        ),
        LevelConfig(
            levelNumber = 14,
            rows = 8, cols = 8,
            maxMoves = 15,
            targetScore = 6500,
            twoStarScore = 9500,
            threeStarScore = 13000,
            availableCandyTypes = 6
        ),
        LevelConfig(
            levelNumber = 15,
            rows = 9, cols = 9,
            maxMoves = 18,
            targetScore = 7000,
            twoStarScore = 10000,
            threeStarScore = 14000,
            availableCandyTypes = 6
        ),

        // ===== HARD LEVELS (16-20): All 6 colors, fewer moves =====
        LevelConfig(
            levelNumber = 16,
            rows = 9, cols = 9,
            maxMoves = 16,
            targetScore = 7500,
            twoStarScore = 11000,
            threeStarScore = 15000,
            availableCandyTypes = 6
        ),
        LevelConfig(
            levelNumber = 17,
            rows = 9, cols = 9,
            maxMoves = 15,
            targetScore = 8000,
            twoStarScore = 12000,
            threeStarScore = 16000,
            availableCandyTypes = 6
        ),
        LevelConfig(
            levelNumber = 18,
            rows = 9, cols = 9,
            maxMoves = 14,
            targetScore = 8500,
            twoStarScore = 12500,
            threeStarScore = 17000,
            availableCandyTypes = 6
        ),
        LevelConfig(
            levelNumber = 19,
            rows = 9, cols = 9,
            maxMoves = 13,
            targetScore = 9000,
            twoStarScore = 13000,
            threeStarScore = 18000,
            availableCandyTypes = 6
        ),
        LevelConfig(
            levelNumber = 20,
            rows = 9, cols = 9,
            maxMoves = 12,
            targetScore = 10000,
            twoStarScore = 15000,
            threeStarScore = 20000,
            availableCandyTypes = 6,
            description = "The ultimate challenge!"
        )
    )
}
