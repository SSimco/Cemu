package info.cemu.cemu.settings.input

import androidx.compose.runtime.Composable
import info.cemu.cemu.core.components.Toggle
import info.cemu.cemu.core.translation.tr
import info.cemu.cemu.nativeinterface.NativeInput

@Composable
fun VPADInputs(
    controllerIndex: Int,
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
            inputIdToString = { vpadButtonToString(it) },
            onInputClick = onInputClick,
            controlsMapping = controlsMapping,
        )
    }
    InputItemsGroup(
        groupName = tr("Buttons"),
        inputIds = listOf(
            NativeInput.VPAD_BUTTON_A,
            NativeInput.VPAD_BUTTON_B,
            NativeInput.VPAD_BUTTON_X,
            NativeInput.VPAD_BUTTON_Y,
            NativeInput.VPAD_BUTTON_L,
            NativeInput.VPAD_BUTTON_R,
            NativeInput.VPAD_BUTTON_ZL,
            NativeInput.VPAD_BUTTON_ZR,
            NativeInput.VPAD_BUTTON_PLUS,
            NativeInput.VPAD_BUTTON_MINUS
        )
    )
    InputItemsGroup(
        groupName = tr("D-pad"),
        inputIds = listOf(
            NativeInput.VPAD_BUTTON_UP,
            NativeInput.VPAD_BUTTON_DOWN,
            NativeInput.VPAD_BUTTON_LEFT,
            NativeInput.VPAD_BUTTON_RIGHT
        )
    )
    InputItemsGroup(
        groupName = tr("Left Axis"),
        inputIds = listOf(
            NativeInput.VPAD_BUTTON_STICKL,
            NativeInput.VPAD_BUTTON_STICKL_UP,
            NativeInput.VPAD_BUTTON_STICKL_DOWN,
            NativeInput.VPAD_BUTTON_STICKL_LEFT,
            NativeInput.VPAD_BUTTON_STICKL_RIGHT
        )
    )
    InputItemsGroup(
        groupName = tr("Right Axis"),
        inputIds = listOf(
            NativeInput.VPAD_BUTTON_STICKR,
            NativeInput.VPAD_BUTTON_STICKR_UP,
            NativeInput.VPAD_BUTTON_STICKR_DOWN,
            NativeInput.VPAD_BUTTON_STICKR_LEFT,
            NativeInput.VPAD_BUTTON_STICKR_RIGHT
        )
    )
    InputItemsGroup(
        groupName = tr("Extra"),
        inputIds = listOf(
            NativeInput.VPAD_BUTTON_MIC,
            NativeInput.VPAD_BUTTON_HOME,
            NativeInput.VPAD_BUTTON_SCREEN
        )
    )
    Toggle(
        label = tr("Toggle screen"),
        description = tr("Makes the \"show screen\" button toggle between the TV and gamepad screens"),
        initialCheckedState = { NativeInput.getVPADScreenToggle(controllerIndex) },
        onCheckedChanged = { NativeInput.setVPADScreenToggle(controllerIndex, it) }
    )
}


fun vpadButtonToString(buttonId: Int) = when (buttonId) {
    NativeInput.VPAD_BUTTON_A -> "A"
    NativeInput.VPAD_BUTTON_B -> "B"
    NativeInput.VPAD_BUTTON_X -> "X"
    NativeInput.VPAD_BUTTON_Y -> "Y"
    NativeInput.VPAD_BUTTON_L -> "L"
    NativeInput.VPAD_BUTTON_R -> "R"
    NativeInput.VPAD_BUTTON_ZL -> "ZL"
    NativeInput.VPAD_BUTTON_ZR -> "ZR"
    NativeInput.VPAD_BUTTON_PLUS -> "+"
    NativeInput.VPAD_BUTTON_MINUS -> "-"
    NativeInput.VPAD_BUTTON_UP -> tr("up")
    NativeInput.VPAD_BUTTON_DOWN -> tr("down")
    NativeInput.VPAD_BUTTON_LEFT -> tr("left")
    NativeInput.VPAD_BUTTON_RIGHT -> tr("right")
    NativeInput.VPAD_BUTTON_STICKL -> tr("click")
    NativeInput.VPAD_BUTTON_STICKR -> tr("click")
    NativeInput.VPAD_BUTTON_STICKL_UP -> tr("up")
    NativeInput.VPAD_BUTTON_STICKL_DOWN -> tr("down")
    NativeInput.VPAD_BUTTON_STICKL_LEFT -> tr("left")
    NativeInput.VPAD_BUTTON_STICKL_RIGHT -> tr("right")
    NativeInput.VPAD_BUTTON_STICKR_UP -> tr("up")
    NativeInput.VPAD_BUTTON_STICKR_DOWN -> tr("down")
    NativeInput.VPAD_BUTTON_STICKR_LEFT -> tr("left")
    NativeInput.VPAD_BUTTON_STICKR_RIGHT -> tr("right")
    NativeInput.VPAD_BUTTON_MIC -> tr("blow mic")
    NativeInput.VPAD_BUTTON_SCREEN -> tr("show screen")
    NativeInput.VPAD_BUTTON_HOME -> tr("home")
    else -> throw IllegalArgumentException("Invalid buttonId $buttonId for VPAD controller type")
}
