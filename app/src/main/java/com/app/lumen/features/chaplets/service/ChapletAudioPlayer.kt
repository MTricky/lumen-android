package com.app.lumen.features.chaplets.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import com.app.lumen.features.chaplets.model.DivineMercyPrayerStep
import com.app.lumen.features.chaplets.model.SevenSorrowsPrayerStep
import com.app.lumen.features.chaplets.model.StMichaelPrayerStep
import com.app.lumen.features.rosary.model.RosaryAudioConfig
import com.app.lumen.features.rosary.model.RosaryAudioFile
import com.app.lumen.features.rosary.service.RosaryAudioService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Plays chaplet prayer audio files, matching each prayer step to the correct audio file.
 * Mirrors the iOS ChapletAudioPlayer logic. Supports Divine Mercy, St. Michael, and Seven Sorrows.
 */
class ChapletAudioPlayer private constructor(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var audioConfig: RosaryAudioConfig? = null
    private var rosaryAudioConfig: RosaryAudioConfig? = null
    private var currentLanguage: String? = null
    private var currentChapletType: String? = null
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

    // Preferences (shared with rosary)
    private val rosaryPrefs = context.getSharedPreferences("rosary_prefs", Context.MODE_PRIVATE)

    val isAudioEnabled: Boolean
        get() = rosaryPrefs.getBoolean("audio_enabled", false)

    val audioSpeed: Float
        get() = rosaryPrefs.getFloat("audio_speed", 1.0f)

    val isAutoAdvanceEnabled: Boolean
        get() = rosaryPrefs.getBoolean("auto_advance", false)

    // MARK: - Configuration

    fun configure(
        config: RosaryAudioConfig,
        rosaryConfig: RosaryAudioConfig?,
        language: String,
        chapletType: String,
    ) {
        this.audioConfig = config
        this.rosaryAudioConfig = rosaryConfig
        this.currentLanguage = language
        this.currentChapletType = chapletType
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

    // MARK: - Playback (Divine Mercy)

    fun playAudio(step: DivineMercyPrayerStep, onFinished: () -> Unit) {
        stopCurrentPlayback()
        _isAutoAdvancing.value = false

        if (!isAudioEnabled) return

        val file = resolveDivineMercyAudioFile(step)

        // Handle auto-advance for announcement steps with no audio
        if (file == null) {
            if (step is DivineMercyPrayerStep.DecadeAnnouncement && isAutoAdvanceEnabled) {
                this.autoAdvanceAction = onFinished
                autoAdvanceJob = scope.launch {
                    _isAutoAdvancing.value = true
                    delay(2000)
                    if (!isActive) {
                        _isAutoAdvancing.value = false
                        return@launch
                    }
                    autoAdvanceAction?.invoke()
                }
            }
            return
        }

        playFile(file, onFinished)
    }

    // MARK: - Playback (St. Michael)

    fun playAudio(step: StMichaelPrayerStep, onFinished: () -> Unit) {
        stopCurrentPlayback()
        _isAutoAdvancing.value = false

        if (!isAudioEnabled) return

        val file = resolveStMichaelAudioFile(step)

        if (file == null) {
            if (step is StMichaelPrayerStep.ArchangelAnnouncement && isAutoAdvanceEnabled) {
                this.autoAdvanceAction = onFinished
                autoAdvanceJob = scope.launch {
                    _isAutoAdvancing.value = true
                    delay(2000)
                    if (!isActive) {
                        _isAutoAdvancing.value = false
                        return@launch
                    }
                    autoAdvanceAction?.invoke()
                }
            }
            return
        }

        playFile(file, onFinished)
    }

    // MARK: - Playback (Seven Sorrows)

    fun playAudio(step: SevenSorrowsPrayerStep, onFinished: () -> Unit) {
        stopCurrentPlayback()
        _isAutoAdvancing.value = false

        if (!isAudioEnabled) return

        val file = resolveSevenSorrowsAudioFile(step)
        if (file == null) return

        playFile(file, onFinished)
    }

    // MARK: - Common playback

    private fun playFile(file: RosaryAudioFile, onFinished: () -> Unit) {
        val language = currentLanguage ?: return
        val chapletType = currentChapletType ?: return
        val localFile = audioService.chapletLocalFile(file.storagePath, language, chapletType) ?: return

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

    fun stopAll() {
        stopCurrentPlayback()
        _isAutoAdvancing.value = false
    }

    fun teardown() {
        stopCurrentPlayback()
        _isAutoAdvancing.value = false
        audioConfig = null
        rosaryAudioConfig = null
        currentLanguage = null
        currentChapletType = null
        autoAdvanceAction = null
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        audioFocusRequest = null
    }

    // MARK: - Playback Complete

    private fun onPlaybackComplete() {
        _isPlaying.value = false

        if (!isAutoAdvanceEnabled || !isAudioEnabled) return

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

    // MARK: - Divine Mercy step resolution

    private fun resolveDivineMercyAudioFile(step: DivineMercyPrayerStep): RosaryAudioFile? {
        val config = audioConfig ?: return null
        val rosary = rosaryAudioConfig

        return when (step) {
            is DivineMercyPrayerStep.Intro -> null

            is DivineMercyPrayerStep.SignOfTheCross,
            is DivineMercyPrayerStep.ClosingSignOfTheCross ->
                randomVariant(config.prayers["signOfTheCross"])
                    ?: rosary?.let { randomVariant(it.prayers["signOfTheCross"]) }

            is DivineMercyPrayerStep.OurFather ->
                randomVariant(config.prayers["ourFather"])
                    ?: rosary?.let { randomVariant(it.prayers["ourFather"]) }

            is DivineMercyPrayerStep.HailMary ->
                randomVariant(config.prayers["hailMary"])
                    ?: rosary?.let { randomVariant(it.prayers["hailMary"]) }

            is DivineMercyPrayerStep.ApostlesCreed ->
                randomVariant(config.prayers["apostlesCreed"])
                    ?: rosary?.let { randomVariant(it.prayers["apostlesCreed"]) }

            is DivineMercyPrayerStep.OpeningPrayer ->
                randomVariant(config.prayers["openingPrayer"])

            is DivineMercyPrayerStep.EternalFather ->
                randomVariant(config.prayers["eternalFather"])

            is DivineMercyPrayerStep.ForTheSake ->
                randomVariant(config.prayers["forTheSake"])

            is DivineMercyPrayerStep.HolyGod ->
                randomVariant(config.prayers["holyGod"])

            is DivineMercyPrayerStep.ClosingPrayer ->
                randomVariant(config.prayers["closingPrayer"])

            is DivineMercyPrayerStep.DecadeAnnouncement -> null
        }
    }

    // MARK: - St. Michael step resolution

    private fun resolveStMichaelAudioFile(step: StMichaelPrayerStep): RosaryAudioFile? {
        val config = audioConfig ?: return null
        val rosary = rosaryAudioConfig

        return when (step) {
            is StMichaelPrayerStep.Intro -> null

            is StMichaelPrayerStep.SignOfTheCross,
            is StMichaelPrayerStep.ClosingSignOfTheCross ->
                randomVariant(config.prayers["signOfTheCross"])
                    ?: rosary?.let { randomVariant(it.prayers["signOfTheCross"]) }

            is StMichaelPrayerStep.SalutationOurFather,
            is StMichaelPrayerStep.ArchangelOurFather ->
                randomVariant(config.prayers["ourFather"])
                    ?: rosary?.let { randomVariant(it.prayers["ourFather"]) }

            is StMichaelPrayerStep.SalutationHailMary ->
                randomVariant(config.prayers["hailMary"])
                    ?: rosary?.let { randomVariant(it.prayers["hailMary"]) }

            is StMichaelPrayerStep.SalutationGloryBe ->
                randomVariant(config.prayers["gloryBe"])
                    ?: rosary?.let { randomVariant(it.prayers["gloryBe"]) }

            is StMichaelPrayerStep.OpeningPrayer ->
                randomVariant(config.prayers["openingPrayer"])

            is StMichaelPrayerStep.ClosingPrayer ->
                randomVariant(config.prayers["closingPrayer"])

            is StMichaelPrayerStep.FinalPrayer ->
                randomVariant(config.prayers["finalPrayer"])

            is StMichaelPrayerStep.SalutationAnnouncement -> {
                val salutationNum = step.salutation
                randomVariant(config.prayers["salutations_$salutationNum"])
            }

            is StMichaelPrayerStep.ArchangelAnnouncement -> null
        }
    }

    // MARK: - Seven Sorrows step resolution

    private fun resolveSevenSorrowsAudioFile(step: SevenSorrowsPrayerStep): RosaryAudioFile? {
        val config = audioConfig ?: return null
        val rosary = rosaryAudioConfig

        return when (step) {
            is SevenSorrowsPrayerStep.Intro -> null

            is SevenSorrowsPrayerStep.SignOfTheCross,
            is SevenSorrowsPrayerStep.ClosingSignOfTheCross ->
                randomVariant(config.prayers["signOfTheCross"])
                    ?: rosary?.let { randomVariant(it.prayers["signOfTheCross"]) }

            is SevenSorrowsPrayerStep.SorrowOurFather ->
                randomVariant(config.prayers["ourFather"])
                    ?: rosary?.let { randomVariant(it.prayers["ourFather"]) }

            is SevenSorrowsPrayerStep.SorrowHailMary ->
                randomVariant(config.prayers["hailMary"])
                    ?: rosary?.let { randomVariant(it.prayers["hailMary"]) }

            is SevenSorrowsPrayerStep.IntroductoryPrayer ->
                randomVariant(config.prayers["introductoryPrayer"])

            is SevenSorrowsPrayerStep.ActOfContrition ->
                randomVariant(config.prayers["actOfContrition"])

            is SevenSorrowsPrayerStep.SorrowfulMotherPrayer ->
                randomVariant(config.prayers["sorrowfulMotherPrayer"])

            is SevenSorrowsPrayerStep.ClosingPrayer ->
                randomVariant(config.prayers["closingPrayer"])

            is SevenSorrowsPrayerStep.FinalInvocation ->
                randomVariant(config.prayers["finalInvocation"])

            is SevenSorrowsPrayerStep.Versicle ->
                randomVariant(config.prayers["versicle"])

            is SevenSorrowsPrayerStep.Response ->
                randomVariant(config.prayers["response"])

            is SevenSorrowsPrayerStep.ConcludingPrayer ->
                randomVariant(config.prayers["concludingPrayer"])

            is SevenSorrowsPrayerStep.SorrowAnnouncement -> {
                val sorrowNum = step.sorrow
                randomVariant(config.prayers["sorrows_$sorrowNum"])
            }
        }
    }

    // MARK: - Helpers

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
        private var instance: ChapletAudioPlayer? = null

        fun getInstance(context: Context): ChapletAudioPlayer {
            return instance ?: synchronized(this) {
                instance ?: ChapletAudioPlayer(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
