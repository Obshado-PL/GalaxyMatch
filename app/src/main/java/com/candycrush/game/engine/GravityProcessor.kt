package com.candycrush.game.engine

import com.candycrush.game.model.BoardState
import com.candycrush.game.model.Candy
import com.candycrush.game.model.CandyType
import com.candycrush.game.model.Position

/**
 * Handles the gravity/cascade system.
 *
 * After matches are cleared (cells set to null), this processor:
 * 1. Makes existing candies fall down to fill the gaps
 * 2. Generates new random candies to fill the empty top cells
 *
 * Each column is processed independently — candies only fall straight down,
 * never sideways.
 *
 * The result includes movement data that the animation system uses to
 * show candies falling and new candies entering from above.
 */
class GravityProcessor {

    /**
     * Represents a candy's movement for animation purposes.
     *
     * @param candyId The unique ID of the candy that moved
     * @param fromRow The row the candy was in before gravity (or -1 if it's a new candy)
     * @param toRow The row the candy ended up in after gravity
     * @param col The column (doesn't change since gravity is vertical)
     * @param isNew True if this is a newly spawned candy (enters from above the board)
     */
    data class CandyMovement(
        val candyId: Long,
        val fromRow: Int,
        val toRow: Int,
        val col: Int,
        val isNew: Boolean = false
    )

    /**
     * The result of applying gravity.
     *
     * @param movements List of all candy movements (for animations)
     * @param newCandies List of newly created candies and their positions
     */
    data class GravityResult(
        val movements: List<CandyMovement>,
        val newCandies: List<Pair<Position, Candy>>
    )

    /**
     * Apply gravity to the board.
     *
     * This modifies the board in place:
     * 1. For each column, collect all non-null candies
     * 2. Place them at the bottom of the column
     * 3. Fill the remaining top cells with new random candies
     *
     * @param board The board to apply gravity to (modified in place)
     * @param availableTypes The candy types available for generating new candies
     * @return GravityResult with movement data for animations
     */
    fun applyGravity(board: BoardState, availableTypes: List<CandyType>): GravityResult {
        val movements = mutableListOf<CandyMovement>()
        val newCandies = mutableListOf<Pair<Position, Candy>>()

        // Process each column independently
        for (col in 0 until board.cols) {

            // Step 1: Collect all non-null candies in this column, from bottom to top
            // This preserves their relative order while removing gaps
            val existingCandies = mutableListOf<Pair<Int, Candy>>() // (originalRow, candy)
            for (row in board.rows - 1 downTo 0) {
                val candy = board.grid[row][col]
                if (candy != null) {
                    existingCandies.add(Pair(row, candy))
                }
            }

            // Step 2: Place existing candies at the bottom of the column
            var writeRow = board.rows - 1
            for ((originalRow, candy) in existingCandies) {
                board.grid[writeRow][col] = candy

                // Record movement if the candy actually moved
                if (originalRow != writeRow) {
                    movements.add(
                        CandyMovement(
                            candyId = candy.id,
                            fromRow = originalRow,
                            toRow = writeRow,
                            col = col,
                            isNew = false
                        )
                    )
                }

                writeRow--
            }

            // Step 3: Fill remaining top cells with new random candies
            val emptyCellCount = writeRow + 1
            for (row in writeRow downTo 0) {
                val newCandy = Candy(type = availableTypes.random())
                board.grid[row][col] = newCandy

                movements.add(
                    CandyMovement(
                        candyId = newCandy.id,
                        // New candies start above the board (negative row)
                        // The further up, the more negative (for staggered entry)
                        fromRow = -(emptyCellCount - row),
                        toRow = row,
                        col = col,
                        isNew = true
                    )
                )
                newCandies.add(Pair(Position(row, col), newCandy))
            }
        }

        return GravityResult(movements, newCandies)
    }
}
