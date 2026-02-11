package com.candycrush.game.model

/**
 * The full state of the game board.
 *
 * The board is stored as a 2D array: grid[row][col].
 * A null entry means the cell is currently empty (this happens temporarily
 * during the cascade phase, after matches are cleared but before gravity fills gaps).
 *
 * @param rows Number of rows in the grid (typically 7-9)
 * @param cols Number of columns in the grid (typically 7-9)
 * @param grid The 2D array of candies. grid[row][col] gives the candy at that position.
 */
data class BoardState(
    val rows: Int,
    val cols: Int,
    val grid: Array<Array<Candy?>>
) {
    /**
     * Get the candy at a specific position.
     * Returns null if the position is out of bounds or the cell is empty.
     */
    fun candyAt(pos: Position): Candy? {
        if (!isInBounds(pos)) return null
        return grid[pos.row][pos.col]
    }

    /**
     * Set a candy at a specific position.
     * Use null to clear a cell.
     */
    fun setCandy(pos: Position, candy: Candy?) {
        if (isInBounds(pos)) {
            grid[pos.row][pos.col] = candy
        }
    }

    /**
     * Check if a position is within the board boundaries.
     */
    fun isInBounds(pos: Position): Boolean {
        return pos.row in 0 until rows && pos.col in 0 until cols
    }

    /**
     * Swap the candies at two positions.
     * This modifies the grid in place.
     */
    fun swap(pos1: Position, pos2: Position) {
        if (!isInBounds(pos1) || !isInBounds(pos2)) return
        val temp = grid[pos1.row][pos1.col]
        grid[pos1.row][pos1.col] = grid[pos2.row][pos2.col]
        grid[pos2.row][pos2.col] = temp
    }

    /**
     * Create a deep copy of this board state.
     * This is important because we sometimes need to test a swap without
     * modifying the real board (e.g., checking if a move is valid).
     */
    fun deepCopy(): BoardState {
        val newGrid = Array(rows) { row ->
            Array(cols) { col ->
                grid[row][col]?.copy()
            }
        }
        return BoardState(rows, cols, newGrid)
    }

    /**
     * Get all positions on the board as a flat list.
     * Useful for iterating over every cell.
     */
    fun allPositions(): List<Position> {
        return (0 until rows).flatMap { row ->
            (0 until cols).map { col ->
                Position(row, col)
            }
        }
    }

    // Override equals and hashCode since we have an Array field
    // (arrays use reference equality by default in Kotlin)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BoardState) return false
        if (rows != other.rows || cols != other.cols) return false
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (grid[r][c] != other.grid[r][c]) return false
            }
        }
        return true
    }

    override fun hashCode(): Int {
        var result = rows
        result = 31 * result + cols
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                result = 31 * result + (grid[r][c]?.hashCode() ?: 0)
            }
        }
        return result
    }

    companion object {
        /**
         * Create an empty board with all null cells.
         */
        fun empty(rows: Int, cols: Int): BoardState {
            return BoardState(rows, cols, Array(rows) { arrayOfNulls(cols) })
        }
    }
}
