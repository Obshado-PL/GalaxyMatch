package com.galaxymatch.game.ui.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.galaxymatch.game.ui.components.GalaxyBackground
import com.galaxymatch.game.ui.theme.GameBackground

/**
 * Settings screen where the player can:
 * - Toggle sound effects on/off
 * - Toggle background music on/off
 * - Reset all progress (with confirmation)
 *
 * The settings are persisted across app restarts using DataStore.
 *
 * @param onBackToMap Called when the player wants to go back to the level map
 */
@Composable
fun SettingsScreen(onBackToMap: () -> Unit) {
    val viewModel = remember { SettingsViewModel() }
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
    ) {
        // Animated galaxy background (stars, comets, nebulae)
        GalaxyBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // === Title ===
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 32.dp)
        )

        // === Sound Effects Toggle ===
        SettingsToggleRow(
            label = "Sound Effects",
            isEnabled = !state.sfxMuted,
            onToggle = { viewModel.toggleSfx() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // === Background Music Toggle ===
        SettingsToggleRow(
            label = "Background Music",
            isEnabled = !state.musicMuted,
            onToggle = { viewModel.toggleMusic() }
        )

        Spacer(modifier = Modifier.height(48.dp))

        // === Reset Progress Button ===
        // Red/danger color to indicate this is a destructive action
        // Bounce animation: shrinks on press, springs back on release
        val resetInteraction = remember { MutableInteractionSource() }
        val resetPressed by resetInteraction.collectIsPressedAsState()
        val resetScale by animateFloatAsState(
            targetValue = if (resetPressed) 0.92f else 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "resetBounce"
        )

        Button(
            onClick = { viewModel.onResetProgressClicked() },
            interactionSource = resetInteraction,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFCC3333)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().scale(resetScale)
        ) {
            Text(
                text = "Reset All Progress",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Text(
            text = "This will erase all stars, scores, and unlocked levels.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // === Back to Map Button ===
        // Bounce animation on press
        val backInteraction = remember { MutableInteractionSource() }
        val backPressed by backInteraction.collectIsPressedAsState()
        val backScale by animateFloatAsState(
            targetValue = if (backPressed) 0.92f else 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "backBounce"
        )

        Button(
            onClick = onBackToMap,
            interactionSource = backInteraction,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .scale(backScale)
        ) {
            Text(
                text = "Back to Map",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }

    // === Reset Confirmation Dialog ===
    if (state.showResetDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onResetProgressDismissed() },
            title = {
                Text(
                    text = "Reset Progress?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("All your stars, scores, and unlocked levels will be erased. This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onResetProgressConfirmed() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFCC3333)
                    )
                ) {
                    Text("Reset", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { viewModel.onResetProgressDismissed() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    )
                ) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF2D2B55),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f)
        )
    }
    } // Box
}

/**
 * A row with a label and a toggle switch.
 *
 * @param label The text describing the setting
 * @param isEnabled Whether the setting is currently ON
 * @param onToggle Called when the player taps the toggle
 */
@Composable
private fun SettingsToggleRow(
    label: String,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color.White
        )
        Switch(
            checked = isEnabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF44BB44),
                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}
