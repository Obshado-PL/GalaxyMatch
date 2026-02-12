package com.galaxymatch.game.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxymatch.game.ServiceLocator
import com.galaxymatch.game.model.GemType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for the Statistics screen.
 *
 * Combines data from two sources:
 * - [StatisticsRepository]: aggregate counters (games played, gems matched, etc.)
 * - [ProgressRepository]: per-level data used to derive levels completed, total stars, best score
 */
class StatisticsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private val statsRepo = ServiceLocator.statisticsRepository
    private val progressRepo = ServiceLocator.progressRepository

    init {
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            combine(
                statsRepo.getStatistics(),
                progressRepo.getProgress()
            ) { stats, progress ->
                StatisticsUiState(
                    totalGamesPlayed = stats.totalGamesPlayed,
                    totalGemsMatched = stats.totalGemsMatched,
                    bestCombo = stats.bestCombo,
                    totalScore = stats.totalScore,
                    favoriteGemColor = stats.favoriteGemColor,
                    specialGemsCreated = stats.specialGemsCreated,
                    powerUpsUsed = stats.powerUpsUsed,
                    levelsCompleted = progress.levelStars.size,
                    totalStars = progress.levelStars.values.sum(),
                    bestLevelScore = progress.levelScores.values.maxOrNull() ?: 0,
                    isLoaded = true
                )
            }.collect { _uiState.value = it }
        }
    }
}

data class StatisticsUiState(
    val totalGamesPlayed: Int = 0,
    val totalGemsMatched: Int = 0,
    val bestCombo: Int = 0,
    val totalScore: Long = 0,
    val favoriteGemColor: GemType? = null,
    val specialGemsCreated: Int = 0,
    val powerUpsUsed: Int = 0,
    val levelsCompleted: Int = 0,
    val totalStars: Int = 0,
    val bestLevelScore: Int = 0,
    val isLoaded: Boolean = false
)
