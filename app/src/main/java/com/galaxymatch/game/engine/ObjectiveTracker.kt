package com.galaxymatch.game.engine

import com.galaxymatch.game.model.*

/**
 * Tracks progress toward the current level's objective.
 *
 * This class is responsible for:
 * - Counting how many ice blocks have been broken
 * - Counting how many gems of a target color have been cleared
 * - Checking whether the objective has been met
 *
 * The GameEngine calls recordIceBroken() and recordGemsCleared() during
 * the cascade loop, and the ViewModel reads the progress to display in the HUD.
 *
 * IMPORTANT: This class has NO Android dependencies — it's pure Kotlin,
 * just like the rest of the engine. It follows the same pattern as
 * ScoreCalculator, MatchDetector, etc.
 *
 * @param objective The objective type for the current level
 * @param levelConfig The level configuration (needed for totalIce count)
 */
class ObjectiveTracker(
    private val objective: ObjectiveType,
    private val levelConfig: LevelConfig
) {

    // ===== Progress counters =====

    /** How many ice blocks have been broken so far this level. */
    var iceBroken: Int = 0
        private set

    /** How many gems of the target color have been cleared so far. */
    var gemsCleared: Int = 0
        private set

    // ===== Derived values (calculated once from LevelConfig) =====

    /**
     * Total number of ice blocks on this level at the start.
     * Calculated from LevelConfig.obstacles by counting Ice entries.
     */
    val totalIce: Int = levelConfig.obstacles.count { it.value == ObstacleType.Ice }

    /**
     * The target count for ClearGemType objectives.
     * Returns 0 for other objective types (not used).
     */
    val targetGemCount: Int = when (objective) {
        is ObjectiveType.ClearGemType -> objective.targetCount
        else -> 0
    }

    /**
     * The gem type to clear for ClearGemType objectives.
     * Returns null for other objective types.
     */
    val targetGemType: GemType? = when (objective) {
        is ObjectiveType.ClearGemType -> objective.gemType
        else -> null
    }

    // ===== Recording events =====

    /**
     * Record that ice blocks were broken during a cascade step or power-up.
     * Called by GameEngine when ice positions are removed from the obstacles map.
     *
     * @param count How many ice blocks broke this step
     */
    fun recordIceBroken(count: Int) {
        iceBroken += count
    }

    /**
     * Record that gems were cleared during a cascade step.
     * Called by GameEngine with the list of MatchResults from each cascade.
     * Only counts gems matching the target type (for ClearGemType objectives).
     * For other objective types, this is a no-op.
     *
     * @param matches The matches that were cleared this cascade step
     */
    fun recordGemsCleared(matches: List<MatchResult>) {
        val target = targetGemType ?: return  // No-op for non-ClearGemType objectives
        for (match in matches) {
            if (match.gemType == target) {
                gemsCleared += match.positions.size
            }
        }
    }

    /**
     * Record gems cleared from special combo activations or power-ups.
     * These clears happen outside the normal match flow (e.g., Color Bomb
     * power-up or two specials swapped together).
     *
     * Checks each position's gem type against the target before counting.
     * Must be called BEFORE the gems are actually removed from the board,
     * so we can still read their types.
     *
     * @param positions The positions that will be cleared
     * @param board The current board state (to look up gem types)
     */
    fun recordSpecialClears(positions: Set<Position>, board: BoardState) {
        val target = targetGemType ?: return
        for (pos in positions) {
            val gem = board.gemAt(pos)
            if (gem != null && gem.type == target) {
                gemsCleared++
            }
        }
    }

    // ===== Checking completion =====

    /**
     * Check if the objective has been completed.
     *
     * @param currentScore The player's current score (for ReachScore check)
     * @param currentObstacles The current obstacles map on the board
     *                         (for BreakAllIce — checks if any Ice remains)
     * @return True if the objective is met
     */
    fun isObjectiveMet(currentScore: Int, currentObstacles: Map<Position, ObstacleType>): Boolean {
        return when (objective) {
            is ObjectiveType.ReachScore -> {
                // Classic: just check score against target
                currentScore >= levelConfig.targetScore
            }
            is ObjectiveType.BreakAllIce -> {
                // Check if any ice remains on the board
                // Using live board state is more reliable than counting
                currentObstacles.none { it.value == ObstacleType.Ice }
            }
            is ObjectiveType.ClearGemType -> {
                // Check if enough of the target color have been cleared
                gemsCleared >= objective.targetCount
            }
        }
    }

    // ===== State management =====

    /**
     * Restore counters from an undo snapshot.
     * Called when the player uses the undo feature to go back one move.
     * The board state is also restored separately by the engine.
     */
    fun restoreCounters(savedIceBroken: Int, savedGemsCleared: Int) {
        iceBroken = savedIceBroken
        gemsCleared = savedGemsCleared
    }

    /**
     * Reset all progress counters. Called when restarting a level.
     */
    fun reset() {
        iceBroken = 0
        gemsCleared = 0
    }
}
