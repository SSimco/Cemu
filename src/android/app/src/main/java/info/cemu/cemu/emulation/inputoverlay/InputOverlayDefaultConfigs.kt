package info.cemu.cemu.emulation.inputoverlay

import info.cemu.cemu.common.inputoverlay.OverlayButton
import info.cemu.cemu.common.inputoverlay.OverlayDpad
import info.cemu.cemu.common.inputoverlay.OverlayJoystick

data class InputOverlayConfig(
    val alignBottom: Boolean = false,
    val alignEnd: Boolean = false,
    val paddingHorizontal: Int = 0,
    val paddingVertical: Int = 0,
    val width: Int,
    val height: Int
) {
    constructor(
        alignBottom: Boolean = false,
        alignEnd: Boolean = false,
        paddingHorizontal: Int = 0,
        paddingVertical: Int = 0,
        size: Int
    ) : this(
        alignBottom = alignBottom,
        alignEnd = alignEnd,
        paddingHorizontal = paddingHorizontal,
        paddingVertical = paddingVertical,
        width = size,
        height = size
    )
}

val DefaultOverlayConfigs = mapOf(
    OverlayButton.PLUS to InputOverlayConfig(
        alignBottom = true,
        alignEnd = true,
        paddingHorizontal = 272,
        paddingVertical = 24,
        size = 48
    ),
    OverlayButton.MINUS to InputOverlayConfig(
        alignBottom = true,
        paddingHorizontal = 272,
        paddingVertical = 24,
        size = 48
    ),
    OverlayButton.HOME to InputOverlayConfig(
        alignBottom = true,
        alignEnd = true,
        paddingHorizontal = 208,
        paddingVertical = 80,
        size = 48
    ),
    OverlayButton.L to InputOverlayConfig(
        paddingHorizontal = 48,
        paddingVertical = 8,
        width = 72,
        height = 36
    ),
    OverlayButton.ZL to InputOverlayConfig(
        paddingHorizontal = 48,
        paddingVertical = 64,
        width = 72,
        height = 36
    ),
    OverlayButton.R to InputOverlayConfig(
        alignEnd = true,
        paddingHorizontal = 48,
        paddingVertical = 8,
        width = 72,
        height = 36
    ),
    OverlayButton.ZR to InputOverlayConfig(
        alignEnd = true,
        paddingHorizontal = 48,
        paddingVertical = 64,
        width = 72,
        height = 36
    ),
    OverlayButton.C to InputOverlayConfig(
        alignEnd = true,
        paddingHorizontal = 60,
        paddingVertical = 8,
        size = 48
    ),
    OverlayButton.Z to InputOverlayConfig(
        alignEnd = true,
        paddingHorizontal = 48,
        paddingVertical = 64,
        width = 72,
        height = 36
    ),
    OverlayButton.A to InputOverlayConfig(
        alignBottom = true,
        alignEnd = true,
        paddingHorizontal = 32,
        paddingVertical = 56,
        size = 48
    ),
    OverlayButton.Y to InputOverlayConfig(
        alignBottom = true,
        alignEnd = true,
        paddingHorizontal = 128,
        paddingVertical = 56,
        size = 48
    ),
    OverlayButton.X to InputOverlayConfig(
        alignBottom = true,
        alignEnd = true,
        paddingHorizontal = 80,
        paddingVertical = 104,
        size = 48
    ),
    OverlayButton.B to InputOverlayConfig(
        alignBottom = true,
        alignEnd = true,
        paddingHorizontal = 80,
        paddingVertical = 8,
        size = 48
    ),
    OverlayButton.ONE to InputOverlayConfig(
        alignBottom = true,
        alignEnd = true,
        paddingHorizontal = 128,
        paddingVertical = 56,
        size = 48
    ),
    OverlayButton.TWO to InputOverlayConfig(
        alignBottom = true,
        alignEnd = true,
        paddingHorizontal = 80,
        paddingVertical = 104,
        size = 48
    ),
    OverlayButton.R_STICK_CLICK to InputOverlayConfig(
        alignBottom = true,
        alignEnd = true,
        paddingHorizontal = 208,
        paddingVertical = 80,
        size = 48
    ),
    OverlayButton.L_STICK_CLICK to InputOverlayConfig(
        alignBottom = true,
        paddingHorizontal = 208,
        paddingVertical = 80,
        size = 48
    ),
    OverlayJoystick.LEFT to InputOverlayConfig(
        alignBottom = true,
        paddingHorizontal = 144,
        paddingVertical = 120,
        size = 72
    ),
    OverlayJoystick.RIGHT to InputOverlayConfig(
        alignEnd = true,
        alignBottom = true,
        paddingHorizontal = 144,
        paddingVertical = 120,
        size = 72
    ),
    OverlayDpad.DPAD_DOWN to InputOverlayConfig(
        alignBottom = true,
        size = 144,
        paddingHorizontal = 8,
        paddingVertical = 8
    ),
    OverlayButton.BLOW_MIC to InputOverlayConfig(
        alignBottom = true,
        alignEnd = true,
        paddingHorizontal = 8,
        paddingVertical = 8,
        size = 40
    )
).mapKeys { it.key.configName }
