package com.candycrush.game.model

/**
 * The type of special power a candy can have.
 *
 * Special candies are created when the player matches 4 or more candies
 * in specific patterns. When a special candy is later matched or activated,
 * it triggers a powerful effect that clears additional candies.
 *
 * How each special is created:
 * - None:              Regular candy (match 3)
 * - StripedHorizontal: Match 4 in a column → clears the entire row when activated
 * - StripedVertical:   Match 4 in a row → clears the entire column when activated
 * - Wrapped:           Match in L or T shape → explodes a 3x3 area when activated
 * - ColorBomb:         Match 5 in a row → clears all candies of one color when activated
 */
enum class SpecialType {
    None,
    StripedHorizontal,
    StripedVertical,
    Wrapped,
    ColorBomb
}
