package info.cemu.cemu.settings

import android.content.Context
import android.content.SharedPreferences
import info.cemu.cemu.core.translation.DEFAULT_LANGUAGE

class EmulationScreenSettings(
    var isDrawerButtonVisible: Boolean,
) {
    companion object {
        private const val IS_BUTTON_VISIBLE_KEY = "IS_BUTTON_VISIBLE"
    }

    constructor(sharedPreferences: SharedPreferences) : this(
        isDrawerButtonVisible = sharedPreferences.getBoolean(
            IS_BUTTON_VISIBLE_KEY,
            false
        ),
    )

    fun save(sharedPreferences: SharedPreferences) {
        sharedPreferences.edit().apply {
            putBoolean(IS_BUTTON_VISIBLE_KEY, isDrawerButtonVisible)
            apply()
        }
    }
}

class GuiSettings(var language: String) {
    companion object {
        private const val LANGUAGE_KEY = "LANGUAGE"
    }

    constructor(sharedPreferences: SharedPreferences) : this(
        language = sharedPreferences.getString(
            LANGUAGE_KEY,
            null
        ) ?: DEFAULT_LANGUAGE,
    )

    fun save(sharedPreferences: SharedPreferences) {
        sharedPreferences.edit().apply {
            putString(LANGUAGE_KEY, language)
            apply()
        }
    }
}

class SettingsManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)

    var emulationScreenSettings: EmulationScreenSettings
        get() = EmulationScreenSettings(sharedPreferences)
        set(value) = value.save(sharedPreferences)

    var guiSettings: GuiSettings
        get() = GuiSettings(sharedPreferences)
        set(value) = value.save(sharedPreferences)

    companion object {
        private const val SETTINGS_NAME = "SETTINGS"
    }
}
