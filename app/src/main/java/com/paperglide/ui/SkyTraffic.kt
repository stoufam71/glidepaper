package com.paperglide.ui

import kotlin.random.Random

/** Something crossing the background. Positions in world units (height 1); [vx] in units per second. */
class Flyer(val kind: Kind, var x: Float, val y: Float, val vx: Float, val count: Int, val phase: Float) {
    enum class Kind { Birds, Plane }
}

/** Decorative birds and airliners that pass behind the pillars now and then. They never touch the game. */
class SkyTraffic(private val random: Random = Random.Default) {

    private val _flyers = ArrayList<Flyer>()
    val flyers: List<Flyer> get() = _flyers

    private var wait = nextWait()

    /** [night] is 0..1 from the day cycle; birds roost once it is more night than day. */
    fun update(dt: Float, width: Float, night: Float) {
        for (f in _flyers) f.x += f.vx * dt
        _flyers.removeAll { it.x < -EDGE || it.x > width + EDGE }
        wait -= dt
        if (wait <= 0f) {
            spawn(width, night)
            wait = nextWait()
        }
    }

    private fun spawn(width: Float, night: Float) {
        val kind = if (night > 0.5f || random.nextFloat() < 0.4f) Flyer.Kind.Plane else Flyer.Kind.Birds
        val rightward = random.nextBoolean()
        val speed = when (kind) {
            Flyer.Kind.Birds -> 0.12f + random.nextFloat() * 0.08f
            Flyer.Kind.Plane -> 0.10f + random.nextFloat() * 0.06f
        }
        _flyers += Flyer(
            kind = kind,
            x = if (rightward) -EDGE + 0.05f else width + EDGE - 0.05f,
            y = MIN_Y + random.nextFloat() * (MAX_Y - MIN_Y),
            vx = if (rightward) speed else -speed,
            count = if (kind == Flyer.Kind.Birds) 1 + random.nextInt(3) else 1,
            phase = random.nextFloat() * 6.28f,
        )
    }

    private fun nextWait() = MIN_WAIT + random.nextFloat() * (MAX_WAIT - MIN_WAIT)

    companion object {
        const val MIN_WAIT = 8f
        const val MAX_WAIT = 20f
        const val MIN_Y = 0.06f
        const val MAX_Y = 0.45f
        private const val EDGE = 0.3f
    }
}
