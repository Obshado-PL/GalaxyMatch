package com.galaxymatch.game.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxymatch.game.ServiceLocator
import com.galaxymatch.game.data.AchievementDefinitions
import com.galaxymatch.game.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the achievements screen.
 *
 * Combines achievement definitions, unlock state, and player stats
 * to produce display items with progress text for locked achievements.
 */
class AchievementsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    private val achievementRepo = ServiceLocator.achievementRepository
    private val statsRepo = ServiceLocator.statisticsRepository
    private val progressRepo = ServiceLocator.progressRepository
    private val dailyRepo = ServiceLocator.dailyChallengeRepository

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Combine all data sources into a single flow
            combine(
                achievementRepo.getUnlockedIds(),
                statsRepo.getStatistics(),
                progressRepo.getProgress(),
                dailyRepo.getState()
            ) { unlockedIds, stats, progress, dailyState ->
                val items = AchievementDefinitions.all.map { def ->
                    val isUnlocked = def.id in unlockedIds
                    val (current, target) = achievementRepo.getProgress(
                        def.criteria, stats, progress, dailyState
                    )
                    AchievementDisplayItem(
                        definition = def,
                        isUnlocked = isUnlocked,
                        currentProgress = current,
                        targetProgress = target
                    )
                }
                AchievementsUiState(
                    items = items,
                    unlockedCount = items.count { it.isUnlocked },
                    totalCount = items.size,
                    isLoaded = true
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}

/**
 * UI state for the achievements screen.
 */
data class AchievementsUiState(
    val items: List<AchievementDisplayItem> = emptyList(),
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val isLoaded: Boolean = false
)

/**
 * A single achievement ready for display.
 */
data class AchievementDisplayItem(
    val definition: AchievementDefinition,
    val isUnlocked: Boolean,
    val currentProgress: Int,
    val targetProgress: Int
) {
    val progressText: String
        get() = if (isUnlocked) "Unlocked!" else "$currentProgress/$targetProgress"
}
