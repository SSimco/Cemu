package info.cemu.cemu.common.motionevent

import android.view.InputDevice
import android.view.MotionEvent

fun MotionEvent.isMotionEventFromJoystickOrGamepad(): Boolean {
    return (source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
            || (source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
}