package info.cemu.cemu.settings.general

import android.content.Context
import androidx.lifecycle.ViewModel
import info.cemu.cemu.common.settings.EmulationSettings
import info.cemu.cemu.common.settings.GuiSettings
import info.cemu.cemu.common.settings.SettingsManager
import info.cemu.cemu.common.ui.localization.getAvailableLanguages

class GeneralSettingsViewModel : ViewModel() {
    val languages: List<String>
    val languageToDisplayNameMap: Map<String, String>
    val emulationSettings: EmulationSettings = SettingsManager.emulationSettings
    val guiSettings: GuiSettings = SettingsManager.guiSettings

    init {
        val availableLanguages = getAvailableLanguages()
        languages = availableLanguages.map { it.code }
        languageToDisplayNameMap = availableLanguages.associateBy({ it.code }, { it.displayName })
    }

    fun setLanguage(language: String, context: Context) {
        info.cemu.cemu.common.ui.localization.setLanguage(language, context)
        guiSettings.language = language
    }
}