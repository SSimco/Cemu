package info.cemu.cemu.graphicpacks

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.cemu.cemu.nativeinterface.NativeGameTitles
import info.cemu.cemu.nativeinterface.NativeGraphicPacks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class GraphicPacksListViewModel : ViewModel() {
    private val installedTitleIds = NativeGameTitles.getInstalledGamesTitleIds()
    private var rootNode = GraphicPackSectionNode()

    private val _installedOnly = MutableStateFlow(installedTitleIds.size > 1)
    val installedOnly = _installedOnly.asStateFlow()
    fun setInstalledOnly(installedOnly: Boolean) {
        _installedOnly.value = installedOnly
    }

    private val _downloadStatus = MutableStateFlow<GraphicPacksDownloadStatus?>(null)
    private suspend fun updateDownloadStatus(status: GraphicPacksDownloadStatus?) {
        _downloadStatus.first { it == null }
        _downloadStatus.value = status
    }

    val downloadStatus = _downloadStatus.asStateFlow()

    @Volatile
    private var downloadJob: Job? = null
    fun downloadNewUpdate(context: Context) {
        if (_downloadStatus.value != null) return
        downloadJob = viewModelScope.launch {
            try {
                GraphicPacksDownloader.download(context) { updateDownloadStatus(it) }
                refreshGraphicPacks()
            } catch (exception: Exception) {
                updateDownloadStatus(GraphicPacksDownloadStatus.ERROR)
            }
        }
    }

    fun downloadStatusRead() {
        _downloadStatus.value = null
    }

    fun cancelDownload() {
        downloadJob?.cancel(CancellationException("Canceled by user"))
        downloadJob = null
        viewModelScope.launch {
            updateDownloadStatus(GraphicPacksDownloadStatus.CANCELED)
        }
    }

    private val _filterText = MutableStateFlow("")
    val filterText: StateFlow<String> = _filterText
    fun setFilterText(filterText: String) {
        _filterText.value = filterText
    }

    private val filterPattern = filterText.map { filterText ->
        if (filterText.isBlank()) {
            return@map null
        }
        return@map buildString {
            filterText.trim()
                .split(" ".toRegex())
                .forEach { append("(?=.*" + Pattern.quote(it) + ")") }
            append(".*")
        }.toPattern(Pattern.CASE_INSENSITIVE)
    }

    private val _graphicPackDataNodes = MutableStateFlow<List<GraphicPackDataNode>>(emptyList())
    val graphicPackDataNodes: StateFlow<List<GraphicPackDataNode>> =
        combine(
            _graphicPackDataNodes,
            installedOnly,
            filterPattern
        ) { graphicPackNodes, installedOnly, pattern ->
            if (!installedOnly && pattern == null) {
                return@combine graphicPackNodes
            }
            if (pattern != null) {
                return@combine graphicPackNodes.filter {
                    pattern.matcher(it.path).matches() && (it.titleIdInstalled || !installedOnly)
                }
            }
            return@combine graphicPackNodes.filter { it.titleIdInstalled }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    private val _graphicPackNodes = MutableStateFlow<List<GraphicPackNode>>(emptyList())
    val graphicPackNodes: StateFlow<List<GraphicPackNode>> =
        installedOnly.combine(_graphicPackNodes) { installedOnly, graphicPackNodes ->
            if (installedOnly) {
                return@combine graphicPackNodes.filter { it.titleIdInstalled }
            }
            return@combine graphicPackNodes
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    init {
        refreshGraphicPacks()
    }

    private fun MutableList<GraphicPackDataNode>.fillWithDataNodes(graphicPackSectionNode: GraphicPackSectionNode): MutableList<GraphicPackDataNode> {
        for (node in graphicPackSectionNode.children) {
            when (node) {
                is GraphicPackSectionNode -> fillWithDataNodes(node)
                is GraphicPackDataNode -> add(node)
            }
        }
        return this
    }

    private fun refreshGraphicPacks() {
        rootNode = GraphicPackSectionNode().apply {
            NativeGraphicPacks.getGraphicPackBasicInfos().forEach {
                val hasTitleInstalled = it.titleIds.any { titleId -> titleId in installedTitleIds }
                addGraphicPackDataByTokens(it, hasTitleInstalled)
            }
            sort()
        }
        _graphicPackDataNodes.value =
            mutableListOf<GraphicPackDataNode>().fillWithDataNodes(rootNode)
        _graphicPackNodes.value = rootNode.children.toList()
    }

    companion object {
        private val GraphicPacksDownloader = GraphicPacksDownloader()
    }
}
