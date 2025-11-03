package info.cemu.cemu.settings.input

import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import info.cemu.cemu.common.input.GamepadInputHandler
import info.cemu.cemu.common.input.GamepadInputManager
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
        factory = ControllersViewModel.Factory, extras = MutableCreationExtras().apply {
            set(ControllersViewModel.CONTROLLER_INDEX_KEY, controllerIndex)
        }),
) {
    val controllers by controllersViewModel.controllers.collectAsState()
    val buttonToBind by controllersViewModel.buttonToBind.collectAsState()

    Box(Modifier.fillMaxSize()) {
        buttonToBind?.let {
            InputBindingPopup(buttonName = it.name, mapKeyEvent = { event ->
                controllersViewModel.mapKeyEvent(event, it.id)
                controllersViewModel.clearButtonToBind()
            }, mapMotionEvent = { event ->
                if (controllersViewModel.tryMapMotionEvent(event, it.id)) {
                    controllersViewModel.clearButtonToBind()
                }
            }, onClear = {
                controllersViewModel.clearButtonMapping(it.id)
                controllersViewModel.clearButtonToBind()
            }, onDismiss = {
                controllersViewModel.clearButtonToBind()
            })
        }

        ControllerInputSettingsScreenContent(
            navigateBack = navigateBack,
            controllerIndex = controllerIndex,
            controllersViewModel = controllersViewModel
        )

    }

    controllers?.let {
        ControllerSelectDialog(
            controllers = it,
            onDismissRequest = controllersViewModel::clearGameControllers,
            onSelect = { deviceId ->
                controllersViewModel.mapAllInputs(deviceId)
                controllersViewModel.clearGameControllers()
            })
    }
}

@Composable
private fun ControllerInputSettingsScreenContent(
    navigateBack: () -> Unit, controllerIndex: Int, controllersViewModel: ControllersViewModel
) {
    val controllerType by controllersViewModel.controllerType.collectAsState()
    val controls by controllersViewModel.controls.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun onInputClick(buttonName: String, buttonId: Int) {
        controllersViewModel.setButtonToBind(ButtonInfo(buttonName, buttonId))
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
                modifier = Modifier.padding(8.dp), onClick = {
                    controllersViewModel.refreshAvailableControllers {
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(tr("No controllers available"))
                        }
                    }
                }) {
                Text(tr("Setup all inputs"))
            }
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
                    start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp
                ),
                text = tr("Select a controller"),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
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
                            .padding(vertical = 16.dp, horizontal = 8.dp))
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

@Composable
private fun InputBindingPopup(
    buttonName: String,
    mapKeyEvent: (KeyEvent) -> Unit,
    mapMotionEvent: (MotionEvent) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val activity = LocalActivity.current

    DisposableEffect(Unit) {
        val inputManager = activity as? GamepadInputManager
        val inputHandler = object : GamepadInputHandler {
            override fun onKeyEvent(event: KeyEvent): Boolean {
                mapKeyEvent(event)
                return true
            }

            override fun onMotionEvent(event: MotionEvent): Boolean {
                mapMotionEvent(event)
                return true
            }
        }

        inputManager?.setHandler(inputHandler)

        onDispose {
            inputManager?.clearHandler()
        }
    }

    Popup(alignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .sizeIn(maxWidth = 560.dp, maxHeight = 560.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = tr("Input binding"),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = tr("Trigger an input to bind it to {0}", buttonName),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onClear) { Text(tr("Clear")) }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = onDismiss) { Text(tr("Cancel")) }
                    }
                }
            }
        }
    }
}
