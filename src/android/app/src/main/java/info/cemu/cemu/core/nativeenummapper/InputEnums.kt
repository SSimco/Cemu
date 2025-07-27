package info.cemu.cemu.core.nativeenummapper

import info.cemu.cemu.core.translation.tr
import info.cemu.cemu.nativeinterface.NativeInput.EmulatedControllerType

fun controllerTypeToString(type: Int) = when (type) {
    EmulatedControllerType.DISABLED -> tr("Disabled")
    EmulatedControllerType.VPAD -> tr("Wii U GamePad")
    EmulatedControllerType.PRO -> tr("Wii U Pro Controller")
    EmulatedControllerType.WIIMOTE -> tr("Wiimote")
    EmulatedControllerType.CLASSIC -> tr("Wii U Classic Controller")
    else -> throw IllegalArgumentException("Invalid controller type: $type")
}
