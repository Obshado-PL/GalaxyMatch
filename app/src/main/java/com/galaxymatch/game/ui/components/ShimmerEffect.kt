package com.galaxymatch.game.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A Modifier extension that applies a shimmer/loading effect.
 *
 * The shimmer is a sweeping gradient band that moves from left to right
 * continuously, commonly used to indicate content is loading.
 *
 * The gradient uses dark-to-light-to-dark purple tones to match the
 * space theme of the app.
 *
 * @param durationMillis How long one full sweep takes (default 1500ms)
 */
@Composable
fun Modifier.shimmerEffect(
    durationMillis: Int = 1500
): Modifier {
    // Animate a float from 0→1 continuously (left-to-right sweep)
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    // Build a linear gradient that sweeps across the element
    // The band is a brighter strip in the middle of two dark edges
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1A1845).copy(alpha = 0.6f),  // Dark space (matches bg)
            Color(0xFF3D3B6B).copy(alpha = 0.8f),  // Slightly brighter
            Color(0xFF5A57A0).copy(alpha = 0.9f),  // Bright band center
            Color(0xFF3D3B6B).copy(alpha = 0.8f),  // Slightly brighter
            Color(0xFF1A1845).copy(alpha = 0.6f)   // Dark space
        ),
        // Sweep position based on animated offset
        // The band width is about 400px; it sweeps from -400 to (width + 400)
        start = Offset(shimmerOffset * 1200f - 400f, 0f),
        end = Offset(shimmerOffset * 1200f, 0f)
    )

    return this.then(Modifier.background(shimmerBrush))
}

/**
 * A skeleton loading grid for the level map.
 *
 * Displays a 4-column grid of circular shimmer placeholders
 * that mimic the LevelNode layout. Shows 20 placeholder circles
 * (matching the number of levels) with a shimmer sweep effect.
 *
 * This creates a smooth loading experience instead of a blank "Loading..." text.
 */
@Composable
fun LevelMapSkeleton() {
    // Animate the shimmer sweep for all placeholders
    val infiniteTransition = rememberInfiniteTransition(label = "mapSkeleton")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "skeletonShimmer"
    )

    // The sweeping gradient brush shared by all skeleton items
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1A1845).copy(alpha = 0.4f),
            Color(0xFF3D3B6B).copy(alpha = 0.7f),
            Color(0xFF5A57A0).copy(alpha = 0.8f),
            Color(0xFF3D3B6B).copy(alpha = 0.7f),
            Color(0xFF1A1845).copy(alpha = 0.4f)
        ),
        start = Offset(shimmerOffset * 1200f - 400f, 0f),
        end = Offset(shimmerOffset * 1200f, 0f)
    )

    // 4-column grid of 20 circular placeholders (matching level count)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title placeholder — a wide rounded rect
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(shimmerBrush)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Level node placeholders — 5 rows of 4 circles
        for (row in 0 until 5) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0 until 4) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(shimmerBrush)
                    )
                }
            }
        }
    }
}

/**
 * A skeleton loading grid for the game board.
 *
 * Displays a grid of rounded-rect shimmer placeholders
 * that mimic the gem board layout. Shows a rows×cols grid
 * of shimmering squares matching the board dimensions.
 *
 * @param rows Number of board rows
 * @param cols Number of board columns
 */
@Composable
fun BoardSkeleton(
    rows: Int = 8,
    cols: Int = 8
) {
    val infiniteTransition = rememberInfiniteTransition(label = "boardSkeleton")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "boardShimmer"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1A1845).copy(alpha = 0.3f),
            Color(0xFF3D3B6B).copy(alpha = 0.6f),
            Color(0xFF5A57A0).copy(alpha = 0.7f),
            Color(0xFF3D3B6B).copy(alpha = 0.6f),
            Color(0xFF1A1845).copy(alpha = 0.3f)
        ),
        start = Offset(shimmerOffset * 1200f - 400f, 0f),
        end = Offset(shimmerOffset * 1200f, 0f)
    )

    // Grid of gem-sized rounded squares
    Column(
        modifier = Modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (row in 0 until rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0 until cols) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmerBrush)
                    )
                }
            }
        }
    }
}
