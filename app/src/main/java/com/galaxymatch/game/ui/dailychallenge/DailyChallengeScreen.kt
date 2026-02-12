package com.galaxymatch.game.ui.dailychallenge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galaxymatch.game.model.ObjectiveType
import com.galaxymatch.game.ui.components.GalaxyBackground
import com.galaxymatch.game.ui.components.bounceClick
import com.galaxymatch.game.ui.theme.GameBackground
import com.galaxymatch.game.ui.theme.StarGold
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Screen for the daily challenge feature.
 *
 * Shows today's date, current streak, best daily score, and a preview
 * of today's challenge (objective, board size, difficulty). The player
 * can tap "Play" to start the challenge (disabled if already completed).
 *
 * @param onPlayChallenge Called when the player taps "Play" to start today's challenge
 * @param onBack Called when the player taps back to return to the level map
 */
@Composable
fun DailyChallengeScreen(
    onPlayChallenge: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel = remember { DailyChallengeViewModel() }
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
    ) {
        GalaxyBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // === Back button ===
            Box(
                modifier = Modifier
                    .align(Alignment.Start)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .bounceClick(onClick = onBack)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "\u2190 Back",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // === Title ===
            Text(
                text = "\uD83D\uDCC5 Daily Challenge",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // === Today's date ===
            val today = LocalDate.now()
            val dateText = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
            Text(
                text = dateText,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (state.isLoaded) {
                // === Streak and Stats Card ===
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            emoji = "\uD83D\uDD25",
                            label = "Streak",
                            value = "${state.challengeState.currentStreak} days"
                        )
                        StatItem(
                            emoji = "\uD83C\uDFC6",
                            label = "Best Score",
                            value = "${state.challengeState.bestDailyScore}"
                        )
                        StatItem(
                            emoji = "\u2705",
                            label = "Completed",
                            value = "${state.challengeState.totalDailiesCompleted}"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // === Challenge Preview Card ===
                val level = state.todayLevel
                if (level != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(20.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Today's Challenge",
                                color = StarGold,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Objective
                            val objectiveText = when (level.objective) {
                                is ObjectiveType.ReachScore -> "Score ${level.targetScore} points"
                                is ObjectiveType.BreakAllIce -> "Break all the ice"
                                is ObjectiveType.ClearGemType -> "Clear ${level.objective.targetCount} ${level.objective.gemType.name} gems"
                            }
                            InfoRow(label = "Objective", value = objectiveText)
                            InfoRow(label = "Board", value = "${level.rows} × ${level.cols}")
                            InfoRow(label = "Moves", value = "${level.maxMoves}")
                            InfoRow(label = "Colors", value = "${level.availableGemTypes}")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // === Play Button ===
                if (state.challengeState.todayCompleted) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Gray.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "\u2705 Completed Today!",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(StarGold)
                            .bounceClick(onClick = onPlayChallenge),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "\u25B6 Play Challenge",
                            color = Color.Black,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(emoji: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, fontSize = 28.sp)
        Text(
            text = value,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
