package com.galaxymatch.game.ui.game

import androidx.compose.ui.graphics.Color
import com.galaxymatch.game.model.Particle
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Manages a pool of particles for visual match-clear effects.
 *
 * The particle system:
 * 1. Spawns bursts of particles at gem positions when they're cleared
 * 2. Updates all particles each frame (move, apply gravity, fade, shrink)
 * 3. Removes dead particles (life <= 0)
 *
 * Performance notes:
 * - Uses a mutable list with in-place updates (no object allocation per frame)
 * - Capped at MAX_PARTICLES to prevent slowdowns on low-end devices
 * - Particles use the Particle class (mutable fields, not data class)
 *
 * This class is created via `remember { ParticleSystem() }` inside BoardCanvas
 * and updated each frame via a LaunchedEffect with withFrameMillis.
 */
class ParticleSystem {

    companion object {
        /** Safety cap to prevent performance issues.
         *  Raised from 300 to 500 to accommodate combo-scaled bursts,
         *  ice shatter, special activation effects, and confetti. */
        const val MAX_PARTICLES = 500
    }

    /** All currently active particles */
    val particles = mutableListOf<Particle>()

    /**
     * Spawn a burst of particles at the given position.
     *
     * Each particle flies outward in a random direction with random speed
     * and lifetime, creating a natural-looking explosion effect.
     *
     * @param x Center X position (Canvas pixels)
     * @param y Center Y position (Canvas pixels)
     * @param color The gem's color — particles inherit it
     * @param count Number of particles to spawn (8 for normal, 16 for specials)
     * @param speed Base speed multiplier — higher = faster outward movement
     * @param baseRadius Base radius for particle circles
     */
    fun spawn(
        x: Float,
        y: Float,
        color: Color,
        count: Int = 8,
        speed: Float = 200f,
        baseRadius: Float = 4f
    ) {
        // Don't exceed the cap
        if (particles.size >= MAX_PARTICLES) return
        val actualCount = minOf(count, MAX_PARTICLES - particles.size)

        for (i in 0 until actualCount) {
            // Random angle for outward direction (full 360 degrees)
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            // Random speed variation (50% to 150% of base)
            val spd = speed * (0.5f + Random.nextFloat())
            // Random lifetime between 0.4s and 0.8s
            val lifetime = 0.4f + Random.nextFloat() * 0.4f
            // Random radius variation
            val radius = baseRadius * (0.6f + Random.nextFloat() * 0.8f)

            particles.add(
                Particle(
                    x = x,
                    y = y,
                    velocityX = cos(angle) * spd,
                    velocityY = sin(angle) * spd,
                    color = color,
                    alpha = 1f,
                    radius = radius,
                    life = lifetime,
                    maxLife = lifetime
                )
            )
        }
    }

    // ===== Enhanced Spawn Methods =====
    // These specialized methods create different visual effects for
    // various game events, making the game feel more alive and rewarding.

    /**
     * Spawn particles scaled by combo level — bigger combos = more particles, faster, bigger.
     *
     * This replaces the basic `spawn()` call during cascade chains. At combo 0 it's
     * identical to the original; at combo 5+ it's a spectacular explosion.
     *
     * @param comboLevel Current cascade depth (0 = first match, 1 = first chain, etc.)
     * @param isSpecial Whether the cleared gem was a special type
     */
    fun spawnComboScaled(
        x: Float,
        y: Float,
        color: Color,
        comboLevel: Int,
        isSpecial: Boolean = false,
        baseRadius: Float = 4f
    ) {
        // Base: 8 normal / 16 special. Combo adds +4 per level
        val count = (if (isSpecial) 16 else 8) + comboLevel * 4
        // Speed increases with combo (caps at 2x)
        val speed = (if (isSpecial) 300f else 200f) * (1f + comboLevel * 0.15f).coerceAtMost(2f)
        // Radius grows slightly with combo
        val radius = baseRadius * (1f + comboLevel * 0.1f).coerceAtMost(1.5f)
        spawn(x, y, color, count.coerceAtMost(30), speed, radius)
    }

    /**
     * Spawn ice shatter particles — white/cyan shards that look like ice fragments.
     *
     * Used when a gem with ice overlay is cleared. The particles are shorter-lived
     * and slightly larger than regular particles, giving a crisp "shatter" feel.
     */
    fun spawnIceShatter(x: Float, y: Float, count: Int = 12, speed: Float = 250f) {
        // Mix of white and light cyan for an icy look
        val iceColors = listOf(Color.White, Color(0xFFAADDFF), Color(0xFF88CCEE))
        val actualCount = minOf(count, MAX_PARTICLES - particles.size)

        for (i in 0 until actualCount) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val spd = speed * (0.6f + Random.nextFloat() * 0.8f)
            // Shorter-lived than gem particles — ice shatters quickly
            val lifetime = 0.3f + Random.nextFloat() * 0.3f
            // Slightly larger shards for visual impact
            val radius = 3f + Random.nextFloat() * 3f

            particles.add(
                Particle(
                    x = x, y = y,
                    velocityX = cos(angle) * spd,
                    velocityY = sin(angle) * spd,
                    color = iceColors.random(),
                    alpha = 0.9f,
                    radius = radius,
                    life = lifetime,
                    maxLife = lifetime
                )
            )
        }
    }

    /**
     * Spawn a directional line burst for striped gem activations.
     *
     * Particles shoot out in the stripe's direction (horizontal or vertical)
     * with a small perpendicular spread, creating a "laser beam" effect.
     *
     * @param horizontal True for horizontal stripe, false for vertical
     * @param cellSize Used to scale the perpendicular spread
     */
    fun spawnStripedLine(
        x: Float,
        y: Float,
        color: Color,
        horizontal: Boolean,
        cellSize: Float
    ) {
        val count = 16
        val actualCount = minOf(count, MAX_PARTICLES - particles.size)

        for (i in 0 until actualCount) {
            // Small perpendicular spread so it looks like a line, not a point
            val spread = (Random.nextFloat() - 0.5f) * cellSize * 0.3f
            val speed = 400f + Random.nextFloat() * 200f
            // Half go left/up, half go right/down
            val direction = if (Random.nextBoolean()) 1f else -1f
            val lifetime = 0.5f + Random.nextFloat() * 0.3f

            // Horizontal stripe: fast X, small Y spread
            // Vertical stripe: small X spread, fast Y
            val vx = if (horizontal) speed * direction else spread * 3f
            val vy = if (horizontal) spread * 3f else speed * direction

            particles.add(
                Particle(
                    x = x, y = y,
                    velocityX = vx, velocityY = vy,
                    color = color,
                    alpha = 1f,
                    radius = 3f + Random.nextFloat() * 2f,
                    life = lifetime,
                    maxLife = lifetime
                )
            )
        }
    }

    /**
     * Spawn an expanding ring burst for wrapped gem activations.
     *
     * Particles are evenly distributed around a circle, creating a
     * satisfying "explosion ring" that expands outward uniformly.
     */
    fun spawnWrappedRing(x: Float, y: Float, color: Color) {
        val count = 20
        val actualCount = minOf(count, MAX_PARTICLES - particles.size)

        for (i in 0 until actualCount) {
            // Evenly spaced angles around the circle
            val angle = (i.toFloat() / count) * 2f * Math.PI.toFloat()
            val speed = 300f + Random.nextFloat() * 100f
            val lifetime = 0.4f + Random.nextFloat() * 0.3f

            particles.add(
                Particle(
                    x = x, y = y,
                    velocityX = cos(angle) * speed,
                    velocityY = sin(angle) * speed,
                    color = color,
                    alpha = 1f,
                    radius = 4f + Random.nextFloat() * 2f,
                    life = lifetime,
                    maxLife = lifetime
                )
            )
        }
    }

    /**
     * Spawn a rainbow multicolor burst for color bomb activations.
     *
     * Uses all 6 gem colors to create a spectacular chromatic explosion.
     * More particles and higher speed than regular spawns — color bombs
     * are the most powerful special, so they deserve the biggest effect!
     */
    fun spawnColorBombRainbow(x: Float, y: Float) {
        val rainbowColors = listOf(
            Color(0xFFFF4444), // Red
            Color(0xFF4488FF), // Blue
            Color(0xFF44DD44), // Green
            Color(0xFFFFDD44), // Yellow
            Color(0xFFFF8844), // Orange
            Color(0xFFDD44FF)  // Purple
        )
        val count = 24
        val actualCount = minOf(count, MAX_PARTICLES - particles.size)

        for (i in 0 until actualCount) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = 350f + Random.nextFloat() * 200f
            val lifetime = 0.5f + Random.nextFloat() * 0.4f

            particles.add(
                Particle(
                    x = x, y = y,
                    velocityX = cos(angle) * speed,
                    velocityY = sin(angle) * speed,
                    // Cycle through rainbow colors
                    color = rainbowColors[i % rainbowColors.size],
                    alpha = 1f,
                    radius = 4f + Random.nextFloat() * 3f,
                    life = lifetime,
                    maxLife = lifetime
                )
            )
        }
    }

    /**
     * Spawn celebratory confetti across the top of the canvas.
     *
     * Used when the player completes a level — particles rain down from
     * the top in multiple colors, creating a "fireworks" celebration.
     * Called in 3 waves (with delays) for a sustained confetti shower.
     *
     * @param canvasWidth Total canvas width to spread confetti across
     * @param canvasHeight Not used for spawn position, but available for scaling
     */
    fun spawnConfetti(canvasWidth: Float, @Suppress("UNUSED_PARAMETER") canvasHeight: Float) {
        val confettiColors = listOf(
            Color(0xFFFF4444), // Red
            Color(0xFF4488FF), // Blue
            Color(0xFF44DD44), // Green
            Color(0xFFFFDD44), // Yellow
            Color(0xFFFF8844), // Orange
            Color(0xFFDD44FF), // Purple
            Color(0xFFFFD700)  // Gold
        )
        // Spawn 40 confetti particles spread across the canvas width
        val count = minOf(40, MAX_PARTICLES - particles.size)

        for (i in 0 until count) {
            // Random X position across the full canvas width
            val px = Random.nextFloat() * canvasWidth
            // Fall downward with some horizontal drift
            val speed = 100f + Random.nextFloat() * 200f
            // Longer-lived than regular particles for a dramatic rain effect
            val lifetime = 1.0f + Random.nextFloat() * 0.5f

            particles.add(
                Particle(
                    x = px,
                    y = 0f, // Start from the very top of the canvas
                    velocityX = (Random.nextFloat() - 0.5f) * 150f, // Gentle horizontal drift
                    velocityY = speed, // Fall downward
                    color = confettiColors.random(),
                    alpha = 1f,
                    radius = 4f + Random.nextFloat() * 4f, // Larger than regular particles
                    life = lifetime,
                    maxLife = lifetime
                )
            )
        }
    }

    /**
     * Update all particles by one frame.
     *
     * Moves each particle along its velocity, applies light downward gravity
     * (so particles arc naturally), decays alpha and radius, and removes
     * any particles that have expired.
     *
     * @param deltaSeconds Time elapsed since last frame, in seconds
     */
    fun update(deltaSeconds: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()

            // Move the particle
            p.x += p.velocityX * deltaSeconds
            p.y += p.velocityY * deltaSeconds

            // Apply light gravity — particles arc downward slightly
            p.velocityY += 150f * deltaSeconds

            // Decay lifetime
            p.life -= deltaSeconds

            // Calculate visual decay based on remaining life fraction
            val lifeFraction = (p.life / p.maxLife).coerceIn(0f, 1f)
            p.alpha = lifeFraction
            // Shrink over time (multiplicative decay per frame)
            p.radius *= (1f - deltaSeconds * 1.5f).coerceAtLeast(0f)

            // Remove dead particles
            if (p.life <= 0f || p.alpha <= 0.01f || p.radius <= 0.3f) {
                iterator.remove()
            }
        }
    }

    /** Clear all particles (used implicitly on level restart via recomposition) */
    fun clear() {
        particles.clear()
    }

    /** Whether there are any active particles to draw */
    fun hasParticles(): Boolean = particles.isNotEmpty()
}
