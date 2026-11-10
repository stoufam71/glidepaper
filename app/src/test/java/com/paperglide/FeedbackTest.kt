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

@RunWith(RobolectricTestRunner::class)
class FeedbackTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Suppress("DEPRECATION")
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val played = ArrayList<GameEvent>()
    private val feedback = Feedback(context) { played += it }

    @Test
    fun eachEventPlaysItsOwnSound() {
        val tones = TonePlayer(context)
        assertEquals(GameEvent.entries.size, GameEvent.entries.map(TonePlayer::rawFor).toSet().size)
        for (event in GameEvent.entries) {
            tones.play(event)
            assertTrue(shadowOf(tones.pool).wasResourcePlayed(TonePlayer.rawFor(event)))
        }
        tones.release()
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
