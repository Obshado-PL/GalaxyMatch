package com.galaxymatch.game.ui.dailychallenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxymatch.game.ServiceLocator
import com.galaxymatch.game.model.DailyChallengeState
import com.galaxymatch.game.model.LevelConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel for the daily challenge screen.
 *
 * Loads the daily challenge state (streak, completion, best score)
 * and generates a preview of today's challenge level.
 */
class DailyChallengeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DailyChallengeUiState())
    val uiState: StateFlow<DailyChallengeUiState> = _uiState.asStateFlow()

    private val repo = ServiceLocator.dailyChallengeRepository

    init {
        loadState()
    }

    private fun loadState() {
        viewModelScope.launch {
            repo.getState().collect { state ->
                val todayLevel = repo.generateTodayLevel()
                _uiState.value = DailyChallengeUiState(
                    challengeState = state,
                    todayLevel = todayLevel,
                    isLoaded = true
                )
            }
        }
    }
}

/**
 * UI state for the daily challenge screen.
 */
data class DailyChallengeUiState(
    val challengeState: DailyChallengeState = DailyChallengeState(),
    val todayLevel: LevelConfig? = null,
    val isLoaded: Boolean = false
)
