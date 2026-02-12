package com.galaxymatch.game.model

import androidx.compose.ui.graphics.Color

/**
 * Data models for the animated galaxy background.
 *
 * These represent the visual elements that make the space theme come alive:
 * twinkling stars, streaking comets, and drifting nebula clouds.
 *
 * All positions use normalized coordinates (0.0 to 1.0) relative to screen
 * dimensions, except for comets which use pixel coordinates for smooth movement.
 */

/**
 * A static twinkling star in the galaxy background.
 *
 * Stars stay at fixed positions and pulse their brightness (alpha)
 * using a sine wave, creating a subtle twinkling effect.
 *
 * @param x Normalized X position (0.0 = left edge, 1.0 = right edge)
 * @param y Normalized Y position (0.0 = top edge, 1.0 = bottom edge)
 * @param radius Size of the star dot in pixels (typically 1-3)
 * @param baseAlpha The resting brightness level (0.3 to 0.8)
 * @param twinkleSpeed How fast this star pulses (radians per second)
 * @param twinklePhase Phase offset so not all stars pulse in sync
 * @param color Star color (white by default, some can be slightly tinted)
 */
data class BackgroundStar(
    val x: Float,
    val y: Float,
    val radius: Float,
    val baseAlpha: Float,
    val twinkleSpeed: Float,
    val twinklePhase: Float,
    val color: Color = Color.White
)

/**
 * A comet that streaks diagonally across the screen.
 *
 * Comets have a bright head and a fading tail made of trail points.
 * They spawn at the top of the screen and move diagonally downward,
 * giving a sense of motion through space.
 *
 * This is a mutable class (not a data class) for performance —
 * we update position in-place each frame instead of allocating new objects.
 *
 * @param x Current X position in pixels (mutated each frame)
 * @param y Current Y position in pixels (mutated each frame)
 * @param velocityX Horizontal speed in pixels per second (typically negative = left)
 * @param velocityY Vertical speed in pixels per second (typically positive = down)
 * @param headRadius Radius of the bright comet head in pixels
 * @param tailLength Number of trail positions to remember for the tail
 * @param life Remaining life in seconds (decremented each frame)
 * @param maxLife Total life span in seconds (used for alpha calculation)
 * @param color Comet color (white/yellow tones look best)
 */
class BackgroundComet(
    var x: Float,
    var y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val headRadius: Float = 3f,
    val tailLength: Int = 12,
    var life: Float,
    val maxLife: Float,
    val color: Color = Color.White
) {
    /** Trail stores recent positions for drawing the fading tail effect */
    val trail = mutableListOf<Pair<Float, Float>>()
}

/**
 * A soft nebula cloud that drifts slowly across the background.
 *
 * Nebulae are large, semi-transparent blobs that add depth and color
 * to the space background. They're very subtle (low alpha) so they
 * don't distract from gameplay.
 *
 * This is a mutable class for in-place position updates each frame.
 *
 * @param x Normalized center X position (0.0 to 1.0)
 * @param y Normalized center Y position (0.0 to 1.0)
 * @param radius Normalized radius (fraction of screen dimension)
 * @param driftSpeedX Horizontal drift speed (normalized units per second)
 * @param driftSpeedY Vertical drift speed (normalized units per second)
 * @param alpha Opacity (0.05 to 0.15 — very subtle so it doesn't distract)
 * @param color Nebula color (soft purples, blues, or pinks work best)
 */
class BackgroundNebula(
    var x: Float,
    var y: Float,
    val radius: Float,
    val driftSpeedX: Float,
    val driftSpeedY: Float,
    val alpha: Float,
    val color: Color
)
