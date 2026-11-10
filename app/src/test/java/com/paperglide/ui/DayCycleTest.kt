package com.paperglide.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DayCycleTest {

    @Test
    fun roundStartsOnACloudyDayWithSunHigh() {
        val sky = DayCycle.at(0f)
        assertEquals(DayCycle.DAY.top, sky.top)
        assertEquals(DayCycle.DAY.bottom, sky.bottom)
        assertEquals(0f, sky.night, 0f)
        assertEquals(DayCycle.SUN_HIGH, sky.sunY, 1e-6f)
    }

    @Test
    fun sunGoesDownDuringSunset() {
        val early = DayCycle.at(20f)
        val late = DayCycle.at(28f)
        assertTrue(early.sunY > DayCycle.SUN_HIGH)
        assertTrue(late.sunY > early.sunY)
        assertTrue(DayCycle.at(31f).sunY >= DayCycle.SUN_GONE)
        assertEquals(DayCycle.SUNSET.top, DayCycle.at(24f).top)
    }

    @Test
    fun nightHasMoonAndStarsAndNoSun() {
        val sky = DayCycle.at(40f)
        assertEquals(1f, sky.night, 0f)
        assertEquals(DayCycle.MOON_HIGH, sky.moonY, 1e-6f)
        assertTrue(sky.sunY >= DayCycle.SUN_GONE)
        assertEquals(DayCycle.NIGHT.top, sky.top)
    }

    @Test
    fun sunComesUpDuringSunrise() {
        val low = DayCycle.at(54f)
        val higher = DayCycle.at(58f)
        assertTrue(higher.sunY < low.sunY)
        assertEquals(DayCycle.SUN_HIGH, DayCycle.at(62f).sunY, 1e-6f)
        assertEquals(DayCycle.SUNRISE.top, low.top)
        assertEquals(0f, DayCycle.at(62f).night, 0f)
    }

    @Test
    fun cycleLoops() {
        assertEquals(DayCycle.at(5f), DayCycle.at(5f + DayCycle.LENGTH))
    }
}
