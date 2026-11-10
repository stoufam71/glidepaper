package com.paperglide

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.paperglide.game.GameEngine
import com.paperglide.game.GameEvent
import com.paperglide.game.Phase

/** Owns the engine and store so a configuration change never resets a round. */
class GameViewModel(app: Application) : AndroidViewModel(app) {

    val engine = GameEngine()
    private val store = GameStore(app)
    private val tones = TonePlayer()
    private val feedback = Feedback(app, tones::play)
    private val music = MusicPlayer(app)
    private var foreground = false

    /** Bumped every tick so the canvas redraws. */
    var frame by mutableLongStateOf(0L)
        private set
    var phase by mutableStateOf(engine.phase)
        private set
    var score by mutableIntStateOf(0)
        private set
    var best by mutableIntStateOf(store.best)
        private set
    var newBest by mutableStateOf(false)
        private set
    var muted by mutableStateOf(store.muted)
        private set
    var autopilot by mutableStateOf(false)
        private set

    fun tick(dt: Float, visibleWidth: Float) {
        engine.visibleWidth = visibleWidth
        engine.step(dt)
        sync()
        frame++
    }

    fun flap() {
        engine.flap()
        sync()
    }

    fun pause() {
        engine.pause()
        sync()
    }

    /** On the phone the code is four mute taps; an even count leaves mute as it was. */
    fun toggleMute() {
        muted = !muted
        store.muted = muted
        music.update(foreground, muted)
        secret()
    }

    fun secret() {
        engine.secretKey()
        sync()
    }

    /** Music follows the activity: on while it is resumed, off when it pauses. */
    fun setForeground(value: Boolean) {
        foreground = value
        music.update(foreground, muted)
    }

    override fun onCleared() {
        tones.release()
        music.release()
    }

    private fun sync() {
        for (event in engine.drainEvents()) {
            feedback.on(event, muted)
            if (event == GameEvent.Crash) {
                newBest = store.record(engine.score)
                best = store.best
            }
        }
        phase = engine.phase
        score = engine.score
        autopilot = engine.autopilot
    }
}
