package info.cemu.cemu.settings.input

import androidx.compose.runtime.Composable
import info.cemu.cemu.core.translation.tr
import info.cemu.cemu.nativeinterface.NativeInput

@Composable
fun ClassicControllerInputs(
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
            inputIdToString = ::classicControllerButtonToString,
            onInputClick = onInputClick,
            controlsMapping = controlsMapping,
        )
    }
    InputItemsGroup(
        groupName = tr("Buttons"),
        inputIds = listOf(
            NativeInput.CLASSIC_BUTTON_A,
            NativeInput.CLASSIC_BUTTON_B,
            NativeInput.CLASSIC_BUTTON_X,
            NativeInput.CLASSIC_BUTTON_Y,
            NativeInput.CLASSIC_BUTTON_L,
            NativeInput.CLASSIC_BUTTON_R,
            NativeInput.CLASSIC_BUTTON_ZL,
            NativeInput.CLASSIC_BUTTON_ZR,
            NativeInput.CLASSIC_BUTTON_PLUS,
            NativeInput.CLASSIC_BUTTON_MINUS,
            NativeInput.CLASSIC_BUTTON_HOME
        )
    )
    InputItemsGroup(
        groupName = tr("D-pad"),
        inputIds = listOf(
            NativeInput.CLASSIC_BUTTON_UP,
            NativeInput.CLASSIC_BUTTON_DOWN,
            NativeInput.CLASSIC_BUTTON_LEFT,
            NativeInput.CLASSIC_BUTTON_RIGHT
        )
    )
    InputItemsGroup(
        groupName = tr("Left Axis"),
        inputIds = listOf(
            NativeInput.CLASSIC_BUTTON_STICKL_UP,
            NativeInput.CLASSIC_BUTTON_STICKL_DOWN,
            NativeInput.CLASSIC_BUTTON_STICKL_LEFT,
            NativeInput.CLASSIC_BUTTON_STICKL_RIGHT
        )
    )
    InputItemsGroup(
        groupName = tr("Right Axis"),
        inputIds = listOf(
            NativeInput.CLASSIC_BUTTON_STICKR_UP,
            NativeInput.CLASSIC_BUTTON_STICKR_DOWN,
            NativeInput.CLASSIC_BUTTON_STICKR_LEFT,
            NativeInput.CLASSIC_BUTTON_STICKR_RIGHT
        )
    )
}

private fun classicControllerButtonToString(buttonId: Int) = when (buttonId) {
    NativeInput.CLASSIC_BUTTON_A -> "A"
    NativeInput.CLASSIC_BUTTON_B -> "B"
    NativeInput.CLASSIC_BUTTON_X -> "X"
    NativeInput.CLASSIC_BUTTON_Y -> "Y"
    NativeInput.CLASSIC_BUTTON_L -> "L"
    NativeInput.CLASSIC_BUTTON_R -> "R"
    NativeInput.CLASSIC_BUTTON_ZL -> "ZL"
    NativeInput.CLASSIC_BUTTON_ZR -> "ZR"
    NativeInput.CLASSIC_BUTTON_PLUS -> "+"
    NativeInput.CLASSIC_BUTTON_MINUS -> "-"
    NativeInput.CLASSIC_BUTTON_HOME -> tr("home")
    NativeInput.CLASSIC_BUTTON_UP -> tr("up")
    NativeInput.CLASSIC_BUTTON_DOWN -> tr("down")
    NativeInput.CLASSIC_BUTTON_LEFT -> tr("left")
    NativeInput.CLASSIC_BUTTON_RIGHT -> tr("right")
    NativeInput.CLASSIC_BUTTON_STICKL_UP -> tr("up")
    NativeInput.CLASSIC_BUTTON_STICKL_DOWN -> tr("down")
    NativeInput.CLASSIC_BUTTON_STICKL_LEFT -> tr("left")
    NativeInput.CLASSIC_BUTTON_STICKL_RIGHT -> tr("right")
    NativeInput.CLASSIC_BUTTON_STICKR_UP -> tr("up")
    NativeInput.CLASSIC_BUTTON_STICKR_DOWN -> tr("down")
    NativeInput.CLASSIC_BUTTON_STICKR_LEFT -> tr("left")
    NativeInput.CLASSIC_BUTTON_STICKR_RIGHT -> tr("right")
    else -> throw IllegalArgumentException("Invalid buttonId $buttonId for Classic controller type")
}