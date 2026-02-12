package com.galaxymatch.game.data

import com.galaxymatch.game.model.*

/**
 * All 30 achievement definitions.
 *
 * Organized into 4 categories:
 * - Milestone (10): Game count, score, gem count thresholds
 * - Skill (8): Combos, special gems, power-ups
 * - Collection (6): Specific gem colors, perfect ratings
 * - Progression (6): Level completion, daily challenges
 */
object AchievementDefinitions {

    val all: List<AchievementDefinition> = listOf(

        // ===== MILESTONE (10) =====
        AchievementDefinition("first_game", "First Steps", "Play your first game", "\uD83D\uDC76", AchievementCategory.Milestone, AchievementCriteria.GamesPlayed(1)),
        AchievementDefinition("games_10", "Getting Hooked", "Play 10 games", "\uD83C\uDFAE", AchievementCategory.Milestone, AchievementCriteria.GamesPlayed(10)),
        AchievementDefinition("games_50", "Dedicated Player", "Play 50 games", "\uD83C\uDFC5", AchievementCategory.Milestone, AchievementCriteria.GamesPlayed(50)),
        AchievementDefinition("games_100", "Galaxy Veteran", "Play 100 games", "\uD83C\uDF1F", AchievementCategory.Milestone, AchievementCriteria.GamesPlayed(100)),
        AchievementDefinition("gems_100", "Gem Collector", "Match 100 gems", "\uD83D\uDC8E", AchievementCategory.Milestone, AchievementCriteria.GemsMatched(100)),
        AchievementDefinition("gems_500", "Gem Hoarder", "Match 500 gems", "\uD83D\uDCA0", AchievementCategory.Milestone, AchievementCriteria.GemsMatched(500)),
        AchievementDefinition("gems_2000", "Gem Master", "Match 2,000 gems", "\u2728", AchievementCategory.Milestone, AchievementCriteria.GemsMatched(2000)),
        AchievementDefinition("score_10k", "Scoring Streak", "Earn 10,000 total score", "\uD83D\uDCB0", AchievementCategory.Milestone, AchievementCriteria.TotalScore(10000)),
        AchievementDefinition("score_100k", "High Roller", "Earn 100,000 total score", "\uD83D\uDCB5", AchievementCategory.Milestone, AchievementCriteria.TotalScore(100000)),
        AchievementDefinition("score_1m", "Millionaire", "Earn 1,000,000 total score", "\uD83D\uDCB8", AchievementCategory.Milestone, AchievementCriteria.TotalScore(1000000)),

        // ===== SKILL (8) =====
        AchievementDefinition("combo_3", "Chain Reaction", "Reach a 3x combo", "\u26A1", AchievementCategory.Skill, AchievementCriteria.ComboReached(3)),
        AchievementDefinition("combo_5", "Combo King", "Reach a 5x combo", "\uD83D\uDD25", AchievementCategory.Skill, AchievementCriteria.ComboReached(5)),
        AchievementDefinition("combo_8", "Cascade Master", "Reach an 8x combo", "\uD83C\uDF0A", AchievementCategory.Skill, AchievementCriteria.ComboReached(8)),
        AchievementDefinition("specials_10", "Special Agent", "Create 10 special gems", "\u2B50", AchievementCategory.Skill, AchievementCriteria.SpecialGemsCreated(10)),
        AchievementDefinition("specials_50", "Gem Engineer", "Create 50 special gems", "\uD83D\uDD27", AchievementCategory.Skill, AchievementCriteria.SpecialGemsCreated(50)),
        AchievementDefinition("specials_200", "Special Forces", "Create 200 special gems", "\uD83D\uDE80", AchievementCategory.Skill, AchievementCriteria.SpecialGemsCreated(200)),
        AchievementDefinition("powerups_5", "Power Player", "Use 5 power-ups", "\uD83D\uDCA5", AchievementCategory.Skill, AchievementCriteria.PowerUpsUsed(5)),
        AchievementDefinition("powerups_25", "Arsenal Master", "Use 25 power-ups", "\uD83C\uDF86", AchievementCategory.Skill, AchievementCriteria.PowerUpsUsed(25)),

        // ===== COLLECTION (6) =====
        AchievementDefinition("perfect_1", "Perfectionist", "Get 3 stars on any level", "\u2B50", AchievementCategory.Collection, AchievementCriteria.PerfectLevels(1)),
        AchievementDefinition("perfect_5", "Star Collector", "Get 3 stars on 5 levels", "\uD83C\uDF1F", AchievementCategory.Collection, AchievementCriteria.PerfectLevels(5)),
        AchievementDefinition("perfect_10", "Constellation", "Get 3 stars on 10 levels", "\u2728", AchievementCategory.Collection, AchievementCriteria.PerfectLevels(10)),
        AchievementDefinition("perfect_20", "Galaxy of Stars", "Get 3 stars on 20 levels", "\uD83C\uDF0C", AchievementCategory.Collection, AchievementCriteria.PerfectLevels(20)),
        AchievementDefinition("gems_5000", "Gem Tycoon", "Match 5,000 gems total", "\uD83D\uDC8E", AchievementCategory.Collection, AchievementCriteria.GemsMatched(5000)),
        AchievementDefinition("gems_10000", "Gem Emperor", "Match 10,000 gems total", "\uD83D\uDC51", AchievementCategory.Collection, AchievementCriteria.GemsMatched(10000)),

        // ===== PROGRESSION (6) =====
        AchievementDefinition("levels_5", "Explorer", "Complete 5 levels", "\uD83D\uDDFA", AchievementCategory.Progression, AchievementCriteria.LevelsCompleted(5)),
        AchievementDefinition("levels_10", "Adventurer", "Complete 10 levels", "\u26F5", AchievementCategory.Progression, AchievementCriteria.LevelsCompleted(10)),
        AchievementDefinition("levels_20", "Trailblazer", "Complete 20 levels", "\uD83E\uDDED", AchievementCategory.Progression, AchievementCriteria.LevelsCompleted(20)),
        AchievementDefinition("daily_3", "Daily Regular", "Complete 3 daily challenges", "\uD83D\uDCC5", AchievementCategory.Progression, AchievementCriteria.DailyChallengesCompleted(3)),
        AchievementDefinition("daily_streak_3", "On a Roll", "Maintain a 3-day daily streak", "\uD83D\uDD25", AchievementCategory.Progression, AchievementCriteria.DailyChallengeStreak(3)),
        AchievementDefinition("daily_streak_7", "Week Warrior", "Maintain a 7-day daily streak", "\uD83D\uDCAA", AchievementCategory.Progression, AchievementCriteria.DailyChallengeStreak(7))
    )
}
