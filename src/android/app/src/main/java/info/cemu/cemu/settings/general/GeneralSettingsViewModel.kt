package info.cemu.cemu.settings.general

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import info.cemu.cemu.CemuApplication
import info.cemu.cemu.core.translation.getAvailableLanguages
import info.cemu.cemu.settings.EmulationScreenSettings
import info.cemu.cemu.settings.GuiSettings
import info.cemu.cemu.settings.SettingsManager

class GeneralSettingsViewModel(private val settingsManager: SettingsManager) : ViewModel() {
    val languages: List<String>
    val languageToDisplayNameMap: Map<String, String>
    val emulationScreenSettings: EmulationScreenSettings
    val guiSettings: GuiSettings

    init {
        this.emulationScreenSettings = settingsManager.emulationScreenSettings
        this.guiSettings = settingsManager.guiSettings
        val availableLanguages = getAvailableLanguages()
        languages = availableLanguages.map { it.code }
        languageToDisplayNameMap = availableLanguages.associateBy({ it.code }, { it.displayName })
    }

    fun setLanguage(language: String, context: Context) {
        info.cemu.cemu.core.translation.setLanguage(language, context)
        guiSettings.language = language
    }

    override fun onCleared() {
        super.onCleared()
        settingsManager.emulationScreenSettings = emulationScreenSettings
        settingsManager.guiSettings = guiSettings
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                GeneralSettingsViewModel(
                    SettingsManager(CemuApplication.Application)
                )
            }
        }
    }
}