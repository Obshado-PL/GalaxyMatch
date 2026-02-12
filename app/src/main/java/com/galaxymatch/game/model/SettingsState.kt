package com.galaxymatch.game.model

/**
 * Represents the player's app settings.
 *
 * These are persisted across app restarts using DataStore.
 * All settings have sensible defaults (everything enabled, tutorial not seen).
 *
 * @param sfxMuted Whether sound effects are muted
 * @param musicMuted Whether background music is muted
 * @param tutorialSeen Whether the player has dismissed the tutorial overlay
 */
data class SettingsState(
    val sfxMuted: Boolean = false,
    val musicMuted: Boolean = false,
    val tutorialSeen: Boolean = false
)
