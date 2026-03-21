package com.app.lumen.features.audio

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ReadingType(val displayName: String) {
    FIRST_READING("First Reading"),
    PSALM("Responsorial Psalm"),
    SECOND_READING("Second Reading"),
    GOSPEL("Gospel"),
}

class AudioPlayerManager private constructor(context: Context) {
    private val player: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentReadingType = MutableStateFlow<ReadingType?>(null)
    val currentReadingType: StateFlow<ReadingType?> = _currentReadingType

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    val hasActiveSession: Boolean
        get() = _currentReadingType.value != null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
            }

            override fun onIsLoadingChanged(loading: Boolean) {
                _isLoading.value = loading
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    stop()
                }
            }
        })
    }

    @OptIn(UnstableApi::class)
    fun play(url: String, readingType: ReadingType, title: String) {
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .build()
            )
            .build()

        player.setMediaItem(mediaItem)
        _currentReadingType.value = readingType
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
        _currentReadingType.value = null
        _isPlaying.value = false
        _progress.value = 0f
    }

    fun updateProgress() {
        if (player.duration > 0) {
            _currentPosition.value = player.currentPosition
            _duration.value = player.duration
            _progress.value = player.currentPosition.toFloat() / player.duration.toFloat()
        }
    }

    fun release() {
        player.release()
    }

    companion object {
        @Volatile
        private var instance: AudioPlayerManager? = null

        fun getInstance(context: Context): AudioPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: AudioPlayerManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
