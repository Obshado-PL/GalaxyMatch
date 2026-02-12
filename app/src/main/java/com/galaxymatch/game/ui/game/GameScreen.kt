package com.galaxymatch.game.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.galaxymatch.game.model.GemType
import com.galaxymatch.game.model.GamePhase
import com.galaxymatch.game.model.ObjectiveType
import com.galaxymatch.game.model.PowerUpType
import com.galaxymatch.game.ui.components.StarRating
import com.galaxymatch.game.ui.components.GalaxyBackground
import com.galaxymatch.game.ui.theme.GameBackground
import com.galaxymatch.game.ui.theme.StarGold

/**
 * The main game screen that contains the board, HUD, and game-over overlay.
 *
 * This screen is the heart of the game. It:
 * - Creates and observes the GameViewModel
 * - Renders the game HUD (score, moves, level)
 * - Renders the game board via BoardCanvas
 * - Shows game-over or level-complete overlays when appropriate
 *
 * @param levelNumber Which level to play
 * @param onGameEnd Called when the game ends (score, stars, won, objectiveText)
 * @param onBackToMap Called when the player wants to return to the level map
 */
@Composable
fun GameScreen(
    levelNumber: Int,
    onGameEnd: (score: Int, stars: Int, won: Boolean, objectiveText: String) -> Unit,
    onBackToMap: () -> Unit
) {
    // Create ViewModel — remember ensures it survives recomposition
    val viewModel = remember(levelNumber) { GameViewModel(levelNumber) }
    val state by viewModel.uiState.collectAsState()

    // === Overlay animation state ===
    // These Animatable values drive the game-over / level-complete overlay
    // entrance animation: fade in the scrim + scale up the content card
    val overlayAlpha = remember { Animatable(0f) }
    val overlayScale = remember { Animatable(0.7f) }

    // Trigger the overlay animation when the phase changes to an end state
    LaunchedEffect(state.phase) {
        if (state.phase == GamePhase.GameOver || state.phase == GamePhase.LevelComplete) {
            // Animate both in parallel using coroutine launch
            launch {
                overlayAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = FastOutSlowInEasing
                    )
                )
            }
            overlayScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 400,
                    easing = FastOutSlowInEasing
                )
            )
        } else {
            // Reset when not showing overlay (e.g. restarting the level)
            overlayAlpha.snapTo(0f)
            overlayScale.snapTo(0.7f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
            .statusBarsPadding()
    ) {
        // Animated galaxy background (stars, comets, nebulae)
        GalaxyBackground()

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // === Game HUD (score, moves, level) ===
            GameHud(
                score = state.score,
                movesRemaining = state.movesRemaining,
                levelConfig = state.levelConfig,
                comboCount = state.comboCount,
                levelNumber = state.levelNumber,
                comboAnimProgress = state.comboAnimProgress,
                currentStars = state.currentStars,
                starJustUnlocked = state.starJustUnlocked,
                starUnlockAnimProgress = state.starUnlockAnimProgress,
                objectiveType = state.objectiveType,
                iceBroken = state.iceBroken,
                totalIce = state.totalIce,
                gemsCleared = state.gemsCleared,
                targetGemCount = state.targetGemCount,
                targetGemType = state.targetGemType,
                objectiveComplete = state.objectiveComplete
            )

            Spacer(modifier = Modifier.height(4.dp))

            // === Power-Up Booster Bar ===
            // Shows 3 booster buttons the player can tap to activate power-ups.
            // When a power-up is in targeting mode, shows instruction text + cancel instead.
            val currentPowerUp = state.activePowerUp
            if (currentPowerUp != null) {
                // === Targeting mode: show instruction + cancel button ===
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Tap a gem to use ${currentPowerUp.emoji}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { viewModel.cancelPowerUp() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Cancel", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else {
                // === Normal mode: show 3 booster buttons ===
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    for (powerUp in PowerUpType.entries) {
                        val canAfford = state.availableStars >= powerUp.starCost
                        val isIdle = state.phase == GamePhase.Idle && state.boardEntryProgress >= 1f

                        Button(
                            onClick = { viewModel.onPowerUpSelected(powerUp) },
                            enabled = canAfford && isIdle,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3D3B6E),
                                disabledContainerColor = Color.White.copy(alpha = 0.05f),
                                disabledContentColor = Color.White.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Emoji icon + star cost label
                            Text(
                                text = "${powerUp.emoji} ${powerUp.starCost}⭐",
                                color = if (canAfford && isIdle) Color.White
                                else Color.White.copy(alpha = 0.3f),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // === Game Board ===
            if (state.board != null) {
                BoardCanvas(
                    boardState = state.board!!,
                    phase = state.phase,
                    matchedPositions = state.matchedPositions,
                    swapAnimation = state.swapAction,
                    swapProgress = state.swapProgress,
                    matchClearProgress = state.matchClearProgress,
                    fallingGems = state.fallingGems,
                    fallProgress = state.fallProgress,
                    isShuffling = state.isShuffling,
                    shuffleProgress = state.shuffleProgress,
                    screenShakeProgress = state.screenShakeProgress,
                    hintPositions = state.hintPositions,
                    hintAnimProgress = state.hintAnimProgress,
                    boardEntryProgress = state.boardEntryProgress,
                    activePowerUp = state.activePowerUp,
                    comboLevel = state.comboCount,
                    onSwipe = { from, to ->
                        viewModel.onSwipe(from, to)
                    },
                    onPowerUpTap = { position ->
                        viewModel.onBoardTapForPowerUp(position)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // === Bottom Action Buttons Row ===
            // Contains Undo, Restart, and Back to Map buttons.
            // Each button has a bounce animation: shrinks on press, springs back on release.

            // Bounce animation state for each button (independent interaction sources)
            val undoInteraction = remember { MutableInteractionSource() }
            val undoPressed by undoInteraction.collectIsPressedAsState()
            val undoScale by animateFloatAsState(
                targetValue = if (undoPressed) 0.92f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "undoBounce"
            )

            val restartInteraction = remember { MutableInteractionSource() }
            val restartPressed by restartInteraction.collectIsPressedAsState()
            val restartScale by animateFloatAsState(
                targetValue = if (restartPressed) 0.92f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "restartBounce"
            )

            val mapInteraction = remember { MutableInteractionSource() }
            val mapPressed by mapInteraction.collectIsPressedAsState()
            val mapScale by animateFloatAsState(
                targetValue = if (mapPressed) 0.92f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "mapBounce"
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Undo button — orange, disabled when not available
                Button(
                    onClick = { viewModel.onUndo() },
                    enabled = state.undoAvailable && state.phase == GamePhase.Idle,
                    interactionSource = undoInteraction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF8C00),
                        disabledContainerColor = Color.White.copy(alpha = 0.08f),
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).scale(undoScale)
                ) {
                    Text(
                        text = if (state.undoUsedThisLevel) "Used" else "Undo",
                        color = if (state.undoAvailable && state.phase == GamePhase.Idle)
                            Color.White else Color.White.copy(alpha = 0.3f)
                    )
                }

                // Restart button
                Button(
                    onClick = { viewModel.onRestartClicked() },
                    interactionSource = restartInteraction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFCC3333)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).scale(restartScale)
                ) {
                    Text(
                        text = "Restart",
                        color = Color.White
                    )
                }

                // Back to Map button
                Button(
                    onClick = onBackToMap,
                    interactionSource = mapInteraction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).scale(mapScale)
                ) {
                    Text(
                        text = "Map",
                        color = Color.White
                    )
                }
            }
        }

        // === Score popup overlay ===
        // Shows a floating "+N" text that drifts upward and fades out
        // after each cascade step scores points
        if (state.scorePopupValue > 0 && state.scorePopupProgress > 0f) {
            val popupAlpha = (1f - state.scorePopupProgress).coerceIn(0f, 1f)
            // Float upward by 120 pixels as progress goes 0→1
            val offsetY = (-120 * state.scorePopupProgress).toInt()

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+${state.scorePopupValue}",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = StarGold.copy(alpha = popupAlpha),
                    modifier = Modifier.offset { IntOffset(0, offsetY) }
                )
            }
        }

        // === Shuffling indicator (with pulsing text) ===
        if (state.isShuffling) {
            // The text pulses in size during the shuffle for a lively feel
            val pulseScale = 1f + 0.15f * kotlin.math.sin(
                state.shuffleProgress * 4f * Math.PI.toFloat()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Shuffling...",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    modifier = Modifier.scale(pulseScale)
                )
            }
        }

        // === Bonus Moves indicator ===
        // Shows "BONUS MOVES!" text and remaining count during the bonus phase.
        // The board is visible behind this so the player sees gems being destroyed.
        if (state.bonusMoveActive && state.bonusMovesRemaining > 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 120.dp)
                ) {
                    Text(
                        text = "BONUS MOVES!",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Color(0xFFFFD700), // Gold
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${state.bonusMovesRemaining} left",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // === Game Over / Level Complete overlay (animated) ===
        // The scrim fades in and the content card scales up from 70% to 100%
        if (state.phase == GamePhase.GameOver || state.phase == GamePhase.LevelComplete) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f * overlayAlpha.value)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .padding(32.dp)
                        .alpha(overlayAlpha.value)
                        .scale(overlayScale.value)
                        .background(
                            color = Color(0xFF2D2B55),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(32.dp)
                ) {
                    Text(
                        text = if (state.phase == GamePhase.LevelComplete) "Level Complete!" else "Game Over",
                        style = MaterialTheme.typography.headlineLarge,
                        color = if (state.phase == GamePhase.LevelComplete) {
                            Color(0xFFFFD700)
                        } else {
                            Color(0xFFFF6666)
                        },
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Score: ${state.score}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )

                    if (state.phase == GamePhase.LevelComplete) {
                        // Show star rating using the proper StarRating composable
                        // (replaces the old text-based asterisk display)
                        StarRating(
                            stars = state.stars,
                            starSize = 40.dp
                        )
                    }

                    // === Objective status text ===
                    // For non-score objectives, show whether the objective was completed
                    val objectiveStatusText = when (state.objectiveType) {
                        is ObjectiveType.BreakAllIce -> {
                            if (state.objectiveComplete) "All ice broken!"
                            else "Ice: ${state.iceBroken}/${state.totalIce}"
                        }
                        is ObjectiveType.ClearGemType -> {
                            val name = state.targetGemType?.name ?: "?"
                            if (state.objectiveComplete) "$name goal reached!"
                            else "$name: ${state.gemsCleared}/${state.targetGemCount}"
                        }
                        else -> null // ReachScore — no extra text needed
                    }

                    if (objectiveStatusText != null) {
                        Text(
                            text = objectiveStatusText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (state.objectiveComplete) Color(0xFF44DD44)
                                else Color(0xFFFF8888)
                        )
                    }

                    // === Feature 1: Best combo summary ===
                    // Show the highest combo chain reached during the level
                    if (state.maxComboReached > 0) {
                        Text(
                            text = "Best Combo: x${state.maxComboReached + 1}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Continue button with bounce feedback
                    val continueInteraction = remember { MutableInteractionSource() }
                    val continuePressed by continueInteraction.collectIsPressedAsState()
                    val continueScale by animateFloatAsState(
                        targetValue = if (continuePressed) 0.92f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "continueBounce"
                    )

                    // Build objective text for the results screen
                    val objectiveText = when (state.objectiveType) {
                        is ObjectiveType.BreakAllIce -> {
                            if (state.objectiveComplete) "All ice broken!"
                            else "Ice: ${state.iceBroken}/${state.totalIce}"
                        }
                        is ObjectiveType.ClearGemType -> {
                            val name = state.targetGemType?.name ?: "?"
                            if (state.objectiveComplete) "$name gems cleared!"
                            else "$name: ${state.gemsCleared}/${state.targetGemCount}"
                        }
                        else -> "" // ReachScore — no objective text needed
                    }

                    Button(
                        onClick = {
                            onGameEnd(
                                state.score,
                                state.stars,
                                state.phase == GamePhase.LevelComplete,
                                objectiveText
                            )
                        },
                        interactionSource = continueInteraction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.phase == GamePhase.LevelComplete) {
                                Color(0xFF44BB44)
                            } else {
                                Color(0xFF6650a4)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().scale(continueScale)
                    ) {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // === Tutorial Overlay ===
        // Shown on level 1 first play to teach the player how to play
        if (state.showTutorial) {
            TutorialOverlay(
                onDismiss = { viewModel.dismissTutorial() }
            )
        }

        // === Pre-Level Dialog ===
        // Shown before the board loads to tell the player the objective.
        // Only shows when tutorial is NOT active (tutorial takes priority on level 1).
        if (state.showPreLevelDialog && !state.showTutorial && state.levelConfig != null) {
            PreLevelDialog(
                levelNumber = state.levelNumber,
                levelConfig = state.levelConfig!!,
                onPlay = { viewModel.dismissPreLevelDialog() }
            )
        }
    }

    // === Restart Confirmation Dialog ===
    // Shown when the player taps the Restart button
    if (state.showRestartDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onRestartDismissed() },
            title = {
                Text(
                    text = "Restart Level?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Your current progress on this level will be lost.")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.restartLevel() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFCC3333)
                    )
                ) {
                    Text("Restart", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { viewModel.onRestartDismissed() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    )
                ) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF2D2B55),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f)
        )
    }
}
