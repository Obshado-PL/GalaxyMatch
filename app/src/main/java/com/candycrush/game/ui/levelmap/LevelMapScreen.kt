package com.candycrush.game.ui.levelmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import com.candycrush.game.ui.components.bounceClick
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
import com.candycrush.game.ui.theme.GameBackground

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
fun LevelMapScreen(onLevelSelected: (Int) -> Unit, onSettingsClicked: () -> Unit) {
    val viewModel = remember { LevelMapViewModel() }
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
            .statusBarsPadding()
    ) {
        // === Title Row with Settings Button ===
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            // Title centered in the row
            Text(
                text = "Select Level",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            // Gear/settings button on the right side
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .bounceClick(onClick = onSettingsClicked),
                contentAlignment = Alignment.Center
            ) {
                // Unicode gear icon — lightweight alternative to adding
                // Material Icons dependency just for one icon
                Text(
                    text = "\u2699",
                    fontSize = 22.sp,
                    color = Color.White
                )
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
    }
}
