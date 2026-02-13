package com.galaxymatch.game.haptic

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Manages haptic feedback for game events.
 *
 * Mirrors the SoundManager pattern — each game event has a corresponding
 * vibration method with intensity that scales based on gameplay context
 * (match size, combo level, etc.).
 *
 * Uses Android's Vibrator API with VibrationEffect (API 26+) for
 * amplitude control. On older devices, falls back to simple duration-based
 * vibration without amplitude.
 *
 * Usage:
 *   val hapticManager = HapticManager(context)
 *   hapticManager.vibrateMatch(4)      // Medium haptic for match-4
 *   hapticManager.vibrateCascade(3)    // Strong cascade haptic
 *   hapticManager.isHapticMuted = true // Disable all haptics
 */
class HapticManager(context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /** When true, all haptic feedback is suppressed. */
    var isHapticMuted: Boolean = false

    // ===== Game Event Haptics =====

    /** Light tap when two gems are swapped. */
    fun vibrateSwap() {
        vibrate(40L, 80)
    }

    /**
     * Haptic feedback when gems are matched and cleared.
     * Intensity scales with match size (mirrors SoundManager.playMatch).
     */
    fun vibrateMatch(matchSize: Int = 3) {
        val duration = when {
            matchSize >= 5 -> 100L
            matchSize >= 4 -> 75L
            else -> 50L
        }
        val amplitude = when {
            matchSize >= 5 -> 200
            matchSize >= 4 -> 150
            else -> 100
        }
        vibrate(duration, amplitude)
    }

    /**
     * Haptic feedback during cascade/gravity.
     * Intensity escalates with combo level (mirrors SoundManager.playCascade).
     */
    fun vibrateCascade(comboLevel: Int = 0) {
        val duration = (40L + comboLevel * 15L).coerceAtMost(120L)
        val amplitude = (80 + comboLevel * 25).coerceAtMost(255)
        vibrate(duration, amplitude)
    }

    /** Strong distinctive pulse when a special gem activates. */
    fun vibrateSpecialActivation() {
        vibrate(80L, 180)
    }

    /** Sharp crisp tap when ice shatters. */
    fun vibrateIceBreak() {
        vibrate(50L, 120)
    }

    /** Heavier pulse when reinforced ice is cracked (first hit — tougher obstacle). */
    fun vibrateReinforcedIceHit() {
        vibrate(80L, 180)
    }

    /** Quick urgent tick when a bomb timer is running low (≤2 moves). */
    fun vibrateBombTick() {
        vibrate(30L, 100)
    }

    /** Strong explosive burst when a bomb detonates (game over trigger). */
    fun vibrateBombExplode() {
        vibrate(150L, 255)
    }

    /** Short rumble pattern when the board shuffles. */
    fun vibrateShuffle() {
        if (isHapticMuted || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 40, 60, 40, 60, 40)
            val amplitudes = intArrayOf(0, 100, 0, 120, 0, 80)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(300L)
        }
    }

    /** Success burst pattern when the level is completed. */
    fun vibrateLevelComplete() {
        if (isHapticMuted || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 60, 80, 80)
            val amplitudes = intArrayOf(0, 120, 0, 200)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(250L)
        }
    }

    /** Soft down-beat pulse on game over. */
    fun vibrateGameOver() {
        vibrate(120L, 150)
    }

    /** Ultra-light tap for UI button presses — barely perceptible but satisfying. */
    fun vibrateButtonTap() {
        vibrate(20L, 50)
    }

    /** Medium celebratory pulse when a star is earned mid-game. */
    fun vibrateStarEarned() {
        vibrate(60L, 140)
    }

    // ===== Internal Helper =====

    private fun vibrate(duration: Long, amplitude: Int) {
        if (isHapticMuted || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(duration, amplitude.coerceIn(1, 255))
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }
}
