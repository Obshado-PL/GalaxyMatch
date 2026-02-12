package com.galaxymatch.game.ui.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
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
import com.galaxymatch.game.ui.components.bounceClick
import com.galaxymatch.game.ui.theme.GameBackground
import com.galaxymatch.game.ui.theme.StarGold

/**
 * Screen displaying all 30 achievements grouped by category.
 *
 * Unlocked achievements are bright with their emoji and description.
 * Locked achievements are dimmed with a progress indicator.
 *
 * @param onBack Called when the player taps back
 */
@Composable
fun AchievementsScreen(onBack: () -> Unit) {
    val viewModel = remember { AchievementsViewModel() }
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
                .padding(horizontal = 16.dp)
        ) {
            // === Header ===
            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "\uD83C\uDFC6 Achievements",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (state.isLoaded) {
                Text(
                    text = "${state.unlockedCount}/${state.totalCount} Unlocked",
                    color = StarGold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // === Achievement List ===
            if (state.isLoaded) {
                // Group by category
                val grouped = state.items.groupBy { it.definition.category }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    grouped.forEach { (category, items) ->
                        item {
                            Text(
                                text = category.label,
                                color = StarGold,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(items) { item ->
                            AchievementCard(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(item: AchievementDisplayItem) {
    val alpha = if (item.isUnlocked) 1f else 0.5f
    val bgAlpha = if (item.isUnlocked) 0.15f else 0.08f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = bgAlpha))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Emoji
            Text(
                text = if (item.isUnlocked) item.definition.emoji else "\uD83D\uDD12",
                fontSize = 32.sp,
                modifier = Modifier.width(48.dp)
            )

            // Title + Description + Progress
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = item.definition.title,
                    color = Color.White.copy(alpha = alpha),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.definition.description,
                    color = Color.White.copy(alpha = alpha * 0.7f),
                    fontSize = 13.sp
                )
                if (!item.isUnlocked && item.targetProgress > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = {
                                (item.currentProgress.toFloat() / item.targetProgress).coerceIn(0f, 1f)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = StarGold,
                            trackColor = Color.White.copy(alpha = 0.1f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.progressText,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
