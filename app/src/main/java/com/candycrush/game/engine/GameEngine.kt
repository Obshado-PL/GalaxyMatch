package com.candycrush.game.engine

import com.candycrush.game.model.*

/**
 * The core game engine that orchestrates all game logic.
 *
 * This is the "brain" of the game. It:
 * - Initializes the board with random candies (no initial matches)
 * - Processes player swaps
 * - Runs the cascade loop (match → clear → gravity → repeat)
 * - Tracks score, moves, and combos
 * - Determines game-over and level-complete conditions
 *
 * IMPORTANT: This class has NO Android dependencies — it's pure Kotlin.
 * This makes it easy to test and keeps game logic separate from UI code.
 *
 * The ViewModel calls the engine's methods and translates the results
 * into UI state changes and animations.
 */
class GameEngine(private val levelConfig: LevelConfig) {

    // ===== Game State =====
    var board: BoardState = BoardState.empty(levelConfig.rows, levelConfig.cols)
        private set
    var score: Int = 0
        private set
    var movesRemaining: Int = levelConfig.maxMoves
        private set
    var comboCount: Int = 0
        private set
    var phase: GamePhase = GamePhase.Idle
        private set

    // ===== Internal Components =====
    private val matchDetector = MatchDetector()
    private val specialResolver = SpecialCandyResolver()
    private val specialEffects = SpecialCandyEffects()
    private val gravityProcessor = GravityProcessor()
    val shuffleChecker = ShuffleChecker(matchDetector)
    private val scoreCalculator = ScoreCalculator()

    // The candy types available for this level
    private val availableTypes = CandyType.forLevel(levelConfig.availableCandyTypes)

    // ===== Results from the last operation (used by ViewModel for animations) =====
    var lastMatches: List<MatchResult> = emptyList()
        private set
    var lastGravityResult: GravityProcessor.GravityResult? = null
        private set
    var lastSpecialActivations: Set<Position> = emptySet()
        private set

    /**
     * Result of a single cascade step, telling the ViewModel what happened.
     */
    data class CascadeStepResult(
        val matches: List<MatchResult>,
        val specialActivations: Set<Position>,
        val gravityResult: GravityProcessor.GravityResult,
        val hasMoreMatches: Boolean,
        val scoreGained: Int
    )

    // ===== Public API =====

    /**
     * Initialize the board with random candies, ensuring no initial matches.
     *
     * Algorithm:
     * For each cell (left to right, top to bottom):
     * 1. Start with all available candy types
     * 2. If the two cells to the left are the same type, exclude that type
     * 3. If the two cells above are the same type, exclude that type
     * 4. Randomly pick from the remaining types
     *
     * This guarantees no pre-existing matches while keeping the board random.
     */
    fun initializeBoard(): BoardState {
        Candy.resetIdCounter()
        val grid = Array<Array<Candy?>>(levelConfig.rows) { arrayOfNulls(levelConfig.cols) }

        for (row in 0 until levelConfig.rows) {
            for (col in 0 until levelConfig.cols) {
                val excludedTypes = mutableSetOf<CandyType>()

                // Check horizontal: if the two cells to the left have the same type,
                // exclude that type to prevent a match of 3
                if (col >= 2) {
                    val left1 = grid[row][col - 1]
                    val left2 = grid[row][col - 2]
                    if (left1 != null && left2 != null && left1.type == left2.type) {
                        excludedTypes.add(left1.type)
                    }
                }

                // Check vertical: same logic for the two cells above
                if (row >= 2) {
                    val up1 = grid[row - 1][col]
                    val up2 = grid[row - 2][col]
                    if (up1 != null && up2 != null && up1.type == up2.type) {
                        excludedTypes.add(up1.type)
                    }
                }

                // Pick a random type from the remaining options
                val allowedTypes = availableTypes.filter { it !in excludedTypes }
                val chosenType = if (allowedTypes.isNotEmpty()) {
                    allowedTypes.random()
                } else {
                    // Extremely rare edge case: all types are excluded
                    availableTypes.random()
                }

                grid[row][col] = Candy(type = chosenType)
            }
        }

        board = BoardState(levelConfig.rows, levelConfig.cols, grid)
        score = 0
        movesRemaining = levelConfig.maxMoves
        comboCount = 0
        phase = GamePhase.Idle

        return board
    }

    /**
     * Process a player's swap action.
     *
     * @param action The swap to attempt (from position and to position)
     * @return True if the swap was valid (produced matches), false if it should be reversed
     */
    fun processSwap(action: SwapAction): Boolean {
        if (phase != GamePhase.Idle) return false

        val from = action.from
        val to = action.to

        // Validate the swap
        if (!board.isInBounds(from) || !board.isInBounds(to)) return false
        if (!from.isAdjacentTo(to)) return false
        if (board.candyAt(from) == null || board.candyAt(to) == null) return false

        // Check for special + special combo
        val candy1 = board.candyAt(from)!!
        val candy2 = board.candyAt(to)!!

        if (candy1.special != SpecialType.None && candy2.special != SpecialType.None) {
            return processSpecialCombo(candy1, from, candy2, to)
        }

        // Perform the swap
        board.swap(from, to)

        // Check for matches
        val matches = matchDetector.findAllMatches(board)

        if (matches.isEmpty()) {
            // No matches — swap back (invalid move)
            board.swap(from, to)
            return false
        }

        // Valid move — deduct a move
        movesRemaining--
        comboCount = 0
        phase = GamePhase.Matching
        lastMatches = matches

        return true
    }

    /**
     * Handle the case where two special candies are swapped together.
     * This triggers a special combo effect without needing a match.
     */
    private fun processSpecialCombo(
        candy1: Candy, pos1: Position,
        candy2: Candy, pos2: Position
    ): Boolean {
        val comboPositions = specialEffects.resolveSpecialCombo(
            candy1, pos1, candy2, pos2, board
        ) ?: return false

        movesRemaining--
        comboCount = 0

        // Clear all positions from the combo
        for (pos in comboPositions) {
            board.setCandy(pos, null)
        }

        // Score based on number of candies cleared
        score += comboPositions.size * 60

        lastSpecialActivations = comboPositions
        phase = GamePhase.Cascading

        return true
    }

    /**
     * Process one step of the cascade loop.
     *
     * This is called repeatedly by the ViewModel after each animation completes.
     * Each call:
     * 1. Clears matched candies (creating specials where appropriate)
     * 2. Activates any special candies that were part of matches
     * 3. Applies gravity (candies fall, new ones fill from top)
     * 4. Checks for new matches
     *
     * @param swapAction The original swap action (for determining special placement).
     *                   Only relevant on the first cascade step.
     * @return CascadeStepResult telling the ViewModel what happened
     */
    fun processCascadeStep(swapAction: SwapAction? = null): CascadeStepResult {
        val matches = if (lastMatches.isNotEmpty()) {
            lastMatches
        } else {
            matchDetector.findAllMatches(board)
        }

        // Calculate score for these matches
        val scoreGained = scoreCalculator.calculateTotalScore(matches, comboCount)
        score += scoreGained

        // Track all positions to clear (from matches + special effects)
        val positionsToClear = mutableSetOf<Position>()
        val specialActivations = mutableSetOf<Position>()

        // Process each match: clear candies and create specials
        val specialsToCreate = mutableListOf<SpecialCandyResolver.SpecialCreation>()

        for (match in matches) {
            positionsToClear.addAll(match.positions)

            // Check if this match should create a special candy
            val specialCreation = specialResolver.resolve(match, swapAction)
            if (specialCreation != null) {
                specialsToCreate.add(specialCreation)
            }

            // Check if any matched candy IS a special — activate its effect
            for (pos in match.positions) {
                val candy = board.candyAt(pos)
                if (candy != null && candy.special != SpecialType.None) {
                    val effectPositions = specialEffects.activate(
                        candy, pos, board,
                        targetType = match.candyType
                    )
                    specialActivations.addAll(effectPositions)
                    positionsToClear.addAll(effectPositions)
                }
            }
        }

        // Check if activated specials trigger OTHER specials (chain reaction)
        val additionalClears = mutableSetOf<Position>()
        for (pos in specialActivations) {
            val candy = board.candyAt(pos)
            if (candy != null && candy.special != SpecialType.None && pos !in positionsToClear) {
                // This is a special that got caught in the blast — activate it too
                val chainPositions = specialEffects.activate(candy, pos, board)
                additionalClears.addAll(chainPositions)
            }
        }
        positionsToClear.addAll(additionalClears)

        // Clear all matched/affected positions
        for (pos in positionsToClear) {
            board.setCandy(pos, null)
        }

        // Place newly created special candies
        for (creation in specialsToCreate) {
            // Only place if the position was cleared (it should be)
            if (board.candyAt(creation.position) == null) {
                board.setCandy(creation.position, creation.candy)
            }
        }

        // Apply gravity — candies fall down, new ones fill from top
        val gravityResult = gravityProcessor.applyGravity(board, availableTypes)

        // Check for new matches (cascade chain)
        val newMatches = matchDetector.findAllMatches(board)
        val hasMore = newMatches.isNotEmpty()

        if (hasMore) {
            comboCount++
            lastMatches = newMatches
            phase = GamePhase.Cascading
        } else {
            lastMatches = emptyList()
            phase = GamePhase.Settled
        }

        lastGravityResult = gravityResult
        lastSpecialActivations = specialActivations

        return CascadeStepResult(
            matches = matches,
            specialActivations = specialActivations,
            gravityResult = gravityResult,
            hasMoreMatches = hasMore,
            scoreGained = scoreGained
        )
    }

    /**
     * Evaluate the end condition after the cascade settles.
     *
     * @return The new game phase:
     *   - LevelComplete if score >= target
     *   - GameOver if moves == 0 and score < target
     *   - Idle if the player still has moves
     */
    fun evaluateEndCondition(): GamePhase {
        phase = when {
            score >= levelConfig.targetScore && movesRemaining <= 0 -> GamePhase.LevelComplete
            score >= levelConfig.targetScore -> GamePhase.Idle // Still has moves, can keep playing for more stars
            movesRemaining <= 0 -> GamePhase.GameOver
            else -> GamePhase.Idle
        }
        return phase
    }

    /**
     * Force check level completion (called when player wants to end early
     * or when last move was used).
     */
    fun checkLevelComplete(): GamePhase {
        if (score >= levelConfig.targetScore) {
            phase = GamePhase.LevelComplete
        } else if (movesRemaining <= 0) {
            phase = GamePhase.GameOver
        }
        return phase
    }

    /**
     * Get the star rating for the current score.
     * @return 0, 1, 2, or 3 stars
     */
    fun getStarRating(): Int {
        return scoreCalculator.calculateStars(
            score = score,
            targetScore = levelConfig.targetScore,
            twoStarScore = levelConfig.twoStarScore,
            threeStarScore = levelConfig.threeStarScore
        )
    }
}
