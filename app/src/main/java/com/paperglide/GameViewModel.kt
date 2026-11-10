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

    fun toggleMute() {
        muted = !muted
        store.muted = muted
    }

    private fun sync() {
        for (event in engine.drainEvents()) {
            if (event == GameEvent.Crash) {
                newBest = store.record(engine.score)
                best = store.best
            }
        }
        phase = engine.phase
        score = engine.score
    }
}
