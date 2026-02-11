package com.candycrush.game.ui.components

import androidx.compose.ui.graphics.Color
import com.candycrush.game.model.CandyType

/**
 * Maps candy types to their display colors.
 *
 * This is a utility used by CandyDrawer to get the correct color
 * for each candy type. Having it in a separate file makes it easy
 * to change the color scheme.
 */
fun CandyType.toColor(): Color = this.color

/**
 * Get a darker version of a candy color (used for special candy indicators).
 */
fun CandyType.toDarkColor(): Color = this.color.copy(
    red = this.color.red * 0.6f,
    green = this.color.green * 0.6f,
    blue = this.color.blue * 0.6f
)

/**
 * Get a lighter version of a candy color (used for highlight effects).
 */
fun CandyType.toLightColor(): Color = this.color.copy(
    red = (this.color.red + (1f - this.color.red) * 0.4f),
    green = (this.color.green + (1f - this.color.green) * 0.4f),
    blue = (this.color.blue + (1f - this.color.blue) * 0.4f)
)
