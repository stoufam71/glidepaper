package com.paperglide

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.paperglide.game.GameEvent
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

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

/** One preloaded static AudioTrack per event. */
class TonePlayer {

    private val tracks: Map<GameEvent, AudioTrack?> = GameEvent.entries.associateWith { build(Tones.forEvent(it)) }

    fun play(event: GameEvent) {
        val track = tracks[event] ?: return
        track.stop()
        track.reloadStaticData()
        track.play()
    }

    fun release() = tracks.values.forEach { it?.release() }

    // Audio hardware can refuse a track; the game then simply plays silent.
    private fun build(pcm: ShortArray): AudioTrack? {
        val track = create(pcm) ?: return null
        if (track.state == AudioTrack.STATE_INITIALIZED) return track
        track.release()
        return null
    }

    private fun create(pcm: ShortArray): AudioTrack? = runCatching {
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(Tones.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
            .also { it.write(pcm, 0, pcm.size) }
    }.getOrNull()
}

/** Original sound effects generated as 16-bit mono PCM. */
object Tones {
    const val SAMPLE_RATE = 22050

    private val flap = sweep(seconds = 0.09f, fromHz = 320f, toHz = 720f, noise = 0.25f)
    private val score = sweep(0.06f, 880f, 880f) + sweep(0.09f, 1320f, 1320f)
    private val crash = sweep(seconds = 0.28f, fromHz = 200f, toHz = 55f, noise = 0.6f)

    fun forEvent(event: GameEvent): ShortArray = when (event) {
        GameEvent.Flap -> flap
        GameEvent.Score -> score
        GameEvent.Crash -> crash
    }

    /** A sine glide with optional noise, shaped by a short attack and a linear fade to silence. */
    private fun sweep(seconds: Float, fromHz: Float, toHz: Float, noise: Float = 0f): ShortArray {
        val n = (seconds * SAMPLE_RATE).toInt()
        val attack = SAMPLE_RATE / 200
        val random = Random(n)
        var phase = 0.0
        return ShortArray(n) { i ->
            val t = i.toFloat() / n
            phase += 2 * PI * (fromHz + (toHz - fromHz) * t) / SAMPLE_RATE
            val envelope = minOf(1f, i.toFloat() / attack) * (1f - t)
            val sample = (1 - noise) * sin(phase).toFloat() + noise * (random.nextFloat() * 2 - 1)
            (sample * envelope * 0.35f * Short.MAX_VALUE).toInt().toShort()
        }
    }
}
