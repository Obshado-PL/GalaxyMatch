package com.candycrush.game.model

/**
 * Represents a single candy on the game board.
 *
 * Each candy has:
 * - [type]: Its color (Red, Blue, Green, etc.)
 * - [special]: Its special power (None for regular candies)
 * - [id]: A unique identifier used to track this specific candy across
 *         animations (so we can animate candy A moving from position X to Y)
 *
 * Candies are immutable data classes — when we need to change a candy's
 * special type, we create a new Candy with the updated field using copy().
 */
data class Candy(
    val type: CandyType,
    val special: SpecialType = SpecialType.None,
    val id: Long = nextId()
) {
    companion object {
        /**
         * Counter for generating unique candy IDs.
         * Each candy gets a unique ID when created, which helps the
         * animation system track individual candies as they move around.
         */
        private var counter = 0L

        private fun nextId(): Long = counter++

        /**
         * Reset the ID counter. Useful when starting a new game.
         */
        fun resetIdCounter() {
            counter = 0L
        }
    }
}
