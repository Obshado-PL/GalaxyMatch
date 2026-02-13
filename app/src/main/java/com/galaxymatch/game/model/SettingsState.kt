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
 * @param hapticMuted Whether haptic (vibration) feedback is muted
 * @param colorblindMode Whether colorblind shape overlays are shown on gems
 * @param fontSizeLevel Font size preference: 0=Small, 1=Normal (default), 2=Large
 * @param highContrastMode Whether gems use brighter, more saturated colors for visibility
 */
data class SettingsState(
    val sfxMuted: Boolean = false,
    val musicMuted: Boolean = false,
    val tutorialSeen: Boolean = false,
    val hapticMuted: Boolean = false,
    val colorblindMode: Boolean = false,
    val fontSizeLevel: Int = 1,
    val highContrastMode: Boolean = false
)
