package com.galaxymatch.game.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.galaxymatch.game.ServiceLocator
import com.galaxymatch.game.ui.components.GalaxyBackground
import com.galaxymatch.game.ui.theme.GameBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Splash screen shown when the app first launches.
 *
 * Displays an animated crystal gem icon with a cosmic glow, followed by
 * a staggered reveal of the game title and subtitle. A shimmer sweep
 * crosses the title text for extra polish.
 *
 * Animation timeline:
 *   0–600ms   → Gem fades in and scales up with glow pulse
 *   400–900ms → "Galaxy" title fades in
 *   600–1100ms → "Match" title fades in
 *   900–1400ms → Subtitle fades in
 *   +600ms hold → Navigate to level map
 *
 * @param onSplashComplete Called when the splash animation is done
 *                         and the app should navigate to the level map.
 */
@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {

    // ── Staggered fade-in animations ──────────────────────────────────

    // Gem icon: appears first (0–600ms)
    val gemAnimatable = remember { Animatable(0f) }
    // "Galaxy" text: starts at 400ms
    val galaxyAnimatable = remember { Animatable(0f) }
    // "Match" text: starts at 600ms
    val matchAnimatable = remember { Animatable(0f) }
    // Subtitle: starts at 900ms
    val subtitleAnimatable = remember { Animatable(0f) }

    // ── Infinite glow pulse on the gem ────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "gemGlow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    // ── Shimmer sweep across the title ────────────────────────────────
    // This creates a bright highlight that moves left → right continuously
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    // ── Sparkle rotation (small stars around the gem spin slowly) ─────
    val sparkleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkleRotation"
    )

    // ── Launch staggered animations + settings load ──────────────────
    LaunchedEffect(Unit) {
        // Kick off all animations with staggered delays
        // (each animateTo is non-blocking within its own coroutine)

        // 1) Gem fades in immediately (0–600ms)
        launch {
            gemAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
        }

        // 2) "Galaxy" starts at 400ms
        launch {
            delay(400)
            galaxyAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(500, easing = FastOutSlowInEasing)
            )
        }

        // 3) "Match" starts at 600ms
        launch {
            delay(600)
            matchAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(500, easing = FastOutSlowInEasing)
            )
        }

        // 4) Subtitle starts at 900ms
        launch {
            delay(900)
            subtitleAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(500, easing = FastOutSlowInEasing)
            )
        }

        // ── Load saved settings while animations play ─────────────
        val settings = ServiceLocator.settingsRepository.getSettings().first()
        ServiceLocator.soundManager.isSfxMuted = settings.sfxMuted
        ServiceLocator.soundManager.isMusicMuted = settings.musicMuted
        ServiceLocator.hapticManager.isHapticMuted = settings.hapticMuted

        // Start background music (SoundManager will respect the mute flag)
        if (!settings.musicMuted) {
            ServiceLocator.soundManager.startBackgroundMusic()
        }

        // Wait for all animations to finish + a brief hold
        delay(2000)
        // Navigate to level map
        onSplashComplete()
    }

    // ── Read animation progress values ────────────────────────────────
    val gemProgress by gemAnimatable.asState()
    val galaxyProgress by galaxyAnimatable.asState()
    val matchProgress by matchAnimatable.asState()
    val subtitleProgress by subtitleAnimatable.asState()

    // ── UI Layout ─────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground),
        contentAlignment = Alignment.Center
    ) {
        // Animated galaxy background (stars, comets, nebulae)
        GalaxyBackground()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {

            // ── Crystal Gem Icon ──────────────────────────────────
            // Drawn via Canvas with glow pulse and sparkle accents
            Canvas(
                modifier = Modifier
                    .size(120.dp)
                    .alpha(gemProgress)
                    .scale(0.5f + gemProgress * 0.5f) // Scale from 0.5 → 1.0
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val gemSize = size.minDimension * 0.38f

                // Outer glow (pulsing)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x337B68EE),
                            Color(0x1A7B68EE),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = gemSize * 2.2f * glowPulse
                    ),
                    radius = gemSize * 2.2f * glowPulse,
                    center = Offset(cx, cy)
                )

                // Inner glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x4400CED1),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = gemSize * 1.5f * glowPulse
                    ),
                    radius = gemSize * 1.5f * glowPulse,
                    center = Offset(cx, cy)
                )

                // Draw the multi-faceted gem
                drawGem(cx, cy, gemSize)

                // Draw rotating sparkles around the gem
                rotate(degrees = sparkleRotation, pivot = Offset(cx, cy)) {
                    drawSparkle(cx - gemSize * 1.4f, cy - gemSize * 0.8f, 6f, Color.White)
                    drawSparkle(cx + gemSize * 1.5f, cy - gemSize * 0.5f, 5f, Color(0xFF7B68EE))
                    drawSparkle(cx - gemSize * 1.1f, cy + gemSize * 1.2f, 4f, Color(0xFF00CED1))
                    drawSparkle(cx + gemSize * 1.2f, cy + gemSize * 1.0f, 5f, Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── "Galaxy" Title ─────────────────────────────────────
            // Shimmer brush overlays the text for a sweeping highlight effect
            Text(
                text = "Galaxy",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    // Shimmer: a bright band moves across the text
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF7B68EE),           // Base purple
                            Color(0xFFBBAAFF),           // Bright shimmer
                            Color(0xFF7B68EE)            // Back to base
                        ),
                        start = Offset(shimmerOffset * 500f, 0f),
                        end = Offset(shimmerOffset * 500f + 200f, 0f)
                    )
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(galaxyProgress)
                    .scale(0.7f + galaxyProgress * 0.3f) // Scale from 0.7 → 1.0
            )

            // ── "Match" Title ─────────────────────────────────────
            Text(
                text = "Match",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00CED1),           // Base teal
                            Color(0xFF66FFFF),           // Bright shimmer
                            Color(0xFF00CED1)            // Back to base
                        ),
                        start = Offset(shimmerOffset * 500f, 0f),
                        end = Offset(shimmerOffset * 500f + 200f, 0f)
                    )
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(matchProgress)
                    .scale(0.7f + matchProgress * 0.3f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Subtitle / tagline ────────────────────────────────
            Text(
                text = "Match gems across the galaxy",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(subtitleProgress)
                    .scale(0.8f + subtitleProgress * 0.2f)
            )
        }
    }
}

// ── Canvas helper: draw the multi-faceted crystal gem ──────────────────
/**
 * Draws a diamond-shaped gem with crown (top) and pavilion (bottom) facets.
 * Uses the same color palette as the app icon foreground for consistency.
 *
 * @param cx Center X coordinate
 * @param cy Center Y coordinate
 * @param size Half-width of the gem (tip to center)
 */
private fun DrawScope.drawGem(cx: Float, cy: Float, size: Float) {
    val top = cy - size * 1.2f    // Top tip
    val bottom = cy + size * 1.2f // Bottom tip
    val left = cx - size          // Left point
    val right = cx + size         // Right point
    val waist = cy - size * 0.1f  // Waist line (where crown meets pavilion)
    val tableY = cy - size * 0.4f // Table facet Y

    // --- Main gem body (rich purple) ---
    val bodyPath = Path().apply {
        moveTo(cx, top)
        lineTo(right, waist)
        lineTo(cx, bottom)
        lineTo(left, waist)
        close()
    }
    drawPath(bodyPath, color = Color(0xFF6A5ACD), style = Fill)

    // --- Crown: top facets (lighter purple, catches the light) ---
    val crownPath = Path().apply {
        moveTo(cx, top)
        lineTo(right, waist)
        lineTo(cx, tableY)
        lineTo(left, waist)
        close()
    }
    drawPath(crownPath, color = Color(0xFF7B68EE), style = Fill)

    // --- Crown: left highlight facet ---
    val crownLeftPath = Path().apply {
        moveTo(cx, top)
        lineTo(cx - size * 0.4f, tableY + size * 0.1f)
        lineTo(left, waist)
        close()
    }
    drawPath(crownLeftPath, color = Color(0xFF8B7BFF), style = Fill)

    // --- Crown: right shadow facet ---
    val crownRightPath = Path().apply {
        moveTo(cx, top)
        lineTo(cx + size * 0.4f, tableY + size * 0.1f)
        lineTo(right, waist)
        close()
    }
    drawPath(crownRightPath, color = Color(0xFF5A4ABD), style = Fill)

    // --- Table: center horizontal facet (brightest, like a window) ---
    val tablePath = Path().apply {
        moveTo(cx - size * 0.4f, tableY + size * 0.1f)
        lineTo(cx + size * 0.4f, tableY + size * 0.1f)
        lineTo(right, waist)
        lineTo(cx, tableY)
        lineTo(left, waist)
        close()
    }
    drawPath(tablePath, color = Color(0xFF9B8BFF), style = Fill)

    // --- Pavilion: bottom left facet (blue-purple, deeper) ---
    val pavLeftPath = Path().apply {
        moveTo(left, waist)
        lineTo(cx, tableY)
        lineTo(cx, bottom)
        close()
    }
    drawPath(pavLeftPath, color = Color(0xFF4A3AAD), style = Fill)

    // --- Pavilion: bottom right facet (teal accent) ---
    val pavRightPath = Path().apply {
        moveTo(right, waist)
        lineTo(cx, tableY)
        lineTo(cx, bottom)
        close()
    }
    drawPath(pavRightPath, color = Color(0xFF3A6A8D), style = Fill)

    // --- Pavilion: bottom-left sub-facet (color variation) ---
    val pavSubLeftPath = Path().apply {
        moveTo(left, waist)
        lineTo(cx, cy + size * 0.3f)
        lineTo(cx, bottom)
        close()
    }
    drawPath(pavSubLeftPath, color = Color(0xFF5548B8), style = Fill)

    // --- Pavilion: bottom-right sub-facet (teal shift) ---
    val pavSubRightPath = Path().apply {
        moveTo(right, waist)
        lineTo(cx, cy + size * 0.3f)
        lineTo(cx, bottom)
        close()
    }
    drawPath(pavSubRightPath, color = Color(0xFF2A8A9A), style = Fill)

    // --- Edge highlight (thin bright outline) ---
    drawPath(bodyPath, color = Color(0x55AABBFF), style = Stroke(width = 1.5f))

    // --- Top-left gloss / light reflection ---
    val glossPath = Path().apply {
        moveTo(cx - size * 0.3f, cy - size * 0.6f)
        lineTo(cx, top)
        lineTo(cx + size * 0.15f, cy - size * 0.55f)
        lineTo(cx - size * 0.15f, cy - size * 0.25f)
        close()
    }
    drawPath(glossPath, color = Color(0x55FFFFFF), style = Fill)
}

// ── Canvas helper: draw a 4-pointed sparkle star ──────────────────────
/**
 * Draws a small 4-pointed star (sparkle) at the given position.
 * Used as accent decorations around the gem icon.
 *
 * @param x Center X
 * @param y Center Y
 * @param radius Size of the sparkle
 * @param color Sparkle color
 */
private fun DrawScope.drawSparkle(x: Float, y: Float, radius: Float, color: Color) {
    val sparklePath = Path().apply {
        moveTo(x, y - radius)          // Top
        lineTo(x + radius * 0.3f, y - radius * 0.3f)
        lineTo(x + radius, y)          // Right
        lineTo(x + radius * 0.3f, y + radius * 0.3f)
        lineTo(x, y + radius)          // Bottom
        lineTo(x - radius * 0.3f, y + radius * 0.3f)
        lineTo(x - radius, y)          // Left
        lineTo(x - radius * 0.3f, y - radius * 0.3f)
        close()
    }
    drawPath(sparklePath, color = color.copy(alpha = 0.9f), style = Fill)
}
