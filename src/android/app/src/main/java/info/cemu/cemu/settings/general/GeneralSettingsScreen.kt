package info.cemu.cemu.settings.general

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import info.cemu.cemu.R
import info.cemu.cemu.guicore.components.Button
import info.cemu.cemu.guicore.components.ScreenContent
import info.cemu.cemu.guicore.components.SingleSelection
import info.cemu.cemu.guicore.components.Toggle
import info.cemu.cemu.guicore.nativeenummapper.consoleLanguageToStringId
import info.cemu.cemu.nativeinterface.NativeSettings

@Composable
fun GeneralSettingsScreen(
    navigateBack: () -> Unit,
    goToGamePathsSettings: () -> Unit,
    generalSettingsViewModel: GeneralSettingsViewModel = viewModel(factory = GeneralSettingsViewModel.Factory),
) {
    ScreenContent(
        appBarText = stringResource(R.string.general_settings),
        navigateBack = navigateBack,
    ) {
        Button(
            label = stringResource(R.string.add_game_path),
            description = stringResource(R.string.games_folder_description),
            onClick = dropUnlessResumed { goToGamePathsSettings() },
        )
        SingleSelection(
            label = stringResource(R.string.console_language),
            initialChoice = NativeSettings::getConsoleLanguage,
            onChoiceChanged = NativeSettings::setConsoleLanguage,
            choiceToString = { stringResource(consoleLanguageToStringId(it)) },
            choices = listOf(
                NativeSettings.CONSOLE_LANGUAGE_JAPANESE,
                NativeSettings.CONSOLE_LANGUAGE_ENGLISH,
                NativeSettings.CONSOLE_LANGUAGE_FRENCH,
                NativeSettings.CONSOLE_LANGUAGE_GERMAN,
                NativeSettings.CONSOLE_LANGUAGE_ITALIAN,
                NativeSettings.CONSOLE_LANGUAGE_SPANISH,
                NativeSettings.CONSOLE_LANGUAGE_CHINESE,
                NativeSettings.CONSOLE_LANGUAGE_KOREAN,
                NativeSettings.CONSOLE_LANGUAGE_DUTCH,
                NativeSettings.CONSOLE_LANGUAGE_PORTUGUESE,
                NativeSettings.CONSOLE_LANGUAGE_RUSSIAN,
                NativeSettings.CONSOLE_LANGUAGE_TAIWANESE,
            ),
        )
        Toggle(
            label = stringResource(R.string.side_menu_button),
            description = stringResource(R.string.side_menu_button_description),
            initialCheckedState = { generalSettingsViewModel.emulationScreenSettings.isDrawerButtonVisible },
            onCheckedChanged = {
                generalSettingsViewModel.emulationScreenSettings.isDrawerButtonVisible = it
            }
        )
    }
}