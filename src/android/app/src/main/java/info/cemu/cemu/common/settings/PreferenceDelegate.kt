package info.cemu.cemu.common.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class PreferenceDelegate<T>(
    private val sharedPreferences: SharedPreferences,
    private val defaultValue: T,
    private val getter: SharedPreferences.(String, T) -> T,
    private val setter: SharedPreferences.Editor.(String, T) -> Unit
) : ReadWriteProperty<Any, T> {
    private var cachedValue: T? = null
    private var isCached = false

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        val key = "${thisRef::class.simpleName}_${property.name}".uppercase()

        if (!isCached) {
            cachedValue = sharedPreferences.getter(key, defaultValue)
            isCached = true
        }

        return cachedValue ?: defaultValue
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        val key = "${thisRef::class.simpleName}_${property.name}".uppercase()
        cachedValue = value
        isCached = true
        sharedPreferences.edit { setter(key, value) }
    }
}

inline fun <reified T : Enum<T>> SharedPreferences.enumPref(default: T) =
    PreferenceDelegate(
        this,
        default,
        { key, default ->
            val enumOrdinal = getInt(key, default.ordinal)
            enumValues<T>().firstOrNull { it.ordinal == enumOrdinal } ?: default
        },
        { key, value -> putInt(key, value.ordinal) }
    )

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


class MapDelegate<T, V>(
    private val sharedPreferences: SharedPreferences,
    private val keyName: (T) -> String,
    private val default: V,
    private val getter: SharedPreferences.(String, V) -> V,
    private val setter: SharedPreferences.Editor.(String, V) -> Unit,
) : ReadOnlyProperty<Any, MapDelegate<T, V>.MapPreference> {
    inner class MapPreference(private val keyPrefix: String) {
        private val cache = mutableMapOf<T, V>()

        operator fun get(key: T): V {
            return cache.getOrPut(key) {
                sharedPreferences.getter("${keyPrefix}_${keyName(key)}", default)
            }
        }

        operator fun set(key: T, value: V) {
            cache[key] = value
            sharedPreferences.edit { setter("${keyPrefix}_${keyName(key)}", value) }
        }
    }

    private var map: MapPreference? = null

    override fun getValue(thisRef: Any, property: KProperty<*>): MapPreference {
        if (map == null) {
            val keyPrefix = "${thisRef::class.simpleName}_${property.name}".uppercase()
            map = MapPreference(keyPrefix)
        }

        return map!!
    }
}

fun <T> SharedPreferences.booleanMapPref(
    keyName: (T) -> String = { it.toString() },
    default: Boolean = true
) = MapDelegate(
    this,
    keyName,
    default,
    SharedPreferences::getBoolean,
    SharedPreferences.Editor::putBoolean
)
