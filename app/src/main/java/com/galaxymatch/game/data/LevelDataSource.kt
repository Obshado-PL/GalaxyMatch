package com.galaxymatch.game.data

import com.galaxymatch.game.model.GemType
import com.galaxymatch.game.model.LevelConfig
import com.galaxymatch.game.model.ObjectiveType
import com.galaxymatch.game.model.ObstacleType
import com.galaxymatch.game.model.Position

/**
 * Hardcoded level definitions for the game.
 *
 * All levels are defined here as a list. To add a new level,
 * simply add a new LevelConfig to the list.
 *
 * Difficulty is controlled by:
 * - availableGemTypes: 4 colors = easy (more matches), 6 colors = hard (fewer matches)
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
            availableGemTypes = 4,
            description = "Match 3 gems to score!"
        ),
        LevelConfig(
            levelNumber = 2,
            rows = 7, cols = 7,
            maxMoves = 25,
            targetScore = 1200,
            twoStarScore = 2000,
            threeStarScore = 3500,
            availableGemTypes = 4,
            description = "Try matching 4 for a special gem!"
        ),
        LevelConfig(
            levelNumber = 3,
            rows = 7, cols = 7,
            maxMoves = 22,
            targetScore = 1500,
            twoStarScore = 2500,
            threeStarScore = 4000,
            availableGemTypes = 4,
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
            availableGemTypes = 5
        ),
        LevelConfig(
            levelNumber = 5,
            rows = 8, cols = 8,
            maxMoves = 22,
            targetScore = 2500,
            twoStarScore = 4000,
            threeStarScore = 6000,
            availableGemTypes = 5
        ),
        LevelConfig(
            levelNumber = 6,
            rows = 8, cols = 8,
            maxMoves = 20,
            targetScore = 3000,
            twoStarScore = 4500,
            threeStarScore = 6500,
            availableGemTypes = 5
        ),
        LevelConfig(
            levelNumber = 7,
            rows = 8, cols = 8,
            maxMoves = 20,
            targetScore = 3500,
            twoStarScore = 5000,
            threeStarScore = 7000,
            availableGemTypes = 5
        ),
        // Level 8: Ice introduction! 4 ice blocks in a center diamond pattern.
        // Players learn that matching through ice breaks it first.
        LevelConfig(
            levelNumber = 8,
            rows = 8, cols = 8,
            maxMoves = 18,
            targetScore = 4000,
            twoStarScore = 6000,
            threeStarScore = 8000,
            availableGemTypes = 5,
            description = "Break the ice to clear gems!",
            obstacles = mapOf(
                Position(3, 4) to ObstacleType.Ice,  // Center diamond pattern
                Position(4, 3) to ObstacleType.Ice,
                Position(4, 5) to ObstacleType.Ice,
                Position(5, 4) to ObstacleType.Ice
            )
        ),

        // ===== MEDIUM LEVELS (9-15): Tighter moves, higher targets =====
        // Level 9: First BreakAllIce objective! 8 ice blocks scattered across the board.
        // The player must break ALL ice — not just score points.
        LevelConfig(
            levelNumber = 9,
            rows = 8, cols = 8,
            maxMoves = 18,
            targetScore = 4500,
            twoStarScore = 6500,
            threeStarScore = 9000,
            availableGemTypes = 5,
            description = "Break all the ice!",
            objective = ObjectiveType.BreakAllIce,
            obstacles = mapOf(
                Position(1, 2) to ObstacleType.Ice,
                Position(1, 5) to ObstacleType.Ice,
                Position(3, 1) to ObstacleType.Ice,
                Position(3, 6) to ObstacleType.Ice,
                Position(4, 1) to ObstacleType.Ice,
                Position(4, 6) to ObstacleType.Ice,
                Position(6, 2) to ObstacleType.Ice,
                Position(6, 5) to ObstacleType.Ice
            )
        ),
        // Level 10: Ice wall! 6 ice blocks in a horizontal line across the middle.
        // BreakAllIce objective — smash through the wall!
        LevelConfig(
            levelNumber = 10,
            rows = 8, cols = 8,
            maxMoves = 16,
            targetScore = 5000,
            twoStarScore = 7000,
            threeStarScore = 10000,
            availableGemTypes = 5,
            description = "Break the ice wall!",
            objective = ObjectiveType.BreakAllIce,
            obstacles = mapOf(
                Position(4, 1) to ObstacleType.Ice,
                Position(4, 2) to ObstacleType.Ice,
                Position(4, 3) to ObstacleType.Ice,
                Position(4, 4) to ObstacleType.Ice,
                Position(4, 5) to ObstacleType.Ice,
                Position(4, 6) to ObstacleType.Ice
            )
        ),
        // Level 11: First ClearGemType objective! Clear 25 red gems.
        // Players learn to focus on a specific color instead of just scoring.
        LevelConfig(
            levelNumber = 11,
            rows = 9, cols = 9,
            maxMoves = 20,
            targetScore = 5500,
            twoStarScore = 8000,
            threeStarScore = 11000,
            availableGemTypes = 5,
            description = "Clear 25 red gems!",
            objective = ObjectiveType.ClearGemType(GemType.Red, targetCount = 25)
        ),
        // Level 12: Stone introduction! A vertical stone column divides the board.
        // Gravity flows independently on each side of the wall.
        LevelConfig(
            levelNumber = 12,
            rows = 9, cols = 9,
            maxMoves = 18,
            targetScore = 6000,
            twoStarScore = 8500,
            threeStarScore = 12000,
            availableGemTypes = 5,
            description = "Stone walls block your path!",
            obstacles = mapOf(
                Position(2, 4) to ObstacleType.Stone,
                Position(3, 4) to ObstacleType.Stone,
                Position(4, 4) to ObstacleType.Stone,
                Position(5, 4) to ObstacleType.Stone
            )
        ),
        // Level 13: ClearGemType objective with 6 colors — clear 30 blue gems.
        // Harder because more colors means fewer blue gems appear naturally.
        LevelConfig(
            levelNumber = 13,
            rows = 8, cols = 8,
            maxMoves = 16,
            targetScore = 6000,
            twoStarScore = 9000,
            threeStarScore = 12000,
            availableGemTypes = 6,
            description = "Clear 30 blue gems!",
            objective = ObjectiveType.ClearGemType(GemType.Blue, targetCount = 30)
        ),
        // Level 14: Stone L-shape barrier + Locked Gems introduction!
        // Two locked gems block key positions — free them by matching adjacent gems.
        LevelConfig(
            levelNumber = 14,
            rows = 8, cols = 8,
            maxMoves = 15,
            targetScore = 6500,
            twoStarScore = 9500,
            threeStarScore = 13000,
            availableGemTypes = 6,
            description = "Locked gems can't be swapped!",
            obstacles = mapOf(
                // L-shape in top-left corner
                Position(2, 2) to ObstacleType.Stone,
                Position(3, 2) to ObstacleType.Stone,
                Position(4, 2) to ObstacleType.Stone,
                Position(4, 3) to ObstacleType.Stone,
                Position(4, 4) to ObstacleType.Stone,
                // Locked gems in strategic spots
                Position(1, 5) to ObstacleType.Locked,
                Position(6, 3) to ObstacleType.Locked
            )
        ),
        // Level 15: Mixed obstacles! Stones create chambers, ice fills the gaps.
        // BreakAllIce objective — break the ice trapped between the stones!
        LevelConfig(
            levelNumber = 15,
            rows = 9, cols = 9,
            maxMoves = 18,
            targetScore = 7000,
            twoStarScore = 10000,
            threeStarScore = 14000,
            availableGemTypes = 6,
            description = "Break all ice between the stones!",
            objective = ObjectiveType.BreakAllIce,
            obstacles = mapOf(
                // Stone barrier across the middle
                Position(4, 2) to ObstacleType.Stone,
                Position(4, 3) to ObstacleType.Stone,
                Position(4, 5) to ObstacleType.Stone,
                Position(4, 6) to ObstacleType.Stone,
                // Ice scattered in both halves
                Position(2, 1) to ObstacleType.Ice,
                Position(2, 7) to ObstacleType.Ice,
                Position(6, 1) to ObstacleType.Ice,
                Position(6, 7) to ObstacleType.Ice,
                Position(1, 4) to ObstacleType.Ice,
                Position(7, 4) to ObstacleType.Ice
            )
        ),

        // ===== HARD LEVELS (16-20): All 6 colors, fewer moves =====
        // Level 16: Locked gem fortress — 4 locked gems guard key intersections.
        LevelConfig(
            levelNumber = 16,
            rows = 9, cols = 9,
            maxMoves = 16,
            targetScore = 7500,
            twoStarScore = 11000,
            threeStarScore = 15000,
            availableGemTypes = 6,
            description = "Free the locked gems!",
            obstacles = mapOf(
                Position(2, 3) to ObstacleType.Locked,
                Position(2, 5) to ObstacleType.Locked,
                Position(6, 3) to ObstacleType.Locked,
                Position(6, 5) to ObstacleType.Locked
            )
        ),
        // Level 17: Timed Bombs introduction! 2 bombs with 8-move timers.
        // Match them before they explode or it's game over!
        LevelConfig(
            levelNumber = 17,
            rows = 9, cols = 9,
            maxMoves = 15,
            targetScore = 8000,
            twoStarScore = 12000,
            threeStarScore = 16000,
            availableGemTypes = 6,
            description = "Defuse the bombs before they explode!",
            bombs = mapOf(
                Position(3, 2) to 8,
                Position(5, 6) to 8
            )
        ),
        // Level 18: Two stone pillars with reinforced ice between them!
        // BreakAllIce objective — reinforced ice takes TWO hits to break!
        LevelConfig(
            levelNumber = 18,
            rows = 9, cols = 9,
            maxMoves = 16,
            targetScore = 8500,
            twoStarScore = 12500,
            threeStarScore = 17000,
            availableGemTypes = 6,
            description = "Reinforced ice takes 2 hits!",
            objective = ObjectiveType.BreakAllIce,
            obstacles = mapOf(
                // Left stone pillar
                Position(2, 2) to ObstacleType.Stone,
                Position(3, 2) to ObstacleType.Stone,
                Position(4, 2) to ObstacleType.Stone,
                // Right stone pillar
                Position(2, 6) to ObstacleType.Stone,
                Position(3, 6) to ObstacleType.Stone,
                Position(4, 6) to ObstacleType.Stone,
                // Reinforced ice between the pillars (takes 2 hits!)
                Position(3, 3) to ObstacleType.ReinforcedIce,
                Position(3, 4) to ObstacleType.ReinforcedIce,
                Position(3, 5) to ObstacleType.ReinforcedIce,
                // Normal ice on the flanks
                Position(5, 3) to ObstacleType.Ice,
                Position(5, 5) to ObstacleType.Ice
            )
        ),
        // Level 19: Frozen cage with mixed obstacles — reinforced ice + locked gems.
        // BreakAllIce objective — break through reinforced ice AND free locked gems!
        LevelConfig(
            levelNumber = 19,
            rows = 9, cols = 9,
            maxMoves = 15,
            targetScore = 9000,
            twoStarScore = 13000,
            threeStarScore = 18000,
            availableGemTypes = 6,
            description = "Break through the reinforced cage!",
            objective = ObjectiveType.BreakAllIce,
            obstacles = mapOf(
                // Stone frame (partial border)
                Position(2, 2) to ObstacleType.Stone,
                Position(2, 6) to ObstacleType.Stone,
                Position(3, 2) to ObstacleType.Stone,
                Position(3, 6) to ObstacleType.Stone,
                Position(5, 2) to ObstacleType.Stone,
                Position(5, 6) to ObstacleType.Stone,
                Position(6, 2) to ObstacleType.Stone,
                Position(6, 6) to ObstacleType.Stone,
                // Reinforced ice in the center (takes 2 hits)
                Position(4, 3) to ObstacleType.ReinforcedIce,
                Position(4, 4) to ObstacleType.ReinforcedIce,
                Position(4, 5) to ObstacleType.ReinforcedIce,
                // Normal ice on top and bottom
                Position(3, 3) to ObstacleType.Ice,
                Position(3, 5) to ObstacleType.Ice,
                Position(5, 3) to ObstacleType.Ice,
                Position(5, 5) to ObstacleType.Ice,
                // Locked gems guarding the flanks
                Position(1, 4) to ObstacleType.Locked,
                Position(7, 4) to ObstacleType.Locked
            )
        ),
        // Level 20: The ultimate challenge! All obstacle types + bombs.
        // ClearGemType objective — clear 40 green gems while dodging bombs!
        LevelConfig(
            levelNumber = 20,
            rows = 9, cols = 9,
            maxMoves = 14,
            targetScore = 10000,
            twoStarScore = 15000,
            threeStarScore = 20000,
            availableGemTypes = 6,
            description = "Clear 40 green gems — watch the bombs!",
            objective = ObjectiveType.ClearGemType(GemType.Green, targetCount = 40),
            obstacles = mapOf(
                // Cross-shaped stone wall through the center
                Position(4, 3) to ObstacleType.Stone,
                Position(4, 4) to ObstacleType.Stone,
                Position(4, 5) to ObstacleType.Stone,
                Position(3, 4) to ObstacleType.Stone,
                Position(5, 4) to ObstacleType.Stone,
                // Reinforced ice in corners (takes 2 hits)
                Position(1, 1) to ObstacleType.ReinforcedIce,
                Position(1, 7) to ObstacleType.ReinforcedIce,
                Position(7, 1) to ObstacleType.ReinforcedIce,
                Position(7, 7) to ObstacleType.ReinforcedIce,
                // Normal ice in mid positions
                Position(2, 2) to ObstacleType.Ice,
                Position(2, 6) to ObstacleType.Ice,
                Position(6, 2) to ObstacleType.Ice,
                Position(6, 6) to ObstacleType.Ice
            ),
            // Timed bombs — defuse them or game over!
            bombs = mapOf(
                Position(1, 4) to 6,
                Position(7, 4) to 6
            )
        )
    )
}
