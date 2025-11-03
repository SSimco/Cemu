package info.cemu.cemu.emulation

import android.annotation.SuppressLint
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import info.cemu.cemu.R
import info.cemu.cemu.common.ui.localization.tr
import info.cemu.cemu.emulation.inputoverlay.InputOverlayController
import info.cemu.cemu.emulation.inputoverlay.InputOverlaySurface
import info.cemu.cemu.emulation.inputoverlay.InputOverlaySurfaceView
import info.cemu.cemu.emulation.inputoverlay.InputOverlaySurfaceView.InputMode.DEFAULT
import info.cemu.cemu.emulation.inputoverlay.InputOverlaySurfaceView.InputMode.EDIT_POSITION
import info.cemu.cemu.emulation.inputoverlay.InputOverlaySurfaceView.InputMode.EDIT_SIZE
import info.cemu.cemu.nativeinterface.NativeEmulation
import kotlinx.coroutines.launch

@Composable
fun EmulationScreen(
    gamePath: String,
    setMotionSensorEnabled: (Boolean) -> Unit,
    onQuit: () -> Unit,
    viewModel: EmulationViewModel = viewModel(
        factory = EmulationViewModel.Factory, extras = MutableCreationExtras().apply {
            set(EmulationViewModel.GAME_PATH_KEY, gamePath)
        }),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val emulationError by viewModel.emulationError.collectAsState()
    val isEmulationInitialized by viewModel.isEmulationInitialized.collectAsState()
    val sideMenuState by viewModel.sideMenuState.collectAsState()
    var showQuitConfirmationDialog by remember { mutableStateOf(false) }
    var inputOverlayController by remember { mutableStateOf<InputOverlayController?>(null) }
    var inputOverlayInputMode by rememberSaveable { mutableStateOf(DEFAULT) }

    LaunchedEffect(inputOverlayInputMode) {
        inputOverlayController?.setInputMode(inputOverlayInputMode)
    }

    fun snackbarMessage(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    fun closeDrawer() {
        scope.launch { drawerState.close() }
    }

    BackHandler {
        scope.launch {
            drawerState.apply {
                if (isClosed) open() else close()
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !sideMenuState.isDrawerLocked || drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .width(IntrinsicSize.Max)
                        .verticalScroll(rememberScrollState())
                ) {
                    EmulationSideMenuContent(
                        sideMenuState = sideMenuState,
                        updateState = {
                            viewModel.updateSideMenuState(it)
                            inputOverlayController?.setVisible(it.isInputOverlayVisible)
                            setMotionSensorEnabled(it.isMotionEnabled)
                            NativeEmulation.setReplaceTVWithPadView(it.isTVReplacedWithPad)
                            closeDrawer()
                        },
                        onEditInputOverlay = {
                            snackbarMessage(tr("Edit input positions"))
                            inputOverlayInputMode = EDIT_POSITION
                            inputOverlayController?.setInputMode(EDIT_POSITION)
                            closeDrawer()
                        },
                        onResetInputOverlay = {
                            inputOverlayController?.resetInputs()
                            closeDrawer()
                        },
                        onQuit = {
                            showQuitConfirmationDialog = true
                            closeDrawer()
                        },
                    )
                }
            }
        },
    ) {
        Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { contentPadding ->
            Box(Modifier.padding(contentPadding)) {
                EmulationSurfaces(viewModel)

                InputOverlaySurface {
                    inputOverlayController = it
                    it.setVisible(sideMenuState.isInputOverlayVisible)
                }

                if (inputOverlayInputMode != DEFAULT) {
                    EditInputsLayout(
                        inputMode = inputOverlayInputMode,
                        onFinishClick = {
                            snackbarMessage(tr("Exited input edit mode"))
                            inputOverlayInputMode = DEFAULT
                        },
                        onMoveClick = {
                            snackbarMessage(tr("Edit input positions"))
                            inputOverlayInputMode = EDIT_POSITION
                        },
                        onResizeClick = {
                            snackbarMessage(tr("Edit input size"))
                            inputOverlayInputMode = EDIT_SIZE
                        },
                    )
                }
            }
        }
    }

    emulationError?.let {
        EmulationErrorDialog(it, onQuit)
    }

    if (!isEmulationInitialized) {
        EmulationLoadingDialog()
    }

    if (showQuitConfirmationDialog) {
        EmulationQuitConfirmationDialog(
            onQuit = onQuit,
            onDismiss = { showQuitConfirmationDialog = false },
        )
    }

    EmulationTextInputDialog()
}

@Composable
private fun EditInputsLayout(
    inputMode: InputOverlaySurfaceView.InputMode,
    onFinishClick: () -> Unit,
    onMoveClick: () -> Unit,
    onResizeClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = onFinishClick) { Text(tr("Done")) }

            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    enabled = inputMode != EDIT_POSITION,
                    onClick = onMoveClick,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_move),
                        contentDescription = tr("Move")
                    )
                }

                FilledIconButton(
                    enabled = inputMode != EDIT_SIZE,
                    onClick = onResizeClick,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_resize),
                        contentDescription = tr("Resize"),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmulationSideMenuContent(
    sideMenuState: SideMenuState,
    updateState: (SideMenuState) -> Unit,
    onEditInputOverlay: () -> Unit,
    onResetInputOverlay: () -> Unit,
    onQuit: () -> Unit,
) {
    CheckboxItem(
        label = tr("Enable motion"),
        checked = sideMenuState.isMotionEnabled,
        onCheckedChange = { updateState(sideMenuState.copy(isMotionEnabled = it)) },
    )

    CheckboxItem(
        label = tr("Lock drawer"),
        checked = sideMenuState.isDrawerLocked,
        onCheckedChange = { updateState(sideMenuState.copy(isDrawerLocked = it)) },
    )

    CheckboxItem(
        label = tr("Replace TV with PAD"),
        checked = sideMenuState.isTVReplacedWithPad,
        onCheckedChange = { updateState(sideMenuState.copy(isTVReplacedWithPad = it)) },
    )

    CheckboxItem(
        label = tr("Show PAD"),
        checked = sideMenuState.isPadVisible,
        onCheckedChange = { updateState(sideMenuState.copy(isPadVisible = it)) },
    )

    CheckboxItem(
        label = tr("Show input overlay"),
        checked = sideMenuState.isInputOverlayVisible,
        onCheckedChange = { updateState(sideMenuState.copy(isInputOverlayVisible = it)) },
    )

    TextButtonItem(
        label = tr("Edit inputs"),
        enabled = sideMenuState.isInputOverlayVisible,
        onClick = onEditInputOverlay,
    )

    TextButtonItem(
        label = tr("Reset input overlay"),
        enabled = sideMenuState.isInputOverlayVisible,
        onClick = onResetInputOverlay,
    )

    TextButtonItem(
        label = tr("Exit"),
        onClick = onQuit,
    )
}

@Composable
private fun CheckboxItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.6f)
            .clickable(enabled) { onCheckedChange(!checked) }
            .padding(8.dp)
            .minimumInteractiveComponentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier
                .padding(end = 8.dp)
                .weight(1f),
            fontSize = 16.sp,
        )

        Checkbox(
            checked = checked,
            onCheckedChange = null,
        )
    }
}

@Composable
private fun TextButtonItem(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.6f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp)
            .heightIn(min = 48.dp)
            .wrapContentHeight(align = Alignment.CenterVertically),
        fontSize = 16.sp,
    )
}

@Composable
private fun EmulationSurfaces(viewModel: EmulationViewModel) {
    val sideMenuState by viewModel.sideMenuState.collectAsState()

    LinearLayout(viewModel.surfacesConfig) { itemModifier ->
        EmulationSurface(
            modifier = itemModifier,
            isTV = true,
            holderCallback = viewModel.mainHolderCallback,
            afterInit = { viewModel.initializeEmulation() },
        )

        if (sideMenuState.isPadVisible) {
            EmulationSurface(
                modifier = itemModifier,
                isTV = false,
                holderCallback = viewModel.padHolderCallback,
            )
        }
    }
}

@Composable
@SuppressLint("ClickableViewAccessibility")
private fun EmulationSurface(
    modifier: Modifier,
    isTV: Boolean,
    holderCallback: SurfaceHolder.Callback,
    afterInit: () -> Unit = {}
) {
    AndroidView(
        modifier = modifier, factory = { context ->
            SurfaceView(context).apply {
                var firstChange = true

                setOnTouchListener(CanvasOnTouchListener(isTV))

                holder.addCallback(holderCallback)

                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceChanged(
                        holder: SurfaceHolder, format: Int, width: Int, height: Int
                    ) {
                        if (firstChange) {
                            afterInit()
                            firstChange = false
                        }
                    }

                    override fun surfaceCreated(holder: SurfaceHolder) {}

                    override fun surfaceDestroyed(holder: SurfaceHolder) {}
                })
            }
        })
}

@Composable
private fun LinearLayout(
    surfacesConfig: SurfacesConfig,
    content: @Composable (itemModifier: Modifier) -> Unit,
) {
    if (surfacesConfig.isVertical) {
        val arrangement = if (surfacesConfig.isReversed) Arrangement.Top else Arrangement.Bottom

        Column(
            modifier = Modifier.fillMaxSize(), verticalArrangement = arrangement
        ) {
            content(Modifier.weight(1f))
        }
    } else {
        val arrangement = if (surfacesConfig.isReversed) Arrangement.Start else Arrangement.End

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = arrangement,
        ) {
            content(Modifier.weight(1f))
        }
    }
}

@Composable
private fun EmulationQuitConfirmationDialog(onQuit: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        title = { Text(tr("Exit confirmation")) },
        text = { Text(tr("Are you sure you want to exit?")) },
        confirmButton = { TextButton(onClick = onQuit) { Text(tr("Yes")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("No")) } },
        onDismissRequest = onDismiss,
    )
}

@Composable
private fun EmulationLoadingDialog() {
    AlertDialog(
        title = { Text(tr("Initializing emulation")) },
        text = { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) },
        confirmButton = {},
        onDismissRequest = {},
    )
}

@Composable
private fun EmulationErrorDialog(errorMessage: String, onQuit: () -> Unit) {
    AlertDialog(
        title = { Text(tr("Error")) },
        text = { Text(errorMessage) },
        confirmButton = { TextButton(onClick = onQuit) { Text(tr("Quit")) } },
        onDismissRequest = {},
    )
}
