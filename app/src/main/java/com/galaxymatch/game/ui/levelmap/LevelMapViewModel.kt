package com.galaxymatch.game.ui.levelmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxymatch.game.ServiceLocator
import com.galaxymatch.game.model.LevelConfig
import com.galaxymatch.game.model.PlayerProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the level map screen.
 *
 * Loads the list of levels and the player's progress (stars, unlocked levels)
 * and provides it to the UI as a StateFlow.
 */
class LevelMapViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LevelMapUiState())
    val uiState: StateFlow<LevelMapUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val repo = ServiceLocator.levelRepository

        // Observe player progress (updates automatically when progress changes)
        viewModelScope.launch {
            ServiceLocator.progressRepository.getProgress().collect { progress ->
                // Build the full level list:
                // 1. All 20 handcrafted levels (always shown)
                // 2. Generated levels up to the highest unlocked level (dynamic)
                val handcraftedLevels = repo.getAllLevels()
                val handcraftedCount = repo.getHandcraftedLevelCount()
                val highestUnlocked = progress.highestUnlockedLevel

                val allLevels = if (highestUnlocked > handcraftedCount) {
                    // Player has progressed beyond handcrafted levels —
                    // generate configs for the procedural levels they've unlocked
                    handcraftedLevels + repo.getLevelsInRange(
                        handcraftedCount + 1,
                        highestUnlocked
                    )
                } else {
                    handcraftedLevels
                }

                _uiState.value = LevelMapUiState(
                    levels = allLevels,
                    progress = progress,
                    isLoaded = true
                )
            }
        }
    }
}

/**
 * UI state for the level map screen.
 */
data class LevelMapUiState(
    val levels: List<LevelConfig> = emptyList(),
    val progress: PlayerProgress = PlayerProgress(),
    val isLoaded: Boolean = false
)
