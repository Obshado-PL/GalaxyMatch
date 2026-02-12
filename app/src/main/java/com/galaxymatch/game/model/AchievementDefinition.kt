package com.galaxymatch.game.model

/**
 * Defines a single achievement that a player can unlock.
 *
 * @param id Unique identifier (used as DataStore key)
 * @param title Display title (e.g., "First Steps")
 * @param description How to unlock (e.g., "Complete your first level")
 * @param emoji Emoji icon for the achievement
 * @param category Which category this belongs to
 * @param criteria What must be met to unlock
 */
data class AchievementDefinition(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val category: AchievementCategory,
    val criteria: AchievementCriteria
)

/**
 * Achievement categories for organizing the display.
 */
enum class AchievementCategory(val label: String) {
    Milestone("Milestones"),
    Skill("Skill"),
    Collection("Collection"),
    Progression("Progression")
}

/**
 * Criteria for unlocking an achievement.
 *
 * Each type checks a different statistic or game state.
 * Sealed class allows exhaustive when expressions.
 */
sealed class AchievementCriteria {
    /** Played at least [threshold] games. */
    data class GamesPlayed(val threshold: Int) : AchievementCriteria()
    /** Matched at least [threshold] total gems. */
    data class GemsMatched(val threshold: Int) : AchievementCriteria()
    /** Reached a combo of at least [threshold] in a single level. */
    data class ComboReached(val threshold: Int) : AchievementCriteria()
    /** Accumulated at least [threshold] total score across all games. */
    data class TotalScore(val threshold: Long) : AchievementCriteria()
    /** Completed at least [threshold] levels. */
    data class LevelsCompleted(val threshold: Int) : AchievementCriteria()
    /** Got 3 stars on at least [count] levels. */
    data class PerfectLevels(val count: Int) : AchievementCriteria()
    /** Created at least [threshold] special gems (striped/wrapped/color bomb). */
    data class SpecialGemsCreated(val threshold: Int) : AchievementCriteria()
    /** Used at least [threshold] power-ups. */
    data class PowerUpsUsed(val threshold: Int) : AchievementCriteria()
    /** Maintained a daily challenge streak of [threshold] days. */
    data class DailyChallengeStreak(val threshold: Int) : AchievementCriteria()
    /** Completed at least [threshold] daily challenges. */
    data class DailyChallengesCompleted(val threshold: Int) : AchievementCriteria()
}
