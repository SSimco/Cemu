package info.cemu.cemu.settings.general

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import info.cemu.cemu.common.ui.components.Button
import info.cemu.cemu.common.ui.components.ScreenContent
import info.cemu.cemu.common.ui.components.SingleSelection
import info.cemu.cemu.common.ui.components.Toggle
import info.cemu.cemu.common.ui.localization.tr
import info.cemu.cemu.nativeinterface.NativeSettings

@Composable
fun GeneralSettingsScreen(
    navigateBack: () -> Unit,
    goToGamePathsSettings: () -> Unit,
    generalSettingsViewModel: GeneralSettingsViewModel = viewModel(),
) {
    val context = LocalContext.current

    ScreenContent(
        appBarText = tr("General settings"),
        navigateBack = navigateBack,
    ) {
        Button(
            label = tr("Add game path"),
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
                NativeSettings.ConsoleLanguage.JAPANESE,
                NativeSettings.ConsoleLanguage.ENGLISH,
                NativeSettings.ConsoleLanguage.FRENCH,
                NativeSettings.ConsoleLanguage.GERMAN,
                NativeSettings.ConsoleLanguage.ITALIAN,
                NativeSettings.ConsoleLanguage.SPANISH,
                NativeSettings.ConsoleLanguage.CHINESE,
                NativeSettings.ConsoleLanguage.KOREAN,
                NativeSettings.ConsoleLanguage.DUTCH,
                NativeSettings.ConsoleLanguage.PORTUGUESE,
                NativeSettings.ConsoleLanguage.RUSSIAN,
                NativeSettings.ConsoleLanguage.TAIWANESE,
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
    NativeSettings.ConsoleLanguage.JAPANESE -> tr("Japanese")
    NativeSettings.ConsoleLanguage.ENGLISH -> tr("English")
    NativeSettings.ConsoleLanguage.FRENCH -> tr("French")
    NativeSettings.ConsoleLanguage.GERMAN -> tr("German")
    NativeSettings.ConsoleLanguage.ITALIAN -> tr("Italian")
    NativeSettings.ConsoleLanguage.SPANISH -> tr("Spanish")
    NativeSettings.ConsoleLanguage.CHINESE -> tr("Chinese")
    NativeSettings.ConsoleLanguage.KOREAN -> tr("Korean")
    NativeSettings.ConsoleLanguage.DUTCH -> tr("Dutch")
    NativeSettings.ConsoleLanguage.PORTUGUESE -> tr("Portuguese")
    NativeSettings.ConsoleLanguage.RUSSIAN -> tr("Russian")
    NativeSettings.ConsoleLanguage.TAIWANESE -> tr("Taiwanese")
    else -> throw IllegalArgumentException("Invalid console language: $channels")
}