package com.candycrush.game.ui.results

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.candycrush.game.ui.components.GameButton
import com.candycrush.game.ui.components.StarRating
import com.candycrush.game.ui.theme.GameBackground

/**
 * Results screen shown after completing or failing a level.
 *
 * Displays:
 * - Win/lose message
 * - Final score
 * - Star rating (for wins)
 * - Buttons to play again, go to next level, or return to map
 *
 * @param levelNumber The level that was just played
 * @param score The final score
 * @param stars Stars earned (0-3)
 * @param won True if the player passed the level
 * @param onPlayAgain Called to replay the same level
 * @param onNextLevel Called to play the next level
 * @param onBackToMap Called to return to the level map
 */
@Composable
fun ResultsScreen(
    levelNumber: Int,
    score: Int,
    stars: Int,
    won: Boolean,
    onPlayAgain: () -> Unit,
    onNextLevel: () -> Unit,
    onBackToMap: () -> Unit
) {
    val hasNext = ResultsHelper.hasNextLevel(levelNumber)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
            .statusBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // === Win/Lose Title ===
        Text(
            text = if (won) "Level Complete!" else "Try Again!",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.ExtraBold
            ),
            color = if (won) Color(0xFFFFD700) else Color(0xFFFF6666),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Level $levelNumber",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // === Star Rating ===
        if (won && stars > 0) {
            StarRating(stars = stars, starSize = 48.dp)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // === Score ===
        Text(
            text = "Score",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color.White
        )

        Spacer(modifier = Modifier.height(40.dp))

        // === Action Buttons ===
        if (won && hasNext) {
            GameButton(
                text = "Next Level",
                onClick = onNextLevel,
                color = Color(0xFF44BB44) // Green
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        GameButton(
            text = if (won) "Play Again" else "Retry",
            onClick = onPlayAgain,
            color = Color(0xFF6650a4) // Purple
        )

        Spacer(modifier = Modifier.height(12.dp))

        GameButton(
            text = "Back to Map",
            onClick = onBackToMap,
            color = Color.White.copy(alpha = 0.15f)
        )
    }
}
