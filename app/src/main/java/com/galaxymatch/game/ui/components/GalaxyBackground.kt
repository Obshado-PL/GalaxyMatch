package com.galaxymatch.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.galaxymatch.game.model.BackgroundComet
import com.galaxymatch.game.model.BackgroundNebula
import com.galaxymatch.game.model.BackgroundStar
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animated galaxy background that renders behind all game content.
 *
 * Draws three layers of space elements on a single Canvas:
 * 1. Nebula clouds — large, barely visible color blobs that drift slowly
 * 2. Twinkling stars — small dots that pulse their brightness
 * 3. Falling comets — diagonal streaks with fading tails
 *
 * This composable is designed to be placed behind other content in a Box:
 * ```
 * Box(modifier = Modifier.fillMaxSize().background(GameBackground)) {
 *     GalaxyBackground()    // animated background layer
 *     Column { ... }        // actual screen content on top
 * }
 * ```
 *
 * Performance notes:
 * - Stars are generated once and only their alpha changes (no allocations)
 * - Comets are capped at 3 maximum to limit draw calls
 * - Nebulae use simple circle draws (no complex shaders)
 * - Uses withFrameNanos for smooth 60fps animation synced to display
 */
@Composable
fun GalaxyBackground(modifier: Modifier = Modifier) {

    // ============================================================
    // STARS: Generated once, positions never change
    // ============================================================
    val stars = remember {
        List(80) {
            BackgroundStar(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                radius = 0.5f + Random.nextFloat() * 2f,    // 0.5 to 2.5 pixels
                baseAlpha = 0.3f + Random.nextFloat() * 0.5f, // 0.3 to 0.8
                twinkleSpeed = 1f + Random.nextFloat() * 3f,  // 1 to 4 rad/s
                twinklePhase = Random.nextFloat() * 2f * PI.toFloat(),
                // Most stars are white, a few have subtle color tints
                color = when (Random.nextInt(10)) {
                    0 -> Color(0xFFAABBFF)    // Slight blue tint
                    1 -> Color(0xFFFFDDAA)    // Slight warm tint
                    else -> Color.White
                }
            )
        }
    }

    // ============================================================
    // NEBULAE: 3 slow-drifting color clouds
    // ============================================================
    val nebulae = remember {
        listOf(
            BackgroundNebula(
                x = 0.2f, y = 0.3f,
                radius = 0.25f,
                driftSpeedX = 0.005f, driftSpeedY = 0.002f,
                alpha = 0.07f,
                color = Color(0xFF6A0DAD) // Deep purple
            ),
            BackgroundNebula(
                x = 0.75f, y = 0.6f,
                radius = 0.2f,
                driftSpeedX = -0.003f, driftSpeedY = 0.004f,
                alpha = 0.05f,
                color = Color(0xFF1E3A5F) // Deep blue
            ),
            BackgroundNebula(
                x = 0.5f, y = 0.85f,
                radius = 0.18f,
                driftSpeedX = 0.002f, driftSpeedY = -0.003f,
                alpha = 0.06f,
                color = Color(0xFF8B1A6D) // Deep magenta
            )
        )
    }

    // ============================================================
    // COMETS: Dynamically spawned and removed
    // ============================================================
    val comets = remember { mutableStateListOf<BackgroundComet>() }

    // Time tracking for animation and comet spawning
    var elapsedTime by remember { mutableFloatStateOf(0f) }
    var nextCometTime by remember { mutableFloatStateOf(2f + Random.nextFloat() * 3f) }

    // Track screen size for comet spawning (set from Canvas)
    var screenWidth by remember { mutableFloatStateOf(0f) }
    var screenHeight by remember { mutableFloatStateOf(0f) }

    // ============================================================
    // ANIMATION LOOP: Runs continuously at ~60fps
    // ============================================================
    LaunchedEffect(Unit) {
        var lastFrameTime = withFrameNanos { it }

        while (isActive) {
            val frameTime = withFrameNanos { it }
            // Calculate delta time in seconds, capped to avoid jumps
            val dt = ((frameTime - lastFrameTime) / 1_000_000_000f).coerceAtMost(0.05f)
            lastFrameTime = frameTime

            elapsedTime += dt

            // --- Update nebulae positions (wrap around screen edges) ---
            for (nebula in nebulae) {
                nebula.x += nebula.driftSpeedX * dt
                nebula.y += nebula.driftSpeedY * dt
                // Wrap around with some padding so they don't pop in/out
                if (nebula.x > 1.3f) nebula.x = -0.3f
                if (nebula.x < -0.3f) nebula.x = 1.3f
                if (nebula.y > 1.3f) nebula.y = -0.3f
                if (nebula.y < -0.3f) nebula.y = 1.3f
            }

            // --- Update existing comets ---
            val iterator = comets.iterator()
            while (iterator.hasNext()) {
                val comet = iterator.next()
                // Save current position as a trail point
                comet.trail.add(0, Pair(comet.x, comet.y))
                if (comet.trail.size > comet.tailLength) {
                    comet.trail.removeAt(comet.trail.size - 1)
                }
                // Move the comet
                comet.x += comet.velocityX * dt
                comet.y += comet.velocityY * dt
                comet.life -= dt
                // Remove dead comets
                if (comet.life <= 0f) {
                    iterator.remove()
                }
            }

            // --- Spawn new comets periodically ---
            if (elapsedTime >= nextCometTime && comets.size < 3 && screenWidth > 0f) {
                comets.add(
                    BackgroundComet(
                        // Start from the right side of the screen, near the top
                        x = screenWidth * (0.5f + Random.nextFloat() * 0.6f),
                        y = -10f + Random.nextFloat() * screenHeight * 0.2f,
                        // Move diagonally: left and down
                        velocityX = -(120f + Random.nextFloat() * 130f),
                        velocityY = 80f + Random.nextFloat() * 100f,
                        headRadius = 2f + Random.nextFloat() * 2f,
                        life = 3f + Random.nextFloat() * 2f,
                        maxLife = 5f,
                        // Slight color variation: white to warm yellow
                        color = when (Random.nextInt(3)) {
                            0 -> Color(0xFFFFF8DC)  // Cornsilk (warm white)
                            1 -> Color(0xFFE0E8FF)  // Lavender (cool white)
                            else -> Color.White
                        }
                    )
                )
                // Schedule next comet in 2-5 seconds
                nextCometTime = elapsedTime + 2f + Random.nextFloat() * 3f
            }
        }
    }

    // ============================================================
    // CANVAS: Draws all three layers
    // ============================================================
    Canvas(modifier = modifier.fillMaxSize()) {
        // Store screen dimensions for comet spawning
        screenWidth = size.width
        screenHeight = size.height

        val w = size.width
        val h = size.height

        // --- Layer 1: Nebulae (lowest, most subtle) ---
        for (nebula in nebulae) {
            drawCircle(
                color = nebula.color.copy(alpha = nebula.alpha),
                radius = nebula.radius * maxOf(w, h),
                center = Offset(nebula.x * w, nebula.y * h)
            )
        }

        // --- Layer 2: Stars (middle layer) ---
        for (star in stars) {
            // Calculate twinkling alpha using a sine wave
            val twinkle = sin(elapsedTime * star.twinkleSpeed + star.twinklePhase)
            // Map sine wave (-1 to 1) to a small alpha variation around baseAlpha
            val alpha = (star.baseAlpha + twinkle * 0.25f).coerceIn(0.05f, 1f)

            drawCircle(
                color = star.color.copy(alpha = alpha),
                radius = star.radius,
                center = Offset(star.x * w, star.y * h)
            )
        }

        // --- Layer 3: Comets (topmost background layer) ---
        for (comet in comets) {
            val lifeRatio = (comet.life / comet.maxLife).coerceIn(0f, 1f)

            // Draw the tail: trail points with decreasing opacity and size
            for ((i, trailPos) in comet.trail.withIndex()) {
                val trailProgress = i.toFloat() / comet.tailLength
                val trailAlpha = lifeRatio * (1f - trailProgress) * 0.5f
                val trailRadius = comet.headRadius * (1f - trailProgress * 0.8f)

                if (trailAlpha > 0.01f) {
                    drawCircle(
                        color = comet.color.copy(alpha = trailAlpha),
                        radius = trailRadius,
                        center = Offset(trailPos.first, trailPos.second)
                    )
                }
            }

            // Draw the comet head: bright dot
            drawCircle(
                color = comet.color.copy(alpha = lifeRatio * 0.9f),
                radius = comet.headRadius,
                center = Offset(comet.x, comet.y)
            )

            // Draw a small glow around the head for extra brightness
            drawCircle(
                color = comet.color.copy(alpha = lifeRatio * 0.3f),
                radius = comet.headRadius * 2.5f,
                center = Offset(comet.x, comet.y)
            )
        }
    }
}
