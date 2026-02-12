package com.galaxymatch.game.model

/**
 * The current phase of the game engine's state machine.
 *
 * The game cycles through these phases during play. Understanding this
 * state machine is key to understanding how the game works:
 *
 * Normal flow:
 *   Idle → (player swipes) → Swapping → Matching → Cascading → Idle
 *
 * End conditions:
 *   Idle → (player swipes, moves hit 0) → Matching → Cascading → GameOver or LevelComplete
 *
 * The UI uses this phase to:
 * - Know when to accept player input (only during Idle)
 * - Know which animations to play
 * - Know when to show the game-over or level-complete screen
 */
enum class GamePhase {
    /** Waiting for the player to swipe. Input is accepted. */
    Idle,

    /** Animating the swap of two gems. Input is blocked. */
    Swapping,

    /** Match detection found matches — animating the match highlight/clear. */
    Matching,

    /** Gravity is pulling gems down, new gems are filling from the top. */
    Cascading,

    /** The board has settled after a cascade. Transitioning back to Idle. */
    Settled,

    /** Bonus moves phase: consuming remaining moves after non-score objective completion.
     *  Each remaining move auto-destroys a random gem for bonus points —
     *  the classic "level complete fireworks" reward for finishing early. */
    BonusMoves,

    /** The player ran out of moves without reaching the target score. */
    GameOver,

    /** The player reached the target score. */
    LevelComplete
}
