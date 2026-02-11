package com.candycrush.game.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import com.candycrush.game.model.GamePhase
import com.candycrush.game.ui.theme.GameBackground
import com.candycrush.game.ui.theme.StarGold

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
 * @param onGameEnd Called when the game ends (score, stars, won)
 * @param onBackToMap Called when the player wants to return to the level map
 */
@Composable
fun GameScreen(
    levelNumber: Int,
    onGameEnd: (score: Int, stars: Int, won: Boolean) -> Unit,
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
                comboAnimProgress = state.comboAnimProgress
            )

            Spacer(modifier = Modifier.height(8.dp))

            // === Game Board ===
            if (state.board != null) {
                BoardCanvas(
                    boardState = state.board!!,
                    phase = state.phase,
                    matchedPositions = state.matchedPositions,
                    swapAnimation = state.swapAction,
                    swapProgress = state.swapProgress,
                    matchClearProgress = state.matchClearProgress,
                    fallingCandies = state.fallingCandies,
                    fallProgress = state.fallProgress,
                    isShuffling = state.isShuffling,
                    shuffleProgress = state.shuffleProgress,
                    onSwipe = { from, to ->
                        viewModel.onSwipe(from, to)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // === Back to Map button ===
            Button(
                onClick = onBackToMap,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Back to Map",
                    color = Color.White
                )
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
                        // Show star rating
                        Text(
                            text = buildString {
                                repeat(state.stars) { append("*") }
                                repeat(3 - state.stars) { append("-") }
                            },
                            style = MaterialTheme.typography.displayMedium,
                            color = Color(0xFFFFD700)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            onGameEnd(
                                state.score,
                                state.stars,
                                state.phase == GamePhase.LevelComplete
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.phase == GamePhase.LevelComplete) {
                                Color(0xFF44BB44)
                            } else {
                                Color(0xFF6650a4)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
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
    }
}
