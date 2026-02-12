package com.galaxymatch.game.model

import androidx.compose.ui.graphics.Color

/**
 * A single visual particle for match-clear effects.
 *
 * Particles are small colored circles that fly outward from a gem's
 * position when it is cleared. They fade out and shrink over their lifetime.
 *
 * This is a mutable CLASS (not a data class) for performance — we update
 * particles in-place each frame instead of creating new objects. This avoids
 * garbage collection pressure during fast animations on mobile devices.
 *
 * @param x Current X position in Canvas pixels
 * @param y Current Y position in Canvas pixels
 * @param velocityX Horizontal speed in pixels per second
 * @param velocityY Vertical speed in pixels per second
 * @param color The particle's color (matches the gem it came from)
 * @param alpha Current opacity (starts at 1, fades to 0)
 * @param radius Current size (starts large, shrinks to 0)
 * @param life Remaining life in seconds (counts down from maxLife to 0)
 * @param maxLife Total lifetime in seconds (used to calculate decay rate)
 */
class Particle(
    var x: Float,
    var y: Float,
    var velocityX: Float,
    var velocityY: Float,
    var color: Color,
    var alpha: Float,
    var radius: Float,
    var life: Float,
    var maxLife: Float
)
