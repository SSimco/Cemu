package info.cemu.cemu.common.settings

import android.content.Context
import android.content.SharedPreferences
import info.cemu.cemu.common.ui.localization.DEFAULT_LANGUAGE
import kotlin.getValue

class EmulationScreenSettings(sharedPreferences: SharedPreferences) {
    var isDrawerButtonVisible by sharedPreferences.booleanPref(false)
}

class GuiSettings(sharedPreferences: SharedPreferences) {
    var language by sharedPreferences.stringPref(DEFAULT_LANGUAGE)
}

class InputOverlaySettings(sharedPreferences: SharedPreferences) {
    var isVibrateOnTouchEnabled by sharedPreferences.booleanPref(false)
    var isOverlayEnabled by sharedPreferences.booleanPref(false)
    var controllerIndex by sharedPreferences.intPref(0)
    var alpha by sharedPreferences.intPref(64)
}

object SettingsManager {
    fun initialize(context: Context) {
        sharedPreferences =
            context.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
    }

    private lateinit var sharedPreferences: SharedPreferences

    val emulationScreenSettings by lazy { EmulationScreenSettings(sharedPreferences) }

    val guiSettings by lazy { GuiSettings(sharedPreferences) }

    val inputOverlaySettings by lazy { InputOverlaySettings(sharedPreferences) }

    private const val SETTINGS_NAME = "SETTINGS"
}
