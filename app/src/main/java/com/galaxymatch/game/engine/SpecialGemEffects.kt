package com.galaxymatch.game.engine

import com.galaxymatch.game.model.BoardState
import com.galaxymatch.game.model.Gem
import com.galaxymatch.game.model.GemType
import com.galaxymatch.game.model.Position
import com.galaxymatch.game.model.SpecialType

/**
 * Handles the effects when special gems are activated.
 *
 * When a special gem is part of a match, it triggers a powerful effect
 * that clears additional gems beyond the normal match. This class
 * determines WHICH positions to clear for each special type.
 *
 * Special gem effects:
 * - StripedHorizontal: Clears the entire row
 * - StripedVertical: Clears the entire column
 * - Wrapped: Clears a 3x3 area centered on the gem
 * - ColorBomb: Clears ALL gems of a specific color on the board
 *
 * When two specials are swapped together, they produce even more powerful
 * combination effects (see resolveSpecialCombo).
 */
class SpecialGemEffects {

    /**
     * Activate a single special gem and get the positions it clears.
     *
     * @param gem The special gem being activated
     * @param position Where the gem is on the board
     * @param board The current board state
     * @param targetType For ColorBomb: the color of the gem it was swapped with.
     *                   For other specials, this is ignored.
     * @return Set of positions that should be cleared by this effect
     */
    fun activate(
        gem: Gem,
        position: Position,
        board: BoardState,
        targetType: GemType? = null
    ): Set<Position> {
        return when (gem.special) {
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
                // Clear a 3x3 area centered on this gem
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
                // Clear ALL gems of the target color
                val colorToRemove = targetType ?: return emptySet()
                val positions = mutableSetOf<Position>()
                for (r in 0 until board.rows) {
                    for (c in 0 until board.cols) {
                        val boardGem = board.grid[r][c]
                        if (boardGem != null && boardGem.type == colorToRemove) {
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
     * @param gem1 First special gem
     * @param pos1 Position of first gem
     * @param gem2 Second special gem
     * @param pos2 Position of second gem
     * @param board The current board state
     * @return Set of all positions to clear, or null if not a valid combo
     */
    fun resolveSpecialCombo(
        gem1: Gem,
        pos1: Position,
        gem2: Gem,
        pos2: Position,
        board: BoardState
    ): Set<Position>? {
        // Both must be special
        if (gem1.special == SpecialType.None || gem2.special == SpecialType.None) {
            return null
        }

        val special1 = gem1.special
        val special2 = gem2.special

        // ColorBomb + ColorBomb = clear entire board
        if (special1 == SpecialType.ColorBomb && special2 == SpecialType.ColorBomb) {
            return board.allPositions().toSet()
        }

        // ColorBomb + other special
        if (special1 == SpecialType.ColorBomb) {
            return colorBombCombo(gem2.type, special2, board)
        }
        if (special2 == SpecialType.ColorBomb) {
            return colorBombCombo(gem1.type, special1, board)
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
     * All gems of the target color become the other special type,
     * and then all of them activate. This is extremely powerful.
     */
    private fun colorBombCombo(
        targetColor: GemType,
        otherSpecial: SpecialType,
        board: BoardState
    ): Set<Position> {
        val positions = mutableSetOf<Position>()

        // Find all positions of the target color
        for (r in 0 until board.rows) {
            for (c in 0 until board.cols) {
                val gem = board.grid[r][c]
                if (gem != null && gem.type == targetColor) {
                    val pos = Position(r, c)
                    positions.add(pos)

                    // Each gem of that color now acts as the other special
                    // Add the effect of activating that special at each position
                    val tempGem = gem.copy(special = otherSpecial)
                    positions.addAll(activate(tempGem, pos, board))
                }
            }
        }

        return positions
    }
}
