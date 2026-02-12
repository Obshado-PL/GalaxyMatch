package com.galaxymatch.game.ui.game

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import com.galaxymatch.game.model.Gem
import com.galaxymatch.game.model.GemType
import com.galaxymatch.game.model.SpecialType
import com.galaxymatch.game.ui.components.toColor
import com.galaxymatch.game.ui.components.toDarkColor

/**
 * Draws individual gem shapes on a Canvas.
 *
 * Each gem is drawn as a colored circle with a gloss highlight.
 * Special gems get additional visual indicators:
 * - Striped (horizontal): Three horizontal lines across the gem
 * - Striped (vertical): Three vertical lines across the gem
 * - Wrapped: A smaller inner circle with a darker shade
 * - Color Bomb: A multicolored starburst pattern
 *
 * These are DrawScope extension functions, so they're called from within
 * a Canvas composable's drawScope like: drawGem(gem, x, y, radius)
 */

/**
 * Draw a single gem at the given center position with the given radius.
 *
 * @param gem The gem to draw
 * @param centerX The x-coordinate of the gem's center
 * @param centerY The y-coordinate of the gem's center
 * @param radius The radius of the gem circle
 * @param alpha Opacity (0f = invisible, 1f = fully visible). Used for animations.
 */
fun DrawScope.drawGem(
    gem: Gem,
    centerX: Float,
    centerY: Float,
    radius: Float,
    alpha: Float = 1f,
    specialAnimProgress: Float = 0f,
    colorblindMode: Boolean = false
) {
    val center = Offset(centerX, centerY)
    val color = gem.type.toColor()

    // === Step 1: Draw the shadow (slightly offset dark circle) ===
    drawCircle(
        color = Color.Black.copy(alpha = 0.3f * alpha),
        radius = radius,
        center = Offset(centerX + radius * 0.05f, centerY + radius * 0.08f)
    )

    // === Step 2: Draw the main gem body ===
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

    // === Step 4: Draw special gem indicators ===
    // specialAnimProgress drives continuous animations: stripe pulsing,
    // wrapped breathing, and color bomb rotation. Cycles 0→1 over 2 seconds.
    when (gem.special) {
        SpecialType.StripedHorizontal -> {
            drawStripedHorizontal(center, radius, alpha, specialAnimProgress)
        }
        SpecialType.StripedVertical -> {
            drawStripedVertical(center, radius, alpha, specialAnimProgress)
        }
        SpecialType.Wrapped -> {
            drawWrappedIndicator(center, radius, gem.type.toDarkColor(), alpha, specialAnimProgress)
        }
        SpecialType.ColorBomb -> {
            drawColorBombPattern(center, radius, alpha, specialAnimProgress)
        }
        SpecialType.None -> { /* Regular gem, no extra decoration */ }
    }

    // === Step 5: Draw colorblind shape overlay ===
    if (colorblindMode) {
        drawColorblindShape(gem.type, center, radius, alpha)
    }
}

/**
 * Draw horizontal stripe lines across the gem (for StripedHorizontal type).
 * The stripe alpha pulses between 0.5 and 0.9 using the animation progress,
 * creating a subtle "breathing" glow effect.
 */
private fun DrawScope.drawStripedHorizontal(
    center: Offset, radius: Float, alpha: Float, animProgress: Float
) {
    // Pulse alpha using a sine wave: 0.5 (dim) → 0.9 (bright) → 0.5 → ...
    val pulseAlpha = 0.5f + 0.4f * kotlin.math.sin(animProgress * 2f * Math.PI.toFloat())
    val stripeColor = Color.White.copy(alpha = pulseAlpha * alpha)
    val strokeWidth = radius * 0.12f
    val offsets = listOf(-0.3f, 0f, 0.3f)

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
 * Draw vertical stripe lines across the gem (for StripedVertical type).
 * Same pulsing alpha effect as horizontal stripes.
 */
private fun DrawScope.drawStripedVertical(
    center: Offset, radius: Float, alpha: Float, animProgress: Float
) {
    val pulseAlpha = 0.5f + 0.4f * kotlin.math.sin(animProgress * 2f * Math.PI.toFloat())
    val stripeColor = Color.White.copy(alpha = pulseAlpha * alpha)
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
 * Draw the wrapped gem indicator (inner circle with darker shade).
 * The outer glow ring "breathes" — its radius and alpha pulse gently
 * to make wrapped gems look like they're charged with energy.
 */
private fun DrawScope.drawWrappedIndicator(
    center: Offset,
    radius: Float,
    darkColor: Color,
    alpha: Float,
    animProgress: Float
) {
    // Breathing sine wave for the glow ring
    val breathe = kotlin.math.sin(animProgress * 2f * Math.PI.toFloat())
    val glowRadius = radius * (0.75f + 0.07f * breathe)  // Pulses between 0.68 and 0.82
    val glowAlpha = 0.4f + 0.2f * breathe                // Pulses between 0.2 and 0.6

    // Outer pulsing glow ring
    drawCircle(
        color = Color.White.copy(alpha = glowAlpha * alpha),
        radius = glowRadius,
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
 * colored dots around it representing all gem colors.
 *
 * The dots slowly rotate around the center using animProgress,
 * and the center sparkle pulses in size for a "charged" look.
 */
private fun DrawScope.drawColorBombPattern(
    center: Offset, radius: Float, alpha: Float, animProgress: Float
) {
    // Dark base
    drawCircle(
        color = Color(0xFF222222).copy(alpha = alpha),
        radius = radius,
        center = center
    )

    // Colored dots arranged in a circle — they rotate continuously
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

    // Rotation: progress 0→1 maps to 0→360 degrees
    val rotationDegrees = animProgress * 360.0

    for (i in dotColors.indices) {
        // Add the rotation offset to each dot's base angle
        val angle = (i * 60.0 - 90.0 + rotationDegrees) * (Math.PI / 180.0)
        val dotX = center.x + (dotDistance * kotlin.math.cos(angle)).toFloat()
        val dotY = center.y + (dotDistance * kotlin.math.sin(angle)).toFloat()

        drawCircle(
            color = dotColors[i].copy(alpha = alpha),
            radius = dotRadius,
            center = Offset(dotX, dotY)
        )
    }

    // Center sparkle: pulses at 2x frequency for a lively twinkling effect
    val sparklePulse = kotlin.math.sin(animProgress * 4f * Math.PI.toFloat())
    val sparkleRadius = radius * (0.12f + 0.06f * sparklePulse)
    val sparkleAlpha = 0.6f + 0.4f * sparklePulse

    drawCircle(
        color = Color.White.copy(alpha = sparkleAlpha * alpha),
        radius = sparkleRadius,
        center = center
    )
}

// ===========================================================================
// Colorblind Shape Overlays
// ===========================================================================

/**
 * Draw a distinctive geometric shape inside the gem for colorblind accessibility.
 * Each GemType gets a unique shape so players can distinguish gems by shape alone.
 *
 * Shapes are drawn as white semi-transparent stroked outlines at ~50% of gem radius.
 */
private fun DrawScope.drawColorblindShape(
    gemType: GemType,
    center: Offset,
    radius: Float,
    alpha: Float
) {
    val shapeColor = Color.White.copy(alpha = 0.85f * alpha)
    val strokeWidth = radius * 0.1f
    val size = radius * 0.45f

    when (gemType) {
        GemType.Red -> drawCrossShape(center, size, shapeColor, strokeWidth)
        GemType.Blue -> drawDiamondShape(center, size, shapeColor, strokeWidth)
        GemType.Green -> drawTriangleShape(center, size, shapeColor, strokeWidth)
        GemType.Yellow -> drawStarShape(center, size, shapeColor, strokeWidth)
        GemType.Orange -> drawSquareShape(center, size, shapeColor, strokeWidth)
        GemType.Purple -> drawHexagonShape(center, size, shapeColor, strokeWidth)
    }
}

/** Red: Cross / Plus (+) */
private fun DrawScope.drawCrossShape(
    center: Offset, size: Float, color: Color, strokeWidth: Float
) {
    drawLine(
        color = color,
        start = Offset(center.x - size, center.y),
        end = Offset(center.x + size, center.y),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(center.x, center.y - size),
        end = Offset(center.x, center.y + size),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}

/** Blue: Diamond shape */
private fun DrawScope.drawDiamondShape(
    center: Offset, size: Float, color: Color, strokeWidth: Float
) {
    val path = Path().apply {
        moveTo(center.x, center.y - size)
        lineTo(center.x + size, center.y)
        lineTo(center.x, center.y + size)
        lineTo(center.x - size, center.y)
        close()
    }
    drawPath(
        path = path,
        color = color,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
    )
}

/** Green: Triangle */
private fun DrawScope.drawTriangleShape(
    center: Offset, size: Float, color: Color, strokeWidth: Float
) {
    val path = Path().apply {
        moveTo(center.x, center.y - size)
        lineTo(center.x + size * 0.866f, center.y + size * 0.5f)
        lineTo(center.x - size * 0.866f, center.y + size * 0.5f)
        close()
    }
    drawPath(
        path = path,
        color = color,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
    )
}

/** Yellow: 5-pointed star */
private fun DrawScope.drawStarShape(
    center: Offset, size: Float, color: Color, strokeWidth: Float
) {
    val outerRadius = size
    val innerRadius = size * 0.4f
    val points = 5

    val path = Path().apply {
        for (i in 0 until points * 2) {
            val angle = (i * Math.PI / points - Math.PI / 2).toFloat()
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val x = center.x + r * kotlin.math.cos(angle)
            val y = center.y + r * kotlin.math.sin(angle)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(
        path = path,
        color = color,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
    )
}

/** Orange: Square */
private fun DrawScope.drawSquareShape(
    center: Offset, size: Float, color: Color, strokeWidth: Float
) {
    drawRect(
        color = color,
        topLeft = Offset(center.x - size, center.y - size),
        size = Size(size * 2, size * 2),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
    )
}

/** Purple: Hexagon */
private fun DrawScope.drawHexagonShape(
    center: Offset, size: Float, color: Color, strokeWidth: Float
) {
    val path = Path().apply {
        for (i in 0 until 6) {
            val angle = (i * Math.PI / 3 - Math.PI / 2).toFloat()
            val x = center.x + size * kotlin.math.cos(angle)
            val y = center.y + size * kotlin.math.sin(angle)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(
        path = path,
        color = color,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
    )
}

// ===========================================================================
// Obstacle Drawing Functions
// ===========================================================================

/**
 * Draw an ice overlay on top of a gem.
 *
 * Ice appears as a translucent blue-white layer with frost sparkles
 * and a cyan border ring. The gem underneath is still fully visible —
 * the ice just sits on top like a frozen shell.
 *
 * @param center The center point of the cell (same as the gem center)
 * @param radius The radius of the gem circle (ice matches gem size)
 * @param alpha Opacity for animations (e.g., fade out when ice breaks)
 */
fun DrawScope.drawIce(
    center: Offset,
    radius: Float,
    alpha: Float = 1f
) {
    // === Outer cyan border ring — gives the ice a crisp frozen edge ===
    drawCircle(
        color = Color(0xFF00CCEE).copy(alpha = 0.6f * alpha),
        radius = radius * 1.05f,
        center = center,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.1f)
    )

    // === Translucent blue-white fill — the "frozen" look ===
    drawCircle(
        color = Color(0xFFCCEEFF).copy(alpha = 0.3f * alpha),
        radius = radius,
        center = center
    )

    // === Frost sparkles — 3 small white dots at different positions ===
    // These give the ice a crystalline, sparkling appearance
    val sparklePositions = listOf(
        Offset(center.x - radius * 0.35f, center.y - radius * 0.3f),  // Top-left
        Offset(center.x + radius * 0.25f, center.y - radius * 0.15f), // Top-right
        Offset(center.x - radius * 0.1f, center.y + radius * 0.35f)   // Bottom-center
    )
    val sparkleRadii = listOf(radius * 0.08f, radius * 0.06f, radius * 0.07f)

    for (i in sparklePositions.indices) {
        drawCircle(
            color = Color.White.copy(alpha = 0.85f * alpha),
            radius = sparkleRadii[i],
            center = sparklePositions[i]
        )
    }

    // === Inner highlight arc — a curved gloss on top-left for that "glass" feel ===
    drawCircle(
        color = Color.White.copy(alpha = 0.15f * alpha),
        radius = radius * 0.5f,
        center = Offset(center.x - radius * 0.2f, center.y - radius * 0.25f)
    )
}

/**
 * Draw a stone wall obstacle.
 *
 * Stone fills the entire cell with a dark rocky texture. No gem exists
 * beneath it — it's a solid, immovable wall. Gravity flows around stones,
 * and players cannot interact with them.
 *
 * The stone has a layered look: dark base → lighter texture patches → crack lines,
 * all clipped to a rounded rectangle matching the board cell shape.
 *
 * @param topLeft The top-left corner of the cell
 * @param cellSize The width/height of the cell (cells are square)
 * @param cornerRadius The corner radius for the cell's rounded rect
 * @param alpha Opacity (stones normally stay at 1.0 since they're permanent)
 */
fun DrawScope.drawStone(
    topLeft: Offset,
    cellSize: Float,
    cornerRadius: Float,
    alpha: Float = 1f
) {
    // Create a rounded rect path to clip all drawing inside the cell shape
    val clipPath = Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(topLeft.x, topLeft.y, topLeft.x + cellSize, topLeft.y + cellSize),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
        )
    }

    clipPath(clipPath) {
        // === Base: dark gray fill ===
        drawRect(
            color = Color(0xFF5A5A5A).copy(alpha = alpha),
            topLeft = topLeft,
            size = Size(cellSize, cellSize)
        )

        // === Lighter texture patches — irregular "rock grain" spots ===
        // These break up the flat gray and make it look like natural stone
        val patches = listOf(
            Triple(Offset(topLeft.x + cellSize * 0.15f, topLeft.y + cellSize * 0.2f), cellSize * 0.22f, Color(0xFF6E6E6E)),
            Triple(Offset(topLeft.x + cellSize * 0.6f, topLeft.y + cellSize * 0.15f), cellSize * 0.18f, Color(0xFF686868)),
            Triple(Offset(topLeft.x + cellSize * 0.35f, topLeft.y + cellSize * 0.6f), cellSize * 0.2f, Color(0xFF727272)),
            Triple(Offset(topLeft.x + cellSize * 0.75f, topLeft.y + cellSize * 0.7f), cellSize * 0.15f, Color(0xFF656565))
        )

        for ((patchCenter, patchRadius, patchColor) in patches) {
            drawCircle(
                color = patchColor.copy(alpha = 0.7f * alpha),
                radius = patchRadius,
                center = patchCenter
            )
        }

        // === Crack lines — thin dark lines that make it look weathered ===
        val crackColor = Color(0xFF404040).copy(alpha = 0.6f * alpha)
        val crackWidth = cellSize * 0.025f

        // Crack 1: diagonal from top-left area to center
        drawLine(
            color = crackColor,
            start = Offset(topLeft.x + cellSize * 0.2f, topLeft.y + cellSize * 0.15f),
            end = Offset(topLeft.x + cellSize * 0.45f, topLeft.y + cellSize * 0.5f),
            strokeWidth = crackWidth,
            cap = StrokeCap.Round
        )
        // Crack 1 branch
        drawLine(
            color = crackColor,
            start = Offset(topLeft.x + cellSize * 0.35f, topLeft.y + cellSize * 0.35f),
            end = Offset(topLeft.x + cellSize * 0.55f, topLeft.y + cellSize * 0.3f),
            strokeWidth = crackWidth * 0.8f,
            cap = StrokeCap.Round
        )

        // Crack 2: from right side going down
        drawLine(
            color = crackColor,
            start = Offset(topLeft.x + cellSize * 0.7f, topLeft.y + cellSize * 0.55f),
            end = Offset(topLeft.x + cellSize * 0.55f, topLeft.y + cellSize * 0.8f),
            strokeWidth = crackWidth,
            cap = StrokeCap.Round
        )

        // === Highlight edge — subtle light edge on top for 3D depth ===
        drawLine(
            color = Color.White.copy(alpha = 0.15f * alpha),
            start = Offset(topLeft.x + cornerRadius, topLeft.y + crackWidth),
            end = Offset(topLeft.x + cellSize - cornerRadius, topLeft.y + crackWidth),
            strokeWidth = crackWidth * 1.5f,
            cap = StrokeCap.Round
        )

        // === Shadow edge — dark edge at bottom for 3D depth ===
        drawLine(
            color = Color.Black.copy(alpha = 0.2f * alpha),
            start = Offset(topLeft.x + cornerRadius, topLeft.y + cellSize - crackWidth),
            end = Offset(topLeft.x + cellSize - cornerRadius, topLeft.y + cellSize - crackWidth),
            strokeWidth = crackWidth * 1.5f,
            cap = StrokeCap.Round
        )
    }
}
