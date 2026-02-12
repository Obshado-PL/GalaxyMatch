package com.galaxymatch.game.model

/**
 * A snapshot of the game state before a move, used for the undo feature.
 *
 * This captures everything needed to restore the board to its pre-swap state.
 * We use BoardState.deepCopy() to ensure the snapshot is independent of the
 * live board — without deep copying, changes to the engine's board would also
 * change our snapshot (since arrays are reference types).
 *
 * @param board Deep copy of the board BEFORE the swap
 * @param score Score BEFORE the swap
 * @param movesRemaining Moves BEFORE the swap (one more than after)
 */
data class UndoSnapshot(
    val board: BoardState,
    val score: Int,
    val movesRemaining: Int,
    /** Ice broken count at snapshot time (for objective undo). Default 0 = backward compatible. */
    val iceBroken: Int = 0,
    /** Gems cleared count at snapshot time (for objective undo). Default 0 = backward compatible. */
    val gemsCleared: Int = 0
)
