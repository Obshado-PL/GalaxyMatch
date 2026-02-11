package com.candycrush.game.model

/**
 * Configuration for a single game level.
 *
 * Each level defines the board size, move limit, score targets, and
 * how many candy colors are in play. These parameters control difficulty:
 *
 * Easier levels: fewer colors (4), more moves, lower score targets, smaller board
 * Harder levels: more colors (6), fewer moves, higher score targets, larger board
 *
 * @param levelNumber The level number (1, 2, 3, ...)
 * @param rows Board height (number of rows, typically 7-9)
 * @param cols Board width (number of columns, typically 7-9)
 * @param maxMoves Maximum number of swaps the player can make
 * @param targetScore Score needed to pass the level (1 star)
 * @param twoStarScore Score needed for 2 stars
 * @param threeStarScore Score needed for 3 stars
 * @param availableCandyTypes How many candy colors are used (3-6, fewer = easier)
 * @param description Optional description shown before the level starts
 */
data class LevelConfig(
    val levelNumber: Int,
    val rows: Int = 8,
    val cols: Int = 8,
    val maxMoves: Int,
    val targetScore: Int,
    val twoStarScore: Int,
    val threeStarScore: Int,
    val availableCandyTypes: Int = 5,
    val description: String = ""
)
