package info.cemu.cemu.core.nativeenummapper

import info.cemu.cemu.core.translation.tr
import info.cemu.cemu.nativeinterface.NativeInput

fun controllerTypeToString(type: Int) = when (type) {
    NativeInput.EMULATED_CONTROLLER_TYPE_DISABLED -> tr("Disabled")
    NativeInput.EMULATED_CONTROLLER_TYPE_VPAD -> tr("Wii U GamePad")
    NativeInput.EMULATED_CONTROLLER_TYPE_PRO -> tr("Wii U Pro Controller")
    NativeInput.EMULATED_CONTROLLER_TYPE_WIIMOTE -> tr("Wiimote")
    NativeInput.EMULATED_CONTROLLER_TYPE_CLASSIC -> tr("Wii U Classic Controller")
    else -> throw IllegalArgumentException("Invalid controller type: $type")
}
