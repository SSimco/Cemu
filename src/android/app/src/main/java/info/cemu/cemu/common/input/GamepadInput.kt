package info.cemu.cemu.common.input

import android.view.KeyEvent
import android.view.MotionEvent

interface GamepadInputHandler {
    fun onKeyEvent(event: KeyEvent): Boolean
    fun onMotionEvent(event: MotionEvent): Boolean
}

object NullGamepadInputHandler : GamepadInputHandler {
    override fun onKeyEvent(event: KeyEvent): Boolean = false
    override fun onMotionEvent(event: MotionEvent): Boolean = false
}

interface GamepadInputManager {
    fun setHandler(handler: GamepadInputHandler)
    fun clearHandler()
}