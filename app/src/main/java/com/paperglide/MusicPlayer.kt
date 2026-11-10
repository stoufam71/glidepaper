package com.paperglide

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.annotation.VisibleForTesting

/**
 * Background loop "Paper Glide Theme" (original, see docs/CREDITS.md).
 * Plays only while the app is in front and sound is on.
 */
class MusicPlayer(context: Context) {

    // A device that cannot decode the track simply plays without music.
    @VisibleForTesting
    internal val player: MediaPlayer? = runCatching {
        MediaPlayer.create(
            context,
            R.raw.bg_music_paper_glide,
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            0,
        )
    }.getOrNull()?.apply {
        isLooping = true
        setVolume(VOLUME, VOLUME)
    }

    fun update(foreground: Boolean, muted: Boolean) {
        val p = player ?: return
        val shouldPlay = foreground && !muted
        if (shouldPlay && !p.isPlaying) p.start()
        if (!shouldPlay && p.isPlaying) p.pause()
    }

    fun release() = player?.release()

    private companion object {
        const val VOLUME = 0.45f
    }
}
