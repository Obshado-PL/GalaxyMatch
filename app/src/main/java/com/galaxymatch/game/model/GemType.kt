package com.galaxymatch.game.model

import androidx.compose.ui.graphics.Color
import com.galaxymatch.game.ui.theme.*

/**
 * The 6 possible gem colors in the game.
 *
 * Not all colors are used in every level — easier levels use fewer colors
 * (4 types = more matches), while harder levels use all 6 (fewer matches).
 *
 * Each gem type has an associated display color for rendering on the Canvas.
 */
enum class GemType(val color: Color) {
    Red(GemRed),
    Blue(GemBlue),
    Green(GemGreen),
    Yellow(GemYellow),
    Orange(GemOrange),
    Purple(GemPurple);

    companion object {
        /**
         * Get a subset of gem types for a level.
         * @param count How many types to use (3-6). Fewer = easier.
         * @return The first [count] gem types.
         */
        fun forLevel(count: Int): List<GemType> {
            return entries.take(count.coerceIn(3, entries.size))
        }
    }
}
