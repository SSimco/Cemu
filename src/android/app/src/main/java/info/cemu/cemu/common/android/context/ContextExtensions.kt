package info.cemu.cemu.common.android.context

import android.content.Context
import java.io.File

fun Context.internalFolder(): File {
    val externalFilesDir = getExternalFilesDir(null)
    if (externalFilesDir != null) {
        return externalFilesDir
    }
    return filesDir
}