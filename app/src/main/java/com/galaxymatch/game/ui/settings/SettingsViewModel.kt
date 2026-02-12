package com.galaxymatch.game.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxymatch.game.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Settings screen.
 *
 * Manages the state of sound toggles and the progress reset feature.
 * When the player toggles a setting, it:
 * 1. Updates the SoundManager immediately (so the change takes effect)
 * 2. Saves to DataStore (so it persists across app restarts)
 */
class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val settingsRepo = ServiceLocator.settingsRepository
    private val progressRepo = ServiceLocator.progressRepository
    private val soundManager = ServiceLocator.soundManager

    init {
        loadSettings()
    }

    /**
     * Load saved settings from DataStore.
     * Collects the first emission from the settings Flow.
     */
    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepo.getSettings().collect { settings ->
                _uiState.update {
                    it.copy(
                        sfxMuted = settings.sfxMuted,
                        musicMuted = settings.musicMuted,
                        isLoaded = true
                    )
                }
            }
        }
    }

    /**
     * Toggle the sound effects on/off.
     * Updates SoundManager immediately and saves to disk.
     */
    fun toggleSfx() {
        val newValue = !_uiState.value.sfxMuted
        _uiState.update { it.copy(sfxMuted = newValue) }
        soundManager.isSfxMuted = newValue
        viewModelScope.launch {
            settingsRepo.saveSfxMuted(newValue)
        }
    }

    /**
     * Toggle the background music on/off.
     * Starts or stops music immediately and saves to disk.
     */
    fun toggleMusic() {
        val newValue = !_uiState.value.musicMuted
        _uiState.update { it.copy(musicMuted = newValue) }
        soundManager.isMusicMuted = newValue
        if (newValue) {
            soundManager.stopBackgroundMusic()
        } else {
            soundManager.startBackgroundMusic()
        }
        viewModelScope.launch {
            settingsRepo.saveMusicMuted(newValue)
        }
    }

    /** Show the reset progress confirmation dialog. */
    fun onResetProgressClicked() {
        _uiState.update { it.copy(showResetDialog = true) }
    }

    /** Dismiss the reset dialog without resetting. */
    fun onResetProgressDismissed() {
        _uiState.update { it.copy(showResetDialog = false) }
    }

    /**
     * Reset all progress AND settings.
     * The player starts completely fresh — only level 1 unlocked,
     * no stars, tutorial not seen, sound settings back to defaults.
     */
    fun onResetProgressConfirmed() {
        _uiState.update { it.copy(showResetDialog = false) }
        viewModelScope.launch {
            progressRepo.clearAllProgress()
            settingsRepo.clearAll()
            // Restore default sound settings
            soundManager.isSfxMuted = false
            soundManager.isMusicMuted = false
            soundManager.startBackgroundMusic()
        }
    }
}

/**
 * UI state for the Settings screen.
 */
data class SettingsUiState(
    val sfxMuted: Boolean = false,
    val musicMuted: Boolean = false,
    val showResetDialog: Boolean = false,
    val isLoaded: Boolean = false
)
