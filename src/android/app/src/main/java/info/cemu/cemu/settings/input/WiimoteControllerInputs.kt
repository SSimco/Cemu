package info.cemu.cemu.settings.input

import androidx.compose.runtime.Composable
import info.cemu.cemu.core.translation.tr
import info.cemu.cemu.nativeinterface.NativeInput

@Composable
fun WiimoteControllerInputs(
    onInputClick: (String, Int) -> Unit,
    controlsMapping: Map<Int, String>,
) {
    @Composable
    fun InputItemsGroup(
        groupName: String,
        inputIds: List<Int>,
    ) {
        InputItemsGroup(
            groupName = groupName,
            inputIds = inputIds,
            inputIdToString = { wiimoteButtonItToString(it) },
            onInputClick = onInputClick,
            controlsMapping = controlsMapping,
        )
    }
    InputItemsGroup(
        groupName = tr("Buttons"),
        inputIds = listOf(
            NativeInput.WIIMOTE_BUTTON_A,
            NativeInput.WIIMOTE_BUTTON_B,
            NativeInput.WIIMOTE_BUTTON_1,
            NativeInput.WIIMOTE_BUTTON_2,
            NativeInput.WIIMOTE_BUTTON_NUNCHUCK_Z,
            NativeInput.WIIMOTE_BUTTON_NUNCHUCK_C,
            NativeInput.WIIMOTE_BUTTON_PLUS,
            NativeInput.WIIMOTE_BUTTON_MINUS,
            NativeInput.WIIMOTE_BUTTON_HOME
        )
    )
    InputItemsGroup(
        groupName = tr("Nunchuck"),
        inputIds = listOf(
            NativeInput.WIIMOTE_BUTTON_UP,
            NativeInput.WIIMOTE_BUTTON_DOWN,
            NativeInput.WIIMOTE_BUTTON_LEFT,
            NativeInput.WIIMOTE_BUTTON_RIGHT
        )
    )
    InputItemsGroup(
        groupName = tr("Right Axis"),
        inputIds = listOf(
            NativeInput.WIIMOTE_BUTTON_NUNCHUCK_UP,
            NativeInput.WIIMOTE_BUTTON_NUNCHUCK_DOWN,
            NativeInput.WIIMOTE_BUTTON_NUNCHUCK_LEFT,
            NativeInput.WIIMOTE_BUTTON_NUNCHUCK_RIGHT
        )
    )
}

fun wiimoteButtonItToString(buttonId: Int) = when (buttonId) {
    NativeInput.WIIMOTE_BUTTON_A -> "A"
    NativeInput.WIIMOTE_BUTTON_B -> "B"
    NativeInput.WIIMOTE_BUTTON_1 -> "1"
    NativeInput.WIIMOTE_BUTTON_2 -> "2"
    NativeInput.WIIMOTE_BUTTON_NUNCHUCK_Z -> "Z"
    NativeInput.WIIMOTE_BUTTON_NUNCHUCK_C -> "C"
    NativeInput.WIIMOTE_BUTTON_PLUS -> "+"
    NativeInput.WIIMOTE_BUTTON_MINUS -> "-"
    NativeInput.WIIMOTE_BUTTON_UP -> tr("up")
    NativeInput.WIIMOTE_BUTTON_DOWN -> tr("down")
    NativeInput.WIIMOTE_BUTTON_LEFT -> tr("left")
    NativeInput.WIIMOTE_BUTTON_RIGHT -> tr("right")
    NativeInput.WIIMOTE_BUTTON_NUNCHUCK_UP -> tr("up")
    NativeInput.WIIMOTE_BUTTON_NUNCHUCK_DOWN -> tr("down")
    NativeInput.WIIMOTE_BUTTON_NUNCHUCK_LEFT -> tr("left")
    NativeInput.WIIMOTE_BUTTON_NUNCHUCK_RIGHT -> tr("right")
    NativeInput.WIIMOTE_BUTTON_HOME -> tr("home")
    else -> throw IllegalArgumentException("Invalid buttonId $buttonId for Wiimote controller type")
}
