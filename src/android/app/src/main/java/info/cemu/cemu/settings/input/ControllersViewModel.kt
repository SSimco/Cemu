package info.cemu.cemu.settings.input

import android.view.KeyEvent
import android.view.MotionEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import info.cemu.cemu.input.InputManager
import info.cemu.cemu.input.getNativeButtonsForControllerType
import info.cemu.cemu.nativeinterface.NativeInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ControllersViewModel(val controllerIndex: Int) : ViewModel() {
    private val inputManager = InputManager()
    private var _controllerType = MutableStateFlow(
        if (NativeInput.isControllerDisabled(controllerIndex)) NativeInput.EmulatedControllerType.DISABLED
        else NativeInput.getControllerType(controllerIndex)
    )
    val controllerType = _controllerType.asStateFlow()

    private val _controls = MutableStateFlow<Map<Int, String>>(emptyMap())
    val controls = _controls.asStateFlow()

    private val _controllers = MutableStateFlow<List<Pair<String, Int>>?>(null)
    val controllers = _controllers.asStateFlow()

    private var vpadCount = 0
    private var wpadCount = 0

    private fun getControllerMapping(buttonId: Int) =
        buttonId to NativeInput.getControllerMapping(controllerIndex, buttonId)

    fun setControllerType(controllerType: Int) {
        if (!isControllerTypeAllowed(controllerType) || _controllerType.value == controllerType)
            return
        _controllerType.value = controllerType
        NativeInput.setControllerType(controllerIndex, controllerType)
        refreshControllerData()
    }

    fun mapKeyEvent(keyEvent: KeyEvent, buttonId: Int) {
        inputManager.mapKeyEventToMappingId(controllerIndex, buttonId, keyEvent)
        _controls.value += getControllerMapping(buttonId)
    }

    fun refreshAvailableControllers(onNoControllersAvailable: () -> Unit) {
        val newControllers = inputManager.getGameControllers()
        if (newControllers.isEmpty()) {
            _controllers.value = null
            onNoControllersAvailable()
            return
        }
        _controllers.value = newControllers
    }

    fun clearGameControllers() {
        _controllers.value = null
    }

    fun mapAllInputs(deviceId: Int) {
        val oldControls = _controls.value
        _controls.value = emptyMap()
        oldControls.keys.forEach { NativeInput.clearControllerMapping(controllerIndex, it) }

        inputManager.mapAllInputs(deviceId, controllerIndex)

        val buttons = getNativeButtonsForControllerType (controllerType.value)
        buttons.forEach { _controls.value += getControllerMapping(it.nativeKeyCode) }
    }

    fun tryMapMotionEvent(motionEvent: MotionEvent, buttonId: Int): Boolean {
        if (inputManager.tryMapMotionEventToMappingId(controllerIndex, buttonId, motionEvent)) {
            _controls.value += getControllerMapping(buttonId)
            return true
        }
        return false
    }

    fun clearButtonMapping(buttonId: Int) {
        _controls.value -= buttonId
        NativeInput.clearControllerMapping(controllerIndex, buttonId)
    }

    private fun refreshControllerData() {
        vpadCount = NativeInput.VPADControllersCount
        wpadCount = NativeInput.WPADControllersCount
        _controls.value = NativeInput.getControllerMappings(controllerIndex)
    }

    fun isControllerTypeAllowed(controllerType: Int): Boolean {
        val currentControllerType = this.controllerType.value
        if (controllerType == NativeInput.EmulatedControllerType.DISABLED) {
            return true
        }
        if (controllerType == NativeInput.EmulatedControllerType.VPAD) {
            return currentControllerType == NativeInput.EmulatedControllerType.VPAD || vpadCount < NativeInput.MAX_VPAD_CONTROLLERS
        }
        val isWPAD = currentControllerType != NativeInput.EmulatedControllerType.VPAD
                && currentControllerType != NativeInput.EmulatedControllerType.DISABLED
        return isWPAD || wpadCount < NativeInput.MAX_WPAD_CONTROLLERS
    }

    init {
        refreshControllerData()
    }

    companion object {
        val CONTROLLER_INDEX_KEY = object : CreationExtras.Key<Int> {}
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ControllersViewModel(
                    this[CONTROLLER_INDEX_KEY] as Int
                )
            }
        }
    }
}