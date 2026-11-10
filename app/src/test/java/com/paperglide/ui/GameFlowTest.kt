package com.paperglide.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.paperglide.MainActivity
import com.paperglide.game.Medal
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun manualClock() {
        // The game loop never idles, so frames are advanced by hand.
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(100)
    }

    private fun tapScreen() = rule.onRoot().performTouchInput { down(center); up() }

    private fun advance(seconds: Float) = rule.mainClock.advanceTimeBy((seconds * 1000).toLong())

    @Test
    fun launchShowsReadyHint() {
        rule.onNodeWithText(READY_HINT).assertIsDisplayed()
    }

    @Test
    fun tapStartsPlay() {
        tapScreen()
        advance(0.1f)
        rule.onNodeWithText(READY_HINT).assertDoesNotExist()
    }

    @Test
    fun fallingShowsGameOverThenLockThenReady() {
        tapScreen()
        var waited = 0f
        while (rule.onAllNodesWithText("Game over").fetchSemanticsNodes().isEmpty() && waited < 5f) {
            advance(0.05f); waited += 0.05f
        }
        rule.onNodeWithText("Game over").assertIsDisplayed()
        rule.onNodeWithContentDescription("Score 0").assertIsDisplayed()
        // Within the input lock a tap is ignored.
        tapScreen()
        advance(0.05f)
        rule.onNodeWithText("Game over").assertIsDisplayed()
        advance(1f)
        tapScreen()
        advance(0.05f)
        rule.onNodeWithText(READY_HINT).assertIsDisplayed()
    }

    @Test
    fun muteButtonTogglesWithoutFlapping() {
        val before = muteLabel()
        rule.onNodeWithContentDescription(before).performClick()
        advance(0.1f)
        rule.onNodeWithContentDescription(flipped(before)).assertIsDisplayed()
        rule.onNodeWithText(READY_HINT).assertIsDisplayed()
    }

    private fun muteLabel() =
        if (rule.onAllNodesWithContentDescription("Mute sound").fetchSemanticsNodes().isNotEmpty()) "Mute sound" else "Unmute sound"

    private fun flipped(label: String) = if (label == "Mute sound") "Unmute sound" else "Mute sound"

    private companion object {
        const val READY_HINT = "Tap or press OK to fly"
    }
}

@RunWith(RobolectricTestRunner::class)
class GameOverCardTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun silverWithoutNewBest() {
        rule.setContent { GameOverCard(score = 27, best = 30, newBest = false, medal = Medal.Silver, textUnit = 20f) }
        rule.onNodeWithContentDescription("Score 27").assertIsDisplayed()
        rule.onNodeWithContentDescription("Best 30").assertIsDisplayed()
        rule.onNodeWithText("Silver medal").assertIsDisplayed()
        rule.onNodeWithText("New best!").assertDoesNotExist()
    }

    @Test
    fun noMedalBelowTen() {
        rule.setContent { GameOverCard(score = 9, best = 9, newBest = true, medal = Medal.None, textUnit = 20f) }
        rule.onNodeWithText("New best!").assertIsDisplayed()
        rule.onNodeWithText("medal", substring = true).assertDoesNotExist()
    }

    @Test
    fun pauseCardSaysHowToGoOnAndQuit() {
        rule.setContent { PauseCard(textUnit = 20f) }
        rule.onNodeWithText("Paused").assertIsDisplayed()
        rule.onNodeWithText("Tap or press OK to go on").assertIsDisplayed()
        rule.onNodeWithText("Press Back again to quit").assertIsDisplayed()
    }
}
