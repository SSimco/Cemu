package info.cemu.cemu.settings.inputoverlay

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import info.cemu.cemu.common.ui.components.ScreenContent
import info.cemu.cemu.common.ui.components.SingleSelection
import info.cemu.cemu.common.ui.components.Slider
import info.cemu.cemu.common.ui.components.Toggle
import info.cemu.cemu.common.ui.localization.tr
import info.cemu.cemu.nativeinterface.NativeInput

private val ControllerIndexChoices = (0..<NativeInput.MAX_CONTROLLERS).toList()

@Composable
fun InputOverlaySettingsScreen(
    inputOverlaySettingsViewModel: InputOverlaySettingsViewModel = viewModel(),
    navigateBack: () -> Unit,
) {
    val overlaySettings = inputOverlaySettingsViewModel.overlaySettings
    ScreenContent(
        appBarText = tr("Input overlay settings"),
        navigateBack = navigateBack
    )
    {
        Toggle(
            label = tr("Input overlay"),
            description = tr("Enable input overlay"),
            initialCheckedState = { overlaySettings.isOverlayEnabled },
            onCheckedChanged = { overlaySettings.isOverlayEnabled = it }
        )
        Toggle(
            label = tr("Vibrate"),
            description = tr("Enable vibrate on touch"),
            initialCheckedState = { overlaySettings.isVibrateOnTouchEnabled },
            onCheckedChanged = { overlaySettings.isVibrateOnTouchEnabled = it }
        )
        Slider(
            label = tr("Inputs opacity"),
            initialValue = { overlaySettings.alpha },
            valueFrom = 0,
            valueTo = 255,
            onValueChange = { overlaySettings.alpha = it },
            labelFormatter = { "${(100 * it) / 255}%" },
        )
        SingleSelection(
            label = tr("Overlay controller"),
            initialChoice = { overlaySettings.controllerIndex },
            choices = ControllerIndexChoices,
            choiceToString = { tr("Controller {0}", it + 1) },
            onChoiceChanged = { overlaySettings.controllerIndex = it }
        )
    }
}
