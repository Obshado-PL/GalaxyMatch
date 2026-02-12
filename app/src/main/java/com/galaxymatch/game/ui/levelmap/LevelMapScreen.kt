package com.galaxymatch.game.ui.levelmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import com.galaxymatch.game.ui.components.bounceClick
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import com.galaxymatch.game.ui.components.GalaxyBackground
import com.galaxymatch.game.ui.theme.GameBackground

/**
 * The level map screen where the player selects which level to play.
 *
 * Shows all levels in a scrollable grid, with star ratings for completed
 * levels and locked indicators for levels that haven't been unlocked yet.
 *
 * @param onLevelSelected Called when the player taps a level to play it
 * @param onSettingsClicked Called when the player taps the settings gear icon
 */
@Composable
fun LevelMapScreen(
    onLevelSelected: (Int) -> Unit,
    onSettingsClicked: () -> Unit,
    onStatsClicked: () -> Unit,
    onDailyChallengeClicked: () -> Unit = {},
    onAchievementsClicked: () -> Unit = {},
    onTimedChallengeClicked: () -> Unit = {}
) {
    val viewModel = remember { LevelMapViewModel() }
    val state by viewModel.uiState.collectAsState()

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
        ) {
        // === Title ===
        Text(
            text = "Select Level",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 8.dp)
        )

        // === Icon buttons row (below title) ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Daily Challenge button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .bounceClick(onClick = onDailyChallengeClicked),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\uD83D\uDCC5", // Calendar emoji
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
                // Timed Challenge button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .bounceClick(onClick = onTimedChallengeClicked),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u23F1", // Stopwatch emoji
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
                // Achievements button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .bounceClick(onClick = onAchievementsClicked),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\uD83C\uDFC6", // Trophy emoji
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
                // Stats button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .bounceClick(onClick = onStatsClicked),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\uD83D\uDCCA", // Bar chart emoji
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
                // Gear/settings button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .bounceClick(onClick = onSettingsClicked),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u2699",
                        fontSize = 20.sp,
                        color = Color.White
                    )
                }
            }
        }

        // === Level Grid ===
        if (state.isLoaded) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.levels) { level ->
                    val stars = state.progress.levelStars[level.levelNumber] ?: 0
                    val isUnlocked = level.levelNumber <= state.progress.highestUnlockedLevel

                    LevelNode(
                        levelNumber = level.levelNumber,
                        stars = stars,
                        isUnlocked = isUnlocked,
                        onClick = { onLevelSelected(level.levelNumber) }
                    )
                }
            }
        } else {
            // Loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
        } // Column
    } // Box
}
