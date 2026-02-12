package com.galaxymatch.game.ui.results

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.galaxymatch.game.ui.components.GameButton
import com.galaxymatch.game.ui.components.drawStar
import com.galaxymatch.game.ui.components.GalaxyBackground
import com.galaxymatch.game.ui.theme.GameBackground
import com.galaxymatch.game.model.TimedDifficulty
import com.galaxymatch.game.ui.theme.StarEmpty
import com.galaxymatch.game.ui.theme.StarGold
import kotlinx.coroutines.delay

/**
 * Results screen shown after completing or failing a level.
 *
 * Displays:
 * - Win/lose message
 * - Final score
 * - Star rating (for wins)
 * - Buttons to play again, go to next level, or return to map
 *
 * @param levelNumber The level that was just played
 * @param score The final score
 * @param stars Stars earned (0-3)
 * @param won True if the player passed the level
 * @param objectiveText Objective status text (e.g. "All ice broken!"). Empty = no objective display.
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
    ) {
        // Animated galaxy background (stars, comets, nebulae)
        GalaxyBackground()

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
            isTimedMode -> "\u23F1 Timed Challenge — ${timedDifficulty?.label ?: "Unknown"}"
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

        // === Score ===
        Text(
            text = "Score",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color.White
        )

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
