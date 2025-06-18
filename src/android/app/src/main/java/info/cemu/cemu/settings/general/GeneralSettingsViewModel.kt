package info.cemu.cemu.settings.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import info.cemu.cemu.CemuApplication
import info.cemu.cemu.settings.SettingsManager

class GeneralSettingsViewModel(
    private val settingsManager: SettingsManager,
) : ViewModel() {
    val emulationScreenSettings = settingsManager.emulationScreenSettings

    override fun onCleared() {
        super.onCleared()
        settingsManager.emulationScreenSettings = emulationScreenSettings
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