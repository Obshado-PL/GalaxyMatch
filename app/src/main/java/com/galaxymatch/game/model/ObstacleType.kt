package com.galaxymatch.game.model

/**
 * Types of obstacles that can appear on the game board.
 *
 * Obstacles add strategic variety to levels by changing how the board
 * behaves. They're defined per-level in LevelConfig and stored separately
 * from the gem grid (as a Map<Position, ObstacleType> on BoardState).
 *
 * - **Ice**: A breakable overlay on top of a normal gem. The gem
 *           underneath participates in matches normally. When the gem
 *           is matched (or hit by Hammer), the ice breaks and the gem
 *           is cleared. Ice is a 1-hit obstacle.
 *
 * - **Stone**: A permanent, indestructible wall. No gem can exist at
 *            a stone position. Gems can't be swapped with stones.
 *            Gravity flows around stones — each column is split into
 *            independent segments above and below stones.
 */
enum class ObstacleType {
    /** Gem underneath, breaks when the gem is matched (1 hit). */
    Ice,

    /** 2-hit ice — downgrades to Ice on first hit, then breaks normally. */
    ReinforcedIce,

    /** Gem underneath, cannot be swapped. Freed when an adjacent gem is matched. */
    Locked,

    /** Permanent wall — no gem, no swap, gravity flows around it. */
    Stone
}
