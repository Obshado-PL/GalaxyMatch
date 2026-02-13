package com.galaxymatch.game.ui.game

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxymatch.game.ServiceLocator
import com.galaxymatch.game.engine.GameEngine
import com.galaxymatch.game.engine.GravityProcessor
import com.galaxymatch.game.model.*
import com.galaxymatch.game.model.PowerUpType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
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

    // ===== Sound & Haptic Managers =====
    private val sound = ServiceLocator.soundManager
    private val haptic = ServiceLocator.hapticManager

    // ===== Hint System =====
    /** Coroutine job for the idle hint timer. Cancelled on any player input. */
    private var hintTimerJob: Job? = null

    // ===== Undo System =====
    /** Snapshot of the board/score/moves before the player's last valid swap. */
    private var undoSnapshot: UndoSnapshot? = null

    // ===== Settings & Statistics =====
    private val settingsRepo = ServiceLocator.settingsRepository
    private val statsRepo = ServiceLocator.statisticsRepository

    /** Accumulates statistics during a single level attempt. Reset on level start. */
    private var gemsMatchedThisGame = 0
    private var specialGemsThisGame = 0
    private var powerUpsThisGame = 0
    private val gemColorCountsThisGame = mutableMapOf<GemType, Int>()

    // ===== Special Mode Detection =====
    /** True if this is a daily challenge (levelNumber == -1) */
    private val isDailyChallenge = levelNumber == -1
    /** True if this is a timed challenge (levelNumber <= -100) */
    private val isTimedMode = levelNumber <= -100
    /** Timed mode difficulty (extracted from sentinel value) */
    private val timedDifficulty: TimedDifficulty? =
        if (isTimedMode) TimedDifficulty.entries.getOrNull(-(levelNumber + 100)) else null
    /** Coroutine job for the timed mode countdown timer */
    private var timerJob: Job? = null

    init {
        loadLevelPreview(levelNumber)
        loadSettings()
    }

    /** Load persisted settings and apply them to managers / UI state. */
    private fun loadSettings() {
        viewModelScope.launch {
            val settings = settingsRepo.getSettings().first()
            haptic.isHapticMuted = settings.hapticMuted
            _uiState.update {
                it.copy(
                    colorblindMode = settings.colorblindMode,
                    highContrastMode = settings.highContrastMode
                )
            }
        }
    }

    /** Persist accumulated statistics for this level attempt. */
    private fun saveStatistics() {
        viewModelScope.launch {
            statsRepo.incrementStats(
                gemsMatched = gemsMatchedThisGame,
                comboReached = _uiState.value.maxComboReached,
                scoreGained = _uiState.value.score,
                gemColorCounts = gemColorCountsThisGame.toMap(),
                specialGemsCreated = specialGemsThisGame,
                powerUpsUsed = powerUpsThisGame
            )
        }
    }

    /**
     * Load level config and show the pre-level dialog.
     *
     * This is the first step of level initialization. It loads the level
     * configuration (needed to display the objective in the dialog) but
     * does NOT create the game engine or board yet. The board appears
     * only after the player taps "Play!" in the pre-level dialog.
     */
    private fun loadLevelPreview(level: Int) {
        val config = getConfigForLevel(level) ?: return
        _uiState.value = GameUiState(
            levelConfig = config,
            levelNumber = level,
            showPreLevelDialog = true,  // Show dialog — board stays null
            isTimedMode = isTimedMode,
            isDailyChallenge = isDailyChallenge
        )
    }

    /**
     * Get the level config, handling special sentinel values:
     * -1 = daily challenge, <= -100 = timed challenge
     */
    private fun getConfigForLevel(level: Int): LevelConfig? {
        return when {
            isDailyChallenge -> {
                ServiceLocator.dailyChallengeRepository.generateTodayLevel()
            }
            isTimedMode -> {
                val diff = timedDifficulty ?: return null
                LevelConfig(
                    levelNumber = level,
                    rows = 8, cols = 8,
                    maxMoves = Int.MAX_VALUE,  // Unlimited moves in timed mode
                    targetScore = Int.MAX_VALUE,
                    twoStarScore = Int.MAX_VALUE,
                    threeStarScore = Int.MAX_VALUE,
                    availableGemTypes = 5,
                    description = "Timed Challenge — ${diff.label} (${diff.seconds}s)"
                )
            }
            else -> ServiceLocator.levelRepository.getLevel(level)
        }
    }

    /**
     * Dismiss the pre-level dialog and start the level.
     * Called when the player taps "Play!" in the dialog.
     */
    fun dismissPreLevelDialog() {
        _uiState.update { it.copy(showPreLevelDialog = false) }
        startLevel(_uiState.value.levelNumber)
    }

    /**
     * Start (or restart) a level.
     */
    private fun startLevel(level: Int) {
        val config = getConfigForLevel(level) ?: return
        engine = GameEngine(config)
        val board = engine.initializeBoard()

        // Reset undo state for the new level
        undoSnapshot = null

        // Reset statistics accumulators
        gemsMatchedThisGame = 0
        specialGemsThisGame = 0
        powerUpsThisGame = 0
        gemColorCountsThisGame.clear()

        _uiState.value = GameUiState(
            board = board,
            score = 0,
            movesRemaining = config.maxMoves,
            phase = GamePhase.Idle,
            levelConfig = config,
            levelNumber = level,
            boardEntryProgress = 0f,  // Start with gems above the board
            // === Initialize objective tracking ===
            objectiveType = config.objective,
            iceBroken = 0,
            totalIce = config.obstacles.count {
                it.value == ObstacleType.Ice || it.value == ObstacleType.ReinforcedIce
            },
            gemsCleared = 0,
            targetGemCount = when (config.objective) {
                is ObjectiveType.ClearGemType -> config.objective.targetCount
                else -> 0
            },
            targetGemType = when (config.objective) {
                is ObjectiveType.ClearGemType -> config.objective.gemType
                else -> null
            },
            objectiveComplete = false,
            isTimedMode = isTimedMode,
            isDailyChallenge = isDailyChallenge,
            timeRemaining = timedDifficulty?.seconds ?: 0
        )

        // Animate gems dropping in from above over 800ms
        viewModelScope.launch {
            animateProgress(durationMs = 800, steps = 24) { state, progress ->
                state.copy(boardEntryProgress = progress)
            }
            // Snap to exactly 1f when done (ensure no floating-point imprecision)
            _uiState.update { it.copy(boardEntryProgress = 1f) }

            // Start countdown timer for timed mode
            if (isTimedMode && timedDifficulty != null) {
                startTimedCountdown()
            }
        }

        // Load the player's available stars for power-ups
        viewModelScope.launch {
            val progress = ServiceLocator.progressRepository.getProgress().first()
            _uiState.update { it.copy(availableStars = progress.availableStars) }
        }

        // Start the hint timer — if the player is idle for 5 seconds,
        // we'll highlight a valid move to help them out
        startHintTimer()

        // === Feature 4: Interactive Tutorial ===
        // Show the interactive tutorial on level 1 if the player hasn't seen it yet.
        // Finds a valid move and highlights those two gems for the player to swap.
        if (level == 1) {
            viewModelScope.launch {
                val settings = settingsRepo.getSettings().first()
                if (!settings.tutorialSeen) {
                    startInteractiveTutorial()
                }
            }
        }
    }

    /**
     * Called when the player swipes between two positions on the board.
     * This is the main input handler.
     */
    fun onSwipe(from: Position, to: Position) {
        // Only accept input during Idle phase
        if (_uiState.value.phase != GamePhase.Idle) return
        // Block input while gems are still dropping in
        if (_uiState.value.boardEntryProgress < 1f) return
        // Block swipes when a power-up is in target selection mode
        if (_uiState.value.activePowerUp != null) return
        // During tutorial: route through tutorial handler.
        // Steps 2-3 block all swipes; step 1 only accepts the highlighted swap.
        if (_uiState.value.showTutorial) {
            if (_uiState.value.tutorialStep > 1) return // Block swipes on info steps
            if (onTutorialSwipe(from, to)) return // Tutorial consumed the swipe
        }

        // Cancel any active hint — player is interacting with the board
        hintTimerJob?.cancel()
        _uiState.update { it.copy(hintPositions = emptySet(), hintAnimProgress = 0f) }

        viewModelScope.launch {
            val action = SwapAction(from, to)
            currentSwapAction = action

            // === Undo: Capture snapshot before the swap ===
            // Only capture if undo hasn't been used this level (one undo per level)
            if (!_uiState.value.undoUsedThisLevel) {
                undoSnapshot = UndoSnapshot(
                    board = engine.board.deepCopy(),
                    score = engine.score,
                    movesRemaining = engine.movesRemaining,
                    iceBroken = engine.objectiveTracker.iceBroken,
                    gemsCleared = engine.objectiveTracker.gemsCleared
                )
            }

            // === Phase 1: Animate the swap ===
            sound.playSwap()
            haptic.vibrateSwap()

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
                // Clear the snapshot since no valid move was made
                undoSnapshot = null

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
            // Mark undo as available since we have a valid snapshot
            _uiState.update {
                it.copy(
                    swapAction = null,
                    swapProgress = 0f,
                    movesRemaining = engine.movesRemaining,
                    undoAvailable = undoSnapshot != null && !it.undoUsedThisLevel
                )
            }

            runCascadeLoop()
        }
    }

    /**
     * Animate the swap progress from 0 to 1.
     * This drives the visual interpolation of two gems sliding positions.
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
        steps: Int = 20,       // Ignored — kept for API compatibility, now frame-synced
        updateState: (GameUiState, Float) -> GameUiState
    ) {
        // Use time-based animation synced to ~60fps for smooth motion.
        // Previous approach used delay(stepDuration) with discrete steps
        // which caused choppy animation (~30fps). Now we update every frame
        // (~16ms) and calculate progress from elapsed wall-clock time.
        val durationNanos = durationMs * 1_000_000L
        val startTime = System.nanoTime()

        while (true) {
            val elapsed = System.nanoTime() - startTime
            val linear = (elapsed.toFloat() / durationNanos).coerceAtMost(1f)

            // Ease-out: starts fast, decelerates at the end
            // Formula: 1 - (1 - t)^3  (cubic ease-out — smoother than quadratic)
            val inv = 1f - linear
            val eased = 1f - inv * inv * inv

            _uiState.update { updateState(it, eased) }

            if (linear >= 1f) break

            // Yield to other coroutines and wait ~1 frame (~16ms)
            delay(16)
        }
    }

    /**
     * Run the cascade loop until no more matches are found.
     *
     * This loop:
     * 1. Shows matched gems (highlight animation)
     * 2. Clears them and creates specials
     * 3. Applies gravity (gems fall)
     * 4. Checks for new matches (chain reaction)
     * 5. Repeats if more matches exist
     */
    private suspend fun runCascadeLoop() {
        var isFirstStep = true

        while (true) {
            // Show the matching phase (highlight matched gems)
            val matchedPositions = engine.lastMatches.flatMap { it.positions }.toSet()

            // Play match sound — pitch scales with the largest match size
            // (match-4 and match-5+ get progressively higher, more exciting pitches)
            val largestMatchSize = engine.lastMatches.maxOfOrNull { it.positions.size } ?: 3
            sound.playMatch(largestMatchSize)
            haptic.vibrateMatch(largestMatchSize)

            // Track statistics: count matched gems by color
            for (match in engine.lastMatches) {
                gemsMatchedThisGame += match.positions.size
                gemColorCountsThisGame[match.gemType] =
                    (gemColorCountsThisGame[match.gemType] ?: 0) + match.positions.size
            }

            _uiState.update {
                it.copy(
                    phase = GamePhase.Matching,
                    matchedPositions = matchedPositions,
                    comboCount = engine.comboCount,
                    matchClearProgress = 0f,  // Reset clear progress for this batch
                    // IMPORTANT: Clear stale fall data from the previous cascade step!
                    // Without this, old movement entries can cause gems to jump
                    // because their IDs might match gems on the new board.
                    fallingGems = emptyList(),
                    fallProgress = 0f
                )
            }

            // Animate matched gems: they shrink and fade out over 300ms
            // (replaces the old static delay(300))
            animateProgress(durationMs = 300) { state, progress ->
                state.copy(matchClearProgress = progress)
            }

            // Track ice count before processing so we can detect ice breaks
            val iceCountBefore = engine.objectiveTracker.iceBroken

            // Process this cascade step in the engine
            val result = engine.processCascadeStep(
                swapAction = if (isFirstStep) currentSwapAction else null
            )
            isFirstStep = false

            // Play cascade sound (pitch + volume escalate with combo level)
            sound.playCascade(engine.comboCount)
            haptic.vibrateCascade(engine.comboCount)

            // If special gems were activated, play the special sound too
            if (result.specialActivations.isNotEmpty()) {
                sound.playSpecialActivation()
                haptic.vibrateSpecialActivation()
                specialGemsThisGame += result.specialActivations.size
            }

            // Play ice break sound if any ice was shattered this step
            val iceCountAfter = engine.objectiveTracker.iceBroken
            if (iceCountAfter > iceCountBefore) {
                sound.playIceBreak()
                haptic.vibrateIceBreak()
            }

            // === Feature 3: Star Progress ===
            // Check if the new score earned a new star
            val newStars = engine.getStarRating()
            val oldStars = _uiState.value.currentStars

            // Update UI with the results: the board is already in its FINAL state
            // (engine applied gravity), but we set fallProgress=0 so BoardCanvas
            // renders gems at their OLD positions first, then animates them down.
            _uiState.update {
                it.copy(
                    board = engine.board,
                    score = engine.score,
                    phase = GamePhase.Cascading,
                    matchedPositions = emptySet(),
                    matchClearProgress = 0f,
                    comboCount = engine.comboCount,
                    lastScoreGained = result.scoreGained,
                    fallingGems = result.gravityResult.movements,
                    fallProgress = 0f,
                    // Feature 1: Track the highest combo reached this level
                    maxComboReached = maxOf(it.maxComboReached, engine.comboCount),
                    // Feature 3: Update live star count
                    currentStars = newStars,
                    // === Sync objective progress from engine tracker ===
                    iceBroken = engine.objectiveTracker.iceBroken,
                    gemsCleared = engine.objectiveTracker.gemsCleared,
                    objectiveComplete = engine.objectiveTracker.isObjectiveMet(
                        engine.score, engine.board.obstacles
                    )
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

            // === Feature 1: Screen shake for big combos (3+ cascades) ===
            // Adds a satisfying physical shake to the board when chains get long.
            // Runs concurrently — doesn't block the fall animation.
            if (engine.comboCount >= 3) {
                viewModelScope.launch {
                    _uiState.update { it.copy(screenShakeProgress = 0f) }
                    // Quick, punchy shake over 300ms
                    animateProgress(durationMs = 300, steps = 15) { state, progress ->
                        state.copy(screenShakeProgress = progress)
                    }
                    _uiState.update { it.copy(screenShakeProgress = 0f) }
                }
            }

            // === Feature 3: Star unlock animation ===
            // When a new star is earned mid-game, play a celebratory scale animation
            // with a rising-pitch chime for each star (1→3 = low→high)
            if (newStars > oldStars) {
                sound.playStarEarned(newStars)
                haptic.vibrateStarEarned()
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(starJustUnlocked = newStars, starUnlockAnimProgress = 0f)
                    }
                    // Star scales up with overshoot then settles over 500ms
                    animateProgress(durationMs = 500, steps = 20) { state, progress ->
                        state.copy(starUnlockAnimProgress = progress)
                    }
                    // Hold briefly, then clear
                    delay(300)
                    _uiState.update {
                        it.copy(starJustUnlocked = 0, starUnlockAnimProgress = 0f)
                    }
                }
            }

            // Animate gems falling into their new positions over 350ms
            // (replaces the old static delay(400) — feels snappier)
            animateProgress(durationMs = 350, steps = 22) { state, progress ->
                state.copy(fallProgress = progress)
            }

            // Clear the fall animation data now that gems are at final positions
            _uiState.update {
                it.copy(
                    fallingGems = emptyList(),
                    fallProgress = 0f
                )
            }

            // === Timed mode: award time bonus for combos ===
            if (isTimedMode && engine.comboCount > 0) {
                awardTimeBonus(engine.comboCount)
            }

            // If no more matches, the cascade is done
            if (!result.hasMoreMatches) break
        }

        // Cascade is complete — check if the board needs shuffling
        if (!engine.shuffleChecker.hasValidMoves(engine.board)) {
            sound.playShuffle()
            haptic.vibrateShuffle()
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

        // Check end condition (game over, level complete, or bonus moves)
        val endPhase = engine.evaluateEndCondition()

        when (endPhase) {
            GamePhase.BonusMoves -> {
                // Non-score objective completed with remaining moves!
                // Trigger the bonus moves loop where each leftover move
                // auto-destroys a random gem for bonus points.
                runBonusMoveLoop()
            }

            GamePhase.LevelComplete, GamePhase.GameOver -> {
                val stars = engine.getStarRating()

                // Play the appropriate end-of-game sound + haptic
                if (endPhase == GamePhase.LevelComplete) {
                    sound.playLevelComplete()
                    haptic.vibrateLevelComplete()
                } else {
                    sound.playGameOver()
                    haptic.vibrateGameOver()
                }

                // --- Determine if this is a new high score BEFORE saving ---
                // We must compare BEFORE saveProgress() overwrites the stored value.
                val newHighScore = if (endPhase == GamePhase.LevelComplete) {
                    val previousBest = try {
                        val progress = ServiceLocator.progressRepository.getProgress().first()
                        progress.levelScores[levelNumber] ?: 0
                    } catch (_: Exception) { 0 }
                    engine.score > previousBest && previousBest >= 0
                } else false

                // Save progress if the player won (skip for special modes)
                if (endPhase == GamePhase.LevelComplete && !isDailyChallenge && !isTimedMode) {
                    viewModelScope.launch {
                        ServiceLocator.progressRepository.saveProgress(
                            levelNumber = levelNumber,
                            stars = stars,
                            score = engine.score
                        )
                    }
                }

                // Daily challenge: mark completed
                if (isDailyChallenge && endPhase == GamePhase.LevelComplete) {
                    viewModelScope.launch {
                        ServiceLocator.dailyChallengeRepository.markCompleted(engine.score)
                    }
                }

                // Timed mode: save best score
                if (isTimedMode) {
                    timerJob?.cancel()
                    val diff = timedDifficulty
                    if (diff != null) {
                        viewModelScope.launch {
                            ServiceLocator.timedChallengeRepository.saveBestScore(diff, engine.score)
                        }
                    }
                }

                // Save statistics (win or lose)
                saveStatistics()

                // Check achievements
                checkAchievements()

                _uiState.update {
                    it.copy(
                        phase = endPhase,
                        stars = stars,
                        isNewHighScore = newHighScore
                    )
                }
            }

            else -> {
                // Back to idle — player can make another move
                // Clean up all animation state so nothing lingers
                _uiState.update {
                    it.copy(
                        phase = GamePhase.Idle,
                        matchedPositions = emptySet(),
                        lastScoreGained = 0,
                        fallingGems = emptyList(),
                        fallProgress = 0f,
                        matchClearProgress = 0f
                    )
                }

                // Restart the hint timer — if the player sits idle for 5 seconds,
                // we'll show them a valid move
                startHintTimer()
            }
        }
    }

    // ===== Bonus Moves System =====
    // When a non-score objective (BreakAllIce, ClearGemType) is completed
    // with remaining moves, each leftover move auto-destroys a random gem
    // for bonus points. This is the classic "level complete fireworks" moment.

    /**
     * Run the bonus moves loop: each remaining move destroys a random gem.
     *
     * Mirrors the cascade loop pattern:
     * - For each move: pause → destroy gem → gravity → cascade if needed
     * - Score popups show points earned per bonus move
     * - After all moves consumed, transitions to LevelComplete
     */
    private suspend fun runBonusMoveLoop() {
        // Brief pause before bonus moves start (dramatic buildup)
        delay(500)

        while (engine.movesRemaining > 0) {
            // Update UI to show bonus move phase and remaining count
            _uiState.update {
                it.copy(
                    bonusMoveActive = true,
                    bonusMovesRemaining = engine.movesRemaining
                )
            }

            // Pause between bonus moves for dramatic effect
            delay(400)

            // Play the special activation sound + haptic for each bonus move
            sound.playSpecialActivation()
            haptic.vibrateSpecialActivation()

            // Perform the bonus move in the engine
            val success = engine.performBonusMove()
            if (!success) break  // Safety: no gems left or no moves

            // Sync board + score + moves to UI
            _uiState.update {
                it.copy(
                    board = engine.board,
                    score = engine.score,
                    movesRemaining = engine.movesRemaining,
                    bonusMovesRemaining = engine.movesRemaining
                )
            }

            // Score popup (runs concurrently while gravity animates)
            val scoreGained = if (engine.lastSpecialActivations.isNotEmpty()) {
                engine.lastSpecialActivations.size * 80
            } else {
                50
            }
            viewModelScope.launch {
                _uiState.update {
                    it.copy(scorePopupValue = scoreGained, scorePopupProgress = 0f)
                }
                animateProgress(durationMs = 600, steps = 24) { state, progress ->
                    state.copy(scorePopupProgress = progress)
                }
                _uiState.update {
                    it.copy(scorePopupValue = 0, scorePopupProgress = 0f)
                }
            }

            // Animate gravity fall (gems dropping into empty spaces)
            val gravityResult = engine.lastGravityResult
            if (gravityResult != null && gravityResult.movements.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        fallingGems = gravityResult.movements,
                        fallProgress = 0f
                    )
                }
                animateProgress(durationMs = 350, steps = 22) { state, progress ->
                    state.copy(fallProgress = progress)
                }
                _uiState.update {
                    it.copy(fallingGems = emptyList(), fallProgress = 0f)
                }
            }

            // If the bonus move triggered matches, run the full cascade loop
            if (engine.lastMatches.isNotEmpty()) {
                runCascadeLoop()
            }

            // Update star rating (bonus moves can push score to new star thresholds)
            _uiState.update { it.copy(currentStars = engine.getStarRating()) }
        }

        // All bonus moves consumed — level complete!
        val stars = engine.getStarRating()
        sound.playLevelComplete()
        haptic.vibrateLevelComplete()

        // --- Determine if this is a new high score BEFORE saving ---
        val newHighScore = run {
            val previousBest = try {
                val progress = ServiceLocator.progressRepository.getProgress().first()
                progress.levelScores[levelNumber] ?: 0
            } catch (_: Exception) { 0 }
            engine.score > previousBest && previousBest >= 0
        }

        // Save progress (skip for special modes — they use negative sentinel level numbers)
        if (!isDailyChallenge && !isTimedMode) {
            viewModelScope.launch {
                ServiceLocator.progressRepository.saveProgress(
                    levelNumber = levelNumber,
                    stars = stars,
                    score = engine.score
                )
            }
        }

        // Daily challenge: mark completed so streak is tracked
        if (isDailyChallenge) {
            viewModelScope.launch {
                ServiceLocator.dailyChallengeRepository.markCompleted(engine.score)
            }
        }

        // Timed mode: save best score and stop countdown timer
        if (isTimedMode) {
            timerJob?.cancel()
            val diff = timedDifficulty
            if (diff != null) {
                viewModelScope.launch {
                    ServiceLocator.timedChallengeRepository.saveBestScore(diff, engine.score)
                }
            }
        }

        // Save statistics (win or lose, all modes)
        saveStatistics()

        // Check achievements (all modes)
        checkAchievements()

        _uiState.update {
            it.copy(
                bonusMoveActive = false,
                bonusMovesRemaining = 0,
                phase = GamePhase.LevelComplete,
                stars = stars,
                movesRemaining = 0,
                isNewHighScore = newHighScore
            )
        }
    }

    // ===== Feature 2: Restart =====

    /** Show the restart confirmation dialog. */
    fun onRestartClicked() {
        _uiState.update { it.copy(showRestartDialog = true) }
    }

    /** Dismiss the restart dialog without restarting. */
    fun onRestartDismissed() {
        _uiState.update { it.copy(showRestartDialog = false) }
    }

    /**
     * Restart the current level from scratch.
     * Cancels the hint timer, clears undo state, and reinitializes the board.
     */
    fun restartLevel() {
        hintTimerJob?.cancel()
        timerJob?.cancel()
        undoSnapshot = null
        _uiState.update { it.copy(showRestartDialog = false) }
        startLevel(_uiState.value.levelNumber)
    }

    // ===== Feature 3: Undo =====

    /**
     * Undo the last move.
     *
     * Restores the board, score, and moves from the snapshot taken before
     * the last valid swap. Can only be used once per level.
     */
    fun onUndo() {
        val snapshot = undoSnapshot ?: return
        if (_uiState.value.phase != GamePhase.Idle) return
        if (_uiState.value.undoUsedThisLevel) return

        // Cancel hint timer — we're changing the board
        hintTimerJob?.cancel()

        // Restore engine state from the snapshot (including objective counters)
        engine.restoreState(
            snapshot.board, snapshot.score, snapshot.movesRemaining,
            snapshot.iceBroken, snapshot.gemsCleared
        )

        // Update UI and mark undo as used
        _uiState.update {
            it.copy(
                board = engine.board,
                score = engine.score,
                movesRemaining = engine.movesRemaining,
                phase = GamePhase.Idle,
                undoAvailable = false,
                undoUsedThisLevel = true,
                // Recalculate stars for the restored score
                currentStars = engine.getStarRating(),
                // Restore objective progress
                iceBroken = engine.objectiveTracker.iceBroken,
                gemsCleared = engine.objectiveTracker.gemsCleared,
                objectiveComplete = engine.objectiveTracker.isObjectiveMet(
                    engine.score, engine.board.obstacles
                )
            )
        }

        // Clear the snapshot — can't undo again
        undoSnapshot = null

        // Restart hint timer for the restored board
        startHintTimer()
    }

    // ===== Feature 4: Interactive Tutorial =====

    /**
     * Start the interactive tutorial (level 1, first play only).
     *
     * Finds a valid swap using ShuffleChecker, highlights the two gems,
     * and enters tutorial step 1 (guiding the player to make that swap).
     */
    private fun startInteractiveTutorial() {
        val shuffleChecker = com.galaxymatch.game.engine.ShuffleChecker(
            com.galaxymatch.game.engine.MatchDetector()
        )
        val validMove = shuffleChecker.findValidMove(engine.board)

        if (validMove != null) {
            // Found a valid move — highlight those two gems and enter step 1
            _uiState.update {
                it.copy(
                    showTutorial = true,
                    tutorialStep = 1,
                    tutorialHighlightPositions = setOf(validMove.from, validMove.to),
                    tutorialSwapFrom = validMove.from,
                    tutorialSwapTo = validMove.to
                )
            }
        } else {
            // Fallback: no valid move found (extremely unlikely on level 1).
            // Show a text-only tutorial instead.
            _uiState.update {
                it.copy(showTutorial = true, tutorialStep = 1)
            }
        }
    }

    /**
     * Handle a swipe during the tutorial.
     *
     * During step 1, only accepts the highlighted swap. Ignores all other swipes.
     * After the correct swap, processes it normally and advances to step 2.
     *
     * @return true if the swipe was handled by the tutorial, false to pass through
     */
    fun onTutorialSwipe(from: Position, to: Position): Boolean {
        val state = _uiState.value
        if (!state.showTutorial || state.tutorialStep != 1) return false

        val expectedFrom = state.tutorialSwapFrom
        val expectedTo = state.tutorialSwapTo
        if (expectedFrom == null || expectedTo == null) return false

        // Check if the swipe matches the expected swap (either direction)
        val isCorrectSwap = (from == expectedFrom && to == expectedTo) ||
                (from == expectedTo && to == expectedFrom)

        if (!isCorrectSwap) {
            // Wrong swap — ignore silently (tutorial highlights stay)
            return true
        }

        // Correct swap! Clear highlights and advance to step 2 after the swap processes
        _uiState.update {
            it.copy(
                tutorialStep = 2,
                tutorialHighlightPositions = emptySet(),
                tutorialSwapFrom = null,
                tutorialSwapTo = null
            )
        }

        // Let the normal onSwipe logic handle the actual swap
        return false
    }

    /**
     * Advance the tutorial to the next step when the player taps "continue".
     * Steps: 1 (swap) → 2 (match result) → 3 (specials info) → done.
     */
    fun onTutorialContinue() {
        val currentStep = _uiState.value.tutorialStep
        if (currentStep >= 3) {
            // Tutorial complete — dismiss and save
            dismissTutorial()
        } else {
            _uiState.update { it.copy(tutorialStep = currentStep + 1) }
        }
    }

    /**
     * Skip the entire tutorial (player taps "Skip Tutorial").
     */
    fun dismissTutorial() {
        _uiState.update {
            it.copy(
                showTutorial = false,
                tutorialStep = 0,
                tutorialHighlightPositions = emptySet(),
                tutorialSwapFrom = null,
                tutorialSwapTo = null
            )
        }
        viewModelScope.launch {
            settingsRepo.saveTutorialSeen(true)
        }
    }

    // ===== Power-Ups / Boosters =====

    /**
     * Called when the player taps a power-up button.
     *
     * For power-ups that need a target (Hammer, Color Bomb):
     * - Enters "target selection" mode — the board waits for a tap instead of a swipe
     * - The selected power-up is stored in `activePowerUp`
     *
     * For power-ups that don't need a target (Extra Moves):
     * - Executes immediately and deducts stars
     */
    fun onPowerUpSelected(type: PowerUpType) {
        val state = _uiState.value
        if (state.phase != GamePhase.Idle) return
        if (state.boardEntryProgress < 1f) return
        if (state.availableStars < type.starCost) return

        // Cancel any active hint — player is interacting
        hintTimerJob?.cancel()
        _uiState.update { it.copy(hintPositions = emptySet(), hintAnimProgress = 0f) }

        if (type.needsTarget) {
            // Enter target selection mode — board will listen for taps
            _uiState.update { it.copy(activePowerUp = type) }
        } else {
            // Extra Moves: execute immediately, no target needed
            executePowerUp(type, position = null)
        }
    }

    /**
     * Called when the player taps a gem while in power-up target selection mode.
     *
     * This is triggered by BoardCanvas when `activePowerUp != null`.
     * The tapped position is used as the target for the active power-up.
     */
    fun onBoardTapForPowerUp(position: Position) {
        val type = _uiState.value.activePowerUp ?: return
        executePowerUp(type, position)
    }

    /**
     * Cancel the current power-up target selection.
     * Returns the board to normal swipe mode without spending any stars.
     */
    fun cancelPowerUp() {
        _uiState.update { it.copy(activePowerUp = null) }
        startHintTimer()
    }

    /**
     * Execute a power-up: run the engine method, deduct stars, and handle cascades.
     *
     * @param type Which power-up to use
     * @param position Target position (null for ExtraMoves which doesn't need one)
     */
    private fun executePowerUp(type: PowerUpType, position: Position?) {
        viewModelScope.launch {
            // Clear the active power-up mode
            _uiState.update { it.copy(activePowerUp = null) }

            // Execute the power-up in the engine
            val success = when (type) {
                PowerUpType.Hammer -> {
                    if (position == null) return@launch
                    sound.playSpecialActivation()
                    haptic.vibrateSpecialActivation()
                    // Trigger hammer impact flash VFX at target position
                    _uiState.update { it.copy(
                        hammerFlashPosition = position,
                        hammerFlashProgress = 0f
                    ) }
                    engine.useHammer(position)
                }
                PowerUpType.ColorBomb -> {
                    if (position == null) return@launch
                    sound.playSpecialActivation()
                    haptic.vibrateSpecialActivation()
                    // Get the target gem's color for the screen tint flash
                    val targetGem = engine.board.gemAt(position)
                    val tintColor = targetGem?.type?.color
                    _uiState.update { it.copy(
                        colorBombFlashColor = tintColor,
                        colorBombFlashProgress = 0f
                    ) }
                    engine.useColorBomb(position)
                }
                PowerUpType.ExtraMoves -> {
                    engine.useExtraMoves()
                }
            }

            if (!success) {
                // Power-up failed (e.g., tapped an empty cell) — don't charge stars
                startHintTimer()
                return@launch
            }

            // Track power-up usage for statistics
            powerUpsThisGame++

            // Deduct stars and persist the spend
            val progressRepo = ServiceLocator.progressRepository
            progressRepo.spendStars(type.starCost)

            // Refresh available stars from persistence
            val updatedProgress = progressRepo.getProgress().first()
            _uiState.update { it.copy(availableStars = updatedProgress.availableStars) }

            // Handle the result based on what the engine did
            when (type) {
                PowerUpType.ExtraMoves -> {
                    // Trigger "+5 Moves!" floating text animation
                    _uiState.update {
                        it.copy(
                            movesRemaining = engine.movesRemaining,
                            extraMovesAnimActive = true,
                            extraMovesAnimProgress = 0f
                        )
                    }
                    // Animate the floating text: 0→1 over 800ms
                    animateProgress(durationMs = 800, steps = 48) { state, progress ->
                        state.copy(extraMovesAnimProgress = progress)
                    }
                    _uiState.update { it.copy(extraMovesAnimActive = false) }
                    startHintTimer()
                }
                PowerUpType.Hammer -> {
                    // Animate hammer impact flash: expanding circle over 200ms
                    animateProgress(durationMs = 200, steps = 12) { state, progress ->
                        state.copy(hammerFlashProgress = progress)
                    }
                    // Brief dramatic pause before gravity
                    kotlinx.coroutines.delay(100)
                    _uiState.update { it.copy(hammerFlashPosition = null, hammerFlashProgress = 0f) }

                    // Animate gravity fall, then run cascade if needed
                    val gravityResult = engine.lastGravityResult
                    _uiState.update {
                        it.copy(
                            board = engine.board,
                            score = engine.score,
                            phase = GamePhase.Cascading,
                            fallingGems = gravityResult?.movements ?: emptyList(),
                            fallProgress = 0f,
                            currentStars = engine.getStarRating()
                        )
                    }

                    animateProgress(durationMs = 350, steps = 22) { state, progress ->
                        state.copy(fallProgress = progress)
                    }
                    _uiState.update {
                        it.copy(fallingGems = emptyList(), fallProgress = 0f)
                    }

                    if (engine.lastMatches.isNotEmpty()) {
                        runCascadeLoop()
                    } else {
                        val endPhase = engine.evaluateEndCondition()
                        handlePostPowerUpEndCondition(endPhase)
                    }
                    return@launch
                }
                PowerUpType.ColorBomb -> {
                    // Animate color bomb tint flash: screen overlay over 300ms
                    animateProgress(durationMs = 300, steps = 18) { state, progress ->
                        state.copy(colorBombFlashProgress = progress)
                    }
                    _uiState.update { it.copy(colorBombFlashColor = null, colorBombFlashProgress = 0f) }

                    // Animate gravity fall, then run cascade if needed
                    val gravityResult = engine.lastGravityResult
                    _uiState.update {
                        it.copy(
                            board = engine.board,
                            score = engine.score,
                            phase = GamePhase.Cascading,
                            fallingGems = gravityResult?.movements ?: emptyList(),
                            fallProgress = 0f,
                            currentStars = engine.getStarRating()
                        )
                    }

                    // Animate the gravity fall
                    animateProgress(durationMs = 350, steps = 22) { state, progress ->
                        state.copy(fallProgress = progress)
                    }
                    _uiState.update {
                        it.copy(fallingGems = emptyList(), fallProgress = 0f)
                    }

                    if (engine.lastMatches.isNotEmpty()) {
                        runCascadeLoop()
                    } else {
                        val endPhase = engine.evaluateEndCondition()
                        handlePostPowerUpEndCondition(endPhase)
                    }
                }
            }
        }
    }

    /**
     * Handle end-condition checking after a power-up finishes.
     *
     * Shared by both Hammer and ColorBomb power-up paths.
     * Checks for bonus moves, level complete, game over, or returns to idle.
     */
    private suspend fun handlePostPowerUpEndCondition(endPhase: GamePhase) {
        if (endPhase == GamePhase.BonusMoves) {
            runBonusMoveLoop()
        } else if (endPhase == GamePhase.LevelComplete || endPhase == GamePhase.GameOver) {
            val stars = engine.getStarRating()
            if (endPhase == GamePhase.LevelComplete) {
                sound.playLevelComplete()
                haptic.vibrateLevelComplete()
                if (!isDailyChallenge && !isTimedMode) {
                    ServiceLocator.progressRepository.saveProgress(
                        levelNumber = levelNumber,
                        stars = stars,
                        score = engine.score
                    )
                }
                if (isDailyChallenge) {
                    ServiceLocator.dailyChallengeRepository.markCompleted(engine.score)
                }
                if (isTimedMode) {
                    timerJob?.cancel()
                    val diff = timedDifficulty
                    if (diff != null) {
                        ServiceLocator.timedChallengeRepository.saveBestScore(diff, engine.score)
                    }
                }
            } else {
                sound.playGameOver()
                haptic.vibrateGameOver()
            }
            saveStatistics()
            checkAchievements()
            _uiState.update {
                it.copy(phase = endPhase, stars = stars)
            }
        } else {
            _uiState.update {
                it.copy(
                    phase = GamePhase.Idle,
                    matchedPositions = emptySet(),
                    lastScoreGained = 0
                )
            }
            startHintTimer()
        }
    }

    /**
     * Start the idle hint timer.
     *
     * If the player doesn't swipe for 5 seconds while the game is in the Idle
     * phase, we highlight a valid move by showing a pulsing glow on the two
     * gems that can be swapped. The hint resets whenever the player interacts
     * with the board (cancelled in onSwipe()).
     */
    private fun startHintTimer() {
        // Cancel any existing timer first
        hintTimerJob?.cancel()

        // Clear any currently showing hint
        _uiState.update { it.copy(hintPositions = emptySet(), hintAnimProgress = 0f) }

        hintTimerJob = viewModelScope.launch {
            // Wait 5 seconds of idle time
            delay(5000L)

            // Only show hint if still in Idle phase
            if (_uiState.value.phase != GamePhase.Idle) return@launch

            // Find a valid move using the engine's shuffle checker
            val board = _uiState.value.board ?: return@launch
            val hint = engine.shuffleChecker.findValidMove(board) ?: return@launch

            // Set the hint positions — BoardCanvas will draw glowing circles here
            _uiState.update {
                it.copy(hintPositions = setOf(hint.from, hint.to))
            }

            // Animate a pulsing glow — loops until this job is cancelled
            // (cancelled when the player swipes or a new hint timer starts)
            // Each cycle: progress goes 0 → 1 over 800ms, creating a "breathing" effect
            while (true) {
                animateProgress(durationMs = 800, steps = 16) { state, progress ->
                    state.copy(hintAnimProgress = progress)
                }
                // Brief pause between pulses
                _uiState.update { it.copy(hintAnimProgress = 0f) }
                delay(200L)
            }
        }
    }

    // ===== Timed Challenge: Countdown Timer =====

    /**
     * Start the 1-second countdown for timed challenge mode.
     * Decrements timeRemaining every second. On reaching 0, triggers game over.
     * Awards time bonus after cascades based on combo count.
     */
    private fun startTimedCountdown() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.timeRemaining > 0) {
                delay(1000L)
                val currentPhase = _uiState.value.phase
                // Only countdown during active gameplay
                if (currentPhase == GamePhase.GameOver || currentPhase == GamePhase.LevelComplete) break

                _uiState.update { it.copy(timeRemaining = it.timeRemaining - 1) }

                if (_uiState.value.timeRemaining <= 0) {
                    // Time's up!
                    timerJob?.cancel()
                    sound.playGameOver()
                    haptic.vibrateGameOver()

                    // Save timed score
                    val diff = timedDifficulty
                    if (diff != null) {
                        ServiceLocator.timedChallengeRepository.saveBestScore(diff, engine.score)
                    }
                    saveStatistics()
                    checkAchievements()

                    _uiState.update {
                        it.copy(phase = GamePhase.GameOver, stars = 0)
                    }
                    break
                }
            }
        }
    }

    /**
     * Award time bonus after a cascade in timed mode.
     * Each combo level beyond 0 adds 2 seconds.
     */
    private fun awardTimeBonus(comboCount: Int) {
        if (!isTimedMode || comboCount <= 0) return
        val bonus = comboCount * 2
        _uiState.update {
            it.copy(
                timeRemaining = it.timeRemaining + bonus,
                timeBonusPopup = bonus
            )
        }
        // Clear the popup after a brief delay
        viewModelScope.launch {
            delay(800L)
            _uiState.update { it.copy(timeBonusPopup = 0) }
        }
    }

    // ===== Achievement System Integration =====

    /**
     * Check and unlock any newly earned achievements.
     * Called after each level completion (win or loss).
     */
    private fun checkAchievements() {
        viewModelScope.launch {
            val stats = statsRepo.getStatistics().first()
            val progress = ServiceLocator.progressRepository.getProgress().first()
            val dailyState = ServiceLocator.dailyChallengeRepository.getState().first()

            val newlyUnlocked = ServiceLocator.achievementRepository
                .checkAndUnlockAchievements(stats, progress, dailyState)

            // Show achievement toast for the first newly unlocked one
            if (newlyUnlocked.isNotEmpty()) {
                val firstId = newlyUnlocked.first()
                val definition = com.galaxymatch.game.data.AchievementDefinitions.all
                    .find { it.id == firstId }
                if (definition != null) {
                    _uiState.update {
                        it.copy(achievementUnlocked = "${definition.emoji} ${definition.title}")
                    }
                    delay(3000L)
                    _uiState.update { it.copy(achievementUnlocked = null) }
                }
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

    /** Gravity fall: list of gem movements from GravityProcessor. */
    val fallingGems: List<GravityProcessor.GemMovement> = emptyList(),
    /** Gravity fall: 0=gems at old positions, 1=gems at final positions. */
    val fallProgress: Float = 0f,

    /** Score popup: the score value to show (e.g. 150). 0 = no popup visible. */
    val scorePopupValue: Int = 0,
    /** Score popup: 0=just appeared, 1=fully faded out and floated away. */
    val scorePopupProgress: Float = 0f,

    /** Combo text bounce: 0=start of bounce, 1=settled at normal size. */
    val comboAnimProgress: Float = 0f,

    /** Shuffle shake: 0=start of shake, 1=shake complete. Drives board wobble. */
    val shuffleProgress: Float = 0f,

    // === Feature 1: Enhanced Combo System ===
    /** Screen shake: 0=start of shake, 1=shake finished. Drives board offset on big combos. */
    val screenShakeProgress: Float = 0f,
    /** Max combo reached during this entire level (for end-of-level summary). */
    val maxComboReached: Int = 0,

    // === Feature 2: Match Hint System ===
    /** Positions of the two gems being hinted (pulsing glow). Empty = no hint showing. */
    val hintPositions: Set<Position> = emptySet(),
    /** Hint glow animation: 0=start of pulse cycle, 1=end of one pulse. Repeats. */
    val hintAnimProgress: Float = 0f,

    // === Feature 3: In-Game Star Progress ===
    /** Number of stars earned so far during gameplay (0-3). Updated each cascade step. */
    val currentStars: Int = 0,
    /** Which star number just unlocked (1, 2, or 3). 0 = no unlock in progress. */
    val starJustUnlocked: Int = 0,
    /** Star unlock animation: 0=start, 1=complete. Non-zero when a new star was just earned. */
    val starUnlockAnimProgress: Float = 0f,

    // === Feature 2: Restart ===
    /** Whether the restart confirmation dialog is showing. */
    val showRestartDialog: Boolean = false,

    // === Feature 3: Undo ===
    /** Whether an undo is currently available (snapshot exists and undo not yet used). */
    val undoAvailable: Boolean = false,
    /** Whether the undo has been used this level (only one undo per level). */
    val undoUsedThisLevel: Boolean = false,

    // === Feature 4: Interactive Tutorial ===
    /** Whether the tutorial overlay is currently showing. */
    val showTutorial: Boolean = false,
    /** Current tutorial step: 0 = not in tutorial, 1 = swap step, 2 = match result, 3 = specials info. */
    val tutorialStep: Int = 0,
    /** Positions to highlight with a pulsing glow during tutorial step 1 (the two gems to swap). */
    val tutorialHighlightPositions: Set<Position> = emptySet(),
    /** The "from" position for the required tutorial swap (step 1). */
    val tutorialSwapFrom: Position? = null,
    /** The "to" position for the required tutorial swap (step 1). */
    val tutorialSwapTo: Position? = null,

    // === Pre-Level Dialog ===
    /** Whether the pre-level dialog is showing (before board loads). */
    val showPreLevelDialog: Boolean = false,

    // === Visual Polish: Board Entry Animation ===
    /** Board entry drop-in: 0=gems above board, 1=fully settled. */
    val boardEntryProgress: Float = 1f,

    // === Power-Ups / Boosters ===
    /** How many stars the player currently has available to spend. */
    val availableStars: Int = 0,
    /** Which power-up is currently in "select target" mode. Null = none active. */
    val activePowerUp: PowerUpType? = null,

    // === Objective System ===
    /** The objective type for the current level. Null before level loads. */
    val objectiveType: ObjectiveType? = null,
    /** For BreakAllIce: how many ice blocks have been broken so far. */
    val iceBroken: Int = 0,
    /** For BreakAllIce: total ice blocks on this level at start. */
    val totalIce: Int = 0,
    /** For ClearGemType: how many of the target color have been cleared. */
    val gemsCleared: Int = 0,
    /** For ClearGemType: how many of the target color need to be cleared. */
    val targetGemCount: Int = 0,
    /** For ClearGemType: which gem color is the target. Null for other objectives. */
    val targetGemType: GemType? = null,
    /** Whether the level objective has been completed (for UI display). */
    val objectiveComplete: Boolean = false,

    // === Bonus Moves ===
    /** Whether bonus moves are currently being performed (auto-destroying gems). */
    val bonusMoveActive: Boolean = false,
    /** How many bonus moves remain to be consumed. */
    val bonusMovesRemaining: Int = 0,

    // === Accessibility ===
    /** Whether colorblind mode is enabled (draws shape overlays on gems). */
    val colorblindMode: Boolean = false,
    /** Whether high-contrast gem colors are enabled for better visibility. */
    val highContrastMode: Boolean = false,

    // === Daily Challenge Mode ===
    /** Whether this is a daily challenge game. */
    val isDailyChallenge: Boolean = false,

    // === Timed Challenge Mode ===
    /** Whether this is a timed challenge game. */
    val isTimedMode: Boolean = false,
    /** Seconds remaining in the countdown (timed mode only). */
    val timeRemaining: Int = 0,
    /** Time bonus popup value (e.g., +4s). 0 = no popup. */
    val timeBonusPopup: Int = 0,

    // === Achievement System ===
    /** Achievement just unlocked (emoji + title text). Null = no toast. */
    val achievementUnlocked: String? = null,

    // === Level Complete Celebration ===
    /** Whether the current score is a new personal best for this level. */
    val isNewHighScore: Boolean = false,

    // === Power-Up Visual Effects ===
    /** Hammer impact flash: progress 0→1 over 200ms (expanding + fading circle at target). */
    val hammerFlashProgress: Float = 0f,
    /** Hammer flash center position (board row, col). Null = no flash active. */
    val hammerFlashPosition: Position? = null,
    /** ColorBomb screen tint color (null = no tint). Fades out using colorBombFlashProgress. */
    val colorBombFlashColor: Color? = null,
    /** ColorBomb tint flash: progress 0→1 over 300ms (fading overlay). */
    val colorBombFlashProgress: Float = 0f,
    /** Extra Moves "+5 Moves!" floating text animation: progress 0→1 over 800ms. */
    val extraMovesAnimProgress: Float = 0f,
    /** Whether the +5 Moves floating text animation is active. */
    val extraMovesAnimActive: Boolean = false
)
