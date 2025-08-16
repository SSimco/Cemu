@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)

package info.cemu.cemu.titlemanager

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import info.cemu.cemu.R
import info.cemu.cemu.common.components.ScreenContentLazy
import info.cemu.cemu.common.format.formatBytes
import info.cemu.cemu.common.nativeenummapper.regionToString
import info.cemu.cemu.common.translation.tr
import info.cemu.cemu.nativeinterface.NativeGameTitles
import kotlinx.coroutines.launch
import java.text.MessageFormat

@Composable
fun TitleManagerScreen(
    navigateBack: () -> Unit,
    titleListViewModel: TitleListViewModel = viewModel(),
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showFilterSheet by remember { mutableStateOf(false) }
    val titleEntries by titleListViewModel.titleEntries.collectAsState()
    val queuedTitleToInstallError by titleListViewModel.queuedTitleToInstallError.collectAsState()
    val queuedTitleToCompress by titleListViewModel.queuedTitleToCompress.collectAsState()
    val showTitleInstallProgress by titleListViewModel.titleInstallInProgress.collectAsState()
    val titleInstallProgress by titleListViewModel.titleInstallProgress.collectAsState()
    val compressTitleInProgress by titleListViewModel.compressInProgress.collectAsState()
    val currentCompressProgress by titleListViewModel.compressProgress.collectAsState()
    val titleToBeDeleted by titleListViewModel.titleToBeDeleted.collectAsState()

    fun showNotificationMessage(text: String) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(text)
        }
    }

    fun installQueuedTitle() {
        titleListViewModel.installQueuedTitle(
            context = context,
            titleInstallCallbacks = object : TitleInstallCallbacks {
                override fun onInstallFinished() {
                    showNotificationMessage(tr("Finished installing"))
                }

                override fun onError() {
                    showNotificationMessage(tr("Error installing"))
                }
            }
        )
    }

    val installTitleLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val documentFile =
                DocumentFile.fromTreeUri(context, uri) ?: return@rememberLauncherForActivityResult
            titleListViewModel.queueTitleToInstall(
                titlePath = documentFile.uri,
                onInvalidTitle = {
                    showNotificationMessage(tr("Invalid title"))
                })
        }

    val compressFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            titleListViewModel.compressQueuedTitle(
                context = context,
                uri = uri,
                onFinished = {
                    showNotificationMessage(tr("Finished converting"))
                },
                onError = {
                    showNotificationMessage(tr("Error while converting"))
                }
            )
        }

    ScreenContentLazy(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        appBarText = tr("Title manager"),
        navigateBack = navigateBack,
        actions = {
            IconButton(onClick = {
                titleListViewModel.refresh()
                showNotificationMessage(tr("Refreshing titles"))
            }) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null
                )
            }
            IconButton(onClick = { showFilterSheet = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_filter),
                    contentDescription = null
                )
            }
            IconButton(onClick = { installTitleLauncher.launch(null) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null
                )
            }
        }
    ) {
        items(items = titleEntries, key = { it.locationUID }) {
            TitleEntryListItem(
                titleEntry = it,
                onDeleteRequest = {
                    titleListViewModel.deleteTitleEntry(
                        titleEntry = it,
                        context = context,
                        deleteCallbacks = object : TitleDeleteCallbacks {
                            override fun onDeleteFinished() {
                                showNotificationMessage(tr("Deleted title entry"))
                            }

                            override fun onError() {
                                showNotificationMessage(tr("Failed to delete title entry"))
                            }
                        })
                },
                onCompressRequested = {
                    titleListViewModel.queueTitleForCompression(it)
                }
            )
        }
    }
    if (showFilterSheet)
        TitleFilterBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            titleListViewModel = titleListViewModel
        )

    queuedTitleToInstallError?.let { titleToInstallError ->
        TitleInstallConfirmDialog(
            error = titleToInstallError,
            onDismissRequest = { titleListViewModel.removedQueuedTitleToInstall() },
            onConfirm = { installQueuedTitle() }
        )
    }

    if (showTitleInstallProgress)
        TitleInstallProgressDialog(
            progress = titleInstallProgress,
            onCancel = {
                titleListViewModel.cancelInstall()
            }
        )

    queuedTitleToCompress?.let { titleToCompressInfo ->
        TitleCompressConfirmationDialog(
            onDismiss = { titleListViewModel.removeQueuedTitleForCompression() },
            onConfirm = {
                val fileName = titleListViewModel.getCompressedFileNameForQueuedTitle()
                    ?: return@TitleCompressConfirmationDialog
                compressFileLauncher.launch(fileName)
            },
            compressTitleInfo = titleToCompressInfo,
        )
    }

    if (compressTitleInProgress)
        TitleCompressProgressDialog(
            bytesWritten = currentCompressProgress,
            onCancel = titleListViewModel::cancelCompression,
        )

    titleToBeDeleted?.let { titleEntry ->
        DeleteTitleProgressDialog(titleEntry)
    }
}

@Composable
private fun TitleCompressProgressDialog(bytesWritten: Long?, onCancel: () -> Unit) {
    var showCancelConfirmDialog by remember { mutableStateOf(false) }

    AlertDialog(
        title = { Text(tr("Compressing title")) },
        text = {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator()
                if (bytesWritten != null)
                    Text(tr("Current progress: {0}", bytesWritten.formatBytes()))
            }
        },
        onDismissRequest = {},
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = { showCancelConfirmDialog = true },
                content = { Text(tr("Cancel")) },
            )
        }
    )

    if (showCancelConfirmDialog)
        AlertDialog(
            title = { Text(tr("Cancel compressing title")) },
            text = { Text(tr("Do you really want to cancel compressing the title?")) },
            onDismissRequest = { showCancelConfirmDialog = false },
            confirmButton = {
                TextButton(onClick = onCancel) {
                    Text(tr("Yes"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmDialog = false }) {
                    Text(tr("No"))
                }
            }
        )
}

@Composable
private fun TitleCompressConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    compressTitleInfo: NativeGameTitles.CompressTitleInfo,
) {
    @Composable
    fun EntryInfo(entryName: String, entryPrintPath: String?) {
        Text(
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            text = entryName,
        )
        Text(
            modifier = Modifier.padding(bottom = 4.dp),
            fontSize = 14.sp,
            text = entryPrintPath ?: tr("Not installed")
        )
    }
    AlertDialog(
        title = { Text(tr("Confirmation")) },
        text = {
            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    text = tr("The following content will be converted to a compressed Wii U archive file (.wua)"),
                )
                EntryInfo(
                    tr("Base game"),
                    compressTitleInfo.basePrintPath
                )
                EntryInfo(
                    tr("Update"),
                    compressTitleInfo.updatePrintPath
                )
                EntryInfo(
                    tr("DLC"),
                    compressTitleInfo.aocPrintPath
                )
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(tr("OK")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("Cancel")) }
        }
    )
}

@Composable
private fun TitleInstallProgressDialog(
    progress: Pair<Long, Long>?,
    onCancel: () -> Unit,
) {
    var showCancelConfirmDialog by remember { mutableStateOf(false) }

    AlertDialog(
        title = { Text(tr("Installing title")) },
        text = {
            val progressModifiers = Modifier
                .fillMaxWidth()
                .padding(8.dp)
            Column {
                if (progress == null) {
                    LinearProgressIndicator(modifier = progressModifiers)
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = tr("Parsing title content...")
                    )
                } else {
                    val (bytesWritten, maxBytes) = progress
                    LinearProgressIndicator(
                        modifier = progressModifiers,
                        progress = {
                            bytesWritten.toFloat() / maxBytes.toFloat()
                        },
                    )
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = "${bytesWritten.formatBytes()}/${maxBytes.formatBytes()}"
                    )
                }
            }
        },
        onDismissRequest = {},
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { showCancelConfirmDialog = true }) {
                Text(tr("Cancel"))
            }
        }
    )

    if (showCancelConfirmDialog)
        AlertDialog(
            title = { Text(tr("Cancel installing title")) },
            text = {
                Text(tr("Do you really want to cancel the installation process?\n\nCanceling the process will delete the applied files."))
            },
            onDismissRequest = { showCancelConfirmDialog = false },
            confirmButton = {
                TextButton(onClick = onCancel) {
                    Text(tr("Yes"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmDialog = false }) {
                    Text(tr("No"))
                }
            }
        )
}


@Composable
private fun TitleInstallConfirmDialog(
    error: NativeGameTitles.TitleExistsError,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {

    val errorMessage = when (error) {
        is NativeGameTitles.TitleExistsError.DifferentType -> tr(
            """It seems that there is already a title installed at the target location but it has a different type.
Currently installed: '{0}' Installing: '{1}'
Do you still want to continue with the installation? It will replace the currently installed title.""",
            error.oldType,
            error.toInstallType
        )

        NativeGameTitles.TitleExistsError.NewVersion -> tr("It seems that a newer version is already installed, do you still want to install the older version?")
        NativeGameTitles.TitleExistsError.SameVersion -> tr("It seems that the selected title is already installed, do you want to reinstall it?")

        NativeGameTitles.TitleExistsError.None -> {
            onConfirm()
            return
        }
    }

    AlertDialog(
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null
            )
        },
        title = { Text(tr("Warning")) },
        text = {
            Text(
                text = errorMessage,
                modifier = Modifier,
            )
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(tr("Yes")) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(tr("No")) }
        }
    )
}

@Composable
private fun TitleFilterBottomSheet(
    onDismissRequest: () -> Unit,
    titleListViewModel: TitleListViewModel,
) {
    val filter by titleListViewModel.filter.collectAsState()

    ModalBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismissRequest,
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            TextField(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                singleLine = true,
                value = filter.query,
                onValueChange = titleListViewModel::setFilterQuery,
                label = { Text(tr("Search titles")) }
            )
            FilterRow(
                filterRowLabel = tr("Types"),
                filterValues = EntryType.entries.map { (it to (it in filter.types)) },
                valueToLabel = { entryTypeToString(it) },
                onFilterAdded = titleListViewModel.typesFilter::add,
                onFilterRemoved = titleListViewModel.typesFilter::remove,
            )
            FilterRow(
                filterRowLabel = tr("Formats"),
                filterValues = EntryFormat.entries.map { (it to (it in filter.formats)) },
                valueToLabel = { formatToString(it) },
                onFilterAdded = titleListViewModel.formatsFilter::add,
                onFilterRemoved = titleListViewModel.formatsFilter::remove,
            )
            FilterRow(
                filterRowLabel = tr("Locations"),
                filterValues = EntryPath.entries.map { (it to (it in filter.paths)) },
                valueToLabel = { pathToString(it) },
                onFilterAdded = titleListViewModel.pathsFilter::add,
                onFilterRemoved = titleListViewModel.pathsFilter::remove,
            )
        }
    }
}

@Composable
private fun <T> FilterRow(
    filterRowLabel: String,
    filterValues: List<Pair<T, Boolean>>,
    valueToLabel: @Composable (T) -> String,
    onFilterAdded: (T) -> Unit,
    onFilterRemoved: (T) -> Unit,
) {
    var showOptions by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .animateContentSize()
    ) {
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showOptions = !showOptions }) {
            Text(text = filterRowLabel, modifier = Modifier.weight(1f))
            Icon(
                modifier = Modifier.rotate(if (showOptions) 180f else 0f),
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null
            )
        }
        if (showOptions)
            FlowRow(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                filterValues.forEach { (value, selected) ->
                    FilterChip(
                        label = valueToLabel(value),
                        selected = selected,
                        onAdd = { onFilterAdded(value) },
                        onRemove = { onFilterRemoved(value) },
                    )
                }
            }
    }
}

@Composable
fun FilterChip(label: String, selected: Boolean, onAdd: () -> Unit, onRemove: () -> Unit) {
    FilterChip(
        leadingIcon = {
            if (selected)
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null
                )
        },
        selected = selected,
        onClick = {
            if (selected) onRemove()
            else onAdd()
        },
        label = { Text(label) }
    )
}

@Composable
private fun TitleEntryListItem(
    titleEntry: TitleEntry,
    onDeleteRequest: () -> Unit,
    onCompressRequested: () -> Unit,
) {
    var showDeleteConfirmationDialog by rememberSaveable { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(8.dp),
    ) {
        var showTitleInfo by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TitleEntryIcon(titleEntry.type)
            Text(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .basicMarquee(iterations = Int.MAX_VALUE)
                    .weight(1.0f),
                text = titleEntry.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            TitleDropDownMenu(
                titleEntry = titleEntry,
                onDeleteClicked = { showDeleteConfirmationDialog = true },
                onCompressClicked = onCompressRequested
            )
            IconButton(
                onClick = { showTitleInfo = !showTitleInfo }) {
                Icon(
                    modifier = Modifier.rotate(if (showTitleInfo) 180f else 0f),
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null
                )
            }
        }
        if (showTitleInfo) {
            TitleEntryData(titleEntry)
        }
    }

    if (showDeleteConfirmationDialog)
        DeleteTitleConfirmationDialog(
            titleEntry = titleEntry,
            onDismissRequest = { showDeleteConfirmationDialog = false },
            onConfirmDelete = onDeleteRequest
        )
}

@Composable
private fun TitleDropDownMenu(
    titleEntry: TitleEntry,
    onDeleteClicked: () -> Unit,
    onCompressClicked: () -> Unit,
) {
    var expandMenu by remember { mutableStateOf(false) }

    @Composable
    fun DropdownMenuItem(text: String, onClick: () -> Unit) {
        DropdownMenuItem(
            text = { Text(text) },
            onClick = {
                expandMenu = false
                onClick()
            }
        )
    }

    Box {
        IconButton(onClick = { expandMenu = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = null
            )
        }
        DropdownMenu(
            expanded = expandMenu,
            onDismissRequest = { expandMenu = false }) {
            DropdownMenuItem(tr("Delete"), onDeleteClicked)
            if (titleEntry.type != EntryType.Save && titleEntry.format != EntryFormat.WUA)
                DropdownMenuItem(
                    tr("Convert to WUA"),
                    onCompressClicked
                )
        }
    }
}

@Composable
private fun DeleteTitleProgressDialog(titleEntry: TitleEntry) {
    AlertDialog(
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null
            )
        },
        title = {
            Text(tr("Deleting"))
        },
        text = {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(tr("Deleting: {0}", getTitleEntryInfo(titleEntry)))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        },
        onDismissRequest = {},
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun DeleteTitleConfirmationDialog(
    titleEntry: TitleEntry,
    onDismissRequest: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    AlertDialog(
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null
            )
        },
        title = { Text(tr("Warning")) },
        text = {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(tr("Are you really sure you want to delete the following entry?"))
                Text(getTitleEntryInfo(titleEntry))
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onConfirmDelete()
                },
                content = { Text(tr("Yes")) },
            )
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(tr("No")) }
        }
    )
}

private fun getTitleEntryInfo(titleEntry: TitleEntry) = MessageFormat.format(
    "[{0}] [{1}] [{2}]",
    titleEntry.name,
    regionToString(titleEntry.region),
    entryTypeToString(titleEntry.type)
)

@Composable
private fun TitleEntryIcon(entryType: EntryType, modifier: Modifier = Modifier) {
    val iconId = when (entryType) {
        EntryType.Base -> R.drawable.ic_controller
        EntryType.Update -> R.drawable.ic_upgrade
        EntryType.Dlc -> R.drawable.ic_box
        EntryType.Save -> R.drawable.ic_save
        EntryType.System -> R.drawable.ic_build
    }
    Icon(
        modifier = modifier,
        painter = painterResource(iconId),
        contentDescription = null
    )
}

@Composable
private fun TitleEntryData(titleEntry: TitleEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        TitleEntryInfo(
            name = tr("Title ID"),
            value = formatTitleId(titleEntry.titleId)
        )
        TitleEntryInfo(
            name = tr("Type"),
            value = entryTypeToString(titleEntry.type),
        )
        TitleEntryInfo(
            name = tr("Version"),
            value = titleEntry.version.toString(),
        )
        TitleEntryInfo(
            name = tr("Region"),
            value = regionToString(titleEntry.region),
        )
        TitleEntryInfo(
            name = tr("Format"),
            value = formatToString(titleEntry.format),
        )
        TitleEntryInfo(
            name = tr("Location"),
            value = if (titleEntry.isInMLC) tr("MLC")
            else tr("Game paths")
        )
    }
}

@Composable
private fun TitleEntryInfo(name: String, value: String) {
    Text(
        modifier = Modifier
            .padding(
                top = 2.dp,
                start = 8.dp,
                end = 8.dp,
            ),
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        text = name,
    )
    Text(
        modifier = Modifier.padding(
            start = 8.dp,
            end = 8.dp,
            bottom = 2.dp,
        ),
        fontSize = 14.sp,
        text = value
    )
}

private fun formatToString(entryFormat: EntryFormat) = when (entryFormat) {
    EntryFormat.Folder -> tr("Folder")
    EntryFormat.WUD -> tr("WUD")
    EntryFormat.NUS -> tr("NUS")
    EntryFormat.WUA -> tr("WUA")
    EntryFormat.WUHB -> tr("WUHB")
    EntryFormat.SaveFolder -> tr("Save folder")
}

private fun pathToString(entryPath: EntryPath) = when (entryPath) {
    EntryPath.MLC -> tr("MLC")
    EntryPath.GamePaths -> tr("Game paths")
}

private fun entryTypeToString(entryType: EntryType) = when (entryType) {
    EntryType.Base -> tr("Base")
    EntryType.Update -> tr("Update")
    EntryType.Dlc -> tr("DLC")
    EntryType.Save -> tr("Save")
    EntryType.System -> tr("System")
}

private fun formatTitleId(titleId: Long) =
    String.format("%08x-%08x", titleId shr 32, titleId and 0xFFFFFFFF)
