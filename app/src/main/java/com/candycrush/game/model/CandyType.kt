package com.candycrush.game.model

import androidx.compose.ui.graphics.Color
import com.candycrush.game.ui.theme.*

/**
 * The 6 possible candy colors in the game.
 *
 * Not all colors are used in every level — easier levels use fewer colors
 * (4 types = more matches), while harder levels use all 6 (fewer matches).
 *
 * Each candy type has an associated display color for rendering on the Canvas.
 */
enum class CandyType(val color: Color) {
    Red(CandyRed),
    Blue(CandyBlue),
    Green(CandyGreen),
    Yellow(CandyYellow),
    Orange(CandyOrange),
    Purple(CandyPurple);

    companion object {
        /**
         * Get a subset of candy types for a level.
         * @param count How many types to use (3-6). Fewer = easier.
         * @return The first [count] candy types.
         */
        fun forLevel(count: Int): List<CandyType> {
            return entries.take(count.coerceIn(3, entries.size))
        }
    }
}
