package info.cemu.cemu.titlemanager

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.cemu.cemu.nativeinterface.NativeActiveSettings
import info.cemu.cemu.nativeinterface.NativeGameTitles
import info.cemu.cemu.nativeinterface.NativeGameTitles.TitleIdToTitlesCallback.Title
import info.cemu.cemu.nativeinterface.fromNativePath
import info.cemu.cemu.common.io.copyInputStreamToFile
import info.cemu.cemu.common.string.isContentUri
import info.cemu.cemu.common.string.urlDecode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.File
import java.nio.file.Path
import java.util.LinkedList
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.relativeToOrNull
import kotlin.math.max
import kotlin.random.Random
import kotlin.random.nextUInt

enum class EntryPath {
    MLC,
    GamePaths,
}

data class TitleListFilter(
    val query: String,
    val types: Set<EntryType>,
    val formats: Set<EntryFormat>,
    val paths: Set<EntryPath>,
)

interface TitleDeleteCallbacks {
    fun onDeleteFinished()
    fun onError()
}

interface TitleInstallCallbacks {
    fun onInstallFinished()
    fun onError()
}

class FilterActions<T>(
    private val currentFilters: () -> Set<T>,
    private val updateFilters: (Set<T>) -> Unit,
) {
    fun add(filter: T) {
        updateFilters(currentFilters() + filter)
    }

    fun remove(filter: T) {
        updateFilters(currentFilters() - filter)
    }
}

class TitleListViewModel : ViewModel() {
    private val mlcPath = Path(NativeActiveSettings.getMLCPath())
    private fun isPathInMLC(path: String): Boolean =
        Path(path).relativeToOrNull(mlcPath)?.let { it.startsWith("sys") || it.startsWith("usr") }
            ?: false

    private val _filter = MutableStateFlow(
        TitleListFilter(
            query = "",
            types = EntryType.entries.toSet(),
            formats = EntryFormat.entries.toSet(),
            paths = EntryPath.entries.toSet(),
        )
    )
    val filter = _filter.asStateFlow()

    fun setFilterQuery(query: String) {
        _filter.value = _filter.value.copy(query = query)
    }

    val typesFilter =
        FilterActions(
            { _filter.value.types },
            { _filter.value = _filter.value.copy(types = it) })

    val formatsFilter =
        FilterActions(
            { _filter.value.formats },
            { _filter.value = _filter.value.copy(formats = it) })

    val pathsFilter =
        FilterActions(
            { _filter.value.paths },
            { _filter.value = _filter.value.copy(paths = it) })

    private val _titleEntries = MutableStateFlow<List<TitleEntry>>(emptyList())
    val titleEntries = filter.combine(_titleEntries) { filter, entries ->
        entries.filter { entry ->
            val isTypeMatching = entry.type in filter.types
            val isFormatMatching = entry.format in filter.formats
            val isPathMatching = when {
                entry.isInMLC -> EntryPath.MLC in filter.paths
                else -> EntryPath.GamePaths in filter.paths
            }
            val isQueryMatching =
                filter.query.isBlank() || entry.name.contains(filter.query, ignoreCase = true)
            isTypeMatching && isFormatMatching && isPathMatching && isQueryMatching
        }.sortedBy { it.name }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val titleListCallbacks = object : NativeGameTitles.TitleListCallbacks {
        override fun onTitleDiscovered(titleData: NativeGameTitles.TitleData) {
            addTitle(titleData.toTitleEntry(isPathInMLC(titleData.path)))
        }

        override fun onTitleRemoved(locationUID: Long) {
            _titleEntries.value =
                _titleEntries.value.toMutableList()
                    .apply {
                        val index = indexOfFirst { it.locationUID == locationUID }
                        if (index >= 0) removeAt(index)
                    }
        }
    }

    private val saveListCallback = NativeGameTitles.SaveListCallback { saveData ->
        addTitle(saveData.toTitleEntry(isPathInMLC(saveData.path)))
    }

    private var lastRefreshTime = 0L
    fun refresh() {
        if (SystemClock.elapsedRealtime() - lastRefreshTime >= REFRESH_DEBOUNCE_TIME_MILLISECONDS) {
            lastRefreshTime = SystemClock.elapsedRealtime()
            NativeGameTitles.refreshCafeTitleList()
        }
    }

    init {
        NativeGameTitles.setTitleListCallbacks(titleListCallbacks)
        NativeGameTitles.setSaveListCallback(saveListCallback)
    }

    override fun onCleared() {
        super.onCleared()
        NativeGameTitles.setTitleListCallbacks(null)
        NativeGameTitles.setSaveListCallback(null)
    }

    private fun addTitle(titleEntry: TitleEntry) {
        if (_titleEntries.value.any { it.locationUID == titleEntry.locationUID }) return
        _titleEntries.value += titleEntry
    }

    private val _titleToBeDeleted = MutableStateFlow<TitleEntry?>(null)
    val titleToBeDeleted = _titleToBeDeleted.asStateFlow()
    fun deleteTitleEntry(
        titleEntry: TitleEntry,
        context: Context,
        deleteCallbacks: TitleDeleteCallbacks,
    ) {
        if (_titleToBeDeleted.value != null)
            return

        if (!_titleEntries.value.any { it.locationUID == titleEntry.locationUID }) {
            deleteCallbacks.onDeleteFinished()
            return
        }

        _titleToBeDeleted.value = titleEntry

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!delete(context.contentResolver, titleEntry.path)) {
                    deleteCallbacks.onError()
                    return@launch
                }
                _titleEntries.value =
                    _titleEntries.value.filter { it.locationUID != titleEntry.locationUID && it.path != titleEntry.path }
                deleteCallbacks.onDeleteFinished()
            } catch (e: Exception) {
                deleteCallbacks.onError()
            } finally {
                _titleToBeDeleted.value = null
            }
        }
    }

    private val _queuedTitleToInstall =
        MutableStateFlow<Pair<Uri, NativeGameTitles.TitleExistsStatus>?>(null)
    val queuedTitleToInstallError =
        _queuedTitleToInstall.map { it?.second?.existsError }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    fun queueTitleToInstall(titlePath: Uri, onInvalidTitle: () -> Unit) {
        if (_queuedTitleToInstall.value != null)
            return

        val existsStatus = NativeGameTitles.checkIfTitleExists(titlePath.toString())
        if (existsStatus == null) {
            onInvalidTitle()
            return
        }

        _queuedTitleToInstall.value = Pair(titlePath, existsStatus)
    }

    fun removedQueuedTitleToInstall() {
        _queuedTitleToInstall.value = null
    }

    private var installTitleJob: Job? = null
    fun cancelInstall() {
        installTitleJob?.cancel()
        installTitleJob = null
    }

    private suspend fun listFilesInSourceDirs(
        contentResolver: ContentResolver,
        titleDir: DocumentFile,
        titleUri: Uri,
        targetLocation: String,
    ): Pair<Long, LinkedList<DirEntry>> {
        val entries = LinkedList<DirEntry>()
        var totalSize = 0L

        for (sourceDir in SOURCE_DIRS) {
            val parentUri = titleDir.findFile(sourceDir)!!.uri
            val parentUriLength = titleUri.toString().length
            val uriToTargetPath: (Uri) -> Path = {
                val relativePath = it.toString().substring(parentUriLength).urlDecode()
                Path(targetLocation, relativePath)
            }

            entries += DirEntry.Dir(Path(targetLocation, sourceDir))
            listFilesRecursively(
                contentResolver = contentResolver,
                dirUri = parentUri,
                onFile = { uri, sizeInBytes ->
                    totalSize += sizeInBytes
                    entries += DirEntry.File(
                        uri,
                        uriToTargetPath(uri),
                        sizeInBytes
                    )
                },
                onDir = { entries += DirEntry.Dir(uriToTargetPath(it)) },
            )
        }

        totalSize = max(totalSize, 1L)

        return Pair(totalSize, entries)
    }

    private val _titleInstallInProgress = MutableStateFlow(false)
    val titleInstallInProgress = _titleInstallInProgress.asStateFlow()

    private val _titleInstallProgress = MutableStateFlow<Pair<Long, Long>?>(null)
    val titleInstallProgress = _titleInstallProgress.asStateFlow()

    fun installQueuedTitle(
        context: Context,
        titleInstallCallbacks: TitleInstallCallbacks,
    ) {
        if (_titleInstallInProgress.value)
            return

        val titleToInstall = _queuedTitleToInstall.value
        if (titleToInstall == null) {
            titleInstallCallbacks.onInstallFinished()
            return
        }
        _queuedTitleToInstall.value = null

        val titleUri = titleToInstall.first
        val targetLocation = titleToInstall.second.targetLocation

        val installPath = Path(targetLocation)
        val installFile = installPath.toFile()
        val backupFile = installPath.getBackupFile()

        _titleInstallProgress.value = null
        _titleInstallInProgress.value = true

        val oldInstallJob = installTitleJob
        installTitleJob = viewModelScope.launch(Dispatchers.IO) {
            var installStarted = false
            try {
                cleanupJob?.join()
                oldInstallJob?.join()
                val contentResolver = context.contentResolver
                val buffer = ByteArray(8192)

                val (totalSize, entries) = listFilesInSourceDirs(
                    contentResolver = contentResolver,
                    titleDir = DocumentFile.fromTreeUri(context, titleUri)!!,
                    titleUri = titleUri,
                    targetLocation = targetLocation,
                )

                if (totalSize > mlcPath.toFile().freeSpace) {
                    titleInstallCallbacks.onError()
                    return@launch
                }

                backupFile.deleteRecursively()
                if (installFile.exists())
                    installFile.renameTo(backupFile)

                installStarted = true
                _titleInstallProgress.value = Pair(0, totalSize)
                var bytesWritten = 0L

                for (file in entries) {
                    yield()
                    when (file) {
                        is DirEntry.Dir -> {
                            file.destinationPath.createDirectories()
                        }

                        is DirEntry.File -> contentResolver.openInputStream(file.uri)?.use {
                            copyInputStreamToFile(it, file.destinationPath, buffer)
                            bytesWritten += file.sizeInBytes
                            _titleInstallProgress.value = Pair(bytesWritten, totalSize)
                        }
                    }
                }

                if (backupFile.exists())
                    backupFile.deleteRecursively()

                NativeGameTitles.addTitleFromPath(targetLocation)
                titleInstallCallbacks.onInstallFinished()
            } catch (exception: Exception) {
                if (installStarted)
                    cleanupInstall(installFile)

                if (exception !is CancellationException)
                    titleInstallCallbacks.onError()
            } finally {
                _titleInstallInProgress.value = false
            }
        }
    }

    private var cleanupJob: Job? = null
    private fun cleanupInstall(installFile: File) {
        val oldCleanupJob = cleanupJob
        cleanupJob = viewModelScope.launch(Dispatchers.IO) {
            oldCleanupJob?.join()
            val installPath = installFile.toPath()
            var tempFile: File? = null
            if (installFile.exists()) {
                val tempName = "${installPath.fileName}-${Random.nextUInt()}"
                tempFile = installPath.resolveSibling(tempName).toFile()
                installFile.renameTo(tempFile!!)
            }
            val backupInstall = installPath.getBackupFile()
            if (backupInstall.exists())
                backupInstall.renameTo(installFile)
            tempFile?.deleteRecursively()
        }
    }

    private val _queuedTitleToCompress =
        MutableStateFlow<NativeGameTitles.CompressTitleInfo?>(null)
    val queuedTitleToCompress = _queuedTitleToCompress.asStateFlow()
    fun queueTitleForCompression(titleEntry: TitleEntry) {
        require(
            value = titleEntry.type != EntryType.Save && titleEntry.format != EntryFormat.WUA,
            lazyMessage = { "Invalid title queued for compression. ${titleEntry.name} (${titleEntry.type.name}) (${titleEntry.format.name})" }
        )

        _queuedTitleToCompress.value = NativeGameTitles.queueTitleToCompress(
            titleId = titleEntry.titleId,
            selectedUID = titleEntry.locationUID,
            titlesCallback = { titleId ->
                titleEntries.value.filter { it.titleId == titleId }
                    .map { Title(it.version, it.locationUID) }
                    .toTypedArray()
            })
    }

    fun removeQueuedTitleForCompression() {
        _queuedTitleToCompress.value = null
    }

    private val _compressProgress = MutableStateFlow<Long?>(null)
    val compressProgress = _compressProgress.asStateFlow()
    private var compressProgressJob: Job? = null

    private val _compressInProgress = MutableStateFlow(false)
    val compressInProgress = _compressInProgress.asStateFlow()

    fun compressQueuedTitle(
        context: Context,
        uri: Uri,
        onFinished: () -> Unit,
        onError: () -> Unit,
    ) {
        if (_compressInProgress.value)
            return

        _queuedTitleToCompress.value = null

        val fd = context.contentResolver.openFileDescriptor(uri, "rw") ?: return

        val oldCompressProgressJob = compressProgressJob
        compressProgressJob = viewModelScope.launch {
            oldCompressProgressJob?.cancelAndJoin()
            _compressInProgress.value = true
            try {
                while (true) {
                    delay(500)
                    _compressProgress.value = NativeGameTitles.getCurrentProgressForCompression()
                }
            } catch (_: CancellationException) {
            } finally {
                _compressInProgress.value = false
                _compressProgress.value = null
            }
        }

        NativeGameTitles.compressQueuedTitle(
            fd = fd.detachFd(),
            compressCallbacks = object : NativeGameTitles.TitleCompressCallbacks {
                override fun onFinished() {
                    compressProgressJob?.cancel()
                    onFinished()
                }

                override fun onError() {
                    compressProgressJob?.cancel()
                    onError()
                }
            })
    }

    fun cancelCompression() {
        viewModelScope.launch(Dispatchers.IO) {
            compressProgressJob?.cancelAndJoin()
            NativeGameTitles.cancelTitleCompression()
        }
    }

    fun getCompressedFileNameForQueuedTitle() =
        NativeGameTitles.getCompressedFileNameForQueuedTitle()

    companion object {
        private const val REFRESH_DEBOUNCE_TIME_MILLISECONDS = 1500L
        private val SOURCE_DIRS = arrayOf(
            "content",
            "code",
            "meta",
        )
    }
}

private fun delete(contentResolver: ContentResolver, path: String): Boolean {
    if (path.isContentUri()) {
        return DocumentsContract.deleteDocument(
            contentResolver,
            path.fromNativePath()
        )
    }

    return Path(path).toFile().deleteRecursively()
}

private fun NativeGameTitles.SaveData.toTitleEntry(isInMlc: Boolean) = TitleEntry(
    titleId = this.titleId,
    name = this.name,
    path = this.path,
    isInMLC = isInMlc,
    locationUID = this.locationUID,
    version = this.version,
    region = this.region,
    type = EntryType.Save,
    format = EntryFormat.SaveFolder,
)

private fun NativeGameTitles.TitleData.toTitleEntry(isInMlc: Boolean) = TitleEntry(
    titleId = this.titleId,
    name = this.name,
    path = this.path,
    isInMLC = isInMlc,
    locationUID = this.locationUID,
    version = this.version,
    region = this.region,
    type = nativeTitleTypeToEnum(this.titleType),
    format = nativeTitleFormatToEnum(this.titleDataFormat),
)

private fun Path.getBackupFile() = resolveSibling("$fileName.backup").toFile()

private sealed class DirEntry {
    data class File(val uri: Uri, val destinationPath: Path, val sizeInBytes: Long) : DirEntry()
    data class Dir(val destinationPath: Path) : DirEntry()
}

private suspend fun listFilesRecursively(
    contentResolver: ContentResolver,
    dirUri: Uri,
    onFile: (Uri, Long) -> Unit,
    onDir: (Uri) -> Unit,
) {
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
        dirUri,
        DocumentsContract.getDocumentId(dirUri)
    )
    val cursor = contentResolver.query(
        childrenUri,
        arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        ),
        null,
        null,
        null
    )
    cursor?.use {
        while (it.moveToNext()) {
            yield()

            val documentId = it.getString(0)
            val documentUri = DocumentsContract.buildDocumentUriUsingTree(dirUri, documentId)

            val mimeType = it.getString(2)
            if (mimeType != DocumentsContract.Document.MIME_TYPE_DIR) {
                val sizeInBytes = it.getLong(1)
                onFile(documentUri, sizeInBytes)
                continue
            }

            onDir(documentUri)

            listFilesRecursively(
                contentResolver,
                DocumentsContract.buildDocumentUriUsingTree(dirUri, documentId),
                onFile,
                onDir,
            )
        }
    }
}
