package com.candycrush.game.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.candycrush.game.model.LevelConfig
import com.candycrush.game.ui.theme.HudBackground
import com.candycrush.game.ui.theme.StarGold

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
 */
@Composable
fun GameHud(
    score: Int,
    movesRemaining: Int,
    levelConfig: LevelConfig?,
    comboCount: Int,
    levelNumber: Int,
    comboAnimProgress: Float = 0f,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // === Level indicator ===
        HudItem(
            label = "LEVEL",
            value = levelNumber.toString(),
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
                text = score.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = StarGold
            )
            // Show target score
            if (levelConfig != null) {
                Text(
                    text = "Target: ${levelConfig.targetScore}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        // === Moves remaining ===
        HudItem(
            label = "MOVES",
            value = movesRemaining.toString(),
            valueColor = if (movesRemaining <= 5) Color(0xFFFF6666) else Color.White,
            modifier = Modifier.weight(1f)
        )
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

            Text(
                text = "COMBO x${comboCount + 1}!",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                ),
                color = StarGold,
                modifier = Modifier
                    .scale(bounceScale)
                    .clip(RoundedCornerShape(12.dp))
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
