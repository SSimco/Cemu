package info.cemu.cemu.settings

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.dropUnlessResumed
import info.cemu.cemu.core.components.Button
import info.cemu.cemu.core.components.ScreenContent
import info.cemu.cemu.core.translation.tr

data class SettingsHomeScreenActions(
    val goToGeneralSettings: () -> Unit,
    val goToInputSettings: () -> Unit,
    val goToGraphicsSettings: () -> Unit,
    val goToAudioSettings: () -> Unit,
    val goToOverlaySettings: () -> Unit,
)

@Composable
fun SettingsHomeScreen(navigateBack: () -> Unit, actions: SettingsHomeScreenActions) {
    ScreenContent(
        appBarText = tr("Settings"),
        navigateBack = navigateBack,
    ) {
        Button(
            label = tr("General settings"),
            onClick = dropUnlessResumed(block = actions.goToGeneralSettings)
        )
        Button(
            label = tr("Input settings"),
            onClick = dropUnlessResumed(block = actions.goToInputSettings)
        )
        Button(
            label = tr("Graphics settings"),
            onClick = dropUnlessResumed(block = actions.goToGraphicsSettings)
        )
        Button(
            label = tr("Audio settings"),
            onClick = dropUnlessResumed(block = actions.goToAudioSettings)
        )
        Button(
            label = tr("Overlay settings"),
            onClick = dropUnlessResumed(block = actions.goToOverlaySettings)
        )
    }
}