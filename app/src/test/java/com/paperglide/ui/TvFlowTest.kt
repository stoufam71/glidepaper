package com.paperglide.ui

import android.view.KeyEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.Lifecycle
import com.paperglide.MainActivity
import com.paperglide.game.GameEngine
import com.paperglide.game.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "television")
class TvFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun manualClock() {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(100)
    }

    private fun press(code: Int) = rule.runOnUiThread {
        rule.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        rule.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }

    private fun back() = rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }

    private fun advance(seconds: Float) = rule.mainClock.advanceTimeBy((seconds * 1000).toLong())

    private val phase get() = rule.activity.viewModel.phase

    @Test
    fun okStartsPlayWithoutTouch() {
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        advance(0.1f)
        assertEquals(Phase.Playing, phase)
    }

    @Test
    fun backPausesThenBackAgainExits() {
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        advance(0.2f)
        back()
        advance(0.05f)
        rule.onNodeWithText("Paused", substring = true).assertIsDisplayed()
        assertFalse(rule.activity.isFinishing)
        back()
        assertTrue(rule.activity.isFinishing)
    }

    @Test
    fun okResumesFromPause() {
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        advance(0.2f)
        back()
        advance(0.05f)
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        advance(0.05f)
        assertEquals(Phase.Playing, phase)
    }

    @Test
    fun backOnReadyExits() {
        back()
        assertTrue(rule.activity.isFinishing)
    }

    @Test
    fun leavingTheAppPausesAndWaits() {
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        advance(0.2f)
        rule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        val y = rule.activity.viewModel.engine.planeY
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        advance(1f)
        assertEquals(Phase.Paused, phase)
        assertEquals(y, rule.activity.viewModel.engine.planeY, 0f)
    }

    @Test
    fun downFourTimesStartsAutopilotThatKeepsFlying() {
        repeat(4) { press(KeyEvent.KEYCODE_DPAD_DOWN); advance(0.1f) }
        rule.onNodeWithText("Autopilot").assertIsDisplayed()
        advance(20f)
        assertEquals(Phase.Playing, phase)
    }

    @Test
    fun dpadReachesMuteAndOkTogglesIt() {
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        advance(0.05f)
        val before = if (rule.activity.viewModel.muted) "Unmute sound" else "Mute sound"
        val after = if (before == "Mute sound") "Unmute sound" else "Mute sound"
        rule.onNodeWithContentDescription(before, useUnmergedTree = true).assertIsFocused()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        advance(0.05f)
        rule.onNodeWithContentDescription(after, useUnmergedTree = true).assertIsDisplayed()
        assertEquals(Phase.Ready, phase)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-land-television")
class TvHintLayoutTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun readyHintSitsLeftOfFirstPillar() {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(100)
        val hint = rule.onNodeWithText("Tap or press OK to fly").fetchSemanticsNode().boundsInRoot
        val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
        val firstPillarLeft = (GameEngine.PLANE_X + GameEngine.FIRST_PILLAR_DISTANCE) * root.height
        assertTrue("hint right ${hint.right} vs pillar $firstPillarLeft", hint.right < firstPillarLeft)
    }
}
