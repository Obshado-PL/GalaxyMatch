package com.galaxymatch.game.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Full-screen tutorial overlay shown when the player starts level 1
 * for the first time.
 *
 * Displays 3 simple instruction cards explaining the core mechanics:
 * 1. Swipe to swap adjacent gems
 * 2. Match 3 or more to score points
 * 3. Match 4+ to create special gems
 *
 * Once dismissed, it saves a flag so it won't show again.
 *
 * @param onDismiss Called when the player taps "Got it!"
 */
@Composable
fun TutorialOverlay(onDismiss: () -> Unit) {
    // Semi-transparent black scrim covering the entire screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        // Dark purple card matching the game overlay style
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(32.dp)
                .background(
                    color = Color(0xFF2D2B55),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(28.dp)
        ) {
            // Title
            Text(
                text = "How to Play",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFFFFD700), // Gold
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // === Instruction 1: Swipe to swap ===
            TutorialCard(
                emoji = "\uD83D\uDC46", // pointing up emoji
                title = "Swipe to Swap",
                description = "Swipe between two adjacent gems to swap them."
            )

            // === Instruction 2: Match 3+ ===
            TutorialCard(
                emoji = "\u2728", // sparkles emoji
                title = "Match 3+",
                description = "Line up 3 or more gems of the same color to clear them and score points."
            )

            // === Instruction 3: Special gems ===
            TutorialCard(
                emoji = "\uD83D\uDCA5", // collision emoji
                title = "Create Specials",
                description = "Match 4 or more to create powerful special gems with explosive effects!"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Dismiss button
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF44BB44)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Got it!",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * A single instruction card in the tutorial.
 *
 * @param emoji An emoji shown as a visual icon
 * @param title Short title for the instruction
 * @param description Detailed explanation
 */
@Composable
private fun TutorialCard(
    emoji: String,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        // Emoji icon
        Text(
            text = emoji,
            fontSize = 28.sp,
            modifier = Modifier.padding(end = 12.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
