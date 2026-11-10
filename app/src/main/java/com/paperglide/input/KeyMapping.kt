package com.paperglide.input

import android.view.KeyEvent

/** Remote, gamepad and keyboard keys that flap. */
object KeyMapping {

    private val flapKeys = setOf(
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER,
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_BUTTON_A,
        KeyEvent.KEYCODE_SPACE,
    )

    fun isFlapKey(keyCode: Int) = keyCode in flapKeys

    /** Only the first key-down flaps; holding a key does not repeat. */
    fun isFlap(event: KeyEvent) =
        event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 && isFlapKey(event.keyCode)

    /** Keys that should click a focused button rather than flap. */
    fun isSelectKey(keyCode: Int) = keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
        keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER

    private val secretKeys = setOf(KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_NUMPAD_1)

    /** One press of the easter-egg code (Down x4 on a remote, 1111 on digits). */
    fun isSecret(event: KeyEvent) =
        event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 && event.keyCode in secretKeys
}
