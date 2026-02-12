package com.galaxymatch.game.model

/**
 * Represents a group of matched gems found by the MatchDetector.
 *
 * When the match detector scans the board, it finds groups of 3+ gems
 * of the same color in a row or column. If two runs overlap (forming an
 * L or T shape), they are merged into a single MatchResult.
 *
 * @param positions All board positions involved in this match
 * @param gemType The color of the matched gems
 * @param matchLength The longest straight-line run in this match (3, 4, or 5+)
 * @param isLShape True if this match forms an L or T shape (merged horizontal + vertical runs)
 * @param pivotPosition The position where a special gem should be created
 *                      (intersection for L/T shapes, middle for straight lines)
 */
data class MatchResult(
    val positions: Set<Position>,
    val gemType: GemType,
    val matchLength: Int,
    val isLShape: Boolean,
    val pivotPosition: Position
)
