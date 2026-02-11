package com.candycrush.game.engine

import com.candycrush.game.model.BoardState
import com.candycrush.game.model.Candy
import com.candycrush.game.model.CandyType
import com.candycrush.game.model.Position
import com.candycrush.game.model.SpecialType

/**
 * Handles the effects when special candies are activated.
 *
 * When a special candy is part of a match, it triggers a powerful effect
 * that clears additional candies beyond the normal match. This class
 * determines WHICH positions to clear for each special type.
 *
 * Special candy effects:
 * - StripedHorizontal: Clears the entire row
 * - StripedVertical: Clears the entire column
 * - Wrapped: Clears a 3x3 area centered on the candy
 * - ColorBomb: Clears ALL candies of a specific color on the board
 *
 * When two specials are swapped together, they produce even more powerful
 * combination effects (see resolveSpecialCombo).
 */
class SpecialCandyEffects {

    /**
     * Activate a single special candy and get the positions it clears.
     *
     * @param candy The special candy being activated
     * @param position Where the candy is on the board
     * @param board The current board state
     * @param targetType For ColorBomb: the color of the candy it was swapped with.
     *                   For other specials, this is ignored.
     * @return Set of positions that should be cleared by this effect
     */
    fun activate(
        candy: Candy,
        position: Position,
        board: BoardState,
        targetType: CandyType? = null
    ): Set<Position> {
        return when (candy.special) {
            SpecialType.StripedHorizontal -> {
                // Clear the entire row
                (0 until board.cols).map { col ->
                    Position(position.row, col)
                }.toSet()
            }

            SpecialType.StripedVertical -> {
                // Clear the entire column
                (0 until board.rows).map { row ->
                    Position(row, position.col)
                }.toSet()
            }

            SpecialType.Wrapped -> {
                // Clear a 3x3 area centered on this candy
                val positions = mutableSetOf<Position>()
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        val pos = Position(position.row + dr, position.col + dc)
                        if (board.isInBounds(pos)) {
                            positions.add(pos)
                        }
                    }
                }
                positions
            }

            SpecialType.ColorBomb -> {
                // Clear ALL candies of the target color
                val colorToRemove = targetType ?: return emptySet()
                val positions = mutableSetOf<Position>()
                for (r in 0 until board.rows) {
                    for (c in 0 until board.cols) {
                        val boardCandy = board.grid[r][c]
                        if (boardCandy != null && boardCandy.type == colorToRemove) {
                            positions.add(Position(r, c))
                        }
                    }
                }
                // Also include the color bomb's own position
                positions.add(position)
                positions
            }

            SpecialType.None -> emptySet()
        }
    }

    /**
     * Handle special + special combinations when two specials are swapped together.
     *
     * These produce extra-powerful effects:
     * - Striped + Striped: Clear full row AND full column (cross shape)
     * - Wrapped + Wrapped: Clear a 5x5 area
     * - Striped + Wrapped: Clear 3 rows AND 3 columns (large cross)
     * - ColorBomb + any: Turn all of that color into the special type, then activate
     * - ColorBomb + ColorBomb: Clear the ENTIRE board
     *
     * @param candy1 First special candy
     * @param pos1 Position of first candy
     * @param candy2 Second special candy
     * @param pos2 Position of second candy
     * @param board The current board state
     * @return Set of all positions to clear, or null if not a valid combo
     */
    fun resolveSpecialCombo(
        candy1: Candy,
        pos1: Position,
        candy2: Candy,
        pos2: Position,
        board: BoardState
    ): Set<Position>? {
        // Both must be special
        if (candy1.special == SpecialType.None || candy2.special == SpecialType.None) {
            return null
        }

        val special1 = candy1.special
        val special2 = candy2.special

        // ColorBomb + ColorBomb = clear entire board
        if (special1 == SpecialType.ColorBomb && special2 == SpecialType.ColorBomb) {
            return board.allPositions().toSet()
        }

        // ColorBomb + other special
        if (special1 == SpecialType.ColorBomb) {
            return colorBombCombo(candy2.type, special2, board)
        }
        if (special2 == SpecialType.ColorBomb) {
            return colorBombCombo(candy1.type, special1, board)
        }

        // Sort specials for easier matching (avoid duplicating cases)
        val specials = setOf(special1, special2)

        // Striped + Striped = cross (full row + full column)
        if (specials.all { it == SpecialType.StripedHorizontal || it == SpecialType.StripedVertical }) {
            val positions = mutableSetOf<Position>()
            // Use pos1 as the center (where the swap happened)
            for (col in 0 until board.cols) positions.add(Position(pos1.row, col))
            for (row in 0 until board.rows) positions.add(Position(row, pos1.col))
            return positions
        }

        // Wrapped + Wrapped = 5x5 area
        if (special1 == SpecialType.Wrapped && special2 == SpecialType.Wrapped) {
            val positions = mutableSetOf<Position>()
            for (dr in -2..2) {
                for (dc in -2..2) {
                    val pos = Position(pos1.row + dr, pos1.col + dc)
                    if (board.isInBounds(pos)) positions.add(pos)
                }
            }
            return positions
        }

        // Striped + Wrapped = 3 rows + 3 columns (large cross)
        if (specials.contains(SpecialType.Wrapped) &&
            (specials.contains(SpecialType.StripedHorizontal) || specials.contains(SpecialType.StripedVertical))
        ) {
            val positions = mutableSetOf<Position>()
            // Clear 3 rows centered on pos1
            for (dr in -1..1) {
                val row = pos1.row + dr
                if (row in 0 until board.rows) {
                    for (col in 0 until board.cols) positions.add(Position(row, col))
                }
            }
            // Clear 3 columns centered on pos1
            for (dc in -1..1) {
                val col = pos1.col + dc
                if (col in 0 until board.cols) {
                    for (row in 0 until board.rows) positions.add(Position(row, col))
                }
            }
            return positions
        }

        return null
    }

    /**
     * Handle ColorBomb + non-ColorBomb special combo.
     *
     * All candies of the target color become the other special type,
     * and then all of them activate. This is extremely powerful.
     */
    private fun colorBombCombo(
        targetColor: CandyType,
        otherSpecial: SpecialType,
        board: BoardState
    ): Set<Position> {
        val positions = mutableSetOf<Position>()

        // Find all positions of the target color
        for (r in 0 until board.rows) {
            for (c in 0 until board.cols) {
                val candy = board.grid[r][c]
                if (candy != null && candy.type == targetColor) {
                    val pos = Position(r, c)
                    positions.add(pos)

                    // Each candy of that color now acts as the other special
                    // Add the effect of activating that special at each position
                    val tempCandy = candy.copy(special = otherSpecial)
                    positions.addAll(activate(tempCandy, pos, board))
                }
            }
        }

        return positions
    }
}
