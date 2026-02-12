package com.galaxymatch.game.engine

import com.galaxymatch.game.model.Gem
import com.galaxymatch.game.model.MatchResult
import com.galaxymatch.game.model.Position
import com.galaxymatch.game.model.SpecialType
import com.galaxymatch.game.model.SwapAction

/**
 * Determines what special gem (if any) to create from a match.
 *
 * Rules:
 * - Match of 3 (straight line): No special gem, just clear them
 * - Match of 4 (straight line): Create a Striped gem
 *     - If the match was horizontal → create StripedVertical (clears column when used)
 *     - If the match was vertical → create StripedHorizontal (clears row when used)
 * - Match of 5+ (straight line): Create a ColorBomb
 * - L-shape or T-shape (merged runs): Create a Wrapped gem
 *
 * The special gem is placed at the match's pivot position.
 */
class SpecialGemResolver {

    /**
     * The result of resolving a match — what special gem to create and where.
     *
     * @param position Where to place the special gem on the board
     * @param gem The special gem to create
     */
    data class SpecialCreation(
        val position: Position,
        val gem: Gem
    )

    /**
     * Determine if a match should create a special gem.
     *
     * @param match The match result to evaluate
     * @param swapAction The player's swap that triggered this match (null for cascade matches).
     *                   Used to determine which position the special gem should appear at.
     * @return A SpecialCreation if a special should be made, or null for regular match-3
     */
    fun resolve(match: MatchResult, swapAction: SwapAction? = null): SpecialCreation? {
        // Rule 1: L/T shape → Wrapped gem (takes priority)
        if (match.isLShape) {
            return SpecialCreation(
                position = match.pivotPosition,
                gem = Gem(
                    type = match.gemType,
                    special = SpecialType.Wrapped
                )
            )
        }

        // Rule 2: Match of 5+ → Color Bomb
        if (match.matchLength >= 5) {
            val position = getBestPosition(match, swapAction)
            return SpecialCreation(
                position = position,
                gem = Gem(
                    type = match.gemType,
                    special = SpecialType.ColorBomb
                )
            )
        }

        // Rule 3: Match of 4 → Striped gem
        if (match.matchLength == 4) {
            val position = getBestPosition(match, swapAction)
            // Determine if the match is horizontal or vertical
            // If all positions have the same row → horizontal match → create vertical stripes
            val isHorizontal = match.positions.map { it.row }.toSet().size == 1
            val specialType = if (isHorizontal) {
                SpecialType.StripedVertical
            } else {
                SpecialType.StripedHorizontal
            }

            return SpecialCreation(
                position = position,
                gem = Gem(
                    type = match.gemType,
                    special = specialType
                )
            )
        }

        // Rule 4: Match of 3 → no special gem
        return null
    }

    /**
     * Determine the best position for a special gem.
     *
     * If the match was triggered by a player swap, place the special
     * at the position that was part of the swap (it feels more natural).
     * Otherwise, use the match's pivot position.
     */
    private fun getBestPosition(match: MatchResult, swapAction: SwapAction?): Position {
        if (swapAction != null) {
            // If one of the swapped positions is in the match, use it
            if (swapAction.from in match.positions) return swapAction.from
            if (swapAction.to in match.positions) return swapAction.to
        }
        return match.pivotPosition
    }
}
