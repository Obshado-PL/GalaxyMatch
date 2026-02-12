package com.galaxymatch.game.model

/**
 * Types of power-ups (boosters) the player can use during gameplay.
 *
 * Power-ups are purchased with stars — the same stars earned by completing
 * levels. Each power-up has a different cost and effect:
 *
 * - **Hammer**: Destroy a single gem of your choice (tap to select)
 * - **ColorBomb**: Remove ALL gems of one color (tap a gem to pick its color)
 * - **ExtraMoves**: Instantly adds 5 extra moves to the current level
 *
 * Power-ups do NOT cost a move — they're bonus actions on top of your normal swaps.
 *
 * @param displayName User-friendly name shown in the UI
 * @param emoji Icon emoji shown on the booster button
 * @param starCost How many stars this power-up costs to use
 * @param needsTarget Whether the player needs to tap a gem on the board to use this
 */
enum class PowerUpType(
    val displayName: String,
    val emoji: String,
    val starCost: Int,
    val needsTarget: Boolean
) {
    /** Destroy any single gem on the board. */
    Hammer("Hammer", "🔨", starCost = 3, needsTarget = true),

    /** Remove ALL gems of the tapped gem's color. */
    ColorBomb("Color Bomb", "🌈", starCost = 5, needsTarget = true),

    /** Add 5 extra moves to the current level (no target needed). */
    ExtraMoves("+5 Moves", "➕", starCost = 2, needsTarget = false)
}
