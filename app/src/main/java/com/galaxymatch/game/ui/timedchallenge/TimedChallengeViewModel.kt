package com.galaxymatch.game.ui.timedchallenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxymatch.game.ServiceLocator
import com.galaxymatch.game.model.TimedChallengeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the timed challenge selection screen.
 * Loads best scores for all three difficulty tiers.
 */
class TimedChallengeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TimedChallengeUiState())
    val uiState: StateFlow<TimedChallengeUiState> = _uiState.asStateFlow()

    private val repo = ServiceLocator.timedChallengeRepository

    init {
        loadState()
    }

    private fun loadState() {
        viewModelScope.launch {
            repo.getState().collect { state ->
                _uiState.value = TimedChallengeUiState(
                    challengeState = state,
                    isLoaded = true
                )
            }
        }
    }
}

data class TimedChallengeUiState(
    val challengeState: TimedChallengeState = TimedChallengeState(),
    val isLoaded: Boolean = false
)
