package com.galaxymatch.game.ui.levelmap

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import com.galaxymatch.game.ui.components.bounceClick
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.galaxymatch.game.ui.components.StarRating
import kotlin.math.roundToInt
import kotlin.math.sin

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
    // === Idle bobbing animation for unlocked nodes ===
    // Gives the level map a lively, playful feel. Each node bobs at
    // its own phase offset so they don't all move in sync.
    val bobTransition = rememberInfiniteTransition(label = "levelBob")
    val bobProgress by bobTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bobProgress"
    )
    // Phase offset: each level bobs at a different point in the cycle
    val phaseOffset = (levelNumber % 4) * 0.25f
    // 3dp vertical motion using sine wave — only for unlocked nodes
    val bobOffsetY = if (isUnlocked) {
        (sin((bobProgress + phaseOffset) * 2.0 * Math.PI).toFloat() * 3f)
            .roundToInt()
    } else 0

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(vertical = 8.dp)
            .offset { IntOffset(0, bobOffsetY) }
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
                    if (isUnlocked) Modifier.bounceClick(onClick = onClick)
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
