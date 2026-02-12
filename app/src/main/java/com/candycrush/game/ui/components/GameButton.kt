package com.candycrush.game.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Styled button used throughout the game UI.
 *
 * Provides a consistent look for all buttons (play, retry, next level, etc.)
 * Includes a scale-bounce press effect: shrinks to 92% on press, springs
 * back to 100% on release for satisfying tactile feedback.
 *
 * @param text The button text
 * @param onClick Called when the button is clicked
 * @param color The button background color
 * @param modifier Optional modifier
 */
@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit,
    color: Color = Color(0xFF6650a4),
    modifier: Modifier = Modifier
) {
    // Track press state for bounce animation
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Scale down to 0.92 when pressed, spring back to 1.0 on release
    val bounceScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "buttonBounce"
    )

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(16.dp),
        interactionSource = interactionSource, // Let us observe press events
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .scale(bounceScale) // Bounce effect on press
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier.padding(vertical = 6.dp)
        )
    }
}
