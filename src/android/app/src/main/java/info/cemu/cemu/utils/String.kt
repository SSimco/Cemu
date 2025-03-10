package info.cemu.cemu.utils

import java.net.URLDecoder

fun String.urlDecode(enc: String = "UTF-8"): String = URLDecoder.decode(this, enc)

fun String.toIntOrZero() = toIntOrNull() ?: 0

fun String.isContentUri() = startsWith("content://")
