package com.galaxymatch.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import kotlin.random.Random

/**
 * Manages all sound effects and background music for the game.
 *
 * Uses two Android audio systems:
 * - **SoundPool**: For short sound effects (match, swap, cascade, etc.)
 *   SoundPool is designed for low-latency playback of short clips and can
 *   play multiple sounds simultaneously (up to maxStreams).
 *
 * - **MediaPlayer**: For background music (longer, looping audio)
 *   MediaPlayer handles streaming and is better for longer audio files.
 *
 * **Sound files:** Place .ogg files in app/src/main/res/raw/ with these names:
 *   swap.ogg, match.ogg, cascade.ogg, special.ogg, win.ogg, lose.ogg, shuffle.ogg, ice_break.ogg, bgm.ogg
 *
 * The game works fine without these files — all loading is done with runtime
 * resource lookup, so missing files won't cause crashes or compile errors.
 * Add real sound files whenever you're ready!
 *
 * Usage:
 *   val soundManager = SoundManager(context)
 *   soundManager.playMatch()        // Play match sound
 *   soundManager.playCascade(2)     // Play cascade with combo pitch
 *   soundManager.startBackgroundMusic()
 *   soundManager.release()          // Call when done (Activity.onDestroy)
 */
class SoundManager(private val context: Context) {

    companion object {
        private const val TAG = "SoundManager"
        private const val MAX_STREAMS = 8  // Max simultaneous sound effects
    }

    // ===== SoundPool for short sound effects =====
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    // Sound effect IDs (loaded from res/raw/ files)
    // Each ID is 0 if the file doesn't exist or couldn't be loaded
    // We use runtime resource lookup so the project compiles even without sound files
    private val swapSoundId: Int = loadSoundByName("swap")
    private val matchSoundId: Int = loadSoundByName("match")
    private val cascadeSoundId: Int = loadSoundByName("cascade")
    private val specialSoundId: Int = loadSoundByName("special")
    private val winSoundId: Int = loadSoundByName("win")
    private val loseSoundId: Int = loadSoundByName("lose")
    private val shuffleSoundId: Int = loadSoundByName("shuffle")
    private val iceBreakSoundId: Int = loadSoundByName("ice_break")

    // Resource ID for background music (looked up at runtime)
    private val bgmResourceId: Int = getResourceId("bgm")

    // ===== MediaPlayer for background music (gapless looping) =====
    // We use TWO MediaPlayers to achieve seamless looping.
    // Android's MediaPlayer.setNextMediaPlayer() tells the system to start
    // the "next" player the instant the "current" one finishes — no gap!
    // When the current player completes, we swap roles: the "next" becomes
    // "current", and we prepare a fresh "next" for the following loop.
    private var currentBgmPlayer: MediaPlayer? = null
    private var nextBgmPlayer: MediaPlayer? = null

    // ===== Mute controls =====
    /** When true, sound effects are muted */
    var isSfxMuted: Boolean = false

    /** When true, background music is muted */
    var isMusicMuted: Boolean = false

    // ===== Sound Effect Playback Methods =====

    /**
     * Play the swap sound when two gems are exchanged.
     */
    fun playSwap() {
        playSound(swapSoundId)
    }

    /**
     * Play the match sound when gems are matched and cleared.
     *
     * Pitch scales with match size to reward bigger matches:
     * - Match-3: pitch 1.0 (normal)
     * - Match-4: pitch 1.15 (slightly higher — rewarding)
     * - Match-5+: pitch 1.3 (noticeably higher — exciting)
     *
     * @param matchSize The number of gems in the largest match (default 3)
     */
    fun playMatch(matchSize: Int = 3) {
        val pitch = when {
            matchSize >= 5 -> 1.3f  // Color bomb / L-shape — exciting!
            matchSize >= 4 -> 1.15f // Striped / wrapped — rewarding
            else -> 1.0f           // Standard match-3
        }
        playSound(matchSoundId, pitch = pitch)
    }

    /**
     * Play the cascade/gravity sound when gems fall down.
     *
     * The pitch increases with the combo level, creating a satisfying
     * escalation effect during chain reactions:
     * - Combo 0: normal pitch (1.0)
     * - Combo 1: slightly higher (1.1)
     * - Combo 2: higher still (1.2)
     * - etc., capped at 2.0
     *
     * @param comboLevel The current cascade combo depth (0 = first match)
     */
    fun playCascade(comboLevel: Int = 0) {
        val pitch = (1.0f + comboLevel * 0.1f).coerceAtMost(2.0f)
        // Volume escalates from 0.7 to 1.0 over the first 4 combo levels,
        // creating a satisfying crescendo during long chain reactions.
        val volume = (0.7f + comboLevel * 0.075f).coerceAtMost(1.0f)
        playSound(cascadeSoundId, volume = volume, pitch = pitch)
    }

    /**
     * Play the special gem activation sound (striped, wrapped, color bomb).
     */
    fun playSpecialActivation() {
        playSound(specialSoundId)
    }

    /**
     * Play the victory/level complete sound.
     */
    fun playLevelComplete() {
        playSound(winSoundId)
    }

    /**
     * Play the game over/failure sound.
     */
    fun playGameOver() {
        playSound(loseSoundId)
    }

    /**
     * Play the shuffle sound when the board has no valid moves.
     */
    fun playShuffle() {
        playSound(shuffleSoundId)
    }

    /**
     * Play the ice break sound when ice is shattered by a match.
     *
     * Pitch is slightly randomized (0.9 to 1.1) so repeated ice breaks
     * in the same cascade don't all sound identical — gives a more
     * natural, crunchy shattering feel.
     */
    fun playIceBreak() {
        val pitch = 0.9f + Random.nextFloat() * 0.2f
        playSound(iceBreakSoundId, pitch = pitch)
    }

    /**
     * Play a deeper ice hit sound when reinforced ice is cracked (first hit).
     * Deeper pitch than normal ice break to indicate "tougher" obstacle.
     * Uses the same ice_break sound file at a lower pitch.
     */
    fun playReinforcedIceHit() {
        playSound(iceBreakSoundId, pitch = 0.7f)
    }

    /**
     * Play a ticking sound when a bomb timer reaches 2 or below.
     * Reuses the match sound at a fast, high pitch for urgency.
     */
    fun playBombTick() {
        playSound(matchSoundId, volume = 0.6f, pitch = 1.8f)
    }

    /**
     * Play an explosion sound when a bomb detonates (timer reaches 0).
     * Reuses the lose sound at a slightly higher pitch for dramatic effect.
     */
    fun playBombExplode() {
        playSound(loseSoundId, pitch = 1.2f)
    }

    // ===== Background Music Methods =====

    /**
     * Start playing background music with gapless looping.
     *
     * How gapless looping works:
     * 1. Create two MediaPlayers pointing at the same bgm.ogg
     * 2. Call setNextMediaPlayer() on the first, pointing to the second
     * 3. When the first finishes, Android seamlessly transitions to the second
     * 4. In the OnCompletionListener, release the finished player, prepare a
     *    NEW "next" player, and chain it — so it loops forever with no gap!
     *
     * This eliminates the audible pause that happens with isLooping=true or
     * seekTo(0)+start(), because the next player is already decoded and ready.
     */
    fun startBackgroundMusic() {
        if (isMusicMuted) return
        if (currentBgmPlayer != null) return  // Already playing
        if (bgmResourceId == 0) return        // No music file found

        try {
            // Create the first (current) player
            currentBgmPlayer = MediaPlayer.create(context, bgmResourceId)?.apply {
                setVolume(0.4f, 0.4f)
            }
            if (currentBgmPlayer == null) return

            // Create the second (next) player and chain it
            nextBgmPlayer = MediaPlayer.create(context, bgmResourceId)?.apply {
                setVolume(0.4f, 0.4f)
            }

            // Chain: when current finishes, next starts instantly
            if (nextBgmPlayer != null) {
                currentBgmPlayer!!.setNextMediaPlayer(nextBgmPlayer)
            }

            // When the current player finishes, swap roles and prepare a new "next"
            currentBgmPlayer!!.setOnCompletionListener { finishedPlayer ->
                onBgmPlayerCompleted(finishedPlayer)
            }

            // Start playing!
            currentBgmPlayer!!.start()
        } catch (e: Exception) {
            Log.w(TAG, "Could not start background music: ${e.message}")
        }
    }

    /**
     * Called when a BGM player finishes. Swaps roles and chains a new player
     * so the music keeps looping seamlessly forever.
     */
    private fun onBgmPlayerCompleted(finishedPlayer: MediaPlayer) {
        try {
            // Release the player that just finished
            finishedPlayer.release()

            // The "next" player is already playing now — it becomes "current"
            currentBgmPlayer = nextBgmPlayer

            // Prepare a fresh "next" player for the following loop
            nextBgmPlayer = MediaPlayer.create(context, bgmResourceId)?.apply {
                setVolume(0.4f, 0.4f)
            }

            // Chain the new "next" to the now-playing "current"
            if (nextBgmPlayer != null && currentBgmPlayer != null) {
                currentBgmPlayer!!.setNextMediaPlayer(nextBgmPlayer)
            }

            // Set the completion listener on the now-current player
            currentBgmPlayer?.setOnCompletionListener { mp ->
                onBgmPlayerCompleted(mp)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error cycling BGM players: ${e.message}")
        }
    }

    /**
     * Stop and release both background music players.
     */
    fun stopBackgroundMusic() {
        try {
            currentBgmPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping current BGM player: ${e.message}")
        }
        try {
            nextBgmPlayer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing next BGM player: ${e.message}")
        }
        currentBgmPlayer = null
        nextBgmPlayer = null
    }

    /**
     * Pause the background music (e.g., when the app goes to background).
     */
    fun pauseBackgroundMusic() {
        try {
            currentBgmPlayer?.let {
                if (it.isPlaying) it.pause()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error pausing background music: ${e.message}")
        }
    }

    /**
     * Resume the background music (e.g., when the app comes back to foreground).
     */
    fun resumeBackgroundMusic() {
        if (isMusicMuted) return
        try {
            currentBgmPlayer?.start()
        } catch (e: Exception) {
            Log.w(TAG, "Error resuming background music: ${e.message}")
        }
    }

    // ===== Lifecycle Management =====

    /**
     * Release all audio resources.
     * MUST be called when the Activity is destroyed to prevent memory leaks.
     */
    fun release() {
        soundPool.release()
        stopBackgroundMusic()
    }

    // ===== Internal Helpers =====

    /**
     * Look up a raw resource ID by name at runtime.
     *
     * This is used instead of R.raw.xxx so the project compiles even if
     * the sound files haven't been added to res/raw/ yet.
     *
     * @param name The resource name (without extension), e.g., "swap"
     * @return The resource ID, or 0 if not found
     */
    private fun getResourceId(name: String): Int {
        return context.resources.getIdentifier(name, "raw", context.packageName)
    }

    /**
     * Load a sound effect by resource name.
     * Returns 0 if the file doesn't exist or can't be loaded.
     *
     * @param name The resource name (without extension), e.g., "match"
     * @return The SoundPool sound ID, or 0 if loading failed
     */
    private fun loadSoundByName(name: String): Int {
        val resId = getResourceId(name)
        if (resId == 0) {
            Log.d(TAG, "Sound file '$name' not found in res/raw/ — skipping")
            return 0
        }
        return try {
            soundPool.load(context, resId, 1)
        } catch (e: Exception) {
            Log.w(TAG, "Could not load sound '$name': ${e.message}")
            0
        }
    }

    /**
     * Play a loaded sound effect with optional pitch adjustment.
     *
     * @param soundId The sound ID returned by soundPool.load()
     * @param volume Volume level (0.0 to 1.0), defaults to full volume
     * @param pitch Playback rate (0.5 to 2.0), 1.0 = normal speed/pitch
     */
    private fun playSound(soundId: Int, volume: Float = 1.0f, pitch: Float = 1.0f) {
        if (isSfxMuted || soundId == 0) return

        try {
            soundPool.play(
                soundId,
                volume,        // Left volume
                volume,        // Right volume
                1,             // Priority (1 = normal)
                0,             // Loop (0 = no loop)
                pitch          // Playback rate
            )
        } catch (e: Exception) {
            // Silently ignore playback errors
            Log.w(TAG, "Error playing sound: ${e.message}")
        }
    }
}
