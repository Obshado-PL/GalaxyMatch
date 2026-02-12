package com.candycrush.game.data

import com.candycrush.game.model.SettingsState
import kotlinx.coroutines.flow.Flow

/**
 * Repository for game settings.
 *
 * This is a thin wrapper around SettingsDataStore that provides
 * a clean API for the rest of the app. It follows the same pattern
 * as ProgressRepository — keeping the data layer abstracted so the
 * storage mechanism could be changed without affecting the rest of the app.
 */
class SettingsRepository(private val dataStore: SettingsDataStore) {

    /** Get settings as a Flow that emits whenever settings change. */
    fun getSettings(): Flow<SettingsState> = dataStore.getSettings()

    /** Save whether sound effects are muted. */
    suspend fun saveSfxMuted(muted: Boolean) = dataStore.saveSfxMuted(muted)

    /** Save whether background music is muted. */
    suspend fun saveMusicMuted(muted: Boolean) = dataStore.saveMusicMuted(muted)

    /** Save whether the tutorial has been seen. */
    suspend fun saveTutorialSeen(seen: Boolean) = dataStore.saveTutorialSeen(seen)

    /** Clear all settings (reset to defaults). */
    suspend fun clearAll() = dataStore.clearAll()
}
