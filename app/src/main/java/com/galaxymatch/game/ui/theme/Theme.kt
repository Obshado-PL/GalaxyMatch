package com.galaxymatch.game.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Custom dark color scheme for the Galaxy Match game.
 * Games typically look best with dark themes so the colorful
 * game elements (gems, effects) stand out more.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = GameBackground,
    surface = BoardBackground,
    onPrimary = ScoreText,
    onSecondary = ScoreText,
    onBackground = ScoreText,
    onSurface = ScoreText
)

/**
 * The main theme composable for the Galaxy Match game.
 * Wrap your top-level content with this to apply consistent
 * colors, typography, and shapes throughout the app.
 */
@Composable
fun GalaxyMatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
