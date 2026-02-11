package com.candycrush.game.engine

import com.candycrush.game.model.MatchResult

/**
 * Calculates scores for matches with combo multipliers.
 *
 * Score formula:
 * - Match of 3: 100 base points
 * - Match of 4: 200 base points
 * - L/T shape:  300 base points
 * - Match of 5+: 500 base points
 * - Each extra candy beyond 3 adds 50 bonus points
 * - Combo multiplier: 1.0x (first match), 1.5x (2nd cascade), 2.0x (3rd), etc.
 *
 * Example: A match of 4 on the 3rd cascade:
 *   Base = 200, Extra = (4-3)*50 = 50, Combo = 2.0x
 *   Total = (200 + 50) * 2.0 = 500 points
 */
class ScoreCalculator {

    /**
     * Calculate the score for a single match.
     *
     * @param matchResult The match to score
     * @param comboLevel The current cascade depth (0 = first match, 1 = first cascade, etc.)
     * @return The score for this match
     */
    fun calculateMatchScore(matchResult: MatchResult, comboLevel: Int): Int {
        // Determine base score from match type
        val baseScore = when {
            matchResult.matchLength >= 5 -> 500  // Color bomb match
            matchResult.isLShape -> 300           // L or T shape
            matchResult.matchLength == 4 -> 200   // Striped candy match
            else -> 100                           // Regular match of 3
        }

        // Bonus for extra candies beyond the minimum 3
        val extraCandyBonus = maxOf(0, matchResult.positions.size - 3) * 50

        // Combo multiplier increases with each cascade level
        // Level 0 = 1.0x, Level 1 = 1.5x, Level 2 = 2.0x, etc.
        val comboMultiplier = 1.0 + (comboLevel * 0.5)

        return ((baseScore + extraCandyBonus) * comboMultiplier).toInt()
    }

    /**
     * Calculate the total score for multiple matches in one step.
     *
     * @param matches All matches found in one cascade step
     * @param comboLevel The current cascade depth
     * @return The total score for all matches
     */
    fun calculateTotalScore(matches: List<MatchResult>, comboLevel: Int): Int {
        return matches.sumOf { calculateMatchScore(it, comboLevel) }
    }

    /**
     * Calculate star rating based on score and level targets.
     *
     * @param score The player's score
     * @param targetScore Score needed for 1 star
     * @param twoStarScore Score needed for 2 stars
     * @param threeStarScore Score needed for 3 stars
     * @return Star count: 0, 1, 2, or 3
     */
    fun calculateStars(
        score: Int,
        targetScore: Int,
        twoStarScore: Int,
        threeStarScore: Int
    ): Int {
        return when {
            score >= threeStarScore -> 3
            score >= twoStarScore -> 2
            score >= targetScore -> 1
            else -> 0
        }
    }
}
