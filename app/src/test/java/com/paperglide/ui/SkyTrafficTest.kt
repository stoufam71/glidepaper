package com.paperglide.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SkyTrafficTest {

    private val dt = 1f / 60f

    private fun SkyTraffic.run(seconds: Float, width: Float = 1.78f, night: Float = 0f) {
        var t = 0f
        while (t < seconds) { update(dt, width, night); t += dt }
    }

    @Test
    fun skyStartsEmpty() {
        assertTrue(SkyTraffic(Random(1)).flyers.isEmpty())
    }

    @Test
    fun somethingFliesByWithinTheLongestWait() {
        val traffic = SkyTraffic(Random(1))
        var seen = false
        var t = 0f
        while (t < SkyTraffic.MAX_WAIT + 0.1f) {
            traffic.update(dt, 1.78f, 0f); t += dt
            if (traffic.flyers.isNotEmpty()) seen = true
        }
        assertTrue(seen)
    }

    @Test
    fun flyersCrossAndLeave() {
        val traffic = SkyTraffic(Random(3))
        traffic.run(SkyTraffic.MAX_WAIT + 0.1f)
        val first = traffic.flyers.first()
        traffic.run(30f)
        assertTrue(first !in traffic.flyers)
    }

    @Test
    fun flyersStayInTheUpperSky() {
        val traffic = SkyTraffic(Random(5))
        repeat(20) {
            traffic.run(5f)
            for (f in traffic.flyers) assertTrue(f.y in SkyTraffic.MIN_Y..SkyTraffic.MAX_Y)
        }
    }

    @Test
    fun onlyPlanesAtNight() {
        val traffic = SkyTraffic(Random(7))
        val kinds = mutableSetOf<Flyer.Kind>()
        repeat(40) {
            traffic.run(5f, night = 1f)
            traffic.flyers.forEach { kinds += it.kind }
        }
        assertEquals(setOf(Flyer.Kind.Plane), kinds)
    }

    @Test
    fun bothKindsShowUpByDay() {
        val traffic = SkyTraffic(Random(9))
        val kinds = mutableSetOf<Flyer.Kind>()
        repeat(60) {
            traffic.run(5f)
            traffic.flyers.forEach { kinds += it.kind }
        }
        assertEquals(setOf(Flyer.Kind.Birds, Flyer.Kind.Plane), kinds)
    }
}
