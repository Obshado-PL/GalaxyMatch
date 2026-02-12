package com.galaxymatch.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.galaxymatch.game.model.SettingsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Extension property to create a single DataStore instance for settings.
 *
 * This is separate from the progress DataStore ("player_progress") so that
 * resetting progress doesn't accidentally wipe settings, and vice versa.
 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "game_settings"
)

/**
 * Handles reading and writing game settings to persistent storage.
 *
 * Uses Android's DataStore (Preferences) — the same approach as ProgressDataStore
 * but for settings like sound mute states and tutorial completion.
 *
 * Settings are stored as boolean key-value pairs:
 * - "sfx_muted" -> Boolean (are sound effects muted?)
 * - "music_muted" -> Boolean (is background music muted?)
 * - "tutorial_seen" -> Boolean (has the player seen the tutorial?)
 */
class SettingsDataStore(private val context: Context) {

    companion object {
        private val SFX_MUTED_KEY = booleanPreferencesKey("sfx_muted")
        private val MUSIC_MUTED_KEY = booleanPreferencesKey("music_muted")
        private val TUTORIAL_SEEN_KEY = booleanPreferencesKey("tutorial_seen")
        private val HAPTIC_MUTED_KEY = booleanPreferencesKey("haptic_muted")
        private val COLORBLIND_MODE_KEY = booleanPreferencesKey("colorblind_mode")
    }

    /**
     * Get the current settings as a Flow.
     *
     * The Flow automatically emits new values whenever settings change,
     * so the UI stays in sync without manual refreshing.
     */
    fun getSettings(): Flow<SettingsState> {
        return context.settingsDataStore.data.map { prefs ->
            SettingsState(
                sfxMuted = prefs[SFX_MUTED_KEY] ?: false,
                musicMuted = prefs[MUSIC_MUTED_KEY] ?: false,
                tutorialSeen = prefs[TUTORIAL_SEEN_KEY] ?: false,
                hapticMuted = prefs[HAPTIC_MUTED_KEY] ?: false,
                colorblindMode = prefs[COLORBLIND_MODE_KEY] ?: false
            )
        }
    }

    /** Save whether sound effects are muted. */
    suspend fun saveSfxMuted(muted: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[SFX_MUTED_KEY] = muted
        }
    }

    /** Save whether background music is muted. */
    suspend fun saveMusicMuted(muted: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[MUSIC_MUTED_KEY] = muted
        }
    }

    /** Save whether the tutorial has been seen. */
    suspend fun saveTutorialSeen(seen: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[TUTORIAL_SEEN_KEY] = seen
        }
    }

    /** Save whether haptic feedback is muted. */
    suspend fun saveHapticMuted(muted: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[HAPTIC_MUTED_KEY] = muted
        }
    }

    /** Save whether colorblind mode is enabled. */
    suspend fun saveColorblindMode(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[COLORBLIND_MODE_KEY] = enabled
        }
    }

    /**
     * Clear all settings (reset to defaults).
     * Used when the player resets all progress.
     */
    suspend fun clearAll() {
        context.settingsDataStore.edit { it.clear() }
    }
}
