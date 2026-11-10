package com.paperglide.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    private val dt = 1f / 60f

    private fun engine(width: Float = 0.5625f, seed: Int = 7) =
        GameEngine(Random(seed)).apply { visibleWidth = width }

    /** Steps for [seconds], stopping early at game over unless [throughGameOver]. */
    private fun GameEngine.run(seconds: Float, flapEvery: Float = 0f, throughGameOver: Boolean = false) {
        var t = 0f
        var sinceFlap = 0f
        while (t < seconds && (throughGameOver || phase != Phase.GameOver)) {
            if (flapEvery > 0f && sinceFlap >= flapEvery) { flap(); sinceFlap = 0f }
            step(dt); t += dt; sinceFlap += dt
        }
    }

    /** Keeps the plane centred on the next gap by flapping whenever it sinks below it. */
    private fun GameEngine.steer(seconds: Float) {
        var t = 0f
        while (t < seconds && phase == Phase.Playing) {
            val target = pillars.firstOrNull { it.x + GameEngine.PILLAR_WIDTH >= GameEngine.PLANE_X }?.gapCenter ?: 0.45f
            if (planeY > target + 0.02f && velocity > 0f) flap()
            step(dt); t += dt
        }
    }

    @Test
    fun readyHoversWithoutScoring() {
        val e = engine()
        e.run(2f)
        assertEquals(Phase.Ready, e.phase)
        assertEquals(0, e.score)
        assertTrue(kotlin.math.abs(e.planeY - GameEngine.START_Y) < 0.05f)
        assertTrue(e.drainEvents().isEmpty())
    }

    @Test
    fun firstFlapStartsPlayWithLift() {
        val e = engine()
        e.flap()
        assertEquals(Phase.Playing, e.phase)
        assertTrue(e.velocity < 0f)
        assertEquals(listOf(GameEvent.Flap), e.drainEvents())
    }

    @Test
    fun fallingIntoGroundEndsRoundOnce() {
        val e = engine()
        e.flap(); e.drainEvents()
        e.run(5f)
        assertEquals(Phase.GameOver, e.phase)
        assertEquals(1, e.drainEvents().count { it == GameEvent.Crash })
    }

    @Test
    fun flyingAboveTopEdgeEndsRound() {
        val e = engine()
        e.flap()
        e.run(1.5f, flapEvery = 0.05f)
        assertEquals(Phase.GameOver, e.phase)
        assertTrue(e.planeY < 0.2f)
    }

    @Test
    fun passingAGapScoresOncePerPillar() {
        val e = engine()
        e.flap(); e.drainEvents()
        e.steer(3.5f)
        val events = e.drainEvents()
        assertEquals(Phase.Playing, e.phase)
        assertTrue(e.score >= 1)
        assertEquals(e.score, events.count { it == GameEvent.Score })
    }

    @Test
    fun hittingPillarBodyEndsRound() {
        val e = engine()
        e.flap()
        // Park the plane far below the first gap and keep it there.
        val first = e.pillars.first()
        var guard = 0
        while (e.phase == Phase.Playing && guard++ < 2000) {
            val wanted = (first.gapCenter + first.gapHeight / 2 + 0.06f).coerceAtMost(GameEngine.GROUND_TOP - 0.05f)
            if (e.planeY > wanted && e.velocity > 0f) e.flap()
            e.step(dt)
        }
        assertEquals(Phase.GameOver, e.phase)
        assertTrue(first.x < GameEngine.PLANE_X + GameEngine.PLANE_RADIUS)
    }

    @Test
    fun gapsAlwaysFitInsidePlayfield() {
        repeat(20) { seed ->
            val e = engine(width = 1.78f, seed = seed)
            e.flap()
            repeat(1200) {
                if (e.phase != Phase.Playing) return@repeat
                for (p in e.pillars) {
                    assertTrue(p.gapCenter - p.gapHeight / 2 >= GameEngine.GAP_MARGIN - 1e-4f)
                    assertTrue(p.gapCenter + p.gapHeight / 2 <= GameEngine.GROUND_TOP - GameEngine.GAP_MARGIN + 1e-4f)
                }
                e.steer(dt)
            }
        }
    }

    @Test
    fun widerScreenShowsMorePillarsWithSameSpacingAndGap() {
        val phone = engine(width = 0.5625f).apply { step(dt) }
        val tv = engine(width = 1.78f).apply { step(dt) }
        assertTrue(tv.pillars.size > phone.pillars.size)
        assertEquals(phone.pillars.first().x, tv.pillars.first().x, 1e-6f)
        assertEquals(phone.pillars.first().gapHeight, tv.pillars.first().gapHeight, 1e-6f)
        tv.pillars.zipWithNext().forEach { (a, b) -> assertEquals(GameEngine.SPACING, b.x - a.x, 1e-4f) }
    }

    @Test
    fun gameOverIgnoresFlapsDuringLockThenReturnsToReady() {
        val e = engine()
        e.flap(); e.run(5f)
        assertEquals(Phase.GameOver, e.phase)
        e.flap()
        assertEquals(Phase.GameOver, e.phase)
        e.run(GameEngine.GAME_OVER_LOCK + 0.05f, throughGameOver = true)
        e.flap()
        assertEquals(Phase.Ready, e.phase)
        assertEquals(0, e.score)
    }

    @Test
    fun pauseFreezesAndFlapResumes() {
        val e = engine()
        e.flap(); e.run(0.3f)
        e.pause()
        assertEquals(Phase.Paused, e.phase)
        val y = e.planeY
        val x = e.pillars.first().x
        e.run(2f)
        assertEquals(y, e.planeY, 0f)
        assertEquals(x, e.pillars.first().x, 0f)
        e.flap()
        assertEquals(Phase.Playing, e.phase)
    }

    @Test
    fun pauseOutsidePlayChangesNothing() {
        val e = engine()
        e.pause()
        assertEquals(Phase.Ready, e.phase)
        e.flap(); e.run(5f)
        e.pause()
        assertEquals(Phase.GameOver, e.phase)
    }

    @Test
    fun hugeFrameIsClampedSoPlaneCannotTunnel() {
        val e = engine()
        e.flap()
        val x = e.pillars.first().x
        e.step(10f)
        assertEquals(x - Difficulty.speed(0) * GameEngine.MAX_DT, e.pillars.first().x, 1e-5f)
        assertFalse(e.phase == Phase.GameOver && e.planeY < GameEngine.GROUND_TOP - 0.2f)
    }

    @Test
    fun playTimeCountsOnlyWhilePlayingAndResetsEachRound() {
        val e = engine()
        e.run(1f)
        assertEquals(0f, e.playTime, 0f)
        e.flap()
        e.run(0.5f)
        val played = e.playTime
        assertTrue(played > 0.4f)
        e.pause(); e.run(1f)
        assertEquals(played, e.playTime, 0f)
        e.flap(); e.run(5f)
        val atCrash = e.playTime
        e.run(1f, throughGameOver = true)
        assertEquals(atCrash, e.playTime, 0f)
        e.flap()
        assertEquals(Phase.Ready, e.phase)
        assertEquals(0f, e.playTime, 0f)
    }

    private fun GameEngine.enterSecret() = repeat(4) { secretKey(); step(0.1f) }

    @Test
    fun fourSecretPressesOnReadyStartAutopilot() {
        val e = engine()
        e.enterSecret()
        assertEquals(Phase.Playing, e.phase)
        assertTrue(e.autopilot)
    }

    @Test
    fun slowSecretPressesDoNothing() {
        val e = engine()
        repeat(3) { e.secretKey(); e.step(0.1f) }
        e.run(2.5f)
        e.secretKey()
        assertEquals(Phase.Ready, e.phase)
        assertFalse(e.autopilot)
    }

    @Test
    fun secretOnlyWorksOnReady() {
        val e = engine()
        e.flap()
        e.enterSecret()
        assertFalse(e.autopilot)
    }

    @Test
    fun autopilotNeverLosesOnPhoneOrTv() {
        for (width in listOf(0.5625f, 1.78f)) {
            for (seed in 0 until 25) {
                val e = engine(width = width, seed = seed)
                e.enterSecret()
                var t = 0f
                while (t < 240f) {
                    e.step(dt); t += dt
                    assertEquals("width $width seed $seed at ${t}s", Phase.Playing, e.phase)
                }
                assertTrue("reached plateau difficulty", e.score > Difficulty.PLATEAU_SCORE)
            }
        }
    }

    @Test
    fun autopilotSurvivesDroppedFrames() {
        for (seed in 0 until 10) {
            val e = engine(width = 1.78f, seed = seed)
            e.enterSecret()
            var t = 0f
            var i = 0
            while (t < 200f) {
                val step = if (i++ % 3 == 0) 1f / 30f else 1f / 60f
                e.step(step); t += step
                assertEquals("seed $seed at ${t}s", Phase.Playing, e.phase)
            }
        }
    }

    @Test
    fun manualFlapsCannotBreakAutopilot() {
        val e = engine()
        e.enterSecret()
        var t = 0f
        while (t < 60f) {
            e.flap()
            e.step(dt); t += dt
        }
        assertEquals(Phase.Playing, e.phase)
    }

    @Test
    fun autopilotSurvivesPauseAndResume() {
        val e = engine()
        e.enterSecret()
        e.run(3f)
        e.pause()
        e.flap()
        assertEquals(Phase.Playing, e.phase)
        assertTrue(e.autopilot)
    }
}
