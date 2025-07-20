package info.cemu.cemu.settings.general

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import info.cemu.cemu.core.components.Button
import info.cemu.cemu.core.components.ScreenContent
import info.cemu.cemu.core.components.SingleSelection
import info.cemu.cemu.core.components.Toggle
import info.cemu.cemu.core.translation.tr
import info.cemu.cemu.nativeinterface.NativeSettings

@Composable
fun GeneralSettingsScreen(
    navigateBack: () -> Unit,
    goToGamePathsSettings: () -> Unit,
    generalSettingsViewModel: GeneralSettingsViewModel = viewModel(factory = GeneralSettingsViewModel.Factory),
) {
    val context = LocalContext.current

    ScreenContent(
        appBarText = tr("General settings"),
        navigateBack = navigateBack,
    ) {
        Button(
            label = "Add game path",
            description = tr("Add the root directory of your game(s). It will scan all directories in it for games"),
            onClick = dropUnlessResumed { goToGamePathsSettings() },
        )
        SingleSelection(
            label = tr("Language"),
            initialChoice = { generalSettingsViewModel.guiSettings.language },
            onChoiceChanged = { generalSettingsViewModel.setLanguage(language = it, context) },
            choiceToString = { generalSettingsViewModel.languageToDisplayNameMap[it] ?: it },
            choices = generalSettingsViewModel.languages,
        )
        SingleSelection(
            label = tr("Console language"),
            initialChoice = NativeSettings::getConsoleLanguage,
            onChoiceChanged = NativeSettings::setConsoleLanguage,
            choiceToString = { consoleLanguageToString(it) },
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
            label = tr("Side menu button"),
            description = tr("Show the emulation side menu button"),
            initialCheckedState = { generalSettingsViewModel.emulationScreenSettings.isDrawerButtonVisible },
            onCheckedChanged = {
                generalSettingsViewModel.emulationScreenSettings.isDrawerButtonVisible = it
            }
        )
    }
}

private fun consoleLanguageToString(channels: Int): String = when (channels) {
    NativeSettings.CONSOLE_LANGUAGE_JAPANESE -> tr("Japanese")
    NativeSettings.CONSOLE_LANGUAGE_ENGLISH -> tr("English")
    NativeSettings.CONSOLE_LANGUAGE_FRENCH -> tr("French")
    NativeSettings.CONSOLE_LANGUAGE_GERMAN -> tr("German")
    NativeSettings.CONSOLE_LANGUAGE_ITALIAN -> tr("Italian")
    NativeSettings.CONSOLE_LANGUAGE_SPANISH -> tr("Spanish")
    NativeSettings.CONSOLE_LANGUAGE_CHINESE -> tr("Chinese")
    NativeSettings.CONSOLE_LANGUAGE_KOREAN -> tr("Korean")
    NativeSettings.CONSOLE_LANGUAGE_DUTCH -> tr("Dutch")
    NativeSettings.CONSOLE_LANGUAGE_PORTUGUESE -> tr("Portuguese")
    NativeSettings.CONSOLE_LANGUAGE_RUSSIAN -> tr("Russian")
    NativeSettings.CONSOLE_LANGUAGE_TAIWANESE -> tr("Taiwanese")
    else -> throw IllegalArgumentException("Invalid console language: $channels")
}