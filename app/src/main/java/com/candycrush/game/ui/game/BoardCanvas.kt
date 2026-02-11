package com.candycrush.game.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.candycrush.game.engine.GravityProcessor
import com.candycrush.game.model.BoardState
import com.candycrush.game.model.GamePhase
import com.candycrush.game.model.Position
import com.candycrush.game.model.SwapAction

/**
 * The main Canvas composable that renders the entire game board.
 *
 * This draws:
 * 1. A background grid with rounded-corner cells
 * 2. All the candies in their positions
 *
 * It also handles swipe gesture detection and converts touch events
 * into game actions (which position did the player swipe from/to).
 *
 * Why Canvas instead of individual Compose UI elements?
 * - A single Canvas is more performant than 64+ Box/Image composables
 * - Full control over pixel-level drawing for smooth animations
 * - No recomposition overhead when animating individual candies
 *
 * @param boardState The current state of the board to render
 * @param phase The current game phase (used to decide when to accept input)
 * @param matchedPositions Positions currently being matched (for highlight animation)
 * @param onSwipe Callback when the player swipes between two positions
 */
@Composable
fun BoardCanvas(
    boardState: BoardState,
    phase: GamePhase,
    matchedPositions: Set<Position> = emptySet(),
    swapAnimation: SwapAction? = null,
    swapProgress: Float = 0f,
    matchClearProgress: Float = 0f,
    fallingCandies: List<GravityProcessor.CandyMovement> = emptyList(),
    fallProgress: Float = 0f,
    isShuffling: Boolean = false,
    shuffleProgress: Float = 0f,
    onSwipe: (Position, Position) -> Unit,
    modifier: Modifier = Modifier
) {
    // Remember the calculated cell size so we can use it in the gesture detector
    val cellSizeState = remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            // Make the canvas aspect ratio match the board proportions
            .aspectRatio(boardState.cols.toFloat() / boardState.rows.toFloat())
            // Handle swipe gestures for candy swapping
            .pointerInput(phase) {
                // Only accept input during Idle phase
                if (phase != GamePhase.Idle) return@pointerInput

                // Track the starting position and whether we've already fired a swipe
                var startPosition = Offset.Zero
                var swipeFired = false

                detectDragGestures(
                    onDragStart = { offset ->
                        startPosition = offset
                        swipeFired = false
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        if (swipeFired) return@detectDragGestures

                        val cellSize = cellSizeState.floatValue
                        if (cellSize <= 0f) return@detectDragGestures

                        // Calculate drag distance from the start position
                        val dx = change.position.x - startPosition.x
                        val dy = change.position.y - startPosition.y
                        val threshold = cellSize * 0.3f

                        // Figure out which cell the player initially touched
                        val startCol = (startPosition.x / cellSize).toInt()
                        val startRow = (startPosition.y / cellSize).toInt()

                        // Determine swipe direction based on total drag from start
                        val toPos = when {
                            dx > threshold && kotlin.math.abs(dx) > kotlin.math.abs(dy) ->
                                Position(startRow, startCol + 1)
                            dx < -threshold && kotlin.math.abs(dx) > kotlin.math.abs(dy) ->
                                Position(startRow, startCol - 1)
                            dy > threshold && kotlin.math.abs(dy) > kotlin.math.abs(dx) ->
                                Position(startRow + 1, startCol)
                            dy < -threshold && kotlin.math.abs(dy) > kotlin.math.abs(dx) ->
                                Position(startRow - 1, startCol)
                            else -> null
                        }

                        if (toPos != null) {
                            val fromPos = Position(startRow, startCol)
                            if (boardState.isInBounds(fromPos) && boardState.isInBounds(toPos)) {
                                swipeFired = true
                                onSwipe(fromPos, toPos)
                            }
                        }
                    }
                )
            }
    ) {
        // Calculate the size of each cell based on the available canvas width
        val cellSize = size.width / boardState.cols
        cellSizeState.floatValue = cellSize

        val cellPadding = cellSize * 0.06f  // Small gap between cells
        val cornerRadius = cellSize * 0.15f  // Rounded corners for cells
        val candyRadius = cellSize * 0.38f   // Candy is slightly smaller than the cell

        // === Draw the background grid ===
        for (row in 0 until boardState.rows) {
            for (col in 0 until boardState.cols) {
                val x = col * cellSize + cellPadding
                val y = row * cellSize + cellPadding
                val cellDrawSize = cellSize - cellPadding * 2

                // Alternate cell colors for a checkerboard-like pattern
                val cellColor = if ((row + col) % 2 == 0) {
                    Color(0xFF3D3B65) // Lighter cell
                } else {
                    Color(0xFF34325A) // Darker cell
                }

                drawRoundRect(
                    color = cellColor,
                    topLeft = Offset(x, y),
                    size = Size(cellDrawSize, cellDrawSize),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            }
        }

        // === Shuffle shake offset ===
        // During shuffling, the entire board shakes side-to-side.
        // The sine wave oscillates faster as progress increases, and the
        // amplitude decreases over time (shakes hard then settles down).
        val shakeOffsetX = if (isShuffling && shuffleProgress > 0f) {
            val amplitude = cellSize * 0.15f * (1f - shuffleProgress)
            val frequency = shuffleProgress * 6f * Math.PI.toFloat()
            amplitude * kotlin.math.sin(frequency)
        } else {
            0f
        }

        // === Build a lookup map for fall animations ===
        // Maps each candy's unique ID to its movement data, so we can
        // quickly check if a candy needs to be drawn at an interpolated position
        val movementMap: Map<Long, GravityProcessor.CandyMovement> =
            fallingCandies.associateBy { it.candyId }

        // === Draw all candies ===
        for (row in 0 until boardState.rows) {
            for (col in 0 until boardState.cols) {
                val candy = boardState.grid[row][col] ?: continue
                val pos = Position(row, col)

                // Calculate the center position of this candy
                // (shakeOffsetX is added for the shuffle wobble effect)
                var centerX = col * cellSize + cellSize / 2 + shakeOffsetX
                var centerY = row * cellSize + cellSize / 2

                // If this candy is part of a swap animation, offset its position
                if (swapAnimation != null && swapProgress > 0f) {
                    if (pos == swapAnimation.from) {
                        // Move from 'from' toward 'to'
                        val targetX = swapAnimation.to.col * cellSize + cellSize / 2
                        val targetY = swapAnimation.to.row * cellSize + cellSize / 2
                        centerX += (targetX - centerX) * swapProgress
                        centerY += (targetY - centerY) * swapProgress
                    } else if (pos == swapAnimation.to) {
                        // Move from 'to' toward 'from'
                        val targetX = swapAnimation.from.col * cellSize + cellSize / 2
                        val targetY = swapAnimation.from.row * cellSize + cellSize / 2
                        centerX += (targetX - centerX) * swapProgress
                        centerY += (targetY - centerY) * swapProgress
                    }
                }

                // === Gravity/fall animation ===
                // If this candy has a pending fall movement, interpolate its
                // Y position from where it was (fromRow) to where it is now (toRow).
                // New candies have negative fromRow — they start above the board
                // and slide in from the top, which looks great!
                val movement = movementMap[candy.id]
                if (movement != null && fallProgress < 1f) {
                    val fromY = movement.fromRow * cellSize + cellSize / 2
                    val toY = movement.toRow * cellSize + cellSize / 2
                    centerY = fromY + (toY - fromY) * fallProgress
                }

                // === Match/clear animation ===
                // Matched candies shrink and fade out as matchClearProgress goes 0→1
                val isMatched = pos in matchedPositions
                var alpha = 1f
                var drawRadius = candyRadius

                if (isMatched && matchClearProgress > 0f) {
                    // Shrink from full size to zero
                    drawRadius = candyRadius * (1f - matchClearProgress)
                    // Fade from fully visible to invisible
                    alpha = 1f - matchClearProgress
                } else if (isMatched) {
                    // matchClearProgress is 0 but candy IS matched:
                    // brief "flash" highlight before shrinking starts
                    alpha = 0.85f
                    drawRadius = candyRadius * 1.05f
                }

                // Only draw if the candy is still visible (not fully shrunk/faded)
                if (alpha > 0.01f && drawRadius > 0.5f) {
                    drawCandy(
                        candy = candy,
                        centerX = centerX,
                        centerY = centerY,
                        radius = drawRadius,
                        alpha = alpha
                    )
                }
            }
        }
    }
}
