package com.galaxymatch.game.model

/**
 * Represents a single gem on the game board.
 *
 * Each gem has:
 * - [type]: Its color (Red, Blue, Green, etc.)
 * - [special]: Its special power (None for regular gems)
 * - [id]: A unique identifier used to track this specific gem across
 *         animations (so we can animate gem A moving from position X to Y)
 *
 * Gems are immutable data classes — when we need to change a gem's
 * special type, we create a new Gem with the updated field using copy().
 */
data class Gem(
    val type: GemType,
    val special: SpecialType = SpecialType.None,
    val id: Long = nextId()
) {
    companion object {
        /**
         * Counter for generating unique gem IDs.
         * Each gem gets a unique ID when created, which helps the
         * animation system track individual gems as they move around.
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
