package com.galaxymatch.game.ui.results

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.galaxymatch.game.ui.components.GameButton
import com.galaxymatch.game.ui.components.drawStar
import com.galaxymatch.game.ui.components.GalaxyBackground
import com.galaxymatch.game.ui.game.ParticleSystem
import com.galaxymatch.game.ui.theme.GameBackground
import com.galaxymatch.game.model.TimedDifficulty
import com.galaxymatch.game.ui.theme.StarEmpty
import com.galaxymatch.game.ui.theme.StarGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

/**
 * Results screen shown after completing or failing a level.
 *
 * Displays:
 * - Win/lose message
 * - Final score (animated counter that counts up from 0)
 * - Star rating (for wins, staggered bounce-in)
 * - "NEW HIGH SCORE!" banner with spring bounce-in (if applicable)
 * - Full-screen confetti particle overlay (on win)
 * - Buttons to play again, go to next level, or return to map
 *
 * @param levelNumber The level that was just played
 * @param score The final score
 * @param stars Stars earned (0-3)
 * @param won True if the player passed the level
 * @param objectiveText Objective status text (e.g. "All ice broken!"). Empty = no objective display.
 * @param isNewHighScore True if this score beats the previous personal best for this level.
 * @param onPlayAgain Called to replay the same level
 * @param onNextLevel Called to play the next level
 * @param onBackToMap Called to return to the level map
 */
@Composable
fun ResultsScreen(
    levelNumber: Int,
    score: Int,
    stars: Int,
    won: Boolean,
    objectiveText: String = "",
    isNewHighScore: Boolean = false,
    onPlayAgain: () -> Unit,
    onNextLevel: () -> Unit,
    onBackToMap: () -> Unit
) {
    // === Detect special modes from sentinel level numbers ===
    val isDailyChallenge = levelNumber == -1
    val isTimedMode = levelNumber <= -100
    val timedDifficulty: TimedDifficulty? =
        if (isTimedMode) TimedDifficulty.entries.getOrNull(-(levelNumber + 100)) else null
    val isSpecialMode = isDailyChallenge || isTimedMode

    val hasNext = !isSpecialMode && ResultsHelper.hasNextLevel(levelNumber)

    // === Animated score counter ===
    // Counts from 0 → finalScore over a duration that scales with score magnitude.
    // Starts after a 600ms delay (after star animations begin).
    val animatedScore = remember { Animatable(0f) }

    // === "NEW HIGH SCORE!" banner animation ===
    // Uses a spring bounce-in for a celebratory feel.
    val highScoreBannerScale = remember { Animatable(0f) }

    // === Full-screen confetti particle system (wins only) ===
    val confettiSystem = remember { ParticleSystem() }
    // Track whether confetti has been spawned to avoid re-spawning on recomposition
    var confettiSpawned by remember { mutableStateOf(false) }
    // Canvas dimensions for confetti (set via BoxWithConstraints)
    var canvasWidth by remember { mutableFloatStateOf(0f) }
    var canvasHeight by remember { mutableFloatStateOf(0f) }

    // === Launch animation timeline ===
    LaunchedEffect(Unit) {
        // Wait for star animations to start (they begin at 200ms intervals)
        delay(600)

        // Start counting up the score
        // Duration scales with score magnitude: 800ms minimum, up to 2000ms for big scores
        val countDuration = (800 + (score / 1000) * 200).coerceIn(800, 2000)
        animatedScore.animateTo(
            targetValue = score.toFloat(),
            animationSpec = tween(
                durationMillis = countDuration,
                easing = FastOutSlowInEasing
            )
        )

        // After score counter finishes, show high score banner if applicable
        if (isNewHighScore && won) {
            delay(200) // Brief pause after score counter
            highScoreBannerScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    // === Confetti spawn + frame update loop ===
    // Spawns 3 waves of confetti across the screen and updates particles each frame.
    LaunchedEffect(won) {
        if (!won) return@LaunchedEffect

        // Wait for canvas dimensions to be available and a brief entrance delay
        delay(300)
        if (canvasWidth <= 0f || canvasHeight <= 0f) return@LaunchedEffect

        // Spawn 3 waves of confetti with staggered delays
        if (!confettiSpawned) {
            confettiSpawned = true

            // Wave 1: 60 particles spread across the top
            confettiSystem.spawnConfetti(canvasWidth, canvasHeight)
            confettiSystem.spawnConfetti(canvasWidth, canvasHeight)

            delay(400)

            // Wave 2: 40 more particles
            confettiSystem.spawnConfetti(canvasWidth, canvasHeight)

            delay(500)

            // Wave 3: 30 more particles
            confettiSystem.spawnFireworkBurst(
                canvasWidth * 0.5f,
                canvasHeight * 0.25f,
                Color(0xFFFFD700) // Gold
            )
        }

        // Frame update loop: update particle positions at ~60fps
        var lastFrameTime = System.nanoTime()
        while (isActive && confettiSystem.hasParticles()) {
            delay(16) // ~60fps
            val now = System.nanoTime()
            val deltaSeconds = (now - lastFrameTime) / 1_000_000_000f
            lastFrameTime = now
            confettiSystem.update(deltaSeconds.coerceAtMost(0.05f))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
    ) {
        // Animated galaxy background (stars, comets, nebulae)
        GalaxyBackground()

        // === Confetti particle overlay ===
        // Rendered between the background and the content Column so particles
        // appear behind the text but above the galaxy background.
        if (won) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                // Capture canvas dimensions for confetti spawning
                canvasWidth = size.width
                canvasHeight = size.height

                // Draw all active particles
                for (particle in confettiSystem.particles) {
                    drawCircle(
                        color = particle.color.copy(alpha = particle.alpha),
                        radius = particle.radius,
                        center = Offset(particle.x, particle.y)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        // === Win/Lose Title ===
        // Special modes get tailored titles instead of generic "Level Complete!"
        val titleText = when {
            isTimedMode && won -> "Time's Up!"
            isTimedMode -> "Time's Up!"
            isDailyChallenge && won -> "Challenge Complete!"
            isDailyChallenge -> "Try Again!"
            won -> "Level Complete!"
            else -> "Try Again!"
        }
        Text(
            text = titleText,
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.ExtraBold
            ),
            color = if (won) Color(0xFFFFD700) else Color(0xFFFF6666),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Show appropriate subtitle for each mode
        val subtitleText = when {
            isTimedMode -> "\u23F1 Timed Challenge \u2014 ${timedDifficulty?.label ?: "Unknown"}"
            isDailyChallenge -> "\uD83D\uDCC5 Daily Challenge"
            else -> "Level $levelNumber"
        }
        Text(
            text = subtitleText,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White.copy(alpha = 0.7f)
        )

        // === Objective status text ===
        // Shows what the objective was and whether it was completed.
        // Green for success, red for failure. Only shown for non-score objectives.
        if (objectiveText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = objectiveText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (won) Color(0xFF44DD44) else Color(0xFFFF8888),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // === Star Rating with staggered bounce-in ===
        // Each earned star bounces in one-by-one with a 200ms delay,
        // creating a satisfying reveal sequence.
        if (won && stars > 0) {
            val star1Scale = remember { Animatable(0f) }
            val star2Scale = remember { Animatable(0f) }
            val star3Scale = remember { Animatable(0f) }
            val starAnims = listOf(star1Scale, star2Scale, star3Scale)

            // Animate earned stars sequentially with staggered delays
            LaunchedEffect(Unit) {
                for (i in 0 until stars) {
                    delay(200L) // Stagger: each star starts 200ms after the previous
                    starAnims[i].animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 400,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
            }

            Row {
                for (i in 0 until 3) {
                    val isEarned = i < stars
                    // Overshoot: scale up to 1.3x then settle to 1.0x
                    val rawScale = starAnims[i].value
                    val displayScale = if (isEarned && rawScale < 1f) {
                        // During animation: overshoot to 1.3x at peak
                        val overshoot = 1.3f
                        if (rawScale < 0.5f) {
                            rawScale * 2f * overshoot
                        } else {
                            overshoot - (rawScale - 0.5f) * 2f * (overshoot - 1f)
                        }
                    } else if (isEarned) {
                        1f // Animation complete
                    } else {
                        1f // Unearned stars shown at normal size (empty)
                    }

                    Canvas(
                        modifier = Modifier
                            .size(48.dp)
                            .scale(displayScale)
                    ) {
                        val color = if (isEarned && rawScale > 0f) StarGold else StarEmpty
                        drawStar(color, center, size.minDimension / 2f)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // === Animated Score Counter ===
        // Counts from 0 up to the final score for a satisfying reveal.
        // The displayed value is the animated float rounded to an integer.
        Text(
            text = "Score",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = animatedScore.value.roundToInt().toString(),
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color.White
        )

        // === "NEW HIGH SCORE!" Banner ===
        // Gold text with shimmer gradient, bounces in with spring animation.
        // Only shown when isNewHighScore is true and the player won.
        if (isNewHighScore && won) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\u2B50 NEW HIGH SCORE! \u2B50",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    // Gold shimmer gradient for celebratory feel
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFD700), // Gold
                            Color(0xFFFFF176), // Light gold
                            Color(0xFFFFD700)  // Gold
                        )
                    )
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .scale(highScoreBannerScale.value)
                    .alpha(highScoreBannerScale.value.coerceIn(0f, 1f))
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // === Action Buttons ===
        if (won && hasNext) {
            GameButton(
                text = "Next Level",
                onClick = onNextLevel,
                color = Color(0xFF44BB44) // Green
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        GameButton(
            text = if (won) "Play Again" else "Retry",
            onClick = onPlayAgain,
            color = Color(0xFF6650a4) // Purple
        )

        Spacer(modifier = Modifier.height(12.dp))

        GameButton(
            text = "Back to Map",
            onClick = onBackToMap,
            color = Color.White.copy(alpha = 0.15f)
        )
        } // Column
    } // Box
}
