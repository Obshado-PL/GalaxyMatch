package com.galaxymatch.game.engine

import com.galaxymatch.game.model.BoardState
import com.galaxymatch.game.model.Position
import com.galaxymatch.game.model.SwapAction

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
     * A "valid move" is a swap of two adjacent gems that would
     * produce at least one match of 3 or more.
     *
     * @param board The current board state
     * @return True if at least one valid move exists, false if deadlocked
     */
    /**
     * Find one valid move on the board, or null if no moves exist.
     *
     * Scans every adjacent pair (left-to-right, top-to-bottom) and returns
     * the first swap that would produce a match. Used by the hint system
     * to show the player a valid move after idle time.
     *
     * @param board The current board state
     * @return A SwapAction representing a valid move, or null if deadlocked
     */
    fun findValidMove(board: BoardState): SwapAction? {
        for (row in 0 until board.rows) {
            for (col in 0 until board.cols) {
                val pos1 = Position(row, col)

                // Skip stone positions — can't swap from a stone wall
                if (board.isStone(pos1)) continue

                // Try swapping with the right neighbor
                if (col + 1 < board.cols) {
                    val pos2 = Position(row, col + 1)
                    // Skip if neighbor is a stone — can't swap with a wall
                    if (!board.isStone(pos2) && wouldMatch(board, pos1, pos2)) {
                        return SwapAction(pos1, pos2)
                    }
                }

                // Try swapping with the bottom neighbor
                if (row + 1 < board.rows) {
                    val pos2 = Position(row + 1, col)
                    if (!board.isStone(pos2) && wouldMatch(board, pos1, pos2)) {
                        return SwapAction(pos1, pos2)
                    }
                }
            }
        }
        return null
    }

    fun hasValidMoves(board: BoardState): Boolean {
        for (row in 0 until board.rows) {
            for (col in 0 until board.cols) {
                val pos1 = Position(row, col)

                // Skip stone positions — can't swap from a stone wall
                if (board.isStone(pos1)) continue

                // Try swapping with the right neighbor
                if (col + 1 < board.cols) {
                    val pos2 = Position(row, col + 1)
                    if (!board.isStone(pos2) && wouldMatch(board, pos1, pos2)) return true
                }

                // Try swapping with the bottom neighbor
                if (row + 1 < board.rows) {
                    val pos2 = Position(row + 1, col)
                    if (!board.isStone(pos2) && wouldMatch(board, pos1, pos2)) return true
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
        // Both cells must contain a gem
        if (board.gemAt(pos1) == null || board.gemAt(pos2) == null) return false

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
     * Collects all gems, randomly redistributes them, and repeats
     * until the new arrangement:
     * 1. Has no pre-existing matches (so the board doesn't immediately cascade)
     * 2. Has at least one valid move (so the player can actually play)
     *
     * @param board The board to shuffle (modified in place)
     */
    fun shuffleBoard(board: BoardState) {
        // Collect all non-null gems (skip stone positions — they have no gem)
        val allGems = mutableListOf<com.galaxymatch.game.model.Gem>()
        val playablePositions = mutableListOf<com.galaxymatch.game.model.Position>()
        for (row in 0 until board.rows) {
            for (col in 0 until board.cols) {
                val pos = com.galaxymatch.game.model.Position(row, col)
                // Only collect gems from non-stone positions
                if (!board.isStone(pos)) {
                    board.grid[row][col]?.let { allGems.add(it) }
                    playablePositions.add(pos)
                }
            }
        }

        // Keep shuffling until we get a valid arrangement
        var attempts = 0
        val maxAttempts = 100 // Safety limit to prevent infinite loop

        do {
            allGems.shuffle()

            // Place shuffled gems back on playable positions only
            // (stone positions are skipped — they stay null)
            var index = 0
            for (pos in playablePositions) {
                if (index < allGems.size) {
                    board.grid[pos.row][pos.col] = allGems[index++]
                }
            }

            attempts++

            // Check conditions: no existing matches AND at least one valid move
            val hasMatches = matchDetector.findAllMatches(board).isNotEmpty()
            val hasValidMove = hasValidMoves(board)

        } while ((hasMatches || !hasValidMove) && attempts < maxAttempts)
    }
}
