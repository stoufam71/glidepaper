package com.paperglide.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CrittersTest {

    private val dt = 1f / 60f

    /** [groundSpeed] is how fast the world scrolls (0 before a round starts). */
    private fun Critters.run(seconds: Float, night: Float = 0f, groundSpeed: Float = 0f, width: Float = 0.5625f) {
        var t = 0f
        while (t < seconds) { update(dt, width, night, groundSpeed * dt); t += dt }
    }

    @Test
    fun meadowStartsEmpty() {
        assertTrue(Critters(Random(1)).critters.isEmpty())
    }

    @Test
    fun allThreeAnimalsWanderBy() {
        val critters = Critters(Random(2))
        val kinds = mutableSetOf<Critter.Kind>()
        repeat(60) {
            critters.run(3f)
            critters.critters.forEach { kinds += it.kind }
        }
        assertEquals(Critter.Kind.entries.toSet(), kinds)
    }

    @Test
    fun nobodyComesOutAtNight() {
        val critters = Critters(Random(3))
        critters.run(Critters.MAX_WAIT * 4, night = 1f)
        assertTrue(critters.critters.isEmpty())
    }

    @Test
    fun animalsCrossAndLeaveWhileIdleOrPlaying() {
        for (speed in listOf(0f, 0.45f)) {
            val critters = Critters(Random(4))
            while (critters.critters.isEmpty()) critters.run(dt, groundSpeed = speed)
            val first = critters.critters.first()
            critters.run(30f, groundSpeed = speed)
            assertTrue(first !in critters.critters)
        }
    }

    @Test
    fun animalsRideTheScrollingGround() {
        val critters = Critters(Random(5))
        while (critters.critters.isEmpty()) critters.run(dt, groundSpeed = 0.45f)
        val c = critters.critters.first()
        val x = c.x
        critters.update(dt, 0.5625f, 0f, 0.01f)
        assertEquals(x + c.vx * dt - 0.01f, c.x, 1e-5f)
    }
}
