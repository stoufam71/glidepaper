package com.paperglide.ui

import kotlin.random.Random

/** A small animal on the grass. [x] in screen units (height 1); [vx] is its own walking speed over the ground. */
class Critter(val kind: Kind, var x: Float, val vx: Float, val phase: Float) {
    enum class Kind { Bunny, Kitten, Chick }
}

/** Baby animals that wander across the meadow now and then. They never touch the game and stay home at night. */
class Critters(private val random: Random = Random.Default) {

    private val _critters = ArrayList<Critter>()
    val critters: List<Critter> get() = _critters

    private var wait = nextWait()

    /** [scroll] is how far the ground moved this frame, so animals stay planted on it. */
    fun update(dt: Float, width: Float, night: Float, scroll: Float) {
        for (c in _critters) c.x += c.vx * dt - scroll
        _critters.removeAll { it.x < -EDGE || it.x > width + EDGE }
        wait -= dt
        if (wait <= 0f) {
            if (night < 0.5f) spawn(width, if (dt > 0f) scroll / dt else 0f)
            wait = nextWait()
        }
    }

    private fun spawn(width: Float, groundSpeed: Float) {
        val kind = Critter.Kind.entries[random.nextInt(Critter.Kind.entries.size)]
        val speed = when (kind) {
            Critter.Kind.Bunny -> 0.09f + random.nextFloat() * 0.04f
            Critter.Kind.Kitten -> 0.06f + random.nextFloat() * 0.03f
            Critter.Kind.Chick -> 0.04f + random.nextFloat() * 0.02f
        }
        val vx = if (random.nextBoolean()) speed else -speed
        // Enter from the side it will actually move away from on screen.
        val net = vx - groundSpeed
        _critters += Critter(
            kind = kind,
            x = if (net > 0f) -EDGE + 0.05f else width + EDGE - 0.05f,
            vx = vx,
            phase = random.nextFloat() * 6.28f,
        )
    }

    private fun nextWait() = MIN_WAIT + random.nextFloat() * (MAX_WAIT - MIN_WAIT)

    companion object {
        const val MIN_WAIT = 6f
        const val MAX_WAIT = 14f
        private const val EDGE = 0.15f
    }
}
