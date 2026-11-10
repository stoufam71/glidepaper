package com.paperglide

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.VisibleForTesting
import com.paperglide.game.GameEvent

/** Sound for every event through [play], and a short buzz on crash. */
class Feedback(context: Context, private val play: (GameEvent) -> Unit) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun on(event: GameEvent, muted: Boolean) {
        if (!muted) play(event)
        if (event == GameEvent.Crash) buzz()
    }

    private fun buzz() {
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return // TV boxes have none
        if (Build.VERSION.SDK_INT >= 26) {
            v.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(60)
        }
    }
}

/**
 * Original sound effects made in SoundCraft, in F major to sit with the music
 * (see docs/CREDITS.md): a paper swish for a flap, a C–F–A chime for a point and a falling
 * thud for a crash. A sound that fails to load simply stays silent.
 */
class TonePlayer(context: Context) {

    @VisibleForTesting
    internal val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4) // rapid flaps overlap instead of cutting each other off
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val sounds: Map<GameEvent, Int> = GameEvent.entries.associateWith { pool.load(context, rawFor(it), 1) }

    fun play(event: GameEvent) {
        pool.play(sounds.getValue(event), 1f, 1f, if (event == GameEvent.Crash) 2 else 1, 0, 1f)
    }

    fun release() = pool.release()

    companion object {
        fun rawFor(event: GameEvent): Int = when (event) {
            GameEvent.Flap -> R.raw.sfx_flap
            GameEvent.Score -> R.raw.sfx_score
            GameEvent.Crash -> R.raw.sfx_crash
        }
    }
}
