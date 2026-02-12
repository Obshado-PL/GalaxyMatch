package com.galaxymatch.game

import android.app.Application
import com.galaxymatch.game.audio.SoundManager
import com.galaxymatch.game.haptic.HapticManager
import com.galaxymatch.game.data.LevelDataSource
import com.galaxymatch.game.data.LevelRepository
import com.galaxymatch.game.data.ProgressDataStore
import com.galaxymatch.game.data.ProgressRepository
import com.galaxymatch.game.data.SettingsDataStore
import com.galaxymatch.game.data.SettingsRepository
import com.galaxymatch.game.data.StatisticsDataStore
import com.galaxymatch.game.data.StatisticsRepository

/**
 * Application class for Galaxy Match.
 *
 * This is the first thing that runs when the app starts.
 * We use it to initialize our services (repositories, sound manager, etc.)
 * using a simple ServiceLocator pattern instead of a dependency injection framework.
 */
class GalaxyMatchApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize the ServiceLocator so all parts of the app
        // can access shared services
        ServiceLocator.initialize(this)
    }
}

/**
 * Simple service locator that holds references to shared services.
 *
 * This is a beginner-friendly alternative to dependency injection frameworks
 * like Hilt or Dagger. It provides the same benefit (shared instances) without
 * the complexity of annotations and modules.
 *
 * Usage: ServiceLocator.levelRepository.getLevel(1)
 */
object ServiceLocator {
    lateinit var levelRepository: LevelRepository
        private set
    lateinit var progressRepository: ProgressRepository
        private set
    lateinit var soundManager: SoundManager
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var hapticManager: HapticManager
        private set
    lateinit var statisticsRepository: StatisticsRepository
        private set

    fun initialize(context: Application) {
        levelRepository = LevelRepository(LevelDataSource)
        progressRepository = ProgressRepository(ProgressDataStore(context))
        soundManager = SoundManager(context)
        settingsRepository = SettingsRepository(SettingsDataStore(context))
        hapticManager = HapticManager(context)
        statisticsRepository = StatisticsRepository(StatisticsDataStore(context))
    }
}
