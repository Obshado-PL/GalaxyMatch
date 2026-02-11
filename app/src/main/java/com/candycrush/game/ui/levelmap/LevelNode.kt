package com.candycrush.game.ui.levelmap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.candycrush.game.ui.components.StarRating

/**
 * A single level node on the level map.
 *
 * Shows a circle with the level number, star rating, and locked/unlocked state.
 *
 * @param levelNumber The level number to display
 * @param stars How many stars the player earned (0 if not completed)
 * @param isUnlocked Whether the player can play this level
 * @param onClick Called when the player taps this level
 */
@Composable
fun LevelNode(
    levelNumber: Int,
    stars: Int,
    isUnlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(vertical = 8.dp)
    ) {
        // Level number circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    if (isUnlocked) {
                        if (stars > 0) Color(0xFF44BB44) // Green for completed
                        else Color(0xFF6650a4) // Purple for unlocked but not completed
                    } else {
                        Color(0xFF444444) // Gray for locked
                    }
                )
                .then(
                    if (isUnlocked) Modifier.clickable(onClick = onClick)
                    else Modifier
                )
        ) {
            Text(
                text = levelNumber.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (isUnlocked) Color.White else Color.White.copy(alpha = 0.4f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Star rating (only shown for completed levels)
        if (stars > 0) {
            StarRating(stars = stars, starSize = 16.dp)
        } else if (!isUnlocked) {
            Text(
                text = "Locked",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Normal
                ),
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}
