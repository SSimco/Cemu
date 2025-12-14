package info.cemu.cemu.common.android.util

import android.util.Log
import info.cemu.cemu.BuildConfig

object LogDebug {
    fun v(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.v(tag, message)
    }

    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, message)
    }

    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.i(tag, message)
    }

    fun w(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.w(tag, message)
    }

    fun e(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.e(tag, message)
    }

    fun wtf(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.wtf(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable) {
        if (BuildConfig.DEBUG) Log.e(tag, message, throwable)
    }

    fun w(tag: String, message: String, throwable: Throwable) {
        if (BuildConfig.DEBUG) Log.w(tag, message, throwable)
    }
}