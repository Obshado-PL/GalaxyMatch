package com.galaxymatch.game.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.galaxymatch.game.model.GemType
import com.galaxymatch.game.model.LevelConfig
import com.galaxymatch.game.model.ObjectiveType
import com.galaxymatch.game.ui.components.drawStar
import com.galaxymatch.game.ui.theme.HudBackground
import com.galaxymatch.game.ui.theme.StarEmpty
import com.galaxymatch.game.ui.theme.StarGold

/**
 * The heads-up display (HUD) shown above the game board.
 *
 * Displays:
 * - Level number
 * - Current score (and target score)
 * - Moves remaining
 * - Combo indicator (when chains are happening)
 *
 * @param score The player's current score
 * @param movesRemaining Number of moves left
 * @param levelConfig The current level's configuration (for target scores)
 * @param comboCount The current cascade combo depth (0 = no combo)
 * @param levelNumber The level number being played
 * @param comboAnimProgress Animation progress (0-1) for combo bounce effect
 * @param currentStars Number of stars currently earned (0-3) for the star progress display
 * @param starJustUnlocked Which star (1-3) just unlocked, 0 if none. Drives scale animation
 * @param starUnlockAnimProgress Animation progress (0-1) for the star unlock bounce
 * @param objectiveType The objective for this level (null = not yet loaded)
 * @param iceBroken How many ice blocks have been broken so far
 * @param totalIce Total ice blocks on the board at level start
 * @param gemsCleared How many of the target gem color have been cleared
 * @param targetGemCount How many target-color gems need to be cleared
 * @param targetGemType Which gem color is the target (for ClearGemType objective)
 * @param objectiveComplete Whether the objective has been met
 */
@Composable
fun GameHud(
    score: Int,
    movesRemaining: Int,
    levelConfig: LevelConfig?,
    comboCount: Int,
    levelNumber: Int,
    comboAnimProgress: Float = 0f,
    currentStars: Int = 0,
    starJustUnlocked: Int = 0,
    starUnlockAnimProgress: Float = 0f,
    objectiveType: ObjectiveType? = null,
    iceBroken: Int = 0,
    totalIce: Int = 0,
    gemsCleared: Int = 0,
    targetGemCount: Int = 0,
    targetGemType: GemType? = null,
    objectiveComplete: Boolean = false,
    isTimedMode: Boolean = false,
    timeRemaining: Int = 0,
    modifier: Modifier = Modifier
) {
    // === Detect special modes from sentinel level numbers ===
    val isDailyChallenge = levelNumber == -1
    // Animate the displayed score so it smoothly counts up to the actual score
    // rather than jumping instantly. Gives a satisfying "rolling numbers" feel.
    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        ),
        label = "scoreAnimation"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // === Level indicator ===
        // Show appropriate label for each mode:
        // - Timed: "TIMED" with a timer icon
        // - Daily: "DAILY"
        // - Normal: "LEVEL" with the number
        val levelLabel = when {
            isTimedMode -> "TIMED"
            isDailyChallenge -> "DAILY"
            else -> "LEVEL"
        }
        val levelValue = when {
            isTimedMode -> "\u23F1" // Stopwatch emoji
            isDailyChallenge -> "\uD83D\uDCC5" // Calendar emoji
            else -> levelNumber.toString()
        }
        HudItem(
            label = levelLabel,
            value = levelValue,
            modifier = Modifier.weight(1f)
        )

        // === Score display ===
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(2f)
        ) {
            Text(
                text = "SCORE",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = animatedScore.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = StarGold
            )
            // Show objective-aware target display
            // Timed mode: no target display (just score as high as possible)
            // Each objective type gets its own indicator:
            // - ReachScore: "Target: 4000" (classic, unchanged)
            // - BreakAllIce: "🧊 Ice: 2/4" (light blue, turns gold when complete)
            // - ClearGemType: "🔴 Red: 15/25" (gem's color, turns gold when complete)
            if (levelConfig != null && !isTimedMode) {
                when (objectiveType) {
                    is ObjectiveType.BreakAllIce -> {
                        val objectiveColor = if (objectiveComplete) StarGold
                            else Color(0xFF88DDFF) // Light blue for ice
                        Text(
                            text = "🧊 Ice: $iceBroken/$totalIce",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 12.sp,
                                fontWeight = if (objectiveComplete) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = objectiveColor
                        )
                    }
                    is ObjectiveType.ClearGemType -> {
                        // Use the gem's color for the display, or gold when complete
                        val objectiveColor = if (objectiveComplete) StarGold
                            else targetGemType?.color ?: Color.White
                        val gemName = targetGemType?.name ?: "?"
                        // Pick an emoji that roughly matches the gem color
                        val gemEmoji = when (targetGemType) {
                            GemType.Red -> "🔴"
                            GemType.Blue -> "🔵"
                            GemType.Green -> "🟢"
                            GemType.Yellow -> "🟡"
                            GemType.Orange -> "🟠"
                            GemType.Purple -> "🟣"
                            null -> "💎"
                        }
                        Text(
                            text = "$gemEmoji $gemName: $gemsCleared/$targetGemCount",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 12.sp,
                                fontWeight = if (objectiveComplete) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = objectiveColor
                        )
                    }
                    else -> {
                        // ReachScore (default) — show target score like before
                        Text(
                            text = "Target: ${levelConfig.targetScore}",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp),
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                // === Feature 3: Star progress indicator ===
                // Shows 3 small stars that light up as score crosses thresholds.
                // When a star is just unlocked, it bounces with an overshoot animation.
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    for (i in 1..3) {
                        val isEarned = i <= currentStars
                        val isUnlocking = i == starJustUnlocked && starUnlockAnimProgress > 0f

                        // Scale animation: overshoots to 1.4x then settles to 1.0x
                        val starScale = if (isUnlocking) {
                            val overshoot = 1.4f
                            if (starUnlockAnimProgress < 0.4f) {
                                // First 40%: scale up from 1.0 to 1.4 (overshoot)
                                1f + (overshoot - 1f) * (starUnlockAnimProgress / 0.4f)
                            } else {
                                // Last 60%: settle from 1.4 back to 1.0
                                overshoot - (overshoot - 1f) * ((starUnlockAnimProgress - 0.4f) / 0.6f)
                            }
                        } else {
                            1f
                        }

                        Canvas(
                            modifier = Modifier
                                .size(16.dp)
                                .scale(starScale)
                        ) {
                            val color = if (isEarned) StarGold else StarEmpty
                            drawStar(color, center, size.minDimension / 2f)
                        }
                    }
                }

                // === Thin progress bar toward next star ===
                // Shows how close the player is to earning the next star.
                // When all stars are earned, the bar is full.
                val (progressStart, progressEnd) = when (currentStars) {
                    0 -> 0 to levelConfig.targetScore
                    1 -> levelConfig.targetScore to levelConfig.twoStarScore
                    2 -> levelConfig.twoStarScore to levelConfig.threeStarScore
                    else -> levelConfig.threeStarScore to levelConfig.threeStarScore
                }

                val progressFraction = if (progressEnd > progressStart) {
                    ((score - progressStart).toFloat() / (progressEnd - progressStart))
                        .coerceIn(0f, 1f)
                } else {
                    1f // All stars earned
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(4.dp)
                        .padding(top = 2.dp)
                ) {
                    // Background track
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.2f),
                        size = size,
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    // Filled portion — gold bar shows progress toward next star
                    drawRoundRect(
                        color = StarGold,
                        size = Size(size.width * progressFraction, size.height),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            } else if (isTimedMode) {
                // Timed mode: show a "High score" subtitle instead of target
                Text(
                    text = "Score as high as you can!",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        // === Moves or Timer (with urgency pulse when low) ===
        // Timed mode: shows countdown timer instead of moves count.
        // Both modes pulse when running low to create urgency.
        val isLowMoves = if (isTimedMode) timeRemaining <= 10 else movesRemaining <= 5
        val movesColor = if (isLowMoves) Color(0xFFFF6666) else Color.White

        val movesPulseTransition = rememberInfiniteTransition(label = "movesPulse")
        val movesPulseProgress by movesPulseTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "movesPulseValue"
        )
        // Only apply the pulse scale when running low
        val movesPulseScale = if (isLowMoves) 1f + 0.06f * movesPulseProgress else 1f

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            if (isTimedMode) {
                // Timed mode: show countdown timer
                Text(
                    text = "TIME",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                val minutes = timeRemaining / 60
                val seconds = timeRemaining % 60
                Text(
                    text = "%d:%02d".format(minutes, seconds),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = movesColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.scale(movesPulseScale)
                )
            } else {
                // Normal mode: show moves remaining
                Text(
                    text = "MOVES",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = movesRemaining.toString(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = movesColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.scale(movesPulseScale)
                )
            }
        }
    }

    // === Combo indicator with bounce animation ===
    // IMPORTANT: We always reserve a fixed height for this area, even when
    // no combo is active. If we used `if (comboCount > 0)`, the combo Box
    // appearing/disappearing would change the HUD height, causing the board
    // below (which uses weight(1f)) to resize and visually jump up/down.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)   // Fixed height — prevents layout shift
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (comboCount > 0) {
            // Calculate bounce scale: overshoots to 1.2x then settles to 1.0x
            // This gives a satisfying "pop in" effect when a combo happens
            val bounceScale = if (comboAnimProgress > 0f) {
                val overshoot = 1.2f
                if (comboAnimProgress < 0.5f) {
                    // First half: scale from small to overshoot (0 → 1.2)
                    comboAnimProgress * 2f * overshoot
                } else {
                    // Second half: settle from overshoot to normal (1.2 → 1.0)
                    overshoot - (comboAnimProgress - 0.5f) * 2f * (overshoot - 1f)
                }
            } else {
                1f  // No animation active, show at normal size
            }

            // === Feature 1: Escalating combo colors ===
            // Higher combos get warmer, more urgent colors:
            // green for small combos → yellow → orange → red for massive chains
            val comboColor = when {
                comboCount >= 5 -> Color(0xFFFF2222) // Red — massive combo!
                comboCount >= 3 -> Color(0xFFFF8844) // Orange — big combo
                comboCount >= 2 -> Color(0xFFFFDD44) // Yellow — good combo
                else -> Color(0xFF44DD44)            // Green — first combo
            }

            // Text grows slightly larger for bigger combos (20sp → 28sp max)
            val comboFontSize = (20 + (comboCount.coerceAtMost(5) * 2)).sp

            Text(
                text = "COMBO x${comboCount + 1}!",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = comboFontSize
                ),
                color = comboColor,
                modifier = Modifier
                    .scale(bounceScale)
                    .clip(RoundedCornerShape(12.dp))
                    // Colored glow background — tinted to match combo level
                    .background(comboColor.copy(alpha = 0.15f))
                    .background(HudBackground)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * A single HUD item with a label and a value.
 */
@Composable
private fun HudItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            ),
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = valueColor,
            textAlign = TextAlign.Center
        )
    }
}
