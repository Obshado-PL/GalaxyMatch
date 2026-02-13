package com.galaxymatch.game.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * A space-themed confirmation dialog with animated entrance/exit.
 *
 * Features:
 * - Entry: scale from 0.8 → 1.0 (spring bounce) + fade in
 * - Exit: scale 1.0 → 0.9 + fade out, THEN invoke callback
 * - Visual: Gradient background, large emoji, gold title, styled buttons
 * - Scrim: Dark overlay behind the dialog, tappable to dismiss
 *
 * @param emoji Large emoji shown at the top of the dialog (e.g., "🔄" or "🚪")
 * @param title The dialog title (e.g., "Restart Level?")
 * @param message The body text explaining the action
 * @param confirmText Text for the confirm button (e.g., "Restart", "Quit")
 * @param dismissText Text for the dismiss/cancel button
 * @param confirmColor Background color for the confirm button (default: red)
 * @param onConfirm Called AFTER the exit animation completes when confirm is tapped
 * @param onDismiss Called AFTER the exit animation completes when dismiss/scrim is tapped
 */
@Composable
fun SpaceDialog(
    emoji: String,
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    confirmColor: Color = Color(0xFFCC3333),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // === Entry/exit animation state ===
    // Scale bounces in from 0.8; alpha fades in from 0.
    val dialogScale = remember { Animatable(0.8f) }
    val dialogAlpha = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Entry animation: spring bounce-in + fade
    LaunchedEffect(Unit) {
        launch {
            dialogScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        launch {
            dialogAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 200)
            )
        }
    }

    /**
     * Plays the exit animation (shrink + fade), then invokes the given callback.
     * This ensures the dialog visually closes before any side effects run.
     */
    fun animateOutThen(callback: () -> Unit) {
        scope.launch {
            // Animate out: scale down slightly and fade to transparent
            launch {
                dialogScale.animateTo(
                    targetValue = 0.9f,
                    animationSpec = tween(durationMillis = 150)
                )
            }
            launch {
                dialogAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 150)
                )
            }
            // Wait for animation to finish, then invoke callback
            kotlinx.coroutines.delay(160)
            callback()
        }
    }

    // === Full-screen scrim ===
    // Tapping outside the dialog dismisses it (same as AlertDialog onDismissRequest).
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f * dialogAlpha.value))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                animateOutThen(onDismiss)
            },
        contentAlignment = Alignment.Center
    ) {
        // === Dialog card ===
        // Gradient background from dark purple to deep space navy.
        // Clickable with no-op to prevent scrim tap-through.
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .scale(dialogScale.value)
                .alpha(dialogAlpha.value)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2D2B55),  // Dark purple (top)
                            Color(0xFF1A1845)   // Deep space navy (bottom)
                        )
                    )
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* Block scrim tap-through */ }
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // === Emoji icon ===
            Text(
                text = emoji,
                fontSize = 40.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // === Title in gold ===
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFFFFD700),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // === Message text ===
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // === Confirm button (red by default) ===
            GameButton(
                text = confirmText,
                onClick = { animateOutThen(onConfirm) },
                color = confirmColor
            )

            Spacer(modifier = Modifier.height(10.dp))

            // === Dismiss button (transparent) ===
            GameButton(
                text = dismissText,
                onClick = { animateOutThen(onDismiss) },
                color = Color.White.copy(alpha = 0.15f)
            )
        }
    }
}
