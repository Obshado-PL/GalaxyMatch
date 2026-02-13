package com.galaxymatch.game.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * CompositionLocal for font size scaling.
 *
 * Provides a scale factor that multiplies all `sp` values throughout the app.
 * The scale is set based on the player's font size preference:
 * - 0 (Small) → 0.85f — smaller text for more screen space
 * - 1 (Normal) → 1.0f — default text size
 * - 2 (Large) → 1.2f — larger text for readability
 *
 * This works by overriding LocalDensity's fontScale, which affects how `sp`
 * units are converted to pixels. All Text composables using `sp` are affected.
 */
val LocalFontScale = compositionLocalOf { 1.0f }

/**
 * Convert a font size level (0=Small, 1=Normal, 2=Large) to a scale factor.
 */
fun fontSizeLevelToScale(level: Int): Float = when (level) {
    0 -> 0.85f  // Small
    2 -> 1.2f   // Large
    else -> 1.0f // Normal (default)
}

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
 *
 * @param fontSizeLevel Font size preference: 0=Small, 1=Normal, 2=Large.
 *   Scales all `sp` text sizes via LocalDensity fontScale override.
 */
@Composable
fun GalaxyMatchTheme(
    fontSizeLevel: Int = 1,
    content: @Composable () -> Unit
) {
    // Calculate the font scale multiplier from the preference level
    val fontScale = fontSizeLevelToScale(fontSizeLevel)

    // Override the system density's fontScale so that all sp values
    // are multiplied by our custom scale factor. This is the cleanest
    // way to implement app-wide font scaling without modifying every
    // Text composable individually.
    val currentDensity = LocalDensity.current
    val scaledDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * fontScale
    )

    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalFontScale provides fontScale
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
