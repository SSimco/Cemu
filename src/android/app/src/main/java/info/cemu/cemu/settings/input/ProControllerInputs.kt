package info.cemu.cemu.settings.input

import androidx.compose.runtime.Composable
import info.cemu.cemu.core.translation.tr
import info.cemu.cemu.nativeinterface.NativeInput

@Composable
fun ProControllerInputs(
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
            inputIdToString = { proControllerButtonToString(it) },
            onInputClick = onInputClick,
            controlsMapping = controlsMapping,
        )
    }
    InputItemsGroup(
        groupName = tr("Buttons"),
        inputIds = listOf(
            NativeInput.PRO_BUTTON_A,
            NativeInput.PRO_BUTTON_B,
            NativeInput.PRO_BUTTON_X,
            NativeInput.PRO_BUTTON_Y,
            NativeInput.PRO_BUTTON_L,
            NativeInput.PRO_BUTTON_R,
            NativeInput.PRO_BUTTON_ZL,
            NativeInput.PRO_BUTTON_ZR,
            NativeInput.PRO_BUTTON_PLUS,
            NativeInput.PRO_BUTTON_MINUS,
            NativeInput.PRO_BUTTON_HOME
        )
    )
    InputItemsGroup(
        groupName = tr("D-pad"),
        inputIds = listOf(
            NativeInput.PRO_BUTTON_UP,
            NativeInput.PRO_BUTTON_DOWN,
            NativeInput.PRO_BUTTON_LEFT,
            NativeInput.PRO_BUTTON_RIGHT
        )
    )
    InputItemsGroup(
        groupName = tr("Left Axis"),
        inputIds = listOf(
            NativeInput.PRO_BUTTON_STICKL,
            NativeInput.PRO_BUTTON_STICKL_UP,
            NativeInput.PRO_BUTTON_STICKL_DOWN,
            NativeInput.PRO_BUTTON_STICKL_LEFT,
            NativeInput.PRO_BUTTON_STICKL_RIGHT
        )
    )
    InputItemsGroup(
        groupName = tr("Right Axis"),
        inputIds = listOf(
            NativeInput.PRO_BUTTON_STICKR,
            NativeInput.PRO_BUTTON_STICKR_UP,
            NativeInput.PRO_BUTTON_STICKR_DOWN,
            NativeInput.PRO_BUTTON_STICKR_LEFT,
            NativeInput.PRO_BUTTON_STICKR_RIGHT
        )
    )
}

fun proControllerButtonToString(buttonId: Int) = when (buttonId) {
    NativeInput.PRO_BUTTON_A -> "A"
    NativeInput.PRO_BUTTON_B -> "B"
    NativeInput.PRO_BUTTON_X -> "X"
    NativeInput.PRO_BUTTON_Y -> "Y"
    NativeInput.PRO_BUTTON_L -> "L"
    NativeInput.PRO_BUTTON_R -> "R"
    NativeInput.PRO_BUTTON_ZL -> "ZL"
    NativeInput.PRO_BUTTON_ZR -> "ZR"
    NativeInput.PRO_BUTTON_PLUS -> "+"
    NativeInput.PRO_BUTTON_MINUS -> "-"
    NativeInput.PRO_BUTTON_HOME -> "home"
    NativeInput.PRO_BUTTON_UP -> tr("up")
    NativeInput.PRO_BUTTON_DOWN -> tr("down")
    NativeInput.PRO_BUTTON_LEFT -> tr("left")
    NativeInput.PRO_BUTTON_RIGHT -> tr("right")
    NativeInput.PRO_BUTTON_STICKL -> tr("click")
    NativeInput.PRO_BUTTON_STICKR -> tr("click")
    NativeInput.PRO_BUTTON_STICKL_UP -> tr("up")
    NativeInput.PRO_BUTTON_STICKL_DOWN -> tr("down")
    NativeInput.PRO_BUTTON_STICKL_LEFT -> tr("left")
    NativeInput.PRO_BUTTON_STICKL_RIGHT -> tr("right")
    NativeInput.PRO_BUTTON_STICKR_UP -> tr("up")
    NativeInput.PRO_BUTTON_STICKR_DOWN -> tr("down")
    NativeInput.PRO_BUTTON_STICKR_LEFT -> tr("left")
    NativeInput.PRO_BUTTON_STICKR_RIGHT -> tr("right")
    else -> throw IllegalArgumentException("Invalid buttonId $buttonId for Pro controller type")
}