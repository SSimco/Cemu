package info.cemu.cemu.titlemanager.usecases

import android.content.Context
import android.net.Uri
import info.cemu.cemu.nativeinterface.NativeGameTitles
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private typealias NativeCompressResult = NativeGameTitles.CompressResult

enum class CompressResult {
    FINISHED,
    ERROR,
}

class CompressTitleUseCase(private val scope: CoroutineScope) {
    private val _inProgress = MutableStateFlow(false)
    val inProgress = _inProgress.asStateFlow()

    private val _progress = MutableStateFlow<Long?>(null)
    val progress = _progress.asStateFlow()

    private var compressJob: Job? = null
    private var progressJob: Job? = null


    fun cancel() {
        scope.launch(Dispatchers.IO) {
            progressJob?.cancelAndJoin()
            NativeGameTitles.cancelTitleCompression()
            _inProgress.value = false
            _progress.value = null
        }
    }

    fun compress(
        context: Context,
        uri: Uri,
        callback: (CompressResult) -> Unit
    ) {
        if (_inProgress.value) return

        val fd = context.contentResolver.openFileDescriptor(uri, "rw")

        if (fd == null) {
            callback(CompressResult.ERROR)
            return
        }

        val oldProgressJob = progressJob
        compressJob = scope.launch {
            oldProgressJob?.cancelAndJoin()
            _inProgress.value = true

            try {
                progressJob = launch {
                    while (isActive) {
                        delay(500)
                        _progress.value = NativeGameTitles.getCurrentProgressForCompression()
                    }
                }

                NativeGameTitles.compressQueuedTitle(
                    fd = fd.detachFd(),
                    callback = { result ->
                        when (result) {
                            NativeCompressResult.FINISHED -> callback(CompressResult.FINISHED)
                            NativeCompressResult.ERROR -> callback(CompressResult.ERROR)
                        }

                        progressJob?.cancel()
                        _inProgress.value = false
                        _progress.value = null
                    }
                )
            } catch (_: Exception) {
                progressJob?.cancel()

                _inProgress.value = false
                _progress.value = null

                callback(CompressResult.ERROR)
            }
        }
    }
}
