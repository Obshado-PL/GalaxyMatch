package com.galaxymatch.game.engine

import com.galaxymatch.game.model.*
import com.galaxymatch.game.model.ObstacleType

/**
 * The core game engine that orchestrates all game logic.
 *
 * This is the "brain" of the game. It:
 * - Initializes the board with random gems (no initial matches)
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
    private val specialResolver = SpecialGemResolver()
    private val specialEffects = SpecialGemEffects()
    private val gravityProcessor = GravityProcessor()
    val shuffleChecker = ShuffleChecker(matchDetector)
    private val scoreCalculator = ScoreCalculator()

    /** Tracks progress toward the level's objective (ice broken, gems cleared, etc.) */
    val objectiveTracker = ObjectiveTracker(levelConfig.objective, levelConfig)

    // The gem types available for this level
    private val availableTypes = GemType.forLevel(levelConfig.availableGemTypes)

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
     * Initialize the board with random gems, ensuring no initial matches.
     *
     * Algorithm:
     * For each cell (left to right, top to bottom):
     * 1. Start with all available gem types
     * 2. If the two cells to the left are the same type, exclude that type
     * 3. If the two cells above are the same type, exclude that type
     * 4. Randomly pick from the remaining types
     *
     * This guarantees no pre-existing matches while keeping the board random.
     */
    fun initializeBoard(): BoardState {
        Gem.resetIdCounter()
        val grid = Array<Array<Gem?>>(levelConfig.rows) { arrayOfNulls(levelConfig.cols) }

        for (row in 0 until levelConfig.rows) {
            for (col in 0 until levelConfig.cols) {
                val pos = Position(row, col)

                // === Obstacle handling ===
                // Stone positions stay null (no gem can exist here).
                // Ice positions get a normal gem — ice is just an overlay.
                if (levelConfig.obstacles[pos] == ObstacleType.Stone) {
                    grid[row][col] = null
                    continue
                }

                val excludedTypes = mutableSetOf<GemType>()

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

                grid[row][col] = Gem(type = chosenType)
            }
        }

        // Copy obstacles and bombs from level config into the board state
        board = BoardState(levelConfig.rows, levelConfig.cols, grid, levelConfig.obstacles, levelConfig.bombs)
        score = 0
        movesRemaining = levelConfig.maxMoves
        comboCount = 0
        phase = GamePhase.Idle
        objectiveTracker.reset()

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
        if (board.gemAt(from) == null || board.gemAt(to) == null) return false

        // Locked gems cannot be swapped
        if (board.isLocked(from) || board.isLocked(to)) return false

        // Check for special + special combo
        val gem1 = board.gemAt(from)!!
        val gem2 = board.gemAt(to)!!

        if (gem1.special != SpecialType.None && gem2.special != SpecialType.None) {
            return processSpecialCombo(gem1, from, gem2, to)
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

        // === Decrement all bomb timers ===
        if (board.bombs.isNotEmpty()) {
            val updatedBombs = board.bombs.mapValues { (_, timer) -> maxOf(0, timer - 1) }
            board = BoardState(board.rows, board.cols, board.grid, board.obstacles, updatedBombs)
            if (updatedBombs.any { it.value <= 0 }) {
                // A bomb exploded! Score the matches the player earned this move
                // before ending the game (they made a valid swap, they deserve the points).
                val scoreGained = scoreCalculator.calculateTotalScore(matches, 0)
                score += scoreGained
                phase = GamePhase.GameOver
                lastMatches = matches
                return true
            }
        }

        comboCount = 0
        phase = GamePhase.Matching
        lastMatches = matches

        return true
    }

    /**
     * Handle the case where two special gems are swapped together.
     * This triggers a special combo effect without needing a match.
     */
    private fun processSpecialCombo(
        gem1: Gem, pos1: Position,
        gem2: Gem, pos2: Position
    ): Boolean {
        val comboPositions = specialEffects.resolveSpecialCombo(
            gem1, pos1, gem2, pos2, board
        ) ?: return false

        movesRemaining--

        // === Decrement all bomb timers ===
        if (board.bombs.isNotEmpty()) {
            val updatedBombs = board.bombs.mapValues { (_, timer) -> maxOf(0, timer - 1) }
            board = BoardState(board.rows, board.cols, board.grid, board.obstacles, updatedBombs)
            if (updatedBombs.any { it.value <= 0 }) {
                phase = GamePhase.GameOver
                return true
            }
        }

        comboCount = 0

        // === Track clears for objectives (before removing gems from the grid) ===
        objectiveTracker.recordSpecialClears(comboPositions, board)

        // Clear all positions from the combo
        for (pos in comboPositions) {
            board.setGem(pos, null)
        }

        // Score based on number of gems cleared
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
     * 1. Clears matched gems (creating specials where appropriate)
     * 2. Activates any special gems that were part of matches
     * 3. Applies gravity (gems fall, new ones fill from top)
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

        // === Track gems cleared for ClearGemType objective ===
        // Must be called before gems are removed from the grid.
        // MatchResult already contains gemType, so this works even after removal.
        objectiveTracker.recordGemsCleared(matches)

        // Track all positions to clear (from matches + special effects)
        val positionsToClear = mutableSetOf<Position>()
        val specialActivations = mutableSetOf<Position>()

        // Process each match: clear gems and create specials
        val specialsToCreate = mutableListOf<SpecialGemResolver.SpecialCreation>()

        for (match in matches) {
            positionsToClear.addAll(match.positions)

            // Check if this match should create a special gem
            val specialCreation = specialResolver.resolve(match, swapAction)
            if (specialCreation != null) {
                specialsToCreate.add(specialCreation)
            }

            // Check if any matched gem IS a special — activate its effect
            for (pos in match.positions) {
                val gem = board.gemAt(pos)
                if (gem != null && gem.special != SpecialType.None) {
                    val effectPositions = specialEffects.activate(
                        gem, pos, board,
                        targetType = match.gemType
                    )
                    specialActivations.addAll(effectPositions)
                    positionsToClear.addAll(effectPositions)
                }
            }
        }

        // Check if activated specials trigger OTHER specials (chain reaction)
        val additionalClears = mutableSetOf<Position>()
        for (pos in specialActivations) {
            val gem = board.gemAt(pos)
            if (gem != null && gem.special != SpecialType.None && pos !in positionsToClear) {
                // This is a special that got caught in the blast — activate it too
                val chainPositions = specialEffects.activate(gem, pos, board)
                additionalClears.addAll(chainPositions)
            }
        }
        positionsToClear.addAll(additionalClears)

        // === Break ice / reinforced ice at cleared positions ===
        var updatedObstacles = board.obstacles
        var iceBrokenThisStep = 0
        for (pos in positionsToClear) {
            when (board.getObstacle(pos)) {
                ObstacleType.ReinforcedIce -> {
                    // Downgrade to normal Ice (first hit)
                    updatedObstacles = updatedObstacles - pos + (pos to ObstacleType.Ice)
                }
                ObstacleType.Ice -> {
                    // Fully break the ice
                    updatedObstacles = updatedObstacles - pos
                    iceBrokenThisStep++
                }
                else -> { /* no-op for Stone, Locked, or no obstacle */ }
            }
        }
        if (iceBrokenThisStep > 0) {
            objectiveTracker.recordIceBroken(iceBrokenThisStep)
        }

        // === Break locks adjacent to cleared positions ===
        val locksToBreak = mutableSetOf<Position>()
        for (clearedPos in positionsToClear) {
            for (neighbor in listOf(clearedPos.up(), clearedPos.down(), clearedPos.left(), clearedPos.right())) {
                if (board.isInBounds(neighbor) && updatedObstacles[neighbor] == ObstacleType.Locked) {
                    locksToBreak.add(neighbor)
                }
            }
        }
        for (lockPos in locksToBreak) {
            updatedObstacles = updatedObstacles - lockPos
        }

        // === Defuse bombs at cleared positions ===
        var updatedBombs = board.bombs
        for (pos in positionsToClear) {
            if (pos in updatedBombs) {
                updatedBombs = updatedBombs - pos
            }
        }

        // Clear all matched/affected positions
        for (pos in positionsToClear) {
            board.setGem(pos, null)
        }

        // Place newly created special gems
        for (creation in specialsToCreate) {
            if (board.gemAt(creation.position) == null) {
                board.setGem(creation.position, creation.gem)
            }
        }

        // Update board with modified obstacles and bombs
        board = BoardState(board.rows, board.cols, board.grid, updatedObstacles, updatedBombs)

        // Apply gravity — gems fall down, new ones fill from top
        val gravityResult = gravityProcessor.applyGravity(board, availableTypes)

        // === Remap bomb positions after gravity ===
        if (board.bombs.isNotEmpty()) {
            val remappedBombs = mutableMapOf<Position, Int>()
            for ((bombPos, timer) in board.bombs) {
                val movement = gravityResult.movements.find { m ->
                    m.fromRow == bombPos.row && m.col == bombPos.col && !m.isNew
                }
                if (movement != null) {
                    remappedBombs[Position(movement.toRow, movement.col)] = timer
                } else if (board.gemAt(bombPos) != null) {
                    // Bomb gem didn't move
                    remappedBombs[bombPos] = timer
                }
                // If gem was cleared (bomb should have been defused), skip it
            }
            board = BoardState(board.rows, board.cols, board.grid, board.obstacles, remappedBombs)
        }

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
     * The win condition depends on the level's objective type:
     * - ReachScore: score >= targetScore (classic behavior)
     * - BreakAllIce: all ice blocks broken → win immediately
     * - ClearGemType: enough target-color gems cleared → win immediately
     *
     * For ReachScore, the player can keep playing for more stars after meeting
     * the target. For BreakAllIce/ClearGemType, the level completes right away.
     *
     * Lose condition (same for all): out of moves AND objective not met.
     *
     * @return The new game phase
     */
    fun evaluateEndCondition(): GamePhase {
        val objectiveMet = objectiveTracker.isObjectiveMet(score, board.obstacles)

        phase = when {
            // Non-score objectives met WITH remaining moves → bonus moves!
            // Each leftover move auto-destroys a random gem for bonus points.
            objectiveMet && levelConfig.objective !is ObjectiveType.ReachScore
                && movesRemaining > 0 -> {
                GamePhase.BonusMoves
            }
            // Non-score objectives met with NO remaining moves → straight to complete
            objectiveMet && levelConfig.objective !is ObjectiveType.ReachScore -> {
                GamePhase.LevelComplete
            }
            // Score objective: met + no moves left → complete
            objectiveMet && movesRemaining <= 0 -> GamePhase.LevelComplete
            // Score objective: met but still has moves → keep playing for stars
            objectiveMet -> GamePhase.Idle
            // Out of moves and objective not met → game over
            movesRemaining <= 0 -> GamePhase.GameOver
            // Still has moves, keep playing
            else -> GamePhase.Idle
        }
        return phase
    }

    /**
     * Force check level completion (called when player wants to end early
     * or when last move was used).
     */
    fun checkLevelComplete(): GamePhase {
        val objectiveMet = objectiveTracker.isObjectiveMet(score, board.obstacles)
        if (objectiveMet) {
            phase = GamePhase.LevelComplete
        } else if (movesRemaining <= 0) {
            phase = GamePhase.GameOver
        }
        return phase
    }

    /**
     * Restore the engine to a previously saved state (for Undo).
     *
     * This resets the board, score, moves, and objective counters to snapshot values.
     * The engine returns to Idle phase so the player can make another move.
     * Called from the ViewModel when the player taps the Undo button.
     *
     * @param savedBoard A deep copy of the board before the undone move
     * @param savedScore The score before the undone move
     * @param savedMoves The moves remaining before the undone move
     * @param savedIceBroken Ice broken count before the undone move
     * @param savedGemsCleared Gems cleared count before the undone move
     */
    fun restoreState(
        savedBoard: BoardState,
        savedScore: Int,
        savedMoves: Int,
        savedIceBroken: Int = 0,
        savedGemsCleared: Int = 0
    ) {
        board = savedBoard
        score = savedScore
        movesRemaining = savedMoves
        comboCount = 0
        phase = GamePhase.Idle
        lastMatches = emptyList()
        lastGravityResult = null
        lastSpecialActivations = emptySet()
        objectiveTracker.restoreCounters(savedIceBroken, savedGemsCleared)
    }

    // ===== Power-Up Methods =====
    // Power-ups are bonus actions that don't cost a move.
    // They can only be used when the board is idle (no animations running).

    /**
     * Use the Hammer power-up: destroy a single gem at the given position.
     *
     * The gem is removed from the board, which triggers gravity (gems above
     * fall down, new gems fill from the top). After gravity, the cascade loop
     * may find new matches — just like after a normal swap.
     *
     * @param position The board position of the gem to destroy
     * @return True if the gem was successfully destroyed, false if invalid
     */
    fun useHammer(position: Position): Boolean {
        if (phase != GamePhase.Idle) return false
        if (!board.isInBounds(position)) return false

        // Hammer on stone does nothing — stones are indestructible
        if (board.isStone(position)) return false

        val gem = board.gemAt(position) ?: return false

        // === Track this gem clear for ClearGemType objective ===
        // Must be done before the gem is removed from the board
        objectiveTracker.recordSpecialClears(setOf(position), board)

        // If the gem is a special, activate its effect first (bonus!)
        if (gem.special != SpecialType.None) {
            val effectPositions = specialEffects.activate(gem, position, board)
            // Track special effect clears for objectives too
            objectiveTracker.recordSpecialClears(effectPositions, board)
            for (pos in effectPositions) {
                board.setGem(pos, null)
            }
            lastSpecialActivations = effectPositions
        }

        // Remove the gem
        board.setGem(position, null)

        // Handle obstacle at this position (ice, reinforced ice, locked)
        var updatedObstacles = board.obstacles
        var updatedBombs = board.bombs
        when (board.getObstacle(position)) {
            ObstacleType.ReinforcedIce -> {
                updatedObstacles = updatedObstacles - position + (position to ObstacleType.Ice)
            }
            ObstacleType.Ice -> {
                updatedObstacles = updatedObstacles - position
                objectiveTracker.recordIceBroken(1)
            }
            ObstacleType.Locked -> {
                updatedObstacles = updatedObstacles - position
            }
            else -> {}
        }
        // Defuse bomb at hammer position
        if (position in updatedBombs) {
            updatedBombs = updatedBombs - position
        }
        board = BoardState(board.rows, board.cols, board.grid, updatedObstacles, updatedBombs)

        // Apply gravity — gems fall down, new ones fill from top
        val gravityResult = gravityProcessor.applyGravity(board, availableTypes)
        lastGravityResult = gravityResult

        // Check if gravity created any new matches
        val newMatches = matchDetector.findAllMatches(board)
        if (newMatches.isNotEmpty()) {
            comboCount = 0
            lastMatches = newMatches
            phase = GamePhase.Cascading
        } else {
            lastMatches = emptyList()
            phase = GamePhase.Settled
        }

        return true
    }

    /**
     * Use the Color Bomb power-up: remove ALL gems of the same color
     * as the gem at the given position.
     *
     * This is similar to the ColorBomb special gem effect, but triggered
     * by a power-up instead of a match. All gems of the target color are
     * cleared, gravity fills the gaps, and the cascade loop checks for new matches.
     *
     * @param position The board position — its gem's color determines which color to clear
     * @return True if gems were successfully cleared, false if invalid
     */
    fun useColorBomb(position: Position): Boolean {
        if (phase != GamePhase.Idle) return false
        if (!board.isInBounds(position)) return false
        val gem = board.gemAt(position) ?: return false

        val targetType = gem.type
        val clearedPositions = mutableSetOf<Position>()

        // Find and clear ALL gems of this color on the board
        for (row in 0 until board.rows) {
            for (col in 0 until board.cols) {
                val pos = Position(row, col)
                val c = board.gemAt(pos)
                if (c != null && c.type == targetType) {
                    // If this gem is a special, activate its effect too (chain reaction!)
                    if (c.special != SpecialType.None) {
                        val effectPositions = specialEffects.activate(c, pos, board)
                        clearedPositions.addAll(effectPositions)
                    }
                    clearedPositions.add(pos)
                }
            }
        }

        // === Track clears for objectives (before removing gems) ===
        objectiveTracker.recordSpecialClears(clearedPositions, board)

        // === Break ice / reinforced ice at cleared positions ===
        var updatedObstacles = board.obstacles
        var iceBrokenByBomb = 0
        for (pos in clearedPositions) {
            when (board.getObstacle(pos)) {
                ObstacleType.ReinforcedIce -> {
                    updatedObstacles = updatedObstacles - pos + (pos to ObstacleType.Ice)
                }
                ObstacleType.Ice -> {
                    updatedObstacles = updatedObstacles - pos
                    iceBrokenByBomb++
                }
                else -> {}
            }
        }
        if (iceBrokenByBomb > 0) {
            objectiveTracker.recordIceBroken(iceBrokenByBomb)
        }

        // Defuse bombs at cleared positions
        var updatedBombs = board.bombs
        for (pos in clearedPositions) {
            if (pos in updatedBombs) {
                updatedBombs = updatedBombs - pos
            }
        }

        // Clear all affected positions
        for (pos in clearedPositions) {
            board.setGem(pos, null)
        }

        // Update board
        board = BoardState(board.rows, board.cols, board.grid, updatedObstacles, updatedBombs)

        // Score based on gems cleared (same rate as special combo)
        score += clearedPositions.size * 60

        lastSpecialActivations = clearedPositions

        // Apply gravity
        val gravityResult = gravityProcessor.applyGravity(board, availableTypes)
        lastGravityResult = gravityResult

        // Check for new matches from the cascade
        val newMatches = matchDetector.findAllMatches(board)
        if (newMatches.isNotEmpty()) {
            comboCount = 0
            lastMatches = newMatches
            phase = GamePhase.Cascading
        } else {
            lastMatches = emptyList()
            phase = GamePhase.Settled
        }

        return true
    }

    /**
     * Use the Extra Moves power-up: add 5 extra moves to the current level.
     *
     * This is the simplest power-up — just increases movesRemaining by 5.
     * Can even be used when the player has 0 moves left (if the game
     * hasn't ended yet, i.e. they're still in idle phase).
     *
     * @return True if extra moves were added, false if the game isn't in idle phase
     */
    fun useExtraMoves(): Boolean {
        if (phase != GamePhase.Idle) return false
        movesRemaining += 5
        return true
    }

    // ===== Bonus Moves =====
    // When a non-score objective (BreakAllIce, ClearGemType) is completed
    // with remaining moves, each leftover move auto-destroys a random gem
    // for bonus points — the classic "reward for finishing early" moment.

    /**
     * Perform one bonus move: randomly destroy a gem and trigger cascades.
     *
     * Picks a random non-null, non-stone gem position, destroys it
     * (activating special effects if applicable), applies gravity, and
     * checks for new matches. The ViewModel animates each step.
     *
     * @return True if a bonus move was performed, false if no gems or moves remain
     */
    fun performBonusMove(): Boolean {
        if (movesRemaining <= 0) return false

        // Collect all non-null gem positions (skip stones — they're indestructible)
        val availablePositions = mutableListOf<Position>()
        for (row in 0 until board.rows) {
            for (col in 0 until board.cols) {
                val pos = Position(row, col)
                if (!board.isStone(pos) && board.gemAt(pos) != null) {
                    availablePositions.add(pos)
                }
            }
        }

        if (availablePositions.isEmpty()) {
            // Board is empty — abort remaining bonus moves
            movesRemaining = 0
            phase = GamePhase.LevelComplete
            return false
        }

        // Pick a random gem to destroy
        val targetPosition = availablePositions.random()
        val gem = board.gemAt(targetPosition)!!

        // Track this clear for objectives (consistency with power-ups)
        objectiveTracker.recordSpecialClears(setOf(targetPosition), board)

        // If the gem is a special, activate its effect (bonus fireworks!)
        if (gem.special != SpecialType.None) {
            val effectPositions = specialEffects.activate(gem, targetPosition, board)
            objectiveTracker.recordSpecialClears(effectPositions, board)
            for (pos in effectPositions) {
                board.setGem(pos, null)
            }
            lastSpecialActivations = effectPositions
            // Bigger bonus for special gem activations
            score += effectPositions.size * 80
        } else {
            lastSpecialActivations = emptySet()
            // Small bonus for regular gem destruction
            score += 50
        }

        // Remove the target gem
        board.setGem(targetPosition, null)

        // Handle obstacle at bonus-move target position
        var updatedObstacles = board.obstacles
        when (board.getObstacle(targetPosition)) {
            ObstacleType.ReinforcedIce -> {
                updatedObstacles = updatedObstacles - targetPosition + (targetPosition to ObstacleType.Ice)
            }
            ObstacleType.Ice -> {
                updatedObstacles = updatedObstacles - targetPosition
                objectiveTracker.recordIceBroken(1)
            }
            else -> {}
        }
        // Defuse bomb at bonus-move target
        var updatedBombs = board.bombs
        if (targetPosition in updatedBombs) {
            updatedBombs = updatedBombs - targetPosition
        }
        if (updatedObstacles !== board.obstacles || updatedBombs !== board.bombs) {
            board = BoardState(board.rows, board.cols, board.grid, updatedObstacles, updatedBombs)
        }

        // Consume one move
        movesRemaining--

        // Apply gravity — gems fall down, new ones fill from top
        val gravityResult = gravityProcessor.applyGravity(board, availableTypes)
        lastGravityResult = gravityResult

        // Check if gravity created any new matches (cascade chain)
        val newMatches = matchDetector.findAllMatches(board)
        lastMatches = newMatches
        if (newMatches.isNotEmpty()) {
            comboCount = 0
            phase = GamePhase.Cascading
        } else {
            // No cascades — stay in bonus moves or complete if done
            phase = if (movesRemaining > 0) GamePhase.BonusMoves
                else GamePhase.LevelComplete
        }

        return true
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
