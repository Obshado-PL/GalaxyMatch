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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import com.galaxymatch.game.model.Gem
import com.galaxymatch.game.model.GemType
import com.galaxymatch.game.model.SpecialType
import com.galaxymatch.game.ui.components.toColor
import com.galaxymatch.game.ui.components.toDarkColor
import com.galaxymatch.game.ui.components.toHighContrastColor
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws individual gem shapes on a Canvas.
 *
 * Each gem type has a unique space-themed shape:
 * - Red → Crystal: Faceted diamond with crown facet highlight
 * - Blue → Planet: Sphere with atmospheric band + thin ring
 * - Green → Star: 5-pointed star, filled, with center highlight
 * - Yellow → Asteroid: Irregular polygon with crater details
 * - Orange → Nebula: Overlapping translucent circles + bright core
 * - Purple → Moon: Sphere with crater indentations
 *
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
 * Dispatches to the appropriate space-themed shape renderer based on gem type,
 * then overlays any special indicators (stripes, wrapped ring, color bomb dots)
 * and colorblind accessibility shapes on top.
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
    colorblindMode: Boolean = false,
    highContrastMode: Boolean = false
) {
    val center = Offset(centerX, centerY)
    // Use brighter, more saturated colors in high-contrast mode
    val color = if (highContrastMode) gem.type.toHighContrastColor() else gem.type.toColor()
    val darkColor = gem.type.toDarkColor()

    // === Step 1: Draw drop shadow ===
    // Slightly offset dark circle behind the gem for a floating 3D look
    drawCircle(
        color = Color.Black.copy(alpha = 0.3f * alpha),
        radius = radius,
        center = Offset(centerX + radius * 0.05f, centerY + radius * 0.08f)
    )

    // === Step 2: Draw the space-themed gem shape ===
    // Each gem type gets a unique shape instead of a plain circle
    when (gem.type) {
        GemType.Red -> drawCrystalGem(center, radius, color, darkColor, alpha)
        GemType.Blue -> drawPlanetGem(center, radius, color, darkColor, alpha)
        GemType.Green -> drawStarGem(center, radius, color, darkColor, alpha)
        GemType.Yellow -> drawAsteroidGem(center, radius, color, darkColor, alpha)
        GemType.Orange -> drawNebulaGem(center, radius, color, darkColor, alpha)
        GemType.Purple -> drawMoonGem(center, radius, color, darkColor, alpha)
    }

    // === Step 3: Draw special gem indicators ===
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
            drawWrappedIndicator(center, radius, darkColor, alpha, specialAnimProgress)
        }
        SpecialType.ColorBomb -> {
            drawColorBombPattern(center, radius, alpha, specialAnimProgress)
        }
        SpecialType.None -> { /* Regular gem, no extra decoration */ }
    }

    // === Step 4: Draw colorblind shape overlay ===
    // In high-contrast mode, shapes get thicker outlines (1.5x stroke) for extra visibility.
    if (colorblindMode) {
        drawColorblindShape(gem.type, center, radius, alpha, highContrastMode)
    }
}

// ===========================================================================
// Space-Themed Gem Shape Renderers
// ===========================================================================

/**
 * Red gem: Crystal — faceted diamond shape with crown facet highlights.
 * A 4-pointed elongated diamond with internal facet lines radiating
 * from the center, giving it a cut-gemstone appearance.
 */
private fun DrawScope.drawCrystalGem(
    center: Offset, radius: Float, color: Color, darkColor: Color, alpha: Float
) {
    // 4-point diamond path (taller than wide for crystal shape)
    val path = Path().apply {
        moveTo(center.x, center.y - radius * 0.95f)          // Top point
        lineTo(center.x + radius * 0.7f, center.y)            // Right point
        lineTo(center.x, center.y + radius * 0.95f)           // Bottom point
        lineTo(center.x - radius * 0.7f, center.y)            // Left point
        close()
    }

    // Main body fill
    drawPath(path = path, color = color.copy(alpha = alpha))

    // Upper-left facet (lighter — catches the light)
    val upperFacet = Path().apply {
        moveTo(center.x, center.y - radius * 0.95f)          // Top
        lineTo(center.x, center.y)                             // Center
        lineTo(center.x - radius * 0.7f, center.y)            // Left
        close()
    }
    drawPath(path = upperFacet, color = Color.White.copy(alpha = 0.25f * alpha))

    // Lower-right facet (darker — in shadow)
    val lowerFacet = Path().apply {
        moveTo(center.x, center.y + radius * 0.95f)           // Bottom
        lineTo(center.x, center.y)                             // Center
        lineTo(center.x + radius * 0.7f, center.y)            // Right
        close()
    }
    drawPath(path = lowerFacet, color = darkColor.copy(alpha = 0.3f * alpha))

    // Facet lines radiating from center — gives the cut-gem look
    val facetColor = Color.White.copy(alpha = 0.3f * alpha)
    val facetStroke = radius * 0.04f
    // Center to top
    drawLine(facetColor, center, Offset(center.x, center.y - radius * 0.95f), facetStroke)
    // Center to right
    drawLine(facetColor, center, Offset(center.x + radius * 0.7f, center.y), facetStroke)
    // Center to bottom
    drawLine(facetColor, center, Offset(center.x, center.y + radius * 0.95f), facetStroke)
    // Center to left
    drawLine(facetColor, center, Offset(center.x - radius * 0.7f, center.y), facetStroke)

    // Crown highlight sparkle at upper-left facet
    drawCircle(
        color = Color.White.copy(alpha = 0.5f * alpha),
        radius = radius * 0.1f,
        center = Offset(center.x - radius * 0.25f, center.y - radius * 0.35f)
    )
}

/**
 * Blue gem: Planet — sphere with atmospheric gradient band and thin ring.
 * Looks like a miniature gas giant or Earth-like planet.
 */
private fun DrawScope.drawPlanetGem(
    center: Offset, radius: Float, color: Color, darkColor: Color, alpha: Float
) {
    // Main sphere body
    drawCircle(color = color.copy(alpha = alpha), radius = radius * 0.85f, center = center)

    // Atmospheric band (darker stripe across the middle)
    val bandPath = Path().apply {
        moveTo(center.x - radius * 0.82f, center.y - radius * 0.1f)
        lineTo(center.x + radius * 0.82f, center.y - radius * 0.1f)
        lineTo(center.x + radius * 0.78f, center.y + radius * 0.15f)
        lineTo(center.x - radius * 0.78f, center.y + radius * 0.15f)
        close()
    }
    drawPath(path = bandPath, color = darkColor.copy(alpha = 0.4f * alpha))

    // Second thinner atmospheric band below
    val band2Path = Path().apply {
        moveTo(center.x - radius * 0.7f, center.y + radius * 0.3f)
        lineTo(center.x + radius * 0.7f, center.y + radius * 0.3f)
        lineTo(center.x + radius * 0.65f, center.y + radius * 0.42f)
        lineTo(center.x - radius * 0.65f, center.y + radius * 0.42f)
        close()
    }
    drawPath(path = band2Path, color = darkColor.copy(alpha = 0.25f * alpha))

    // Thin orbital ring (elliptical — wider than tall)
    drawOval(
        color = Color.White.copy(alpha = 0.4f * alpha),
        topLeft = Offset(center.x - radius * 1.0f, center.y - radius * 0.18f),
        size = Size(radius * 2.0f, radius * 0.36f),
        style = Stroke(width = radius * 0.06f)
    )

    // Upper-left atmospheric glow (spherical highlight)
    drawCircle(
        color = Color.White.copy(alpha = 0.3f * alpha),
        radius = radius * 0.4f,
        center = Offset(center.x - radius * 0.25f, center.y - radius * 0.35f)
    )
}

/**
 * Green gem: Star — 5-pointed filled star with a bright center highlight.
 * A classic celestial star shape.
 */
private fun DrawScope.drawStarGem(
    center: Offset, radius: Float, color: Color, darkColor: Color, alpha: Float
) {
    val outerR = radius * 0.95f
    val innerR = radius * 0.4f
    val points = 5

    // Build the 5-pointed star path
    val starPath = Path().apply {
        for (i in 0 until points * 2) {
            val angle = (i * Math.PI / points - Math.PI / 2).toFloat()
            val r = if (i % 2 == 0) outerR else innerR
            val x = center.x + r * cos(angle)
            val y = center.y + r * sin(angle)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    // Filled star body
    drawPath(path = starPath, color = color.copy(alpha = alpha))

    // Dark outline for definition
    drawPath(
        path = starPath,
        color = darkColor.copy(alpha = 0.4f * alpha),
        style = Stroke(width = radius * 0.05f)
    )

    // Bright center glow (makes it look like a glowing star)
    drawCircle(
        color = Color.White.copy(alpha = 0.5f * alpha),
        radius = radius * 0.3f,
        center = center
    )

    // Sparkle highlight at top-left
    drawCircle(
        color = Color.White.copy(alpha = 0.35f * alpha),
        radius = radius * 0.12f,
        center = Offset(center.x - radius * 0.2f, center.y - radius * 0.3f)
    )
}

/**
 * Yellow gem: Asteroid — irregular polygon with crater details.
 * A bumpy, rocky-looking polygon with small crater indentations.
 */
private fun DrawScope.drawAsteroidGem(
    center: Offset, radius: Float, color: Color, darkColor: Color, alpha: Float
) {
    // 8 irregular points making a bumpy asteroid silhouette
    val r = radius * 0.9f
    val offsets = listOf(
        0.85f, 0.95f, 0.78f, 0.92f, 0.80f, 0.98f, 0.75f, 0.88f
    )

    val asteroidPath = Path().apply {
        for (i in 0 until 8) {
            val angle = (i * Math.PI / 4.0 - Math.PI / 8.0).toFloat()
            val pointR = r * offsets[i]
            val x = center.x + pointR * cos(angle)
            val y = center.y + pointR * sin(angle)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    // Main body fill
    drawPath(path = asteroidPath, color = color.copy(alpha = alpha))

    // Dark outline
    drawPath(
        path = asteroidPath,
        color = darkColor.copy(alpha = 0.4f * alpha),
        style = Stroke(width = radius * 0.05f)
    )

    // Crater 1 (large, top-right area)
    drawCircle(
        color = darkColor.copy(alpha = 0.35f * alpha),
        radius = radius * 0.2f,
        center = Offset(center.x + radius * 0.2f, center.y - radius * 0.2f)
    )
    // Crater rim highlight
    drawCircle(
        color = Color.White.copy(alpha = 0.15f * alpha),
        radius = radius * 0.2f,
        center = Offset(center.x + radius * 0.2f, center.y - radius * 0.2f),
        style = Stroke(width = radius * 0.04f)
    )

    // Crater 2 (smaller, bottom-left)
    drawCircle(
        color = darkColor.copy(alpha = 0.3f * alpha),
        radius = radius * 0.13f,
        center = Offset(center.x - radius * 0.3f, center.y + radius * 0.25f)
    )

    // Crater 3 (tiny, center-bottom)
    drawCircle(
        color = darkColor.copy(alpha = 0.25f * alpha),
        radius = radius * 0.08f,
        center = Offset(center.x + radius * 0.05f, center.y + radius * 0.4f)
    )

    // Surface highlight (top-left illumination)
    drawCircle(
        color = Color.White.copy(alpha = 0.2f * alpha),
        radius = radius * 0.35f,
        center = Offset(center.x - radius * 0.15f, center.y - radius * 0.25f)
    )
}

/**
 * Orange gem: Nebula — overlapping translucent circles forming a cloud
 * with a bright glowing core. Looks like a cosmic gas cloud.
 */
private fun DrawScope.drawNebulaGem(
    center: Offset, radius: Float, color: Color, darkColor: Color, alpha: Float
) {
    // 4 overlapping translucent cloud circles at different positions
    val cloudPositions = listOf(
        Offset(center.x - radius * 0.2f, center.y - radius * 0.15f),
        Offset(center.x + radius * 0.25f, center.y - radius * 0.1f),
        Offset(center.x - radius * 0.1f, center.y + radius * 0.2f),
        Offset(center.x + radius * 0.15f, center.y + radius * 0.25f)
    )
    val cloudRadii = listOf(0.55f, 0.5f, 0.48f, 0.45f)

    // Draw outer translucent clouds (darker shade for depth)
    for (i in cloudPositions.indices) {
        drawCircle(
            color = darkColor.copy(alpha = 0.35f * alpha),
            radius = radius * cloudRadii[i],
            center = cloudPositions[i]
        )
    }

    // Draw inner brighter clouds
    for (i in cloudPositions.indices) {
        drawCircle(
            color = color.copy(alpha = 0.5f * alpha),
            radius = radius * cloudRadii[i] * 0.8f,
            center = cloudPositions[i]
        )
    }

    // Bright glowing core at center
    drawCircle(
        color = color.copy(alpha = 0.9f * alpha),
        radius = radius * 0.35f,
        center = center
    )

    // Hot white center (the nebula's bright heart)
    drawCircle(
        color = Color.White.copy(alpha = 0.5f * alpha),
        radius = radius * 0.18f,
        center = center
    )

    // Subtle outer haze
    drawCircle(
        color = color.copy(alpha = 0.15f * alpha),
        radius = radius * 0.95f,
        center = center
    )
}

/**
 * Purple gem: Moon — sphere with 3 crater indentations (dark circles
 * with light rims). Looks like a small moon or planetoid.
 */
private fun DrawScope.drawMoonGem(
    center: Offset, radius: Float, color: Color, darkColor: Color, alpha: Float
) {
    // Main sphere body
    drawCircle(color = color.copy(alpha = alpha), radius = radius * 0.88f, center = center)

    // Crater 1 (large, upper-right)
    drawCircle(
        color = darkColor.copy(alpha = 0.4f * alpha),
        radius = radius * 0.22f,
        center = Offset(center.x + radius * 0.25f, center.y - radius * 0.2f)
    )
    // Crater 1 light rim (illuminated top edge)
    drawArc(
        color = Color.White.copy(alpha = 0.2f * alpha),
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(
            center.x + radius * 0.25f - radius * 0.22f,
            center.y - radius * 0.2f - radius * 0.22f
        ),
        size = Size(radius * 0.44f, radius * 0.44f),
        style = Stroke(width = radius * 0.04f)
    )

    // Crater 2 (medium, lower-left)
    drawCircle(
        color = darkColor.copy(alpha = 0.35f * alpha),
        radius = radius * 0.16f,
        center = Offset(center.x - radius * 0.3f, center.y + radius * 0.2f)
    )
    // Crater 2 light rim
    drawArc(
        color = Color.White.copy(alpha = 0.18f * alpha),
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(
            center.x - radius * 0.3f - radius * 0.16f,
            center.y + radius * 0.2f - radius * 0.16f
        ),
        size = Size(radius * 0.32f, radius * 0.32f),
        style = Stroke(width = radius * 0.04f)
    )

    // Crater 3 (small, center-bottom)
    drawCircle(
        color = darkColor.copy(alpha = 0.3f * alpha),
        radius = radius * 0.1f,
        center = Offset(center.x + radius * 0.05f, center.y + radius * 0.45f)
    )

    // Overall spherical highlight (upper-left, moon surface catching light)
    drawCircle(
        color = Color.White.copy(alpha = 0.25f * alpha),
        radius = radius * 0.45f,
        center = Offset(center.x - radius * 0.2f, center.y - radius * 0.25f)
    )

    // Terminator shadow (dark edge on lower-right for 3D depth)
    drawArc(
        color = Color.Black.copy(alpha = 0.15f * alpha),
        startAngle = 20f,
        sweepAngle = 160f,
        useCenter = true,
        topLeft = Offset(center.x - radius * 0.88f, center.y - radius * 0.88f),
        size = Size(radius * 1.76f, radius * 1.76f)
    )
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
    val pulseAlpha = 0.5f + 0.4f * sin(animProgress * 2f * Math.PI.toFloat())
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
    val pulseAlpha = 0.5f + 0.4f * sin(animProgress * 2f * Math.PI.toFloat())
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
    val breathe = sin(animProgress * 2f * Math.PI.toFloat())
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
        val dotX = center.x + (dotDistance * cos(angle)).toFloat()
        val dotY = center.y + (dotDistance * sin(angle)).toFloat()

        drawCircle(
            color = dotColors[i].copy(alpha = alpha),
            radius = dotRadius,
            center = Offset(dotX, dotY)
        )
    }

    // Center sparkle: pulses at 2x frequency for a lively twinkling effect
    val sparklePulse = sin(animProgress * 4f * Math.PI.toFloat())
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
    alpha: Float,
    highContrast: Boolean = false
) {
    val shapeColor = Color.White.copy(alpha = 0.85f * alpha)
    // Thicker outlines in high-contrast mode for extra visibility
    val strokeWidth = radius * if (highContrast) 0.15f else 0.1f
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
        style = Stroke(width = strokeWidth)
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
        style = Stroke(width = strokeWidth)
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
            val x = center.x + r * cos(angle)
            val y = center.y + r * sin(angle)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth)
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
        style = Stroke(width = strokeWidth)
    )
}

/** Purple: Hexagon */
private fun DrawScope.drawHexagonShape(
    center: Offset, size: Float, color: Color, strokeWidth: Float
) {
    val path = Path().apply {
        for (i in 0 until 6) {
            val angle = (i * Math.PI / 3 - Math.PI / 2).toFloat()
            val x = center.x + size * cos(angle)
            val y = center.y + size * sin(angle)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth)
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
        style = Stroke(width = radius * 0.1f)
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
 * Draw a reinforced ice overlay (2-hit ice).
 *
 * Thicker, deeper blue than normal ice with more sparkles.
 * Downgrades to normal Ice after the first hit.
 */
fun DrawScope.drawReinforcedIce(
    center: Offset,
    radius: Float,
    alpha: Float = 1f
) {
    // Thicker cyan border ring
    drawCircle(
        color = Color(0xFF0099CC).copy(alpha = 0.7f * alpha),
        radius = radius * 1.1f,
        center = center,
        style = Stroke(width = radius * 0.14f)
    )

    // Second inner ring for layered look
    drawCircle(
        color = Color(0xFF00BBEE).copy(alpha = 0.4f * alpha),
        radius = radius * 1.0f,
        center = center,
        style = Stroke(width = radius * 0.06f)
    )

    // Deeper blue tint fill
    drawCircle(
        color = Color(0xFF88BBFF).copy(alpha = 0.4f * alpha),
        radius = radius,
        center = center
    )

    // 6 frost sparkles (doubled from normal ice's 3)
    val sparklePositions = listOf(
        Offset(center.x - radius * 0.35f, center.y - radius * 0.3f),
        Offset(center.x + radius * 0.25f, center.y - radius * 0.15f),
        Offset(center.x - radius * 0.1f, center.y + radius * 0.35f),
        Offset(center.x + radius * 0.35f, center.y + radius * 0.25f),
        Offset(center.x - radius * 0.3f, center.y + radius * 0.1f),
        Offset(center.x + radius * 0.1f, center.y - radius * 0.4f)
    )
    val sparkleRadii = listOf(
        radius * 0.08f, radius * 0.06f, radius * 0.07f,
        radius * 0.06f, radius * 0.05f, radius * 0.07f
    )

    for (i in sparklePositions.indices) {
        drawCircle(
            color = Color.White.copy(alpha = 0.9f * alpha),
            radius = sparkleRadii[i],
            center = sparklePositions[i]
        )
    }

    // Inner highlight
    drawCircle(
        color = Color.White.copy(alpha = 0.2f * alpha),
        radius = radius * 0.5f,
        center = Offset(center.x - radius * 0.2f, center.y - radius * 0.25f)
    )
}

/**
 * Draw a lock cage overlay on a gem.
 *
 * Shows dark metallic cage bars in an X-pattern with a small padlock circle.
 * Indicates the gem cannot be swapped until freed by an adjacent match.
 */
fun DrawScope.drawLocked(
    center: Offset,
    radius: Float,
    alpha: Float = 1f
) {
    val cageColor = Color(0xFF666666).copy(alpha = 0.75f * alpha)
    val barWidth = radius * 0.1f

    // X-pattern cage bars
    drawLine(
        color = cageColor,
        start = Offset(center.x - radius * 0.7f, center.y - radius * 0.7f),
        end = Offset(center.x + radius * 0.7f, center.y + radius * 0.7f),
        strokeWidth = barWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = cageColor,
        start = Offset(center.x + radius * 0.7f, center.y - radius * 0.7f),
        end = Offset(center.x - radius * 0.7f, center.y + radius * 0.7f),
        strokeWidth = barWidth,
        cap = StrokeCap.Round
    )

    // Horizontal and vertical bars
    drawLine(
        color = cageColor,
        start = Offset(center.x - radius * 0.7f, center.y),
        end = Offset(center.x + radius * 0.7f, center.y),
        strokeWidth = barWidth * 0.8f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = cageColor,
        start = Offset(center.x, center.y - radius * 0.7f),
        end = Offset(center.x, center.y + radius * 0.7f),
        strokeWidth = barWidth * 0.8f,
        cap = StrokeCap.Round
    )

    // Small padlock circle at center
    drawCircle(
        color = Color(0xFF555555).copy(alpha = 0.85f * alpha),
        radius = radius * 0.2f,
        center = center
    )
    drawCircle(
        color = Color(0xFF888888).copy(alpha = 0.9f * alpha),
        radius = radius * 0.2f,
        center = center,
        style = Stroke(width = radius * 0.06f)
    )
    // Padlock shackle (small arc above the circle)
    drawCircle(
        color = Color(0xFF888888).copy(alpha = 0.9f * alpha),
        radius = radius * 0.12f,
        center = Offset(center.x, center.y - radius * 0.18f),
        style = Stroke(width = radius * 0.05f)
    )
}

/**
 * Draw a bomb timer overlay on a gem.
 *
 * Shows a countdown number with urgency coloring:
 * 3+ moves = orange, 2 = red, 1 = bright red pulsing.
 * A small fuse indicator sits above the number.
 */
fun DrawScope.drawBomb(
    center: Offset,
    radius: Float,
    timer: Int,
    alpha: Float = 1f
) {
    // Background circle for the countdown
    val urgencyColor = when {
        timer <= 1 -> Color(0xFFFF0000)  // Bright red — about to explode!
        timer <= 2 -> Color(0xFFDD2200)  // Red
        else -> Color(0xFFFF8800)        // Orange
    }

    // Dark circle behind the number
    drawCircle(
        color = Color.Black.copy(alpha = 0.6f * alpha),
        radius = radius * 0.38f,
        center = Offset(center.x, center.y + radius * 0.15f)
    )

    // Colored ring
    drawCircle(
        color = urgencyColor.copy(alpha = 0.9f * alpha),
        radius = radius * 0.38f,
        center = Offset(center.x, center.y + radius * 0.15f),
        style = Stroke(width = radius * 0.08f)
    )

    // Fuse spark (small dot above)
    drawCircle(
        color = Color(0xFFFFCC00).copy(alpha = 0.9f * alpha),
        radius = radius * 0.08f,
        center = Offset(center.x, center.y - radius * 0.55f)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.7f * alpha),
        radius = radius * 0.04f,
        center = Offset(center.x, center.y - radius * 0.55f)
    )

    // Fuse line
    drawLine(
        color = Color(0xFF885500).copy(alpha = 0.8f * alpha),
        start = Offset(center.x, center.y - radius * 0.2f),
        end = Offset(center.x, center.y - radius * 0.5f),
        strokeWidth = radius * 0.06f,
        cap = StrokeCap.Round
    )

    // Draw the countdown number using drawCircle-based digit representation
    // We draw a simple number by rendering small circles/lines
    val numColor = urgencyColor.copy(alpha = alpha)
    val textSize = radius * 0.35f

    // Draw number as centered text-like shape
    // Since we can't easily draw text in DrawScope without nativeCanvas,
    // draw the number as a bright colored dot pattern or use the number directly
    // via nativeCanvas for crisp rendering
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.argb(
            (alpha * 255).toInt(),
            (numColor.red * 255).toInt(),
            (numColor.green * 255).toInt(),
            (numColor.blue * 255).toInt()
        )
        this.textSize = textSize * 2.5f
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }
    // Clamp to 0 minimum so a negative timer is never displayed
    drawContext.canvas.nativeCanvas.drawText(
        maxOf(0, timer).toString(),
        center.x,
        center.y + radius * 0.3f,
        paint
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
