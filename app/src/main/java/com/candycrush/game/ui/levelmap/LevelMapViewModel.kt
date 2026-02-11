package com.candycrush.game.ui.levelmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.candycrush.game.ServiceLocator
import com.candycrush.game.model.LevelConfig
import com.candycrush.game.model.PlayerProgress
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
        val levels = ServiceLocator.levelRepository.getAllLevels()

        // Observe player progress (updates automatically when progress changes)
        viewModelScope.launch {
            ServiceLocator.progressRepository.getProgress().collect { progress ->
                _uiState.value = LevelMapUiState(
                    levels = levels,
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
