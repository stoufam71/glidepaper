package com.paperglide

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.annotation.VisibleForTesting

/**
 * Background loop "Paper Glide Theme" (original, see docs/CREDITS.md).
 * Plays only while the app is in front and sound is on.
 *
 * `isLooping` seeks back to the start when the track ends, which leaves an audible gap on many
 * devices (TVs especially). Instead a second copy is queued with [MediaPlayer.setNextMediaPlayer],
 * which the platform starts the moment the first one ends; each finished copy hands over and
 * queues a fresh one.
 */
class MusicPlayer(private val context: Context) {

    // A device that cannot decode the track simply plays without music.
    @VisibleForTesting
    internal var player: MediaPlayer? = create()
        private set
    private var next: MediaPlayer? = null

    init {
        player?.let(::queueNext)
    }

    fun update(foreground: Boolean, muted: Boolean) {
        val p = player ?: return
        val shouldPlay = foreground && !muted
        if (shouldPlay && !p.isPlaying) p.start()
        if (!shouldPlay && p.isPlaying) p.pause()
    }

    fun release() {
        player?.release()
        next?.release()
        player = null
        next = null
    }

    private fun queueNext(current: MediaPlayer) {
        val queued = create()
        if (queued == null || runCatching { current.setNextMediaPlayer(queued) }.isFailure) {
            queued?.release()
            current.isLooping = true // fall back to the platform loop
            return
        }
        next = queued
        current.setOnCompletionListener { done ->
            done.release()
            player = queued
            next = null
            queueNext(queued)
        }
    }

    private fun create(): MediaPlayer? = runCatching {
        MediaPlayer.create(
            context,
            R.raw.bg_music_paper_glide,
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            0,
        )
    }.getOrNull()?.apply { setVolume(VOLUME, VOLUME) }

    private companion object {
        const val VOLUME = 0.45f
    }
}
