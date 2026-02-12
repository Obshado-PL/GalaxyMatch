package com.candycrush.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.candycrush.game.ui.theme.StarEmpty
import com.candycrush.game.ui.theme.StarGold
import kotlin.math.cos
import kotlin.math.sin

/**
 * Reusable star rating display.
 *
 * Shows 3 stars, colored gold for earned stars and gray for unearned.
 *
 * @param stars Number of stars earned (0-3)
 * @param starSize Size of each star
 */
@Composable
fun StarRating(
    stars: Int,
    starSize: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        for (i in 1..3) {
            Canvas(modifier = Modifier.size(starSize)) {
                val color = if (i <= stars) StarGold else StarEmpty
                drawStar(color, center, size.minDimension / 2f)
            }
        }
    }
}

/**
 * Draw a 5-pointed star shape.
 */
internal fun DrawScope.drawStar(color: Color, center: Offset, radius: Float) {
    val path = Path()
    val innerRadius = radius * 0.45f

    for (i in 0 until 10) {
        // Alternate between outer and inner points
        val r = if (i % 2 == 0) radius else innerRadius
        val angle = Math.toRadians((i * 36.0) - 90.0) // Start from top
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()

    drawPath(path, color)
}
