package com.galaxymatch.game.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale

/**
 * A reusable modifier that adds a scale-bounce effect when pressed.
 *
 * When the user presses down, the element shrinks to 92%.
 * When released, it springs back to 100% with a slight bounce.
 * This gives buttons and clickable items a satisfying "squish" feel.
 *
 * HOW IT WORKS:
 * 1. MutableInteractionSource is passed to clickable() to observe press events
 * 2. collectIsPressedAsState() tells us when the finger is down
 * 3. animateFloatAsState() smoothly transitions the scale (0.92 ↔ 1.0)
 * 4. The spring spec gives a natural bounce-back on release
 *
 * Usage:
 *   Modifier.bounceClick(onClick = { doSomething() })
 *
 * NOTE: This removes the default Material ripple effect — the scale animation
 * IS the visual feedback. If you want both, replace `indication = null`
 * with a ripple indication.
 *
 * @param enabled Whether the element is clickable (default true)
 * @param onClick The action to perform when clicked
 */
fun Modifier.bounceClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    // InteractionSource lets us observe press/release events
    val interactionSource = remember { MutableInteractionSource() }

    // True while the user's finger is pressing down
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smoothly animate between pressed scale (0.92) and normal scale (1.0)
    // MediumBouncy damping gives a nice little overshoot on release
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounceScale"
    )

    this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null, // No ripple — the scale IS the feedback
            enabled = enabled,
            onClick = onClick
        )
}
