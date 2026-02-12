package com.candycrush.game.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.candycrush.game.ServiceLocator
import com.candycrush.game.ui.theme.GameBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * Splash screen shown when the app first launches.
 *
 * Displays the game title with a fade-in and scale animation,
 * then automatically navigates to the level map after a short delay.
 *
 * @param onSplashComplete Called when the splash animation is done
 *                         and the app should navigate to the level map.
 */
@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    // Fade-in and scale animation
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Animate from 0 to 1 over 800ms
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )
        // Load saved sound settings before starting music
        // so the player's mute preferences are respected on app launch
        val settings = ServiceLocator.settingsRepository.getSettings().first()
        ServiceLocator.soundManager.isSfxMuted = settings.sfxMuted
        ServiceLocator.soundManager.isMusicMuted = settings.musicMuted

        // Start background music (SoundManager will respect the mute flag)
        if (!settings.musicMuted) {
            ServiceLocator.soundManager.startBackgroundMusic()
        }

        // Hold for a moment
        delay(600)
        // Navigate to level map
        onSplashComplete()
    }

    val progress by animatable.asState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(progress)
                .scale(0.5f + progress * 0.5f) // Scale from 0.5 to 1.0
        ) {
            // Game title
            Text(
                text = "Candy",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color(0xFFFF6B9D), // Pink
                textAlign = TextAlign.Center
            )
            Text(
                text = "Crush",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color(0xFFFFD700), // Gold
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Match 3 Game",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
