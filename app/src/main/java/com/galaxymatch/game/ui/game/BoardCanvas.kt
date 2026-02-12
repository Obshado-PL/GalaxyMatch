package com.galaxymatch.game.ui.game

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.galaxymatch.game.engine.GravityProcessor
import com.galaxymatch.game.model.BoardState
import com.galaxymatch.game.model.GamePhase
import com.galaxymatch.game.model.ObstacleType
import com.galaxymatch.game.ui.theme.StarGold
import com.galaxymatch.game.model.Position
import com.galaxymatch.game.model.PowerUpType
import com.galaxymatch.game.model.SpecialType
import com.galaxymatch.game.model.SwapAction
import com.galaxymatch.game.ui.components.toColor
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay

/**
 * The main Canvas composable that renders the entire game board.
 *
 * This draws:
 * 1. A background grid with rounded-corner cells
 * 2. All the gems in their positions
 *
 * It also handles swipe gesture detection and converts touch events
 * into game actions (which position did the player swipe from/to).
 *
 * Why Canvas instead of individual Compose UI elements?
 * - A single Canvas is more performant than 64+ Box/Image composables
 * - Full control over pixel-level drawing for smooth animations
 * - No recomposition overhead when animating individual gems
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
    fallingGems: List<GravityProcessor.GemMovement> = emptyList(),
    fallProgress: Float = 0f,
    isShuffling: Boolean = false,
    shuffleProgress: Float = 0f,
    screenShakeProgress: Float = 0f,
    hintPositions: Set<Position> = emptySet(),
    hintAnimProgress: Float = 0f,
    boardEntryProgress: Float = 1f,
    activePowerUp: PowerUpType? = null,
    comboLevel: Int = 0,
    onSwipe: (Position, Position) -> Unit,
    onPowerUpTap: (Position) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Remember the calculated cell size so we can use it in the gesture detector
    val cellSizeState = remember { mutableFloatStateOf(0f) }

    // === Special gem animation loop ===
    // Runs continuously from 0→1 over 2 seconds, then restarts.
    // Used to pulse stripes, breathe wrapped glow, and rotate color bomb dots.
    val infiniteTransition = rememberInfiniteTransition(label = "specialAnim")
    val specialAnimProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "specialAnimLoop"
    )

    // === Particle System ===
    // Managed entirely in the composable (not in ViewModel) because
    // particles are purely visual and need frame-by-frame updates.
    val particleSystem = remember { ParticleSystem() }

    // Track whether we already spawned particles for the current match clear
    // to prevent spawning duplicates every recomposition.
    val hasSpawnedForCurrentMatch = remember { mutableStateOf(false) }

    // When matchClearProgress transitions from 0 to >0, spawn particles
    // at each matched gem's position using the gem's color.
    //
    // Enhanced: Different particle effects for different gem/obstacle types:
    // - Normal gem: combo-scaled burst (bigger combos → more particles)
    // - StripedH/V: directional line burst in the stripe's direction
    // - Wrapped: expanding ring burst
    // - ColorBomb: rainbow multicolor burst
    // - Ice overlay: additional white/cyan ice shatter particles
    LaunchedEffect(matchClearProgress) {
        if (matchClearProgress > 0f && matchClearProgress < 0.15f && !hasSpawnedForCurrentMatch.value) {
            hasSpawnedForCurrentMatch.value = true
            val cellSize = cellSizeState.floatValue
            if (cellSize > 0f) {
                for (pos in matchedPositions) {
                    val gem = boardState.grid[pos.row][pos.col] ?: continue
                    val cx = pos.col * cellSize + cellSize / 2
                    val cy = pos.row * cellSize + cellSize / 2
                    val color = gem.type.toColor()

                    // Choose particle effect based on special type
                    when (gem.special) {
                        SpecialType.StripedHorizontal ->
                            particleSystem.spawnStripedLine(cx, cy, color, horizontal = true, cellSize)
                        SpecialType.StripedVertical ->
                            particleSystem.spawnStripedLine(cx, cy, color, horizontal = false, cellSize)
                        SpecialType.Wrapped ->
                            particleSystem.spawnWrappedRing(cx, cy, color)
                        SpecialType.ColorBomb ->
                            particleSystem.spawnColorBombRainbow(cx, cy)
                        else ->
                            // Normal gems get combo-scaled particles
                            particleSystem.spawnComboScaled(
                                cx, cy, color, comboLevel,
                                baseRadius = cellSize * 0.04f
                            )
                    }

                    // Ice shatter particles: if this position has ice, add icy shards
                    val obstacle = boardState.getObstacle(pos)
                    if (obstacle == ObstacleType.Ice) {
                        particleSystem.spawnIceShatter(cx, cy)
                    }
                }
            }
        }
        if (matchClearProgress == 0f) {
            hasSpawnedForCurrentMatch.value = false
        }
    }

    // === Confetti celebration on level complete ===
    // When the game phase transitions to LevelComplete, spawn 3 waves of
    // colorful confetti that rain down from the top of the board canvas.
    LaunchedEffect(phase) {
        if (phase == GamePhase.LevelComplete) {
            val cellSize = cellSizeState.floatValue
            if (cellSize > 0f) {
                val canvasWidth = cellSize * boardState.cols
                val canvasHeight = cellSize * boardState.rows
                // 3 staggered waves for a sustained confetti shower
                particleSystem.spawnConfetti(canvasWidth, canvasHeight)
                delay(300)
                particleSystem.spawnConfetti(canvasWidth, canvasHeight)
                delay(300)
                particleSystem.spawnConfetti(canvasWidth, canvasHeight)
            }
        }
    }

    // Continuous frame loop for particle animation.
    // Runs at ~60fps using a 16ms delay, updating particle positions each tick.
    LaunchedEffect(Unit) {
        while (isActive) {
            if (particleSystem.hasParticles()) {
                // ~60fps frame time in seconds (capped to prevent physics jumps)
                particleSystem.update(0.016f)
            }
            delay(16L)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            // Make the canvas aspect ratio match the board proportions
            .aspectRatio(boardState.cols.toFloat() / boardState.rows.toFloat())
            // Handle swipe gestures for gem swapping
            // === Power-up target selection: tap gesture ===
            // When a power-up is active, we listen for taps instead of drags.
            // The tap converts pixel coordinates to a board Position and calls
            // onPowerUpTap so the ViewModel can execute the power-up there.
            .pointerInput(activePowerUp) {
                if (activePowerUp == null) return@pointerInput

                detectTapGestures { offset ->
                    val cellSize = cellSizeState.floatValue
                    if (cellSize <= 0f) return@detectTapGestures

                    val col = (offset.x / cellSize).toInt()
                    val row = (offset.y / cellSize).toInt()
                    val pos = Position(row, col)

                    if (boardState.isInBounds(pos)) {
                        onPowerUpTap(pos)
                    }
                }
            }
            // === Normal swipe gesture for gem swapping ===
            .pointerInput(phase, activePowerUp) {
                // Only accept input during Idle phase and when no power-up is active
                if (phase != GamePhase.Idle) return@pointerInput
                if (activePowerUp != null) return@pointerInput

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
        val gemRadius = cellSize * 0.38f   // Gem is slightly smaller than the cell

        // === Draw the background grid ===
        for (row in 0 until boardState.rows) {
            for (col in 0 until boardState.cols) {
                val x = col * cellSize + cellPadding
                val y = row * cellSize + cellPadding
                val cellDrawSize = cellSize - cellPadding * 2

                // Alternate cell colors for a checkerboard-like pattern
                val cellColor = if ((row + col) % 2 == 0) {
                    Color(0xFF2A2855) // Lighter cell (space grid)
                } else {
                    Color(0xFF222050) // Darker cell (space grid)
                }

                drawRoundRect(
                    color = cellColor,
                    topLeft = Offset(x, y),
                    size = Size(cellDrawSize, cellDrawSize),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            }
        }

        // === Draw stone obstacles ===
        // Stones are drawn right after the background grid, BEFORE gems.
        // They replace the cell visually — no gem exists at stone positions.
        // Stones don't participate in board entry animation (they're always there).
        for ((obstaclePos, obstacleType) in boardState.obstacles) {
            if (obstacleType == ObstacleType.Stone) {
                val stoneX = obstaclePos.col * cellSize + cellPadding
                val stoneY = obstaclePos.row * cellSize + cellPadding
                val stoneCellSize = cellSize - cellPadding * 2

                drawStone(
                    topLeft = Offset(stoneX, stoneY),
                    cellSize = stoneCellSize,
                    cornerRadius = cornerRadius,
                    alpha = 1f
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

        // === Feature 1: Screen shake for big combos ===
        // A decaying sine wave that shakes the board in both X and Y directions.
        // The amplitude decreases as progress increases (shakes hard then settles).
        val comboShakeOffsetX = if (screenShakeProgress > 0f) {
            val amplitude = cellSize * 0.08f * (1f - screenShakeProgress)
            val frequency = screenShakeProgress * 8f * Math.PI.toFloat()
            amplitude * kotlin.math.sin(frequency)
        } else 0f

        val comboShakeOffsetY = if (screenShakeProgress > 0f) {
            val amplitude = cellSize * 0.05f * (1f - screenShakeProgress)
            val frequency = screenShakeProgress * 8f * Math.PI.toFloat()
            amplitude * kotlin.math.cos(frequency)
        } else 0f

        // === Build a lookup map for fall animations ===
        // Maps each gem's unique ID to its movement data, so we can
        // quickly check if a gem needs to be drawn at an interpolated position
        val movementMap: Map<Long, GravityProcessor.GemMovement> =
            fallingGems.associateBy { it.gemId }

        // === Draw all gems ===
        for (row in 0 until boardState.rows) {
            for (col in 0 until boardState.cols) {
                val gem = boardState.grid[row][col] ?: continue
                val pos = Position(row, col)

                // Calculate the center position of this gem
                // (shakeOffsetX is added for the shuffle wobble effect,
                //  comboShake offsets add screen shake on big combos)
                var centerX = col * cellSize + cellSize / 2 + shakeOffsetX + comboShakeOffsetX
                var centerY = row * cellSize + cellSize / 2 + comboShakeOffsetY

                // === Board entry animation (gem drop-in) ===
                // When a level starts, each gem drops in from above the board.
                // Staggered by column (left→right) and row (top→bottom) to create
                // a wave effect. Uses a bounce easing for a satisfying settle.
                var entryAlphaMultiplier = 1f
                if (boardEntryProgress < 1f) {
                    val maxCols = boardState.cols.toFloat()
                    val maxRows = boardState.rows.toFloat()
                    // Stagger: left columns and top rows appear first
                    val staggerDelay = (col / maxCols) * 0.3f + (row / maxRows) * 0.15f
                    // Per-gem progress (0→1) accounting for stagger
                    val gemProgress = ((boardEntryProgress - staggerDelay) / (1f - staggerDelay))
                        .coerceIn(0f, 1f)

                    // Bounce easing: fast approach, then overshoot and settle
                    val bounceEased = if (gemProgress < 0.6f) {
                        val t = gemProgress / 0.6f
                        t * t // Accelerating approach
                    } else {
                        val t = (gemProgress - 0.6f) / 0.4f
                        // Overshoot ~12% past target, then settle back
                        1f + 0.12f * kotlin.math.sin(t * Math.PI.toFloat())
                    }

                    // Drop from above: gems start 1.2x board height above final position
                    val dropDistance = boardState.rows * cellSize * 1.2f
                    centerY -= dropDistance * (1f - bounceEased)

                    // Fade in at the very start of each gem's drop
                    if (gemProgress < 0.2f) {
                        entryAlphaMultiplier = gemProgress / 0.2f
                    }
                }

                // If this gem is part of a swap animation, offset its position
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
                // If this gem has a pending fall movement, interpolate its
                // Y position from where it was (fromRow) to where it is now (toRow).
                // New gems have negative fromRow — they start above the board
                // and slide in from the top, which looks great!
                val movement = movementMap[gem.id]
                if (movement != null && fallProgress < 1f) {
                    val fromY = movement.fromRow * cellSize + cellSize / 2
                    val toY = movement.toRow * cellSize + cellSize / 2
                    centerY = fromY + (toY - fromY) * fallProgress
                }

                // === Match/clear animation ===
                // Matched gems shrink and fade out as matchClearProgress goes 0→1
                val isMatched = pos in matchedPositions
                var alpha = 1f * entryAlphaMultiplier
                var drawRadius = gemRadius

                if (isMatched && matchClearProgress > 0f) {
                    // Shrink from full size to zero
                    drawRadius = gemRadius * (1f - matchClearProgress)
                    // Fade from fully visible to invisible
                    alpha = 1f - matchClearProgress
                } else if (isMatched) {
                    // matchClearProgress is 0 but gem IS matched:
                    // brief "flash" highlight before shrinking starts
                    alpha = 0.85f
                    drawRadius = gemRadius * 1.05f
                }

                // === Feature 2: Hint glow animation ===
                // Hinted gems pulse with a gentle white glow ring behind them.
                // The glow uses a sine wave to create a smooth "breathing" effect.
                val isHinted = pos in hintPositions
                if (isHinted && hintAnimProgress > 0f) {
                    val glowPulse = kotlin.math.sin(hintAnimProgress * Math.PI.toFloat())
                    val glowRadius = gemRadius * (1.1f + 0.15f * glowPulse)
                    val glowAlpha = 0.3f + 0.3f * glowPulse

                    // Outer gold ring — gives the hint glow more visibility
                    drawCircle(
                        color = StarGold.copy(alpha = glowAlpha * 0.4f),
                        radius = glowRadius * 1.15f,
                        center = Offset(centerX, centerY)
                    )
                    // Inner white glow
                    drawCircle(
                        color = Color.White.copy(alpha = glowAlpha),
                        radius = glowRadius,
                        center = Offset(centerX, centerY)
                    )
                }

                // Only draw if the gem is still visible (not fully shrunk/faded)
                if (alpha > 0.01f && drawRadius > 0.5f) {
                    drawGem(
                        gem = gem,
                        centerX = centerX,
                        centerY = centerY,
                        radius = drawRadius,
                        alpha = alpha,
                        specialAnimProgress = specialAnimProgress
                    )

                    // === Ice overlay ===
                    // If this gem has ice on it, draw the frozen overlay ON TOP.
                    // The ice alpha follows the gem alpha so they fade out together
                    // when the gem is matched (ice breaks when its gem is cleared).
                    val obstacle = boardState.getObstacle(pos)
                    if (obstacle == ObstacleType.Ice) {
                        drawIce(
                            center = Offset(centerX, centerY),
                            radius = drawRadius,
                            alpha = alpha
                        )
                    }
                }
            }
        }

        // === Power-up targeting overlay ===
        // When a power-up is in target selection mode, draw a semi-transparent
        // colored overlay on the board to signal "tap a gem" mode.
        if (activePowerUp != null) {
            val overlayColor = when (activePowerUp) {
                PowerUpType.Hammer -> Color(0x22FF6600)    // Orange tint
                PowerUpType.ColorBomb -> Color(0x229966FF) // Purple tint
                else -> Color(0x22FFFFFF)                  // White (shouldn't happen)
            }
            drawRect(
                color = overlayColor,
                size = size
            )
        }

        // === Draw particles ON TOP of everything ===
        // Particles are small colored circles that burst outward from
        // cleared gems. They're drawn last so they appear above the grid.
        for (particle in particleSystem.particles) {
            drawCircle(
                color = particle.color.copy(alpha = particle.alpha),
                radius = particle.radius,
                center = Offset(particle.x, particle.y)
            )
        }
    }
}
