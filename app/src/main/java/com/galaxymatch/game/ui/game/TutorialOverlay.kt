package com.galaxymatch.game.ui.game

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galaxymatch.game.ui.components.GameButton

/**
 * Interactive step-by-step tutorial overlay for first-time players.
 *
 * The tutorial has 3 steps:
 * 1. **Swap Step**: Highlights two swappable gems with a pulsing glow and arrow.
 *    The board remains fully interactive so the player can perform the swap.
 *    Only a bottom instruction card is shown — NO full-screen blocking scrim.
 * 2. **Match Result**: Shows "Nice! You made a match!" after the swap.
 *    Player taps to continue. Full scrim blocks the board.
 * 3. **Specials Info**: Shows "Match 4+ for specials!" info.
 *    Player taps to dismiss and start playing. Full scrim blocks the board.
 *
 * A "Skip Tutorial" button is always visible.
 *
 * IMPORTANT: Step 1 must NOT block touch events from reaching the board canvas!
 * The instruction card is positioned at the bottom using fillMaxSize() +
 * Alignment.BottomCenter, but with NO clickable modifier on the outer Box,
 * so swipe gestures pass through to the BoardCanvas underneath.
 *
 * @param tutorialStep Current step (1, 2, or 3)
 * @param hasHighlightedGems Whether there are gems highlighted for step 1
 * @param onContinue Called when the player taps "Continue" or "Got it!" (steps 2-3)
 * @param onSkip Called when the player taps "Skip Tutorial"
 */
@Composable
fun TutorialOverlay(
    tutorialStep: Int = 1,
    hasHighlightedGems: Boolean = false,
    onContinue: () -> Unit = {},
    onSkip: () -> Unit = {}
) {
    // Pulsing animation for attention-grabbing elements
    val pulseTransition = rememberInfiniteTransition(label = "tutorialPulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    when (tutorialStep) {
        1 -> {
            // === Step 1: Swap Step ===
            // CRITICAL: No blocking scrim! The board must remain swipeable.
            // We only show a floating instruction card at the bottom.
            // The card itself has a clickable modifier to prevent taps on it
            // from passing through, but the rest of the screen is untouched.
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                TutorialStep1(
                    hasHighlightedGems = hasHighlightedGems,
                    pulseAlpha = pulseAlpha,
                    onSkip = onSkip
                )
            }
        }
        2, 3 -> {
            // === Steps 2-3: Info cards with blocking scrim ===
            // These are non-interactive info screens. The full scrim blocks
            // touch events so the player reads the info before continuing.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* Block taps from passing through to the board */ },
                contentAlignment = Alignment.Center
            ) {
                if (tutorialStep == 2) {
                    TutorialStep2(onContinue = onContinue, onSkip = onSkip)
                } else {
                    TutorialStep3(onContinue = onContinue, onSkip = onSkip)
                }
            }
        }
    }
}

/**
 * Step 1: Swap Step — instructs the player to swap the highlighted gems.
 * Shows a bottom card with instructions while the board remains visible AND
 * interactive above. The card itself blocks taps on it (so button clicks work),
 * but does NOT cover the board area.
 */
@Composable
private fun TutorialStep1(
    hasHighlightedGems: Boolean,
    pulseAlpha: Float,
    onSkip: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                color = Color(0xFF2D2B55).copy(alpha = 0.95f),
                shape = RoundedCornerShape(20.dp)
            )
            // The card itself is clickable to block taps from passing through
            // to the board underneath, so button taps work correctly.
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* Block taps on the card itself */ }
            .padding(20.dp)
    ) {
        // Step indicator
        Text(
            text = "Step 1 of 3",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Emoji and title
        Text(
            text = "👆",
            fontSize = 36.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Swipe to Swap!",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color(0xFFFFD700),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Instruction text
        val instructionText = if (hasHighlightedGems) {
            "Swipe between the two glowing gems on the board to swap them and make a match!"
        } else {
            "Swipe between two adjacent gems to swap them. Line up 3 or more of the same color!"
        }

        Text(
            text = instructionText,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        // Pulsing arrow indicator when gems are highlighted
        if (hasHighlightedGems) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "⬆ Look at the board above! ⬆",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFF44DD44).copy(alpha = pulseAlpha),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Skip button
        GameButton(
            text = "Skip Tutorial",
            onClick = onSkip,
            color = Color.White.copy(alpha = 0.15f)
        )
    }
}

/**
 * Step 2: Match Result — congratulates the player on their first match.
 */
@Composable
private fun TutorialStep2(
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    TutorialInfoCard(
        stepLabel = "Step 2 of 3",
        emoji = "✨",
        title = "Nice Match!",
        description = "You matched 3 gems and scored points! Matched gems disappear and new ones fall in from above.",
        continueText = "Continue",
        onContinue = onContinue,
        onSkip = onSkip
    )
}

/**
 * Step 3: Specials Info — teaches about special gem creation.
 */
@Composable
private fun TutorialStep3(
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    TutorialInfoCard(
        stepLabel = "Step 3 of 3",
        emoji = "💥",
        title = "Create Specials!",
        description = "Match 4 gems for a Striped gem (clears a row or column).\n\nMatch 5 in an L/T shape for a Wrapped gem (explodes area).\n\nMatch 5 in a row for a Color Bomb (clears all of one color)!",
        continueText = "Got it! Let's play!",
        onContinue = onContinue,
        onSkip = onSkip
    )
}

/**
 * Reusable info card for tutorial steps 2 and 3.
 * Shows centered card with emoji, title, description, and continue/skip buttons.
 */
@Composable
private fun TutorialInfoCard(
    stepLabel: String,
    emoji: String,
    title: String,
    description: String,
    continueText: String,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(32.dp)
            .background(
                color = Color(0xFF2D2B55),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(24.dp)
    ) {
        // Step indicator
        Text(
            text = stepLabel,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Emoji
        Text(
            text = emoji,
            fontSize = 44.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color(0xFFFFD700),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Description
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Continue button (green, prominent)
        GameButton(
            text = continueText,
            onClick = onContinue,
            color = Color(0xFF44BB44)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Skip button (subtle)
        GameButton(
            text = "Skip Tutorial",
            onClick = onSkip,
            color = Color.White.copy(alpha = 0.15f)
        )
    }
}
