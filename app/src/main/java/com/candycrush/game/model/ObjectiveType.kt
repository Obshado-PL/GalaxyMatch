package com.candycrush.game.model

/**
 * Defines what the player must do to win a level.
 *
 * Each level has ONE objective. The objective determines the win condition:
 * - ReachScore: Get enough points (the classic/default mode)
 * - BreakAllIce: Break every ice block on the board
 * - ClearCandyType: Clear a certain number of a specific candy color
 *
 * The lose condition is always the same: out of moves AND objective not met.
 * Score still matters for star ratings regardless of objective type.
 *
 * Why a sealed class instead of an enum?
 * Because ClearCandyType needs to carry data (which candy color and how many).
 * Enums can't hold per-instance data that varies, but sealed classes can.
 * Sealed classes still allow exhaustive `when` expressions just like enums.
 */
sealed class ObjectiveType {

    /**
     * Classic score-based objective.
     * Win: score >= targetScore (uses LevelConfig.targetScore).
     * This is the DEFAULT — all existing levels use this automatically.
     */
    data object ReachScore : ObjectiveType()

    /**
     * Break every ice block on the board.
     * Win: all ice positions from LevelConfig.obstacles have been broken.
     * The total ice count is calculated from LevelConfig.obstacles at level start.
     */
    data object BreakAllIce : ObjectiveType()

    /**
     * Clear N candies of a specific color.
     * Win: the player has cleared at least [targetCount] candies of [candyType].
     *
     * @param candyType Which candy color to clear (e.g., CandyType.Red)
     * @param targetCount How many of that color must be cleared (e.g., 25)
     */
    data class ClearCandyType(
        val candyType: CandyType,
        val targetCount: Int
    ) : ObjectiveType()
}
