package com.candycrush.game.ui.levelmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.candycrush.game.ui.theme.GameBackground

/**
 * The level map screen where the player selects which level to play.
 *
 * Shows all levels in a scrollable grid, with star ratings for completed
 * levels and locked indicators for levels that haven't been unlocked yet.
 *
 * @param onLevelSelected Called when the player taps a level to play it
 */
@Composable
fun LevelMapScreen(onLevelSelected: (Int) -> Unit) {
    val viewModel = remember { LevelMapViewModel() }
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
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
                .padding(top = 24.dp, bottom = 16.dp)
        )

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
