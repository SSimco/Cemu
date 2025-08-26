package info.cemu.cemu.graphicpacks

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.cemu.cemu.nativeinterface.NativeGameTitles
import info.cemu.cemu.nativeinterface.NativeGraphicPacks
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
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

data class Preset(
    val index: Int,
    val category: String?,
    val activeChoice: String,
    val choices: List<String>,
) {
    companion object {
        fun fromNativeGraphicPack(graphicPack: NativeGraphicPacks.GraphicPack): List<Preset> {
            return graphicPack.presets.mapIndexed { index, preset ->
                Preset(
                    index = index,
                    category = preset.category,
                    activeChoice = preset.activePreset,
                    choices = preset.presets,
                )
            }
        }

    }
}

data class GraphicPackData(
    val description: String,
    val active: Boolean,
    val presets: List<Preset>,
)

private fun MutableList<GraphicPackDataNode>.fillWithDataNodes(graphicPackSectionNode: GraphicPackSectionNode): MutableList<GraphicPackDataNode> {
    for (node in graphicPackSectionNode.children) {
        when (node) {
            is GraphicPackSectionNode -> fillWithDataNodes(node)
            is GraphicPackDataNode -> add(node)
        }
    }
    return this
}

class GraphicPacksViewModel : ViewModel() {
    private var rootNode = GraphicPackSectionNode()

    val installedTitleIds = NativeGameTitles.getInstalledGamesTitleIds()

    private val _installedOnly = MutableStateFlow(installedTitleIds.size > 1)
    val installedOnly = _installedOnly.asStateFlow()
    fun setInstalledOnly(installedOnly: Boolean) {
        _installedOnly.value = installedOnly
    }

    private val path = MutableStateFlow(listOf<GraphicPackNode>(rootNode))

    val currentNode = path.map { it.last() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            rootNode
        )

    private var currentNativeGraphicPack: NativeGraphicPacks.GraphicPack? = null
    private val _currentDataGraphicPack = MutableStateFlow<GraphicPackData?>(null)
    val currentDataGraphicPack = _currentDataGraphicPack.asStateFlow()

    private fun setCurrentDataGraphicPack(graphicPackDataNode: GraphicPackDataNode) {
        val nativeGraphicPack = NativeGraphicPacks.getGraphicPack(graphicPackDataNode.id) ?: return
        currentNativeGraphicPack = nativeGraphicPack
        _currentDataGraphicPack.value = GraphicPackData(
            description = nativeGraphicPack.description,
            active = nativeGraphicPack.isActive(),
            presets = Preset.fromNativeGraphicPack(nativeGraphicPack)
        )
    }

    private fun clearCurrentDataGraphicPack() {
        currentNativeGraphicPack = null
        _currentDataGraphicPack.value = null
    }

    fun setCurrentGraphicPackActive(active: Boolean) {
        val dataNode = currentNode.value
        if (dataNode !is GraphicPackDataNode) {
            return
        }
        dataNode.enabled = active
        currentNativeGraphicPack?.setActive(active)
        _currentDataGraphicPack.value = _currentDataGraphicPack.value?.copy(active = active)
    }

    private fun refreshCurrentGraphicPackPresets() {
        val nativeGraphicPack = currentNativeGraphicPack ?: return

        val oldPresets = nativeGraphicPack.presets

        nativeGraphicPack.reloadPresets()

        if (oldPresets == nativeGraphicPack.presets) {
            return
        }

        _currentDataGraphicPack.value = _currentDataGraphicPack.value?.copy(
            presets = Preset.fromNativeGraphicPack(nativeGraphicPack)
        )
    }

    fun setCurrentGraphicPackActivePreset(index: Int, activePreset: String) {
        val nativeGraphicPack = currentNativeGraphicPack ?: return

        var presets = _currentDataGraphicPack.value?.presets ?: return

        presets = presets.toMutableList().apply {
            set(index, get(index).copy(activeChoice = activePreset))
        }

        _currentDataGraphicPack.value = _currentDataGraphicPack.value?.copy(presets = presets)

        nativeGraphicPack.presets[index].activePreset = activePreset

        refreshCurrentGraphicPackPresets()
    }


    fun navigateBack() {
        val currentPath = path.value
        val lastNode = currentPath.last()

        if (currentPath.size > 1) {
            path.value = currentPath.dropLast(1)
        }

        if (lastNode is GraphicPackDataNode) {
            clearCurrentDataGraphicPack()
        }
    }

    fun navigateTo(node: GraphicPackNode) {
        val currentPath = path.value

        if (node is GraphicPackSectionNode) {
            path.value += node
            return
        }

        if (node !is GraphicPackDataNode) {
            return
        }

        setCurrentDataGraphicPack(node)

        if (currentPath.last() === node.parent) {
            path.value += node
            return
        }

        val newPath = mutableListOf<GraphicPackNode>(node)
        var currentNode = node.parent
        while (currentNode != null) {
            newPath.add(currentNode)
            currentNode = currentNode.parent
        }

        path.value = newPath.reversed()
    }

    private val _downloadStatus = MutableStateFlow<GraphicPacksDownloadStatus?>(null)
    private suspend fun updateDownloadStatus(status: GraphicPacksDownloadStatus?) {
        _downloadStatus.first { it == null }
        _downloadStatus.value = status
    }

    val downloadStatus = _downloadStatus.asStateFlow()

    private var downloadJob: Job? = null
    fun downloadNewUpdate(context: Context) {
        if (_downloadStatus.value != null) return
        downloadJob = viewModelScope.launch {
            try {
                GraphicPacksDownloader.download(context) { updateDownloadStatus(it) }
                refreshGraphicPacks()
            } catch (_: Exception) {
                updateDownloadStatus(GraphicPacksDownloadStatus.ERROR)
            }
        }
    }

    fun downloadStatusRead() {
        _downloadStatus.value = null
    }

    fun cancelDownload() {
        val oldDownloadJob = downloadJob ?: return
        downloadJob = null
        viewModelScope.launch {
            oldDownloadJob.cancelAndJoin()
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
            filterText.trim().split(" ".toRegex())
                .forEach { append("(?=.*" + Pattern.quote(it) + ")") }
            append(".*")
        }.toPattern(Pattern.CASE_INSENSITIVE)
    }

    private val _graphicPackDataNodes = MutableStateFlow<List<GraphicPackDataNode>>(emptyList())
    val graphicPackDataNodes: StateFlow<List<GraphicPackDataNode>> = combine(
        _graphicPackDataNodes, installedOnly, filterPattern
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

    private fun refreshGraphicPacks() {
        rootNode = GraphicPackSectionNode().apply {
            NativeGraphicPacks.getGraphicPackBasicInfos().forEach {
                val hasTitleInstalled = it.titleIds.any { titleId -> titleId in installedTitleIds }
                addGraphicPackDataByTokens(it, hasTitleInstalled)
            }
            sort()
        }

        path.value = listOf(rootNode)

        _graphicPackDataNodes.value =
            mutableListOf<GraphicPackDataNode>().fillWithDataNodes(rootNode)
    }

    init {
        refreshGraphicPacks()
    }

    companion object {
        private val GraphicPacksDownloader = GraphicPacksDownloader()
    }
}
