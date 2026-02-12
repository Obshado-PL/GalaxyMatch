package com.galaxymatch.game.model

/**
 * Aggregate statistics tracked across all gameplay sessions.
 *
 * These are cumulative counters that only go up (except on reset).
 * Statistics that can be derived from [PlayerProgress] (levels completed,
 * total stars, best score) are NOT stored here — they're computed at display time.
 */
data class StatisticsState(
    val totalGamesPlayed: Int = 0,
    val totalGemsMatched: Int = 0,
    val bestCombo: Int = 0,
    val totalScore: Long = 0,
    val specialGemsCreated: Int = 0,
    val powerUpsUsed: Int = 0,
    val gemColorCounts: Map<GemType, Int> = emptyMap()
) {
    /** The gem color the player has matched the most. Null if no games played yet. */
    val favoriteGemColor: GemType?
        get() = gemColorCounts.maxByOrNull { it.value }?.key
}
