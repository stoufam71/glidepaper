package com.paperglide

import android.content.Context
import android.os.Vibrator
import androidx.test.core.app.ApplicationProvider
import com.paperglide.game.GameEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
class FeedbackTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Suppress("DEPRECATION")
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val played = ArrayList<GameEvent>()
    private val feedback = Feedback(context) { played += it }

    @Test
    fun tonesAreShortAndClickFree() {
        for (event in GameEvent.entries) {
            val pcm = Tones.forEvent(event)
            assertTrue(pcm.isNotEmpty())
            assertTrue(pcm.size < Tones.SAMPLE_RATE / 2)
            assertTrue(abs(pcm.first().toInt()) < Short.MAX_VALUE / 20)
            assertTrue(abs(pcm.last().toInt()) < Short.MAX_VALUE / 20)
        }
    }

    @Test
    fun eachEventSoundsDifferent() {
        val (a, b, c) = GameEvent.entries.map { Tones.forEvent(it).toList() }
        assertTrue(a != b && b != c && a != c)
    }

    @Test
    fun unmutedEventsPlaySound() {
        feedback.on(GameEvent.Flap, muted = false)
        feedback.on(GameEvent.Score, muted = false)
        assertEquals(listOf(GameEvent.Flap, GameEvent.Score), played)
    }

    @Test
    fun mutedCrashIsSilentButStillVibrates() {
        feedback.on(GameEvent.Crash, muted = true)
        assertTrue(played.isEmpty())
        assertTrue(shadowOf(vibrator).isVibrating)
    }

    @Test
    fun crashWithoutVibratorDoesNotThrow() {
        shadowOf(vibrator).setHasVibrator(false)
        feedback.on(GameEvent.Crash, muted = false)
        assertFalse(shadowOf(vibrator).isVibrating)
        assertEquals(listOf(GameEvent.Crash), played)
    }
}
