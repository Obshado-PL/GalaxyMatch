package com.galaxymatch.game.model

/**
 * A position on the game board grid.
 *
 * @param row Row index (0 = top row, increases downward)
 * @param col Column index (0 = leftmost column, increases rightward)
 *
 * Example for an 8x8 board:
 *   Position(0, 0) = top-left corner
 *   Position(0, 7) = top-right corner
 *   Position(7, 0) = bottom-left corner
 *   Position(7, 7) = bottom-right corner
 */
data class Position(val row: Int, val col: Int) {

    /**
     * Get the position above this one (one row up).
     */
    fun up() = Position(row - 1, col)

    /**
     * Get the position below this one (one row down).
     */
    fun down() = Position(row + 1, col)

    /**
     * Get the position to the left of this one.
     */
    fun left() = Position(row, col - 1)

    /**
     * Get the position to the right of this one.
     */
    fun right() = Position(row, col + 1)

    /**
     * Check if this position is adjacent to another position
     * (horizontally or vertically, not diagonally).
     */
    fun isAdjacentTo(other: Position): Boolean {
        val rowDiff = kotlin.math.abs(row - other.row)
        val colDiff = kotlin.math.abs(col - other.col)
        return (rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1)
    }
}
