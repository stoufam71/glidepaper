package com.paperglide.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DifficultyTest {

    @Test
    fun gapNarrowsThenPlateaus() {
        assertEquals(Difficulty.START_GAP, Difficulty.gap(0), 1e-6f)
        assertEquals(Difficulty.MIN_GAP, Difficulty.gap(60), 1e-6f)
        assertEquals(Difficulty.MIN_GAP, Difficulty.gap(200), 1e-6f)
        for (s in 0 until 60) assertTrue(Difficulty.gap(s + 1) <= Difficulty.gap(s))
        assertTrue(Difficulty.gap(30) < Difficulty.START_GAP)
    }

    @Test
    fun speedRisesThenPlateaus() {
        assertEquals(Difficulty.START_SPEED, Difficulty.speed(0), 1e-6f)
        assertEquals(Difficulty.MAX_SPEED, Difficulty.speed(60), 1e-6f)
        assertEquals(Difficulty.MAX_SPEED, Difficulty.speed(500), 1e-6f)
        for (s in 0 until 60) assertTrue(Difficulty.speed(s + 1) >= Difficulty.speed(s))
    }

    @Test
    fun medalThresholds() {
        assertEquals(Medal.None, Difficulty.medal(0))
        assertEquals(Medal.None, Difficulty.medal(9))
        assertEquals(Medal.Bronze, Difficulty.medal(10))
        assertEquals(Medal.Bronze, Difficulty.medal(24))
        assertEquals(Medal.Silver, Difficulty.medal(25))
        assertEquals(Medal.Silver, Difficulty.medal(27))
        assertEquals(Medal.Silver, Difficulty.medal(49))
        assertEquals(Medal.Gold, Difficulty.medal(50))
    }
}
