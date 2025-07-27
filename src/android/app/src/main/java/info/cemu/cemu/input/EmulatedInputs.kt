package info.cemu.cemu.input

import info.cemu.cemu.nativeinterface.NativeInput
import info.cemu.cemu.nativeinterface.NativeInput.EmulatedControllerType

sealed interface NativeInputButton {
    val nativeKeyCode: Int
}

enum class VPadButtons(override val nativeKeyCode: Int) : NativeInputButton {
    A(NativeInput.VPADButton.A),
    B(NativeInput.VPADButton.B),
    X(NativeInput.VPADButton.X),
    Y(NativeInput.VPADButton.Y),
    L(NativeInput.VPADButton.L),
    R(NativeInput.VPADButton.R),
    ZL(NativeInput.VPADButton.ZL),
    ZR(NativeInput.VPADButton.ZR),
    PLUS(NativeInput.VPADButton.PLUS),
    MINUS(NativeInput.VPADButton.MINUS),
    UP(NativeInput.VPADButton.UP),
    DOWN(NativeInput.VPADButton.DOWN),
    LEFT(NativeInput.VPADButton.LEFT),
    RIGHT(NativeInput.VPADButton.RIGHT),
    STICKL(NativeInput.VPADButton.STICKL),
    STICKR(NativeInput.VPADButton.STICKR),
    STICKL_UP(NativeInput.VPADButton.STICKL_UP),
    STICKL_DOWN(NativeInput.VPADButton.STICKL_DOWN),
    STICKL_LEFT(NativeInput.VPADButton.STICKL_LEFT),
    STICKL_RIGHT(NativeInput.VPADButton.STICKL_RIGHT),
    STICKR_UP(NativeInput.VPADButton.STICKR_UP),
    STICKR_DOWN(NativeInput.VPADButton.STICKR_DOWN),
    STICKR_LEFT(NativeInput.VPADButton.STICKR_LEFT),
    STICKR_RIGHT(NativeInput.VPADButton.STICKR_RIGHT),
    MIC(NativeInput.VPADButton.MIC),
    SCREEN(NativeInput.VPADButton.SCREEN),
    HOME(NativeInput.VPADButton.HOME),
}

enum class ProControllerButtons(override val nativeKeyCode: Int) : NativeInputButton {
    A(NativeInput.ProButton.A),
    B(NativeInput.ProButton.B),
    X(NativeInput.ProButton.X),
    Y(NativeInput.ProButton.Y),
    L(NativeInput.ProButton.L),
    R(NativeInput.ProButton.R),
    ZL(NativeInput.ProButton.ZL),
    ZR(NativeInput.ProButton.ZR),
    PLUS(NativeInput.ProButton.PLUS),
    MINUS(NativeInput.ProButton.MINUS),
    HOME(NativeInput.ProButton.HOME),
    UP(NativeInput.ProButton.UP),
    DOWN(NativeInput.ProButton.DOWN),
    LEFT(NativeInput.ProButton.LEFT),
    RIGHT(NativeInput.ProButton.RIGHT),
    STICKL(NativeInput.ProButton.STICKL),
    STICKR(NativeInput.ProButton.STICKR),
    STICKL_UP(NativeInput.ProButton.STICKL_UP),
    STICKL_DOWN(NativeInput.ProButton.STICKL_DOWN),
    STICKL_LEFT(NativeInput.ProButton.STICKL_LEFT),
    STICKL_RIGHT(NativeInput.ProButton.STICKL_RIGHT),
    STICKR_UP(NativeInput.ProButton.STICKR_UP),
    STICKR_DOWN(NativeInput.ProButton.STICKR_DOWN),
    STICKR_LEFT(NativeInput.ProButton.STICKR_LEFT),
    STICKR_RIGHT(NativeInput.ProButton.STICKR_RIGHT),
}

enum class ClassicControllerButtons(override val nativeKeyCode: Int) : NativeInputButton {
    A(NativeInput.ClassicButton.A),
    B(NativeInput.ClassicButton.B),
    X(NativeInput.ClassicButton.X),
    Y(NativeInput.ClassicButton.Y),
    L(NativeInput.ClassicButton.L),
    R(NativeInput.ClassicButton.R),
    ZL(NativeInput.ClassicButton.ZL),
    ZR(NativeInput.ClassicButton.ZR),
    PLUS(NativeInput.ClassicButton.PLUS),
    MINUS(NativeInput.ClassicButton.MINUS),
    HOME(NativeInput.ClassicButton.HOME),
    UP(NativeInput.ClassicButton.UP),
    DOWN(NativeInput.ClassicButton.DOWN),
    LEFT(NativeInput.ClassicButton.LEFT),
    RIGHT(NativeInput.ClassicButton.RIGHT),
    STICKL_UP(NativeInput.ClassicButton.STICKL_UP),
    STICKL_DOWN(NativeInput.ClassicButton.STICKL_DOWN),
    STICKL_LEFT(NativeInput.ClassicButton.STICKL_LEFT),
    STICKL_RIGHT(NativeInput.ClassicButton.STICKL_RIGHT),
    STICKR_UP(NativeInput.ClassicButton.STICKR_UP),
    STICKR_DOWN(NativeInput.ClassicButton.STICKR_DOWN),
    STICKR_LEFT(NativeInput.ClassicButton.STICKR_LEFT),
    STICKR_RIGHT(NativeInput.ClassicButton.STICKR_RIGHT),
}

enum class WiimoteButtons(override val nativeKeyCode: Int) : NativeInputButton {
    A(NativeInput.WiimoteButton.A),
    B(NativeInput.WiimoteButton.B),
    ONE(NativeInput.WiimoteButton.ONE),
    TWO(NativeInput.WiimoteButton.TWO),
    NUNCHUCK_Z(NativeInput.WiimoteButton.NUNCHUCK_Z),
    NUNCHUCK_C(NativeInput.WiimoteButton.NUNCHUCK_C),
    PLUS(NativeInput.WiimoteButton.PLUS),
    MINUS(NativeInput.WiimoteButton.MINUS),
    UP(NativeInput.WiimoteButton.UP),
    DOWN(NativeInput.WiimoteButton.DOWN),
    LEFT(NativeInput.WiimoteButton.LEFT),
    RIGHT(NativeInput.WiimoteButton.RIGHT),
    NUNCHUCK_UP(NativeInput.WiimoteButton.NUNCHUCK_UP),
    NUNCHUCK_DOWN(NativeInput.WiimoteButton.NUNCHUCK_DOWN),
    NUNCHUCK_LEFT(NativeInput.WiimoteButton.NUNCHUCK_LEFT),
    NUNCHUCK_RIGHT(NativeInput.WiimoteButton.NUNCHUCK_RIGHT),
    HOME(NativeInput.WiimoteButton.HOME),
}

fun getNativeButtonsForControllerType(controllerType: Int): Array<NativeInputButton> {
    return when (controllerType) {
        EmulatedControllerType.VPAD -> VPadButtons.entries.toTypedArray()
        EmulatedControllerType.PRO -> ProControllerButtons.entries.toTypedArray()
        EmulatedControllerType.CLASSIC -> ClassicControllerButtons.entries.toTypedArray()
        EmulatedControllerType.WIIMOTE -> WiimoteButtons.entries.toTypedArray()
        else -> arrayOf()
    }
}
