package com.candycrush.game.model

/**
 * Represents a swap action — the player's attempt to swap two adjacent candies.
 *
 * @param from The position of the first candy (where the swipe started)
 * @param to The position of the second candy (where the swipe ended)
 *
 * The two positions must be adjacent (horizontally or vertically).
 */
data class SwapAction(
    val from: Position,
    val to: Position
)
