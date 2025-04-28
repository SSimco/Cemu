@file:Suppress("unused")

package info.cemu.cemu.nativeinterface

import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.annotation.Keep
import info.cemu.cemu.CemuApplication

private const val PATH_SEPARATOR_ENCODED = "%2F"
private const val PATH_SEPARATOR_DECODED = "/"
private const val COLON_ENCODED = "%3A"
private const val MODE = "r"

private val ContentResolver
    get() = CemuApplication.Application.applicationContext.contentResolver

fun Uri.toNativePath(): String {
    val uriPath = toString()
    val delimiterPos = uriPath.lastIndexOf(COLON_ENCODED)
    if (delimiterPos == -1) {
        return uriPath
    }
    return uriPath.substring(0, delimiterPos) + uriPath.substring(delimiterPos).replace(
        PATH_SEPARATOR_ENCODED, PATH_SEPARATOR_DECODED
    )
}

fun String.fromNativePath(): Uri {
    val delimiterPos = lastIndexOf(COLON_ENCODED)
    if (delimiterPos == -1) {
        return Uri.parse(this)
    }
    return Uri.parse(
        substring(0, delimiterPos) + substring(delimiterPos).replace(
            PATH_SEPARATOR_DECODED, PATH_SEPARATOR_ENCODED
        )
    )
}

@Keep
fun openContentUri(uri: String): Int {
    try {
//        Log.i("NATIVE_FILES", "openContentUri $uri")
        val parcelFileDescriptor =
            ContentResolver.openFileDescriptor(
                uri.fromNativePath(), MODE
            )
        if (parcelFileDescriptor != null) {
            val fd = parcelFileDescriptor.detachFd()
            parcelFileDescriptor.close()
            return fd
        }
    } catch (e: Exception) {
        Log.e("NativeFiles", "Cannot open content uri, error: ${e.message}")
    }
    return -1
}

@Keep
fun listFiles(uri: String): Array<String?> {
////    Log.i("NATIVE_FILES", "listFiles $uri")

    val files = ArrayList<String>()
    val directoryUri = uri.fromNativePath()
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
        directoryUri,
        DocumentsContract.getDocumentId(directoryUri)
    )
    try {
        ContentResolver.query(
            childrenUri,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
            null,
            null,
            null
        ).use { cursor ->
            while (cursor != null && cursor.moveToNext()) {
                val documentId = cursor.getString(0)
                val documentUri =
                    DocumentsContract.buildDocumentUriUsingTree(directoryUri, documentId)
                files.add(documentUri.toNativePath())
            }
        }
    } catch (e: Exception) {
        Log.e("NativeFiles", "Cannot list files: ${e.message}")
    }
    var filesArray = arrayOfNulls<String>(files.size)
    filesArray = files.toArray(filesArray)
    return filesArray
}

@Keep
fun isDirectory(uri: String): Boolean {
//    Log.i("NATIVE_FILES", "isDirectory $uri")

    val mimeType = ContentResolver.getType(
        uri.fromNativePath()
    )
    return DocumentsContract.Document.MIME_TYPE_DIR == mimeType
}

@Keep
fun isFile(uri: String): Boolean {
    return !isDirectory(uri)
}

@Keep
fun exists(uri: String): Boolean {
//    Log.i("NATIVE_FILES", "exists $uri")

    try {
        ContentResolver.query(uri.fromNativePath(), null, null, null, null).use { cursor ->
            return cursor != null && cursor.moveToFirst()
        }
    } catch (e: Exception) {
        Log.e("NativeFiles", "Failed checking if file exists: ${e.message}")
        return false
    }
}
