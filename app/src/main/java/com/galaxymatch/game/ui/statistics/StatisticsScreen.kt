package com.galaxymatch.game.ui.statistics

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galaxymatch.game.model.GemType
import com.galaxymatch.game.ui.components.GalaxyBackground
import com.galaxymatch.game.ui.components.toColor
import com.galaxymatch.game.ui.theme.GameBackground
import com.galaxymatch.game.ui.theme.StarGold

/**
 * Statistics screen displaying aggregate gameplay stats.
 *
 * Shows a scrollable list of stat cards covering gameplay metrics,
 * progression data, and a "favorite gem color" highlight.
 */
@Composable
fun StatisticsScreen(onBackToMap: () -> Unit) {
    val viewModel = remember { StatisticsViewModel() }
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // === Title ===
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 24.dp)
            )

            // === Stats List ===
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Gameplay stats
                item { StatCard(label = "Games Played", value = "${state.totalGamesPlayed}") }
                item { StatCard(label = "Total Gems Matched", value = formatNumber(state.totalGemsMatched)) }
                item { StatCard(label = "Best Combo", value = "${state.bestCombo}x") }
                item { StatCard(label = "Total Score", value = formatNumber(state.totalScore)) }
                item { StatCard(label = "Special Gems Created", value = formatNumber(state.specialGemsCreated)) }
                item { StatCard(label = "Power-Ups Used", value = "${state.powerUpsUsed}") }

                // Favorite gem color
                item { FavoriteGemCard(gemType = state.favoriteGemColor) }

                // Progress stats (derived from PlayerProgress)
                item { StatCard(label = "Levels Completed", value = "${state.levelsCompleted}") }
                item {
                    StatCard(
                        label = "Total Stars",
                        value = "${state.totalStars}",
                        valueColor = StarGold
                    )
                }
                item { StatCard(label = "Best Level Score", value = formatNumber(state.bestLevelScore)) }
            }

            // === Back Button ===
            val backInteraction = remember { MutableInteractionSource() }
            val backPressed by backInteraction.collectIsPressedAsState()
            val backScale by animateFloatAsState(
                targetValue = if (backPressed) 0.92f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "backBounce"
            )

            Button(
                onClick = onBackToMap,
                interactionSource = backInteraction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .scale(backScale)
            ) {
                Text(
                    text = "Back to Map",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

/** A single stat display row with label and value. */
@Composable
private fun StatCard(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = valueColor
        )
    }
}

/** Special card showing the player's most-matched gem color with a color swatch. */
@Composable
private fun FavoriteGemCard(gemType: GemType?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Favorite Color",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color.White.copy(alpha = 0.8f)
        )
        if (gemType != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = gemType.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = gemType.toColor()
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(gemType.toColor())
                )
            }
        } else {
            Text(
                text = "---",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

/** Format large numbers with comma separators for readability. */
private fun formatNumber(number: Int): String = String.format("%,d", number)
private fun formatNumber(number: Long): String = String.format("%,d", number)
