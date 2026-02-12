package com.galaxymatch.game.ui.timedchallenge

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
import com.galaxymatch.game.model.TimedDifficulty
import com.galaxymatch.game.ui.components.GalaxyBackground
import com.galaxymatch.game.ui.components.bounceClick
import com.galaxymatch.game.ui.theme.GameBackground
import com.galaxymatch.game.ui.theme.StarGold

/**
 * Screen for selecting a timed challenge difficulty.
 *
 * Shows 3 difficulty cards (Easy, Medium, Hard) with timer info
 * and best scores. Tapping a card starts the timed game.
 *
 * @param onStartTimed Called with the difficulty ordinal when the player picks one
 * @param onBack Called when the player taps back
 */
@Composable
fun TimedChallengeScreen(
    onStartTimed: (Int) -> Unit,
    onBack: () -> Unit
) {
    val viewModel = remember { TimedChallengeViewModel() }
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
                Text(text = "\u2190 Back", color = Color.White, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "\u23F1 Timed Challenge",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Score as high as you can before time runs out!\nCombos add bonus seconds.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (state.isLoaded) {
                val cs = state.challengeState

                // Difficulty cards
                DifficultyCard(
                    difficulty = TimedDifficulty.Easy,
                    bestScore = cs.bestScoreEasy,
                    color = Color(0xFF4CAF50),
                    onClick = { onStartTimed(TimedDifficulty.Easy.ordinal) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                DifficultyCard(
                    difficulty = TimedDifficulty.Medium,
                    bestScore = cs.bestScoreMedium,
                    color = Color(0xFFFF9800),
                    onClick = { onStartTimed(TimedDifficulty.Medium.ordinal) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                DifficultyCard(
                    difficulty = TimedDifficulty.Hard,
                    bestScore = cs.bestScoreHard,
                    color = Color(0xFFF44336),
                    onClick = { onStartTimed(TimedDifficulty.Hard.ordinal) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Total timed games: ${cs.totalTimedGames}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun DifficultyCard(
    difficulty: TimedDifficulty,
    bestScore: Int,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.2f))
            .bounceClick(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = difficulty.label,
                    color = color,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                val minutes = difficulty.seconds / 60
                val secs = difficulty.seconds % 60
                Text(
                    text = "${minutes}:${secs.toString().padStart(2, '0')}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Best",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
                Text(
                    text = if (bestScore > 0) "$bestScore" else "—",
                    color = StarGold,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
