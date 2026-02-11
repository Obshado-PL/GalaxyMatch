package com.candycrush.game.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.candycrush.game.ServiceLocator
import com.candycrush.game.engine.GameEngine
import com.candycrush.game.engine.GravityProcessor
import com.candycrush.game.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the main game screen.
 *
 * This is the bridge between the pure-logic GameEngine and the Compose UI.
 * It:
 * - Creates and manages the GameEngine instance
 * - Converts game events into UI state changes
 * - Handles the timing of animations using coroutine delays
 * - Saves progress when a level is completed
 *
 * The UI observes the [uiState] StateFlow and renders accordingly.
 * The ViewModel processes player input through [onSwipe].
 */
class GameViewModel(private val levelNumber: Int) : ViewModel() {

    // ===== UI State =====
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // ===== Game Engine =====
    private lateinit var engine: GameEngine
    private var currentSwapAction: SwapAction? = null

    // ===== Sound Manager =====
    private val sound = ServiceLocator.soundManager

    init {
        startLevel(levelNumber)
    }

    /**
     * Start (or restart) a level.
     */
    fun startLevel(level: Int) {
        val config = ServiceLocator.levelRepository.getLevel(level) ?: return
        engine = GameEngine(config)
        val board = engine.initializeBoard()

        _uiState.value = GameUiState(
            board = board,
            score = 0,
            movesRemaining = config.maxMoves,
            phase = GamePhase.Idle,
            levelConfig = config,
            levelNumber = level
        )
    }

    /**
     * Called when the player swipes between two positions on the board.
     * This is the main input handler.
     */
    fun onSwipe(from: Position, to: Position) {
        // Only accept input during Idle phase
        if (_uiState.value.phase != GamePhase.Idle) return

        viewModelScope.launch {
            val action = SwapAction(from, to)
            currentSwapAction = action

            // === Phase 1: Animate the swap ===
            sound.playSwap()

            _uiState.update {
                it.copy(
                    phase = GamePhase.Swapping,
                    swapAction = action,
                    swapProgress = 0f
                )
            }

            // Animate swap progress from 0 to 1
            animateSwap()

            // === Phase 2: Check if the swap produces matches ===
            val isValid = engine.processSwap(action)

            if (!isValid) {
                // Invalid swap — animate back
                _uiState.update {
                    it.copy(
                        swapAction = SwapAction(to, from), // Reverse direction
                        swapProgress = 0f
                    )
                }
                animateSwap()

                // Return to idle
                _uiState.update {
                    it.copy(
                        phase = GamePhase.Idle,
                        swapAction = null,
                        swapProgress = 0f
                    )
                }
                return@launch
            }

            // === Phase 3: Valid swap — run the cascade loop ===
            _uiState.update {
                it.copy(
                    swapAction = null,
                    swapProgress = 0f,
                    movesRemaining = engine.movesRemaining
                )
            }

            runCascadeLoop()
        }
    }

    /**
     * Animate the swap progress from 0 to 1.
     * This drives the visual interpolation of two candies sliding positions.
     * Uses ease-in-out for a smooth, natural feel.
     */
    private suspend fun animateSwap() {
        val steps = 20          // Doubled from 10 for smoother motion
        val totalDuration = 250L // milliseconds
        val stepDuration = totalDuration / steps

        for (i in 1..steps) {
            val linear = i.toFloat() / steps
            // Ease-in-out: smooth acceleration and deceleration
            // Formula: 3t² - 2t³ (Hermite smoothstep)
            val eased = linear * linear * (3f - 2f * linear)
            _uiState.update { it.copy(swapProgress = eased) }
            delay(stepDuration)
        }
    }

    /**
     * Animate a progress value from 0 to 1 and update the UI state each step.
     *
     * This is a reusable helper that uses the same manual-step approach as
     * animateSwap(). It applies an ease-out curve so animations start fast
     * and decelerate naturally (like a ball rolling to a stop).
     *
     * @param durationMs Total animation time in milliseconds
     * @param steps Number of discrete steps (more = smoother, but more state updates)
     * @param updateState Lambda that takes the current state and a progress float (0-1),
     *                    and returns a new state with that progress applied
     */
    private suspend fun animateProgress(
        durationMs: Long,
        steps: Int = 20,       // Higher default for smoother animations
        updateState: (GameUiState, Float) -> GameUiState
    ) {
        val stepDuration = durationMs / steps
        for (i in 1..steps) {
            // Ease-out: starts fast, decelerates at the end
            // Formula: 1 - (1 - t)^3  (cubic ease-out — smoother than quadratic)
            val linear = i.toFloat() / steps
            val inv = 1f - linear
            val eased = 1f - inv * inv * inv
            _uiState.update { updateState(it, eased) }
            delay(stepDuration)
        }
    }

    /**
     * Run the cascade loop until no more matches are found.
     *
     * This loop:
     * 1. Shows matched candies (highlight animation)
     * 2. Clears them and creates specials
     * 3. Applies gravity (candies fall)
     * 4. Checks for new matches (chain reaction)
     * 5. Repeats if more matches exist
     */
    private suspend fun runCascadeLoop() {
        var isFirstStep = true

        while (true) {
            // Show the matching phase (highlight matched candies)
            val matchedPositions = engine.lastMatches.flatMap { it.positions }.toSet()

            // Play match sound effect
            sound.playMatch()

            _uiState.update {
                it.copy(
                    phase = GamePhase.Matching,
                    matchedPositions = matchedPositions,
                    comboCount = engine.comboCount,
                    matchClearProgress = 0f,  // Reset clear progress for this batch
                    // IMPORTANT: Clear stale fall data from the previous cascade step!
                    // Without this, old movement entries can cause candies to jump
                    // because their IDs might match candies on the new board.
                    fallingCandies = emptyList(),
                    fallProgress = 0f
                )
            }

            // Animate matched candies: they shrink and fade out over 300ms
            // (replaces the old static delay(300))
            animateProgress(durationMs = 300) { state, progress ->
                state.copy(matchClearProgress = progress)
            }

            // Process this cascade step in the engine
            val result = engine.processCascadeStep(
                swapAction = if (isFirstStep) currentSwapAction else null
            )
            isFirstStep = false

            // Play cascade sound (pitch increases with combo level for satisfaction)
            sound.playCascade(engine.comboCount)

            // If special candies were activated, play the special sound too
            if (result.specialActivations.isNotEmpty()) {
                sound.playSpecialActivation()
            }

            // Update UI with the results: the board is already in its FINAL state
            // (engine applied gravity), but we set fallProgress=0 so BoardCanvas
            // renders candies at their OLD positions first, then animates them down.
            _uiState.update {
                it.copy(
                    board = engine.board,
                    score = engine.score,
                    phase = GamePhase.Cascading,
                    matchedPositions = emptySet(),
                    matchClearProgress = 0f,
                    comboCount = engine.comboCount,
                    lastScoreGained = result.scoreGained,
                    fallingCandies = result.gravityResult.movements,
                    fallProgress = 0f
                )
            }

            // Launch the score popup animation concurrently — it runs alongside
            // the fall animation and doesn't block the cascade loop
            if (result.scoreGained > 0) {
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(
                            scorePopupValue = result.scoreGained,
                            scorePopupProgress = 0f
                        )
                    }
                    // Float up and fade out over 600ms
                    animateProgress(durationMs = 600, steps = 24) { state, progress ->
                        state.copy(scorePopupProgress = progress)
                    }
                    // Clear the popup after it fades out
                    _uiState.update {
                        it.copy(scorePopupValue = 0, scorePopupProgress = 0f)
                    }
                }
            }

            // Launch the combo text bounce animation concurrently
            // (only shown when chain reactions are happening — comboCount > 0)
            if (engine.comboCount > 0) {
                viewModelScope.launch {
                    _uiState.update { it.copy(comboAnimProgress = 0f) }
                    // Bounce in over 400ms
                    animateProgress(durationMs = 400, steps = 20) { state, progress ->
                        state.copy(comboAnimProgress = progress)
                    }
                    // Hold the combo text briefly visible, then reset
                    delay(200)
                    _uiState.update { it.copy(comboAnimProgress = 0f) }
                }
            }

            // Animate candies falling into their new positions over 350ms
            // (replaces the old static delay(400) — feels snappier)
            animateProgress(durationMs = 350, steps = 22) { state, progress ->
                state.copy(fallProgress = progress)
            }

            // Clear the fall animation data now that candies are at final positions
            _uiState.update {
                it.copy(
                    fallingCandies = emptyList(),
                    fallProgress = 0f
                )
            }

            // If no more matches, the cascade is done
            if (!result.hasMoreMatches) break
        }

        // Cascade is complete — check if the board needs shuffling
        if (!engine.shuffleChecker.hasValidMoves(engine.board)) {
            sound.playShuffle()
            _uiState.update {
                it.copy(isShuffling = true, shuffleProgress = 0f)
            }

            // Animate the shuffle shake over 500ms — the board wobbles
            // side-to-side while the "Shuffling..." text pulses
            animateProgress(durationMs = 500, steps = 20) { state, progress ->
                state.copy(shuffleProgress = progress)
            }

            engine.shuffleChecker.shuffleBoard(engine.board)
            _uiState.update {
                it.copy(
                    board = engine.board,
                    isShuffling = false,
                    shuffleProgress = 0f
                )
            }
            delay(200) // Brief pause so new board is visible before input resumes
        }

        // Check end condition (game over or level complete)
        val endPhase = engine.evaluateEndCondition()

        if (endPhase == GamePhase.LevelComplete || endPhase == GamePhase.GameOver) {
            val stars = engine.getStarRating()

            // Play the appropriate end-of-game sound
            if (endPhase == GamePhase.LevelComplete) {
                sound.playLevelComplete()
            } else {
                sound.playGameOver()
            }

            // Save progress if the player won
            if (endPhase == GamePhase.LevelComplete) {
                viewModelScope.launch {
                    ServiceLocator.progressRepository.saveProgress(
                        levelNumber = levelNumber,
                        stars = stars,
                        score = engine.score
                    )
                }
            }

            _uiState.update {
                it.copy(
                    phase = endPhase,
                    stars = stars
                )
            }
        } else {
            // Back to idle — player can make another move
            // Clean up all animation state so nothing lingers
            _uiState.update {
                it.copy(
                    phase = GamePhase.Idle,
                    matchedPositions = emptySet(),
                    lastScoreGained = 0,
                    fallingCandies = emptyList(),
                    fallProgress = 0f,
                    matchClearProgress = 0f
                )
            }
        }
    }
}

/**
 * All the data the UI needs to render one frame of the game.
 *
 * This is an immutable snapshot — the ViewModel creates new instances
 * whenever the state changes, and Compose efficiently recomposes only
 * the parts of the UI that depend on the changed fields.
 */
data class GameUiState(
    val board: BoardState? = null,
    val score: Int = 0,
    val movesRemaining: Int = 0,
    val phase: GamePhase = GamePhase.Idle,
    val levelConfig: LevelConfig? = null,
    val levelNumber: Int = 1,
    val swapAction: SwapAction? = null,
    val swapProgress: Float = 0f,
    val matchedPositions: Set<Position> = emptySet(),
    val comboCount: Int = 0,
    val stars: Int = 0,
    val isShuffling: Boolean = false,
    val lastScoreGained: Int = 0,

    // === Animation state fields ===
    // Each animation is driven by a progress float (0.0 to 1.0).
    // The ViewModel steps these values using animateProgress(), and
    // the UI reads them each frame to interpolate visual properties.

    /** Match/clear: 0=normal, 1=fully shrunk/faded. Drives scale-down + fade-out. */
    val matchClearProgress: Float = 0f,

    /** Gravity fall: list of candy movements from GravityProcessor. */
    val fallingCandies: List<GravityProcessor.CandyMovement> = emptyList(),
    /** Gravity fall: 0=candies at old positions, 1=candies at final positions. */
    val fallProgress: Float = 0f,

    /** Score popup: the score value to show (e.g. 150). 0 = no popup visible. */
    val scorePopupValue: Int = 0,
    /** Score popup: 0=just appeared, 1=fully faded out and floated away. */
    val scorePopupProgress: Float = 0f,

    /** Combo text bounce: 0=start of bounce, 1=settled at normal size. */
    val comboAnimProgress: Float = 0f,

    /** Shuffle shake: 0=start of shake, 1=shake complete. Drives board wobble. */
    val shuffleProgress: Float = 0f
)
