package info.cemu.cemu.inputoverlay

sealed interface OverlayInput {
    val configName: String
}

enum class OverlayJoystick : OverlayInput {
    LEFT,
    RIGHT;

    override val configName = "AXIS_$name"
}

enum class OverlayButton : OverlayInput {
    A,
    B,
    ONE,
    TWO,
    C,
    Z,
    HOME,
    L,
    L_STICK_CLICK,
    MINUS,
    PLUS,
    R,
    R_STICK_CLICK,
    X,
    Y,
    ZL,
    ZR;

    override val configName = "BUTTON_$name"
}

enum class OverlayDpad : OverlayInput {
    DPAD_DOWN,
    DPAD_LEFT,
    DPAD_RIGHT,
    DPAD_UP;

    override val configName = "DPAD"
}

val OverlayInputList: List<OverlayInput> =
    OverlayButton.entries + OverlayJoystick.entries + OverlayDpad.entries