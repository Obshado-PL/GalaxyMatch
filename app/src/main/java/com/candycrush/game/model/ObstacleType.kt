package com.candycrush.game.model

/**
 * Types of obstacles that can appear on the game board.
 *
 * Obstacles add strategic variety to levels by changing how the board
 * behaves. They're defined per-level in LevelConfig and stored separately
 * from the candy grid (as a Map<Position, ObstacleType> on BoardState).
 *
 * - **Ice**: A breakable overlay on top of a normal candy. The candy
 *           underneath participates in matches normally. When the candy
 *           is matched (or hit by Hammer), the ice breaks and the candy
 *           is cleared. Ice is a 1-hit obstacle.
 *
 * - **Stone**: A permanent, indestructible wall. No candy can exist at
 *            a stone position. Candies can't be swapped with stones.
 *            Gravity flows around stones — each column is split into
 *            independent segments above and below stones.
 */
enum class ObstacleType {
    /** Candy underneath, breaks when the candy is matched (1 hit). */
    Ice,

    /** Permanent wall — no candy, no swap, gravity flows around it. */
    Stone
}
