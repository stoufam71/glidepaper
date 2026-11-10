package com.paperglide.game

import kotlin.math.sin
import kotlin.random.Random

enum class Phase { Ready, Playing, Paused, GameOver }

enum class GameEvent { Flap, Score, Crash }

/** A cloud pillar pair: solid above and below a gap centred at [gapCenter]. [x] is the left edge. */
class Pillar(var x: Float, val gapCenter: Float, val gapHeight: Float) {
    var scored = false
        internal set
}

/**
 * All game rules, free of Android. World height is 1 (y grows downward), width is [visibleWidth]
 * (screen aspect ratio), so the same round plays the same on any screen height.
 */
class GameEngine(private val random: Random = Random.Default) {

    var phase = Phase.Ready
        private set
    var score = 0
        private set
    var planeY = START_Y
        private set
    var velocity = 0f
        private set
    /** Seconds since launch; drives idle animation. */
    var time = 0f
        private set

    /** Visible world width; pillars spawn just past it. */
    var visibleWidth = 0.5625f

    private val _pillars = ArrayList<Pillar>()
    val pillars: List<Pillar> get() = _pillars

    private var lockLeft = 0f
    private val events = ArrayList<GameEvent>()

    init {
        fillPillars()
    }

    /** Called every frame; returns the shared empty list when nothing happened to avoid per-frame garbage. */
    fun drainEvents(): List<GameEvent> =
        if (events.isEmpty()) emptyList() else events.toList().also { events.clear() }

    fun flap() {
        when (phase) {
            Phase.Ready, Phase.Paused -> {
                phase = Phase.Playing
                lift()
            }
            Phase.Playing -> lift()
            Phase.GameOver -> if (lockLeft <= 0f) reset()
        }
    }

    fun pause() {
        if (phase == Phase.Playing) phase = Phase.Paused
    }

    fun step(rawDt: Float) {
        val dt = rawDt.coerceIn(0f, MAX_DT)
        time += dt
        fillPillars()
        when (phase) {
            Phase.Ready -> planeY = START_Y + sin(time * 4f) * 0.015f
            Phase.Playing -> play(dt)
            Phase.GameOver -> lockLeft -= dt
            Phase.Paused -> Unit
        }
    }

    private fun lift() {
        velocity = FLAP_VELOCITY
        events += GameEvent.Flap
    }

    private fun reset() {
        phase = Phase.Ready
        score = 0
        planeY = START_Y
        velocity = 0f
        _pillars.clear()
        fillPillars()
    }

    private fun play(dt: Float) {
        velocity = (velocity + GRAVITY * dt).coerceAtMost(MAX_FALL)
        planeY += velocity * dt

        val dx = Difficulty.speed(score) * dt
        for (p in _pillars) {
            p.x -= dx
            if (!p.scored && p.x + PILLAR_WIDTH < PLANE_X) {
                p.scored = true
                score++
                events += GameEvent.Score
            }
        }
        _pillars.removeAll { it.x + PILLAR_WIDTH < 0f }

        if (planeY - PLANE_RADIUS <= 0f || planeY + PLANE_RADIUS >= GROUND_TOP || _pillars.any(::hits)) {
            planeY = planeY.coerceIn(PLANE_RADIUS, GROUND_TOP - PLANE_RADIUS)
            phase = Phase.GameOver
            lockLeft = GAME_OVER_LOCK
            events += GameEvent.Crash
        }
    }

    private fun fillPillars() {
        while (true) {
            val last = _pillars.lastOrNull()
            // The first pillar always exists so a narrow (phone) screen has one approaching.
            val x = last?.let { it.x + SPACING } ?: (PLANE_X + FIRST_PILLAR_DISTANCE)
            if (last != null && x > visibleWidth + PILLAR_WIDTH) return
            val gap = Difficulty.gap(score)
            val lo = GAP_MARGIN + gap / 2
            val hi = GROUND_TOP - GAP_MARGIN - gap / 2
            _pillars += Pillar(x, lo + random.nextFloat() * (hi - lo), gap)
        }
    }

    /** Circle-vs-rectangle test against both solid parts of the pillar. */
    private fun hits(p: Pillar): Boolean {
        val nearestX = PLANE_X.coerceIn(p.x, p.x + PILLAR_WIDTH)
        val dx = PLANE_X - nearestX
        val top = p.gapCenter - p.gapHeight / 2
        val bottom = p.gapCenter + p.gapHeight / 2
        val dyTop = (planeY - planeY.coerceIn(0f, top))
        val dyBottom = (planeY - planeY.coerceIn(bottom, GROUND_TOP))
        val r2 = PLANE_RADIUS * PLANE_RADIUS
        return dx * dx + dyTop * dyTop < r2 || dx * dx + dyBottom * dyBottom < r2
    }

    companion object {
        const val PLANE_X = 0.18f
        const val PLANE_RADIUS = 0.028f
        const val START_Y = 0.42f
        const val GROUND_TOP = 0.9f
        const val GRAVITY = 2.6f
        const val FLAP_VELOCITY = -0.9f
        const val MAX_FALL = 1.6f
        const val PILLAR_WIDTH = 0.16f
        const val SPACING = 0.6f
        const val FIRST_PILLAR_DISTANCE = 0.8f
        const val GAP_MARGIN = 0.08f
        const val GAME_OVER_LOCK = 0.6f
        const val MAX_DT = 1f / 30f
    }
}
