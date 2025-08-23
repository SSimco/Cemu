package info.cemu.cemu.settings

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.dropUnlessResumed
import info.cemu.cemu.common.ui.components.Button
import info.cemu.cemu.common.ui.components.ScreenContent
import info.cemu.cemu.common.ui.localization.tr

data class SettingsHomeScreenActions(
    val goToGeneralSettings: () -> Unit,
    val goToInputSettings: () -> Unit,
    val goToGraphicsSettings: () -> Unit,
    val goToAudioSettings: () -> Unit,
    val goToAccountSettings: () -> Unit,
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
        Button(
            label = tr("Account settings"),
            onClick = dropUnlessResumed(block = actions.goToAccountSettings)
        )
    }
}