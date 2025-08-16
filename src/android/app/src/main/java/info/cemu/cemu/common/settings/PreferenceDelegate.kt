package info.cemu.cemu.common.settings

import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import androidx.core.content.edit

class PreferenceDelegate<T>(
    private val sharedPreferences: SharedPreferences,
    private val defaultValue: T,
    private val getter: SharedPreferences.(String, T) -> T,
    private val setter: SharedPreferences.Editor.(String, T) -> SharedPreferences.Editor
) : ReadWriteProperty<Any, T> {

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        val key = "${thisRef::class.simpleName}_${property.name}".uppercase()
        return sharedPreferences.getter(key, defaultValue)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        val key = "${thisRef::class.simpleName}_${property.name}".uppercase()
        sharedPreferences.edit { setter(key, value) }
    }
}

fun SharedPreferences.booleanPref(default: Boolean) =
    PreferenceDelegate(
        this,
        default,
        SharedPreferences::getBoolean,
        SharedPreferences.Editor::putBoolean
    )

fun SharedPreferences.stringPref(default: String) =
    PreferenceDelegate(
        this, default,
        { key, def -> getString(key, def) ?: def },
        SharedPreferences.Editor::putString
    )

fun SharedPreferences.intPref(default: Int) =
    PreferenceDelegate(
        this,
        default,
        SharedPreferences::getInt,
        SharedPreferences.Editor::putInt
    )