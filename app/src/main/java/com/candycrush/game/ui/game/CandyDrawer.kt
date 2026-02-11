package com.candycrush.game.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.candycrush.game.model.Candy
import com.candycrush.game.model.SpecialType
import com.candycrush.game.ui.components.toColor
import com.candycrush.game.ui.components.toDarkColor

/**
 * Draws individual candy shapes on a Canvas.
 *
 * Each candy is drawn as a colored circle with a gloss highlight.
 * Special candies get additional visual indicators:
 * - Striped (horizontal): Three horizontal lines across the candy
 * - Striped (vertical): Three vertical lines across the candy
 * - Wrapped: A smaller inner circle with a darker shade
 * - Color Bomb: A multicolored starburst pattern
 *
 * These are DrawScope extension functions, so they're called from within
 * a Canvas composable's drawScope like: drawCandy(candy, x, y, radius)
 */

/**
 * Draw a single candy at the given center position with the given radius.
 *
 * @param candy The candy to draw
 * @param centerX The x-coordinate of the candy's center
 * @param centerY The y-coordinate of the candy's center
 * @param radius The radius of the candy circle
 * @param alpha Opacity (0f = invisible, 1f = fully visible). Used for animations.
 */
fun DrawScope.drawCandy(
    candy: Candy,
    centerX: Float,
    centerY: Float,
    radius: Float,
    alpha: Float = 1f
) {
    val center = Offset(centerX, centerY)
    val color = candy.type.toColor()

    // === Step 1: Draw the shadow (slightly offset dark circle) ===
    drawCircle(
        color = Color.Black.copy(alpha = 0.3f * alpha),
        radius = radius,
        center = Offset(centerX + radius * 0.05f, centerY + radius * 0.08f)
    )

    // === Step 2: Draw the main candy body ===
    drawCircle(
        color = color.copy(alpha = alpha),
        radius = radius,
        center = center
    )

    // === Step 3: Draw the gloss highlight (smaller, lighter circle, offset up-left) ===
    drawCircle(
        color = Color.White.copy(alpha = 0.35f * alpha),
        radius = radius * 0.55f,
        center = Offset(centerX - radius * 0.15f, centerY - radius * 0.2f)
    )

    // === Step 4: Draw special candy indicators ===
    when (candy.special) {
        SpecialType.StripedHorizontal -> {
            drawStripedHorizontal(center, radius, alpha)
        }
        SpecialType.StripedVertical -> {
            drawStripedVertical(center, radius, alpha)
        }
        SpecialType.Wrapped -> {
            drawWrappedIndicator(center, radius, candy.type.toDarkColor(), alpha)
        }
        SpecialType.ColorBomb -> {
            drawColorBombPattern(center, radius, alpha)
        }
        SpecialType.None -> { /* Regular candy, no extra decoration */ }
    }
}

/**
 * Draw horizontal stripe lines across the candy (for StripedHorizontal type).
 */
private fun DrawScope.drawStripedHorizontal(center: Offset, radius: Float, alpha: Float) {
    val stripeColor = Color.White.copy(alpha = 0.7f * alpha)
    val strokeWidth = radius * 0.12f
    val offsets = listOf(-0.3f, 0f, 0.3f) // Three lines: above center, at center, below center

    for (offset in offsets) {
        val y = center.y + radius * offset
        drawLine(
            color = stripeColor,
            start = Offset(center.x - radius * 0.65f, y),
            end = Offset(center.x + radius * 0.65f, y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Draw vertical stripe lines across the candy (for StripedVertical type).
 */
private fun DrawScope.drawStripedVertical(center: Offset, radius: Float, alpha: Float) {
    val stripeColor = Color.White.copy(alpha = 0.7f * alpha)
    val strokeWidth = radius * 0.12f
    val offsets = listOf(-0.3f, 0f, 0.3f)

    for (offset in offsets) {
        val x = center.x + radius * offset
        drawLine(
            color = stripeColor,
            start = Offset(x, center.y - radius * 0.65f),
            end = Offset(x, center.y + radius * 0.65f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Draw the wrapped candy indicator (inner circle with darker shade).
 */
private fun DrawScope.drawWrappedIndicator(
    center: Offset,
    radius: Float,
    darkColor: Color,
    alpha: Float
) {
    // Outer ring
    drawCircle(
        color = Color.White.copy(alpha = 0.5f * alpha),
        radius = radius * 0.75f,
        center = center
    )
    // Inner dark circle
    drawCircle(
        color = darkColor.copy(alpha = alpha),
        radius = radius * 0.6f,
        center = center
    )
    // Inner gloss
    drawCircle(
        color = Color.White.copy(alpha = 0.25f * alpha),
        radius = radius * 0.35f,
        center = Offset(center.x - radius * 0.1f, center.y - radius * 0.12f)
    )
}

/**
 * Draw the color bomb pattern (multicolored star/sparkle).
 * Color bombs are special — they're drawn as a dark sphere with
 * colored dots around it representing all candy colors.
 */
private fun DrawScope.drawColorBombPattern(center: Offset, radius: Float, alpha: Float) {
    // Dark base
    drawCircle(
        color = Color(0xFF222222).copy(alpha = alpha),
        radius = radius,
        center = center
    )

    // Colored dots arranged in a circle
    val dotColors = listOf(
        Color(0xFFFF4444), // Red
        Color(0xFF4488FF), // Blue
        Color(0xFF44DD44), // Green
        Color(0xFFFFDD44), // Yellow
        Color(0xFFFF8844), // Orange
        Color(0xFFDD44FF)  // Purple
    )
    val dotRadius = radius * 0.18f
    val dotDistance = radius * 0.55f

    for (i in dotColors.indices) {
        val angle = (i * 60.0 - 90.0) * (Math.PI / 180.0) // Start from top, 60 degrees apart
        val dotX = center.x + (dotDistance * kotlin.math.cos(angle)).toFloat()
        val dotY = center.y + (dotDistance * kotlin.math.sin(angle)).toFloat()

        drawCircle(
            color = dotColors[i].copy(alpha = alpha),
            radius = dotRadius,
            center = Offset(dotX, dotY)
        )
    }

    // Center sparkle
    drawCircle(
        color = Color.White.copy(alpha = 0.8f * alpha),
        radius = radius * 0.15f,
        center = center
    )
}
