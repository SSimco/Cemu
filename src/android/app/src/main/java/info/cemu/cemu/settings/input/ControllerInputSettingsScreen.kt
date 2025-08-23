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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import info.cemu.cemu.common.ui.components.ScreenContent
import info.cemu.cemu.common.ui.components.SingleSelection
import info.cemu.cemu.common.ui.localization.controllerTypeToString
import info.cemu.cemu.common.ui.localization.tr
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
        appBarText = tr("Controller {0}", controllerIndex + 1),
        navigateBack = navigateBack,
    ) {

        SingleSelection(
            isChoiceEnabled = controllersViewModel::isControllerTypeAllowed,
            label = tr("Emulated controller"),
            initialChoice = { controllerType },
            choices = listOf(
                NativeInput.EmulatedControllerType.DISABLED,
                NativeInput.EmulatedControllerType.VPAD,
                NativeInput.EmulatedControllerType.PRO,
                NativeInput.EmulatedControllerType.CLASSIC,
                NativeInput.EmulatedControllerType.WIIMOTE
            ),
            choiceToString = { controllerTypeToString(it) },
            onChoiceChanged = controllersViewModel::setControllerType
        )

        if (controllerType != NativeInput.EmulatedControllerType.DISABLED) {
            MaterialButton(
                modifier = Modifier.padding(8.dp),
                onClick = {
                    controllersViewModel.refreshAvailableControllers {
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(tr("No controllers available"))
                        }
                    }
                }
            ) {
                Text(tr("Setup all inputs"))
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
            NativeInput.EmulatedControllerType.VPAD -> VPADInputs(
                controllerIndex = controllerIndex,
                onInputClick = ::onInputClick,
                controlsMapping = controls,
            )

            NativeInput.EmulatedControllerType.PRO -> ProControllerInputs(
                onInputClick = ::onInputClick,
                controlsMapping = controls,
            )

            NativeInput.EmulatedControllerType.CLASSIC -> ClassicControllerInputs(
                onInputClick = ::onInputClick,
                controlsMapping = controls,
            )

            NativeInput.EmulatedControllerType.WIIMOTE -> WiimoteControllerInputs(
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
                text = tr("Select a controller"),
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
                Text(tr("Cancel"))
            }
        }
    }
}

private fun openInputDialog(
    context: Context,
    buttonName: String,
    onClear: () -> Unit,
    mapKeyEvent: (KeyEvent) -> Unit,
    tryMapMotionEvent: (MotionEvent) -> Boolean,
) {
    MaterialAlertDialogBuilder(context).setTitle(tr("Input binding"))
        .setMessage(tr("Trigger an input to bind it to {0}", buttonName))
        .setNeutralButton(tr("Clear")) { _, _ -> onClear() }
        .setNegativeButton(tr("Cancel")) { _, _ -> }
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
