package com.candycrush.game.engine

import com.candycrush.game.model.*

/**
 * Tracks progress toward the current level's objective.
 *
 * This class is responsible for:
 * - Counting how many ice blocks have been broken
 * - Counting how many candies of a target color have been cleared
 * - Checking whether the objective has been met
 *
 * The GameEngine calls recordIceBroken() and recordCandiesCleared() during
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

    /** How many candies of the target color have been cleared so far. */
    var candiesCleared: Int = 0
        private set

    // ===== Derived values (calculated once from LevelConfig) =====

    /**
     * Total number of ice blocks on this level at the start.
     * Calculated from LevelConfig.obstacles by counting Ice entries.
     */
    val totalIce: Int = levelConfig.obstacles.count { it.value == ObstacleType.Ice }

    /**
     * The target count for ClearCandyType objectives.
     * Returns 0 for other objective types (not used).
     */
    val targetCandyCount: Int = when (objective) {
        is ObjectiveType.ClearCandyType -> objective.targetCount
        else -> 0
    }

    /**
     * The candy type to clear for ClearCandyType objectives.
     * Returns null for other objective types.
     */
    val targetCandyType: CandyType? = when (objective) {
        is ObjectiveType.ClearCandyType -> objective.candyType
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
     * Record that candies were cleared during a cascade step.
     * Called by GameEngine with the list of MatchResults from each cascade.
     * Only counts candies matching the target type (for ClearCandyType objectives).
     * For other objective types, this is a no-op.
     *
     * @param matches The matches that were cleared this cascade step
     */
    fun recordCandiesCleared(matches: List<MatchResult>) {
        val target = targetCandyType ?: return  // No-op for non-ClearCandyType objectives
        for (match in matches) {
            if (match.candyType == target) {
                candiesCleared += match.positions.size
            }
        }
    }

    /**
     * Record candies cleared from special combo activations or power-ups.
     * These clears happen outside the normal match flow (e.g., Color Bomb
     * power-up or two specials swapped together).
     *
     * Checks each position's candy type against the target before counting.
     * Must be called BEFORE the candies are actually removed from the board,
     * so we can still read their types.
     *
     * @param positions The positions that will be cleared
     * @param board The current board state (to look up candy types)
     */
    fun recordSpecialClears(positions: Set<Position>, board: BoardState) {
        val target = targetCandyType ?: return
        for (pos in positions) {
            val candy = board.candyAt(pos)
            if (candy != null && candy.type == target) {
                candiesCleared++
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
            is ObjectiveType.ClearCandyType -> {
                // Check if enough of the target color have been cleared
                candiesCleared >= objective.targetCount
            }
        }
    }

    // ===== State management =====

    /**
     * Restore counters from an undo snapshot.
     * Called when the player uses the undo feature to go back one move.
     * The board state is also restored separately by the engine.
     */
    fun restoreCounters(savedIceBroken: Int, savedCandiesCleared: Int) {
        iceBroken = savedIceBroken
        candiesCleared = savedCandiesCleared
    }

    /**
     * Reset all progress counters. Called when restarting a level.
     */
    fun reset() {
        iceBroken = 0
        candiesCleared = 0
    }
}
