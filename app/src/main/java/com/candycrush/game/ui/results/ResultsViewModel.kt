package com.candycrush.game.ui.results

import com.candycrush.game.ServiceLocator

/**
 * Simple helper for the results screen.
 *
 * Checks if there's a next level available so the UI can show
 * or hide the "Next Level" button.
 */
object ResultsHelper {
    /**
     * Check if a next level exists.
     *
     * @param currentLevel The level that was just completed
     * @return True if there's a level after this one
     */
    fun hasNextLevel(currentLevel: Int): Boolean {
        return ServiceLocator.levelRepository.getLevel(currentLevel + 1) != null
    }
}
