package info.cemu.cemu.settings.inputoverlay

import androidx.lifecycle.ViewModel
import info.cemu.cemu.common.settings.SettingsManager

class InputOverlaySettingsViewModel : ViewModel() {
    val overlaySettings = SettingsManager.inputOverlaySettings
}