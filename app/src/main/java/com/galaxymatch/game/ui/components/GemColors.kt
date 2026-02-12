package com.galaxymatch.game.ui.components

import androidx.compose.ui.graphics.Color
import com.galaxymatch.game.model.GemType

/**
 * Maps gem types to their display colors.
 *
 * This is a utility used by GemDrawer to get the correct color
 * for each gem type. Having it in a separate file makes it easy
 * to change the color scheme.
 */
fun GemType.toColor(): Color = this.color

/**
 * Get a darker version of a gem color (used for special gem indicators).
 */
fun GemType.toDarkColor(): Color = this.color.copy(
    red = this.color.red * 0.6f,
    green = this.color.green * 0.6f,
    blue = this.color.blue * 0.6f
)

/**
 * Get a lighter version of a gem color (used for highlight effects).
 */
fun GemType.toLightColor(): Color = this.color.copy(
    red = (this.color.red + (1f - this.color.red) * 0.4f),
    green = (this.color.green + (1f - this.color.green) * 0.4f),
    blue = (this.color.blue + (1f - this.color.blue) * 0.4f)
)
