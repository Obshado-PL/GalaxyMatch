package com.galaxymatch.game.engine

import com.galaxymatch.game.model.BoardState
import com.galaxymatch.game.model.Gem
import com.galaxymatch.game.model.GemType
import com.galaxymatch.game.model.Position

/**
 * Handles the gravity/cascade system.
 *
 * After matches are cleared (cells set to null), this processor:
 * 1. Makes existing gems fall down to fill the gaps
 * 2. Generates new random gems to fill the empty top cells
 *
 * Each column is processed independently — gems only fall straight down,
 * never sideways.
 *
 * **Stone obstacle support:** When a column contains stone walls, the column
 * is split into independent "segments" above and below each stone. Each segment
 * gets its own gravity processing, as if it were a separate mini-column.
 * Example: Stone at row 4 → segment [0..3] above and segment [5..7] below.
 *
 * The result includes movement data that the animation system uses to
 * show gems falling and new gems entering from above.
 */
class GravityProcessor {

    /**
     * Represents a gem's movement for animation purposes.
     *
     * @param gemId The unique ID of the gem that moved
     * @param fromRow The row the gem was in before gravity (or -1 if it's a new gem)
     * @param toRow The row the gem ended up in after gravity
     * @param col The column (doesn't change since gravity is vertical)
     * @param isNew True if this is a newly spawned gem (enters from above the board)
     */
    data class GemMovement(
        val gemId: Long,
        val fromRow: Int,
        val toRow: Int,
        val col: Int,
        val isNew: Boolean = false
    )

    /**
     * The result of applying gravity.
     *
     * @param movements List of all gem movements (for animations)
     * @param newGems List of newly created gems and their positions
     */
    data class GravityResult(
        val movements: List<GemMovement>,
        val newGems: List<Pair<Position, Gem>>
    )

    /**
     * Apply gravity to the board.
     *
     * This modifies the board in place:
     * 1. For each column, find stone walls and split into segments
     * 2. For each segment, collect all non-null gems
     * 3. Place them at the bottom of the segment
     * 4. Fill the remaining top cells of the segment with new random gems
     *
     * @param board The board to apply gravity to (modified in place)
     * @param availableTypes The gem types available for generating new gems
     * @return GravityResult with movement data for animations
     */
    fun applyGravity(board: BoardState, availableTypes: List<GemType>): GravityResult {
        val movements = mutableListOf<GemMovement>()
        val newGems = mutableListOf<Pair<Position, Gem>>()

        // Process each column independently
        for (col in 0 until board.cols) {

            // === Find stone positions in this column ===
            // Stones divide the column into separate vertical segments.
            // Each segment has its own gravity (gems above a stone can't
            // fall through it, and new gems fill from the segment's top).
            val stoneRows = mutableListOf<Int>()
            for (row in 0 until board.rows) {
                if (board.isStone(Position(row, col))) {
                    stoneRows.add(row)
                }
            }

            // === Build segments (ranges of rows between stones) ===
            // Example: board has 8 rows, stones at rows 2 and 5
            //   → segments: [0..1], [3..4], [6..7]
            // No stones → one segment covering the entire column: [0..7]
            val segments = mutableListOf<IntRange>()
            var segmentStart = 0

            for (stoneRow in stoneRows) {
                // Add the segment above this stone (if it has any rows)
                if (stoneRow > segmentStart) {
                    segments.add(segmentStart until stoneRow)
                }
                // Next segment starts after this stone
                segmentStart = stoneRow + 1
            }
            // Add the final segment (from after last stone to bottom of board)
            if (segmentStart < board.rows) {
                segments.add(segmentStart until board.rows)
            }

            // If no stones at all, the entire column is one segment
            if (segments.isEmpty() && stoneRows.isEmpty()) {
                segments.add(0 until board.rows)
            }

            // === Process each segment independently ===
            for (segment in segments) {
                if (segment.isEmpty()) continue

                // Step 1: Collect all non-null gems in this segment, from bottom to top
                // This preserves their relative order while removing gaps
                val existingGems = mutableListOf<Pair<Int, Gem>>() // (originalRow, gem)
                for (row in segment.last downTo segment.first) {
                    val gem = board.grid[row][col]
                    if (gem != null) {
                        existingGems.add(Pair(row, gem))
                    }
                }

                // Step 2: Place existing gems at the bottom of the segment
                var writeRow = segment.last
                for ((originalRow, gem) in existingGems) {
                    board.grid[writeRow][col] = gem

                    // Record movement if the gem actually moved
                    if (originalRow != writeRow) {
                        movements.add(
                            GemMovement(
                                gemId = gem.id,
                                fromRow = originalRow,
                                toRow = writeRow,
                                col = col,
                                isNew = false
                            )
                        )
                    }

                    writeRow--
                }

                // Step 3: Fill remaining top cells in this segment with new random gems
                // New gems enter from above the segment's top edge
                val emptyCellCount = writeRow - segment.first + 1
                for (row in writeRow downTo segment.first) {
                    val newGem = Gem(type = availableTypes.random())
                    board.grid[row][col] = newGem

                    movements.add(
                        GemMovement(
                            gemId = newGem.id,
                            // New gems start above the segment (negative offset)
                            // For segments below a stone, they start just above the segment
                            fromRow = segment.first - (emptyCellCount - (row - segment.first)),
                            toRow = row,
                            col = col,
                            isNew = true
                        )
                    )
                    newGems.add(Pair(Position(row, col), newGem))
                }
            }
        }

        return GravityResult(movements, newGems)
    }
}
