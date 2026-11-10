package com.paperglide.input

import android.view.KeyEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KeyMappingTest {

    private fun down(code: Int, repeat: Int = 0) = KeyEvent(0, 0, KeyEvent.ACTION_DOWN, code, repeat)

    @Test
    fun remoteAndGamepadKeysFlap() {
        for (code in listOf(
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_SPACE,
        )) {
            assertTrue("$code", KeyMapping.isFlapKey(code))
            assertTrue("$code", KeyMapping.isFlap(down(code)))
        }
    }

    @Test
    fun heldKeyRepeatDoesNotFlap() = assertFalse(KeyMapping.isFlap(down(KeyEvent.KEYCODE_DPAD_CENTER, repeat = 1)))

    @Test
    fun keyUpDoesNotFlap() =
        assertFalse(KeyMapping.isFlap(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER)))

    @Test
    fun otherKeysDoNotFlap() {
        for (code in listOf(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_BACK)) {
            assertFalse("$code", KeyMapping.isFlapKey(code))
        }
    }
}
