package info.cemu.cemu.common.android.inputdevice

import android.view.InputDevice

fun InputDevice.isGameController(): Boolean {
    return !isVirtual && (
            sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD
                    || sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
                    || sources and InputDevice.SOURCE_DPAD == InputDevice.SOURCE_DPAD
            )
}