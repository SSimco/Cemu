package info.cemu.cemu.settings.input

import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.TextView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import info.cemu.cemu.R
import info.cemu.cemu.guicore.components.ScreenContent
import info.cemu.cemu.guicore.components.SingleSelection
import info.cemu.cemu.guicore.nativeenummapper.controllerTypeToStringId
import info.cemu.cemu.nativeinterface.NativeInput
import kotlinx.coroutines.launch
import androidx.compose.material3.Button as MaterialButton


@Composable
fun ControllerInputSettingsScreen(
    navigateBack: () -> Unit,
    controllerIndex: Int,
    controllersViewModel: ControllersViewModel = viewModel(
        factory = ControllersViewModel.Factory,
        extras = MutableCreationExtras().apply {
            set(ControllersViewModel.CONTROLLER_INDEX_KEY, controllerIndex)
        }
    ),
) {
    val context = LocalContext.current
    val controllerType by controllersViewModel.controllerType.collectAsState()
    val controls by controllersViewModel.controls.collectAsState()
    val controllers by controllersViewModel.controllers.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun onInputClick(buttonName: String, buttonId: Int) {
        openInputDialog(
            context = context,
            buttonName = buttonName,
            onClear = { controllersViewModel.clearButtonMapping(buttonId) },
            mapKeyEvent = { controllersViewModel.mapKeyEvent(it, buttonId) },
            tryMapMotionEvent = { controllersViewModel.tryMapMotionEvent(it, buttonId) },
        )
    }

    ScreenContent(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        appBarText = stringResource(R.string.controller_numbered, controllerIndex + 1),
        navigateBack = navigateBack,
    ) {

        SingleSelection(
            isChoiceEnabled = controllersViewModel::isControllerTypeAllowed,
            label = stringResource(R.string.emulated_controller),
            initialChoice = { controllerType },
            choices = listOf(
                NativeInput.EMULATED_CONTROLLER_TYPE_DISABLED,
                NativeInput.EMULATED_CONTROLLER_TYPE_VPAD,
                NativeInput.EMULATED_CONTROLLER_TYPE_PRO,
                NativeInput.EMULATED_CONTROLLER_TYPE_CLASSIC,
                NativeInput.EMULATED_CONTROLLER_TYPE_WIIMOTE
            ),
            choiceToString = { stringResource(controllerTypeToStringId(it)) },
            onChoiceChanged = controllersViewModel::setControllerType
        )

        if (controllerType != NativeInput.EMULATED_CONTROLLER_TYPE_DISABLED) {
            MaterialButton(
                modifier = Modifier.padding(8.dp),
                onClick = {
                    controllersViewModel.refreshAvailableControllers {
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(context.getString(R.string.no_controllers_available))
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.controller_setup_all_inputs))
            }
        }

        controllers?.let {
            ControllerSelectDialog(
                controllers = it,
                onDismissRequest = controllersViewModel::clearGameControllers,
                onSelect = { deviceId ->
                    controllersViewModel.mapAllInputs(deviceId)
                    controllersViewModel.clearGameControllers()
                }
            )
        }

        when (controllerType) {
            NativeInput.EMULATED_CONTROLLER_TYPE_VPAD -> VPADInputs(
                controllerIndex = controllerIndex,
                onInputClick = ::onInputClick,
                controlsMapping = controls,
            )

            NativeInput.EMULATED_CONTROLLER_TYPE_PRO -> ProControllerInputs(
                onInputClick = ::onInputClick,
                controlsMapping = controls,
            )

            NativeInput.EMULATED_CONTROLLER_TYPE_CLASSIC -> ClassicControllerInputs(
                onInputClick = ::onInputClick,
                controlsMapping = controls,
            )

            NativeInput.EMULATED_CONTROLLER_TYPE_WIIMOTE -> WiimoteControllerInputs(
                onInputClick = ::onInputClick,
                controlsMapping = controls,
            )
        }
    }
}

@Composable
private fun ControllerSelectDialog(
    controllers: List<Pair<String, Int>>,
    onDismissRequest: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier
                .sizeIn(maxWidth = 560.dp, maxHeight = 560.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 8.dp
                ),
                text = stringResource(R.string.controller_select_dialog_title),
                fontSize = 24.sp,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .weight(weight = 1.0f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                controllers.forEach { (controllerName, deviceId) ->
                    Text(
                        text = controllerName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(deviceId) }
                            .padding(vertical = 16.dp, horizontal = 8.dp)
                    )
                }
            }

            HorizontalDivider()

            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.End),
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

fun openInputDialog(
    context: Context,
    buttonName: String,
    onClear: () -> Unit,
    mapKeyEvent: (KeyEvent) -> Unit,
    tryMapMotionEvent: (MotionEvent) -> Boolean,
) {
    MaterialAlertDialogBuilder(context).setTitle(R.string.inputBindingDialogTitle)
        .setMessage(context.getString(R.string.inputBindingDialogMessage, buttonName))
        .setNeutralButton(context.getString(R.string.clear)) { _, _ -> onClear() }
        .setNegativeButton(context.getString(R.string.cancel)) { _, _ -> }
        .show()
        .also { alertDialog ->
            alertDialog.requireViewById<TextView>(android.R.id.message).apply {
                isFocusableInTouchMode = true
                requestFocus()
                setOnKeyListener { _, _, keyEvent: KeyEvent ->
                    mapKeyEvent(keyEvent)
                    alertDialog.dismiss()
                    true
                }
                setOnGenericMotionListener { _, motionEvent: MotionEvent? ->
                    if (motionEvent != null && tryMapMotionEvent(motionEvent)) {
                        alertDialog.dismiss()
                    }
                    true
                }
            }
        }
}
