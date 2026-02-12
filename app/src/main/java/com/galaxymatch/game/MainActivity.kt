package com.galaxymatch.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.galaxymatch.game.navigation.AppNavigation
import com.galaxymatch.game.ui.theme.GalaxyMatchTheme

/**
 * The single Activity for the entire app.
 *
 * In modern Android development with Jetpack Compose, we use a single Activity
 * and handle all screen navigation within Compose using Navigation Compose.
 * This Activity just sets up the Compose content and applies our custom theme.
 *
 * It also manages the sound lifecycle:
 * - Pauses background music when the app goes to background (onPause)
 * - Resumes background music when the app comes back (onResume)
 * - Releases all audio resources when the Activity is destroyed (onDestroy)
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge display (content extends behind status bar)
        enableEdgeToEdge()
        // Set the Compose UI content
        setContent {
            GalaxyMatchTheme {
                AppNavigation()
            }
        }
    }

    /**
     * Called when the app goes to the background (user presses home, switches apps, etc.)
     * We pause the background music so it doesn't keep playing when the user isn't looking.
     */
    override fun onPause() {
        super.onPause()
        ServiceLocator.soundManager.pauseBackgroundMusic()
    }

    /**
     * Called when the app comes back to the foreground.
     * We resume the background music if it was playing before.
     */
    override fun onResume() {
        super.onResume()
        ServiceLocator.soundManager.resumeBackgroundMusic()
    }

    /**
     * Called when the Activity is being destroyed.
     * We release all audio resources to prevent memory leaks.
     * This is important because SoundPool and MediaPlayer hold native resources.
     */
    override fun onDestroy() {
        super.onDestroy()
        ServiceLocator.soundManager.release()
    }
}
