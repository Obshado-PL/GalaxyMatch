package com.galaxymatch.game.model

/**
 * Represents a swap action — the player's attempt to swap two adjacent gems.
 *
 * @param from The position of the first gem (where the swipe started)
 * @param to The position of the second gem (where the swipe ended)
 *
 * The two positions must be adjacent (horizontally or vertically).
 */
data class SwapAction(
    val from: Position,
    val to: Position
)
