package info.cemu.cemu.settings.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.dropUnlessResumed
import info.cemu.cemu.common.components.Button
import info.cemu.cemu.common.components.ScreenContent
import info.cemu.cemu.common.nativeenummapper.controllerTypeToString
import info.cemu.cemu.common.translation.tr
import info.cemu.cemu.nativeinterface.NativeInput

data class InputSettingsScreenActions(
    val goToInputOverlaySettings: () -> Unit,
    val goToControllerSettings: (controllerIndex: Int) -> Unit,
)

@Composable
fun InputSettingsScreen(navigateBack: () -> Unit, actions: InputSettingsScreenActions) {
    val controllers = remember {
        (0..<NativeInput.MAX_CONTROLLERS).map { controllerIndex ->
            controllerIndex to getControllerType(controllerIndex)
        }
    }
    ScreenContent(
        appBarText = tr("Input settings"),
        navigateBack = navigateBack,
    ) {
        Button(
            label = tr("Input overlay settings"),
            onClick = dropUnlessResumed { actions.goToInputOverlaySettings() },
        )
        controllers.forEach { controllerTypePair ->
            val (controllerIndex, controllerEmulatedType) = controllerTypePair
            Button(
                label = tr("Controller {0}", controllerIndex + 1),
                description = tr(
                    "Emulated controller: {0}",
                    controllerTypeToString(controllerEmulatedType)
                ),
                onClick = dropUnlessResumed { actions.goToControllerSettings(controllerIndex) },
            )
        }
    }
}

fun getControllerType(index: Int): Int =
    if (NativeInput.isControllerDisabled(index))
        NativeInput.EmulatedControllerType.DISABLED
    else NativeInput.getControllerType(index)
