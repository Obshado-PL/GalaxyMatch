package com.galaxymatch.game.model

/**
 * The full state of the game board.
 *
 * The board is stored as a 2D array: grid[row][col].
 * A null entry means the cell is currently empty (this happens temporarily
 * during the cascade phase, after matches are cleared but before gravity fills gaps).
 *
 * Obstacles are stored separately from the gem grid as an immutable map.
 * This keeps the grid simple (Gem? only) while supporting ice and stone tiles.
 * - Stone positions have grid[row][col] = null (no gem can exist there)
 * - Ice positions have a normal gem in the grid (ice is just an overlay)
 *
 * @param rows Number of rows in the grid (typically 7-9)
 * @param cols Number of columns in the grid (typically 7-9)
 * @param grid The 2D array of gems. grid[row][col] gives the gem at that position.
 * @param obstacles Map of obstacle positions and their types. Empty map = no obstacles.
 */
data class BoardState(
    val rows: Int,
    val cols: Int,
    val grid: Array<Array<Gem?>>,
    val obstacles: Map<Position, ObstacleType> = emptyMap()
) {
    /**
     * Get the gem at a specific position.
     * Returns null if the position is out of bounds or the cell is empty.
     */
    fun gemAt(pos: Position): Gem? {
        if (!isInBounds(pos)) return null
        return grid[pos.row][pos.col]
    }

    /**
     * Set a gem at a specific position.
     * Use null to clear a cell.
     */
    fun setGem(pos: Position, gem: Gem?) {
        if (isInBounds(pos)) {
            grid[pos.row][pos.col] = gem
        }
    }

    /**
     * Check if a position is within the board boundaries.
     */
    fun isInBounds(pos: Position): Boolean {
        return pos.row in 0 until rows && pos.col in 0 until cols
    }

    /**
     * Swap the gems at two positions.
     * This modifies the grid in place.
     */
    fun swap(pos1: Position, pos2: Position) {
        if (!isInBounds(pos1) || !isInBounds(pos2)) return
        val temp = grid[pos1.row][pos1.col]
        grid[pos1.row][pos1.col] = grid[pos2.row][pos2.col]
        grid[pos2.row][pos2.col] = temp
    }

    // ===== Obstacle Helpers =====

    /**
     * Check if a position has any obstacle (ice or stone).
     */
    fun hasObstacle(pos: Position): Boolean = pos in obstacles

    /**
     * Get the obstacle type at a position, or null if no obstacle.
     */
    fun getObstacle(pos: Position): ObstacleType? = obstacles[pos]

    /**
     * Check if a position is a stone wall.
     * Stones are permanent — no gem, no swap, gravity flows around them.
     */
    fun isStone(pos: Position): Boolean = obstacles[pos] == ObstacleType.Stone

    /**
     * Create a deep copy of this board state.
     * This is important because we sometimes need to test a swap without
     * modifying the real board (e.g., checking if a move is valid).
     * The obstacles map is immutable so it can be shared safely.
     */
    fun deepCopy(): BoardState {
        val newGrid = Array(rows) { row ->
            Array(cols) { col ->
                grid[row][col]?.copy()
            }
        }
        // Obstacles map is immutable — safe to share without copying
        return BoardState(rows, cols, newGrid, obstacles)
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
        if (obstacles != other.obstacles) return false
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
        result = 31 * result + obstacles.hashCode()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                result = 31 * result + (grid[r][c]?.hashCode() ?: 0)
            }
        }
        return result
    }

    companion object {
        /**
         * Create an empty board with all null cells and no obstacles.
         */
        fun empty(rows: Int, cols: Int): BoardState {
            return BoardState(rows, cols, Array(rows) { arrayOfNulls(cols) })
        }
    }
}
