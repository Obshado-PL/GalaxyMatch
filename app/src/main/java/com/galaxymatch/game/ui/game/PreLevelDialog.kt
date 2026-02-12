package com.galaxymatch.game.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galaxymatch.game.model.GemType
import com.galaxymatch.game.model.LevelConfig
import com.galaxymatch.game.model.ObjectiveType
import com.galaxymatch.game.model.ObstacleType
import com.galaxymatch.game.model.TimedDifficulty

/**
 * Pre-level dialog shown before each level starts.
 *
 * Tells the player what they need to do before the board loads:
 * - Level number (big gold title)
 * - Objective description (what to do to win)
 * - Level description (flavor text, if any)
 * - Moves and target score info
 * - A "Play!" button to begin
 *
 * Follows the same visual style as TutorialOverlay: full-screen scrim
 * with a dark purple card centered on screen.
 *
 * @param levelNumber Which level is about to start
 * @param levelConfig The config containing objective, description, and target scores
 * @param onPlay Called when the player taps "Play!" to start the level
 */
@Composable
fun PreLevelDialog(
    levelNumber: Int,
    levelConfig: LevelConfig,
    onPlay: () -> Unit
) {
    // === Detect special modes from sentinel level numbers ===
    val isDailyChallenge = levelNumber == -1
    val isTimedMode = levelNumber <= -100
    val timedDifficulty: TimedDifficulty? =
        if (isTimedMode) TimedDifficulty.entries.getOrNull(-(levelNumber + 100)) else null

    // Full-screen semi-transparent scrim (same as TutorialOverlay)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        // Dark purple card matching the game's overlay style
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(32.dp)
                .background(
                    color = Color(0xFF2D2B55),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(28.dp)
        ) {
            // === Title ===
            // Show appropriate title for each mode:
            // - Timed: "Timed Challenge" with difficulty
            // - Daily: "Daily Challenge"
            // - Normal: "Level #"
            val titleText = when {
                isTimedMode -> "\u23F1 Timed Challenge"
                isDailyChallenge -> "\uD83D\uDCC5 Daily Challenge"
                else -> "Level $levelNumber"
            }
            Text(
                text = titleText,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color(0xFFFFD700), // Gold — same as TutorialOverlay title
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // === Objective description ===
            // Timed mode: explain the time-based scoring
            // Normal/Daily: standard objective text
            val objectiveText = when {
                isTimedMode -> "\u26A1 Score as many points as you can before time runs out!"
                else -> buildObjectiveText(levelConfig)
            }
            Text(
                text = objectiveText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 24.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // === Level description (flavor text) ===
            // Only shown if the level has a custom description.
            // Dimmer color so it doesn't compete with the objective.
            if (levelConfig.description.isNotBlank()) {
                Text(
                    text = levelConfig.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // === Mode-specific info ===
            if (isTimedMode && timedDifficulty != null) {
                // Timed mode: show difficulty and time instead of moves/target
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    // Difficulty label
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = timedDifficulty.label,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = when (timedDifficulty) {
                                TimedDifficulty.Easy -> Color(0xFF44DD44)
                                TimedDifficulty.Medium -> Color(0xFFFFAA33)
                                TimedDifficulty.Hard -> Color(0xFFFF4444)
                            }
                        )
                        Text(
                            text = "Difficulty",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    // Divider dot
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White.copy(alpha = 0.3f)
                    )

                    // Time
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${timedDifficulty.seconds}s",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFFFD700) // Gold
                        )
                        Text(
                            text = "Time",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                // Normal/Daily mode: show moves and target score
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    // Moves count
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${levelConfig.maxMoves}",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "Moves",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    // Divider dot
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White.copy(alpha = 0.3f)
                    )

                    // Target score (for star rating)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${levelConfig.targetScore}",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFFFD700) // Gold
                        )
                        Text(
                            text = "Target",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // === Play button ===
            // Green button matching the tutorial's "Got it!" style.
            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF44BB44) // Same green as tutorial
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Play!",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Build a human-readable objective description with emoji from the level config.
 *
 * Examples:
 * - ReachScore: "⭐ Score 4000 points!"
 * - BreakAllIce: "🧊 Break all 8 ice blocks!"
 * - ClearGemType: "🔴 Clear 25 Red gems!"
 */
private fun buildObjectiveText(config: LevelConfig): String {
    return when (val objective = config.objective) {
        is ObjectiveType.ReachScore -> {
            "⭐ Score ${config.targetScore} points!"
        }
        is ObjectiveType.BreakAllIce -> {
            val iceCount = config.obstacles.count { it.value == ObstacleType.Ice }
            "🧊 Break all $iceCount ice blocks!"
        }
        is ObjectiveType.ClearGemType -> {
            val colorName = objective.gemType.name
            // Pick a matching emoji for the gem color
            val emoji = when (objective.gemType) {
                GemType.Red -> "🔴"
                GemType.Blue -> "🔵"
                GemType.Green -> "🟢"
                GemType.Yellow -> "🟡"
                GemType.Orange -> "🟠"
                GemType.Purple -> "🟣"
            }
            "$emoji Clear ${objective.targetCount} $colorName gems!"
        }
    }
}
