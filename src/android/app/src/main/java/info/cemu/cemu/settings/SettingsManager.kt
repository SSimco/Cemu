package info.cemu.cemu.settings

import android.content.Context
import android.content.SharedPreferences

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

class SettingsManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)

    var emulationScreenSettings: EmulationScreenSettings
        get() = EmulationScreenSettings(sharedPreferences)
        set(value) = value.save(sharedPreferences)


    companion object {
        private const val SETTINGS_NAME = "SETTINGS"
    }
}
