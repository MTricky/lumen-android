package com.app.lumen.features.rosary.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import com.app.lumen.features.rosary.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Plays rosary prayer audio files, matching each prayer step to the correct audio file.
 * Mirrors the iOS RosaryAudioPlayer logic.
 */
class RosaryAudioPlayer private constructor(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var audioConfig: RosaryAudioConfig? = null
    private var currentLanguage: String? = null
    private var autoAdvanceAction: (() -> Unit)? = null
    private var autoAdvanceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val audioService = RosaryAudioService.getInstance(context)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isAutoAdvancing = MutableStateFlow(false)
    val isAutoAdvancing: StateFlow<Boolean> = _isAutoAdvancing

    // Audio focus
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    // Preferences
    private val rosaryPrefs = context.getSharedPreferences("rosary_prefs", Context.MODE_PRIVATE)

    val isAudioEnabled: Boolean
        get() = rosaryPrefs.getBoolean("audio_enabled", false)

    val audioSpeed: Float
        get() = rosaryPrefs.getFloat("audio_speed", 1.0f)

    val isAutoAdvanceEnabled: Boolean
        get() = rosaryPrefs.getBoolean("auto_advance", false)

    // MARK: - Configuration

    fun configure(config: RosaryAudioConfig, language: String) {
        this.audioConfig = config
        this.currentLanguage = language
        requestAudioFocus()
    }

    private fun requestAudioFocus() {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .build()
        audioFocusRequest = request
        audioManager.requestAudioFocus(request)
    }

    // MARK: - Playback

    fun playAudio(
        step: RosaryPrayerStep,
        mysteryType: MysteryType?,
        onFinished: () -> Unit,
    ) {
        stopCurrentPlayback()
        _isAutoAdvancing.value = false

        if (!isAudioEnabled) return

        val file = resolveAudioFile(step, mysteryType) ?: return
        val language = currentLanguage ?: return
        val localFile = audioService.localFile(file.storagePath, language) ?: return

        this.autoAdvanceAction = onFinished

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(localFile.absolutePath)
                prepare()

                // Set playback speed
                val speed = audioSpeed
                playbackParams = playbackParams.setSpeed(speed)

                setOnCompletionListener { onPlaybackComplete() }
                start()
            }
            mediaPlayer = player
            _isPlaying.value = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopCurrentPlayback() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        _isPlaying.value = false
        cancelAutoAdvance()
    }

    fun updatePlaybackSpeed() {
        mediaPlayer?.let { player ->
            val speed = audioSpeed
            player.playbackParams = player.playbackParams.setSpeed(speed)
        }
    }

    /** Stops all playback and clears auto-advancing state. */
    fun stopAll() {
        stopCurrentPlayback()
        _isAutoAdvancing.value = false
    }

    fun handleAudioToggled(
        enabled: Boolean,
        currentStep: RosaryPrayerStep?,
        mysteryType: MysteryType?,
        onFinished: () -> Unit,
    ) {
        if (enabled) {
            if (currentStep != null) {
                playAudio(currentStep, mysteryType, onFinished)
            }
        } else {
            stopAll()
        }
    }

    fun teardown() {
        stopCurrentPlayback()
        _isAutoAdvancing.value = false
        audioConfig = null
        currentLanguage = null
        autoAdvanceAction = null
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        audioFocusRequest = null
    }

    // MARK: - Playback Complete

    private fun onPlaybackComplete() {
        _isPlaying.value = false

        if (!isAutoAdvanceEnabled || !isAudioEnabled) return

        // Keep bars animating during transition
        _isAutoAdvancing.value = true

        autoAdvanceJob = scope.launch {
            delay(600) // Natural prayer rhythm pause
            if (!isActive) {
                _isAutoAdvancing.value = false
                return@launch
            }
            autoAdvanceAction?.invoke()
        }
    }

    // MARK: - Step → Audio File Resolution

    private fun resolveAudioFile(
        step: RosaryPrayerStep,
        mysteryType: MysteryType?,
    ): RosaryAudioFile? {
        val config = audioConfig ?: return null

        return when (step) {
            is RosaryPrayerStep.Intro -> null

            is RosaryPrayerStep.SignOfTheCross,
            is RosaryPrayerStep.ClosingSignOfTheCross ->
                randomVariant(config.prayers["signOfTheCross"])

            is RosaryPrayerStep.ApostlesCreed ->
                randomVariant(config.prayers["apostlesCreed"])

            is RosaryPrayerStep.IntroOurFather,
            is RosaryPrayerStep.DecadeOurFather ->
                randomVariant(config.prayers["ourFather"])

            is RosaryPrayerStep.IntroHailMary -> {
                val specificKey = when (step.virtue) {
                    IntroVirtue.FAITH -> "introHailMaryFaith"
                    IntroVirtue.HOPE -> "introHailMaryHope"
                    IntroVirtue.CHARITY -> "introHailMaryCharity"
                }
                randomVariant(config.prayers[specificKey])
                    ?: randomVariant(config.prayers["hailMary"])
            }

            is RosaryPrayerStep.DecadeHailMary ->
                randomVariant(config.prayers["hailMary"])

            is RosaryPrayerStep.IntroGloryBe,
            is RosaryPrayerStep.DecadeGloryBe ->
                randomVariant(config.prayers["gloryBe"])

            is RosaryPrayerStep.DecadeFatimaPrayer ->
                randomVariant(config.prayers["fatimaPrayer"])

            is RosaryPrayerStep.HailHolyQueen ->
                randomVariant(config.prayers["hailHolyQueen"])

            is RosaryPrayerStep.FinalPrayer ->
                randomVariant(config.prayers["finalPrayer"])

            is RosaryPrayerStep.MysteryAnnouncement -> {
                val mt = mysteryType ?: return null
                val mysteryFiles = config.mysteries[mt.name.lowercase()]
                if (mysteryFiles != null && step.decade in 1..mysteryFiles.size) {
                    mysteryFiles[step.decade - 1]
                } else null
            }
        }
    }

    private fun randomVariant(files: List<RosaryAudioFile>?): RosaryAudioFile? {
        if (files.isNullOrEmpty()) return null
        return files.random()
    }

    private fun cancelAutoAdvance() {
        autoAdvanceJob?.cancel()
        autoAdvanceJob = null
    }

    companion object {
        @Volatile
        private var instance: RosaryAudioPlayer? = null

        fun getInstance(context: Context): RosaryAudioPlayer {
            return instance ?: synchronized(this) {
                instance ?: RosaryAudioPlayer(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
