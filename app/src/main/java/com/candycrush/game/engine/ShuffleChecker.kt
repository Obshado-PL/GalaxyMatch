package com.candycrush.game.engine

import com.candycrush.game.model.BoardState
import com.candycrush.game.model.Position

/**
 * Detects when the board has no valid moves (deadlock) and shuffles it.
 *
 * After each cascade settles, we check if the player can make any valid swap.
 * If no swaps would produce a match, the board is in a deadlock state and
 * needs to be shuffled.
 *
 * The check works by trying every possible adjacent swap and seeing if
 * any would produce at least one match. This is O(rows * cols) which is
 * fine for an 8x8 or 9x9 board.
 */
class ShuffleChecker(private val matchDetector: MatchDetector) {

    /**
     * Check if there is at least one valid move on the board.
     *
     * A "valid move" is a swap of two adjacent candies that would
     * produce at least one match of 3 or more.
     *
     * @param board The current board state
     * @return True if at least one valid move exists, false if deadlocked
     */
    fun hasValidMoves(board: BoardState): Boolean {
        for (row in 0 until board.rows) {
            for (col in 0 until board.cols) {
                // Try swapping with the right neighbor
                if (col + 1 < board.cols) {
                    val pos1 = Position(row, col)
                    val pos2 = Position(row, col + 1)
                    if (wouldMatch(board, pos1, pos2)) return true
                }

                // Try swapping with the bottom neighbor
                if (row + 1 < board.rows) {
                    val pos1 = Position(row, col)
                    val pos2 = Position(row + 1, col)
                    if (wouldMatch(board, pos1, pos2)) return true
                }
            }
        }
        return false
    }

    /**
     * Test if swapping two positions would produce a match.
     *
     * We swap, check for matches, then swap back (so the board is unchanged).
     */
    private fun wouldMatch(board: BoardState, pos1: Position, pos2: Position): Boolean {
        // Both cells must contain a candy
        if (board.candyAt(pos1) == null || board.candyAt(pos2) == null) return false

        // Perform the swap
        board.swap(pos1, pos2)

        // Check for matches
        val hasMatch = matchDetector.findAllMatches(board).isNotEmpty()

        // Swap back to restore the original board
        board.swap(pos1, pos2)

        return hasMatch
    }

    /**
     * Shuffle the board when no valid moves exist.
     *
     * Collects all candies, randomly redistributes them, and repeats
     * until the new arrangement:
     * 1. Has no pre-existing matches (so the board doesn't immediately cascade)
     * 2. Has at least one valid move (so the player can actually play)
     *
     * @param board The board to shuffle (modified in place)
     */
    fun shuffleBoard(board: BoardState) {
        // Collect all non-null candies
        val allCandies = mutableListOf<com.candycrush.game.model.Candy>()
        for (row in 0 until board.rows) {
            for (col in 0 until board.cols) {
                board.grid[row][col]?.let { allCandies.add(it) }
            }
        }

        // Keep shuffling until we get a valid arrangement
        var attempts = 0
        val maxAttempts = 100 // Safety limit to prevent infinite loop

        do {
            allCandies.shuffle()

            // Place shuffled candies back on the board
            var index = 0
            for (row in 0 until board.rows) {
                for (col in 0 until board.cols) {
                    if (index < allCandies.size) {
                        board.grid[row][col] = allCandies[index++]
                    }
                }
            }

            attempts++

            // Check conditions: no existing matches AND at least one valid move
            val hasMatches = matchDetector.findAllMatches(board).isNotEmpty()
            val hasValidMove = hasValidMoves(board)

        } while ((hasMatches || !hasValidMove) && attempts < maxAttempts)
    }
}
