package info.cemu.cemu.settings.inputoverlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import info.cemu.cemu.common.inputoverlay.OverlayButton
import info.cemu.cemu.common.inputoverlay.OverlayDpad
import info.cemu.cemu.common.inputoverlay.OverlayInput
import info.cemu.cemu.common.inputoverlay.OverlayJoystick
import info.cemu.cemu.common.ui.components.Header
import info.cemu.cemu.common.ui.components.ScreenContent
import info.cemu.cemu.common.ui.components.SingleSelection
import info.cemu.cemu.common.ui.components.Slider
import info.cemu.cemu.common.ui.components.Toggle
import info.cemu.cemu.common.ui.localization.tr
import info.cemu.cemu.nativeinterface.NativeInput

private val ControllerIndexChoices = (0..<NativeInput.MAX_CONTROLLERS).toList()

@Composable
fun InputOverlaySettingsScreen(
    viewModel: InputOverlaySettingsViewModel = viewModel(),
    navigateBack: () -> Unit,
) {
    val overlaySettings by viewModel.overlaySettings.collectAsState()

    @Composable
    fun VisibleInputToggle(inputName: String, input: OverlayInput) {
        Toggle(
            label = inputName,
            initialCheckedState = { overlaySettings.inputVisibilityMap[input] ?: true },
            onCheckedChanged = { viewModel.setInputVisibility(input, it) }
        )
    }

    ScreenContent(
        appBarText = tr("Input overlay settings"),
        navigateBack = navigateBack
    )
    {
        Toggle(
            label = tr("Input overlay"),
            description = tr("Enable input overlay"),
            initialCheckedState = { overlaySettings.isOverlayEnabled },
            onCheckedChanged = { viewModel.setOverlayEnabled(it) }
        )

        Toggle(
            label = tr("Vibrate"),
            description = tr("Enable vibrate on touch"),
            initialCheckedState = { overlaySettings.isVibrateOnTouchEnabled },
            onCheckedChanged = { viewModel.setVibrateOnTouch(it) }
        )

        Slider(
            label = tr("Inputs opacity"),
            initialValue = { overlaySettings.alpha },
            valueFrom = 0,
            steps = 16,
            valueTo = 255,
            onValueChange = { viewModel.setAlpha(it) },
            labelFormatter = { it.toString() },
        )

        SingleSelection(
            label = tr("Overlay controller"),
            initialChoice = { overlaySettings.controllerIndex },
            choices = ControllerIndexChoices,
            choiceToString = { tr("Controller {0}", it + 1) },
            onChoiceChanged = { viewModel.setControllerIndex(it) },
        )

        Header(tr("Visible Inputs"))

        VisibleInputToggle("A", OverlayButton.A)
        VisibleInputToggle("B", OverlayButton.B)
        VisibleInputToggle("X", OverlayButton.X)
        VisibleInputToggle("Y", OverlayButton.Y)
        VisibleInputToggle("1", OverlayButton.ONE)
        VisibleInputToggle("2", OverlayButton.TWO)
        VisibleInputToggle("C", OverlayButton.C)
        VisibleInputToggle("Z", OverlayButton.Z)
        VisibleInputToggle(tr("Home"), OverlayButton.HOME)
        VisibleInputToggle("L", OverlayButton.L)
        VisibleInputToggle("R", OverlayButton.R)
        VisibleInputToggle(tr("Left stick click"), OverlayButton.L_STICK_CLICK)
        VisibleInputToggle(tr("Right stick click"), OverlayButton.R_STICK_CLICK)
        VisibleInputToggle(tr("Plus"), OverlayButton.PLUS)
        VisibleInputToggle(tr("Minus"), OverlayButton.MINUS)
        VisibleInputToggle("ZL", OverlayButton.ZL)
        VisibleInputToggle("ZR", OverlayButton.ZR)
        VisibleInputToggle(tr("Blow mic"), OverlayButton.BLOW_MIC)

        VisibleInputToggle(tr("Left joystick"), OverlayJoystick.LEFT)
        VisibleInputToggle(tr("Right joystick"), OverlayJoystick.RIGHT)

        VisibleInputToggle(tr("DPad"), OverlayDpad.DPAD_UP)
    }
}
