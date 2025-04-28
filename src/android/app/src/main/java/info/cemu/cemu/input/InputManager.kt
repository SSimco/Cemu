package info.cemu.cemu.input

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import info.cemu.cemu.nativeinterface.NativeInput
import info.cemu.cemu.nativeinterface.NativeInput.VPadButtons
import info.cemu.cemu.nativeinterface.NativeInput.ProControllerButtons
import info.cemu.cemu.nativeinterface.NativeInput.ClassicControllerButtons
import info.cemu.cemu.nativeinterface.NativeInput.WiimoteButtons
import info.cemu.cemu.nativeinterface.NativeInput.onNativeAxis
import info.cemu.cemu.nativeinterface.NativeInput.onNativeKey
import info.cemu.cemu.nativeinterface.NativeInput.setControllerMapping

import kotlin.math.abs

class InputManager {
    private class InvalidAxisException(axis: Int) : Exception("Invalid axis $axis")

    @Throws(InvalidAxisException::class)
    private fun getNativeAxisKey(axis: Int, isPositive: Boolean): Int {
        return if (isPositive) {
            when (axis) {
                MotionEvent.AXIS_X -> NativeInput.AXIS_X_POS
                MotionEvent.AXIS_Y -> NativeInput.AXIS_Y_POS
                MotionEvent.AXIS_RX, MotionEvent.AXIS_Z -> NativeInput.ROTATION_X_POS
                MotionEvent.AXIS_RY, MotionEvent.AXIS_RZ -> NativeInput.ROTATION_Y_POS
                MotionEvent.AXIS_LTRIGGER -> NativeInput.TRIGGER_X_POS
                MotionEvent.AXIS_RTRIGGER -> NativeInput.TRIGGER_Y_POS
                MotionEvent.AXIS_HAT_X -> NativeInput.DPAD_RIGHT
                MotionEvent.AXIS_HAT_Y -> NativeInput.DPAD_DOWN
                else -> throw InvalidAxisException(axis)
            }
        } else {
            when (axis) {
                MotionEvent.AXIS_X -> NativeInput.AXIS_X_NEG
                MotionEvent.AXIS_Y -> NativeInput.AXIS_Y_NEG
                MotionEvent.AXIS_RX, MotionEvent.AXIS_Z -> NativeInput.ROTATION_X_NEG
                MotionEvent.AXIS_RY, MotionEvent.AXIS_RZ -> NativeInput.ROTATION_Y_NEG
                MotionEvent.AXIS_HAT_X -> NativeInput.DPAD_LEFT
                MotionEvent.AXIS_HAT_Y -> NativeInput.DPAD_UP
                else -> throw InvalidAxisException(axis)
            }
        }
    }

    private fun isMotionEventFromJoystickOrGamepad(event: MotionEvent): Boolean {
        return (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                || (event.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
    }

    fun tryMapMotionEventToMappingId(
        controllerIndex: Int,
        mappingId: Int,
        event: MotionEvent,
    ): Boolean {
        if (!isMotionEventFromJoystickOrGamepad(event)) {
            return false
        }
        val device = event.device
        var maxAbsAxisValue = 0.0f
        var maxAxis = -1
        val actionPointerIndex = event.actionIndex
        for (motionRange in device.motionRanges) {
            val axisValue = event.getAxisValue(motionRange.axis, actionPointerIndex)
            var axis: Int
            try {
                axis = getNativeAxisKey(motionRange.axis, axisValue > 0)
            } catch (e: InvalidAxisException) {
                continue
            }
            if (abs(axisValue.toDouble()) > maxAbsAxisValue) {
                maxAxis = axis
                maxAbsAxisValue = abs(axisValue.toDouble()).toFloat()
            }
        }
        if (maxAbsAxisValue > MIN_ABS_AXIS_VALUE) {
            setControllerMapping(
                device.descriptor,
                device.name,
                controllerIndex,
                mappingId,
                maxAxis
            )
            return true
        }
        return false
    }

    private fun isSpecialKey(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        return keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_CAMERA || keyCode == KeyEvent.KEYCODE_ZOOM_IN || keyCode == KeyEvent.KEYCODE_ZOOM_OUT
    }

    private fun isController(inputDevice: InputDevice): Boolean {
        val sources = inputDevice.sources
        return (sources and InputDevice.SOURCE_CLASS_JOYSTICK) != 0 ||
                ((sources and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD) ||
                ((sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        if (isSpecialKey(event)) {
            return false
        }
        if (event.deviceId < 0) {
            return false
        }
        val device = event.device
        if (!isController(device)) {
            return false
        }
        onNativeKey(
            device.descriptor,
            device.name,
            event.keyCode,
            event.action == KeyEvent.ACTION_DOWN
        )
        return true
    }

    fun onMotionEvent(event: MotionEvent): Boolean {
        if (!isMotionEventFromJoystickOrGamepad(event)) {
            return false
        }
        val device = event.device
        val actionPointerIndex = event.actionIndex
        for (motionRange in device.motionRanges) {
            val axisValue = event.getAxisValue(motionRange.axis, actionPointerIndex)
            val axis = motionRange.axis
            onNativeAxis(device.descriptor, device.name, axis, axisValue)
        }
        return true
    }

    fun mapKeyEventToMappingId(controllerIndex: Int, mappingId: Int, event: KeyEvent) {
        val device = event.device
        setControllerMapping(
            device.descriptor,
            device.name,
            controllerIndex,
            mappingId,
            event.keyCode
        )
    }

    private fun mapAxisCodeToMappingId(
        controllerIndex: Int,
        mappingId: Int,
        deviceDescriptor: String,
        deviceName: String,
        axisCode: Int,
        isPositive: Boolean,
    ) {
        setControllerMapping(
            deviceDescriptor,
            deviceName,
            controllerIndex,
            mappingId,
            getNativeAxisKey(axisCode, isPositive)
        )
    }

    private fun mapKeyCodeToMappingId(
        controllerIndex: Int,
        mappingId: Int,
        deviceDescriptor: String,
        deviceName: String,
        keyCode: Int,
    ) {
        setControllerMapping(
            deviceDescriptor,
            deviceName,
            controllerIndex,
            mappingId,
            keyCode
        )
    }

    private fun InputDevice.isGameController(): Boolean {
        return !isVirtual && (
                sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD
                        || sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
                        || sources and InputDevice.SOURCE_DPAD == InputDevice.SOURCE_DPAD
                )
    }

    fun getGameControllers(): List<Pair<String, Int>> {
        val gameControllers = mutableListOf<Pair<String, Int>>()

        InputDevice.getDeviceIds().forEach { deviceId ->
            val device = InputDevice.getDevice(deviceId) ?: return@forEach

            if (gameControllers.any { (_, id) -> id == deviceId }) {
                return@forEach
            }

            if (device.isGameController()) {
                gameControllers.add(Pair(device.name, deviceId))
            }
        }

        return gameControllers
    }

    fun mapAllInputs(deviceId: Int, controllerIndex: Int) {
        if (NativeInput.isControllerDisabled(controllerIndex)) {
            return
        }
        val controllerType = NativeInput.getControllerType(controllerIndex)
        val device = InputDevice.getDevice(deviceId) ?: return

        val inputs = mutableMapOf<InputMapping, Boolean>().apply {
            val buttonKeyCodes =
                ButtonInputMapping.entries.map { it.keyCode }.toIntArray()
            ButtonInputMapping.entries
                .zip(device.hasKeys(*buttonKeyCodes).toTypedArray())
                .forEach { (button, hasKey) -> put(button, hasKey) }

            device.motionRanges.forEach { motionRange ->
                AxisInputMapping.entries.filter {
                    val isPositive = motionRange.min < 0 && !it.isPositive
                    val isNegative = motionRange.max > 0 && it.isPositive
                    (isPositive || isNegative) && it.axisCode == motionRange.axis
                }.forEach { put(it, true) }
            }
        }

        val buttons = NativeInput.getNativeButtonsForControllerType(controllerType)

        for (button in buttons) {
            val mapping = getButtonMappings(button).firstOrNull { inputs[it] == true }
                ?: FALLBACK_BUTTONS.firstOrNull { inputs[it] == true }
            if (mapping == null) {
                continue
            }
            inputs[mapping] = false

            val buttonId = button.nativeKeyCode

            if (mapping is ButtonInputMapping) {
                mapKeyCodeToMappingId(
                    controllerIndex,
                    buttonId,
                    device.descriptor,
                    device.name,
                    mapping.keyCode
                )
            }

            if (mapping is AxisInputMapping) {
                mapAxisCodeToMappingId(
                    controllerIndex,
                    buttonId,
                    device.descriptor,
                    device.name,
                    mapping.axisCode,
                    mapping.isPositive,
                )
            }
        }
    }

    private fun getButtonMappings(button: NativeInput.NativeInputButton): Array<InputMapping> {
        return when (button) {
            VPadButtons.A,
            ProControllerButtons.A,
            ClassicControllerButtons.A,
            WiimoteButtons.A,
                -> arrayOf(ButtonInputMapping.BUTTON_A)

            VPadButtons.B,
            ProControllerButtons.B,
            ClassicControllerButtons.B,
            WiimoteButtons.B,
                -> arrayOf(ButtonInputMapping.BUTTON_B)

            VPadButtons.X,
            ProControllerButtons.X,
            ClassicControllerButtons.X,
            WiimoteButtons.ONE,
                -> arrayOf(ButtonInputMapping.BUTTON_X)

            VPadButtons.Y,
            ProControllerButtons.Y,
            ClassicControllerButtons.Y,
            WiimoteButtons.TWO,
                -> arrayOf(ButtonInputMapping.BUTTON_Y)

            VPadButtons.L,
            ProControllerButtons.L,
            ClassicControllerButtons.L,
            WiimoteButtons.NUNCHUCK_C,
                -> arrayOf(ButtonInputMapping.BUTTON_L1)

            VPadButtons.R,
            ProControllerButtons.R,
            ClassicControllerButtons.R,
            WiimoteButtons.NUNCHUCK_Z,
                -> arrayOf(ButtonInputMapping.BUTTON_R1)

            VPadButtons.ZL,
            ProControllerButtons.ZL,
            ClassicControllerButtons.ZL,
                -> arrayOf(ButtonInputMapping.BUTTON_L2, AxisInputMapping.LTRIGGER)

            VPadButtons.ZR,
            ProControllerButtons.ZR,
            ClassicControllerButtons.ZR,
                -> arrayOf(ButtonInputMapping.BUTTON_R2, AxisInputMapping.RTRIGGER)

            VPadButtons.PLUS,
            ProControllerButtons.PLUS,
            ClassicControllerButtons.PLUS,
            WiimoteButtons.PLUS,
                -> arrayOf(ButtonInputMapping.BUTTON_START)

            VPadButtons.MINUS,
            ProControllerButtons.MINUS,
            ClassicControllerButtons.MINUS,
            WiimoteButtons.MINUS,
                -> arrayOf(ButtonInputMapping.BUTTON_SELECT)

            VPadButtons.STICKL_UP,
            ProControllerButtons.STICKL_UP,
            ClassicControllerButtons.STICKL_UP,
            WiimoteButtons.NUNCHUCK_UP,
                -> arrayOf(AxisInputMapping.Y_NEG)

            VPadButtons.STICKL_DOWN,
            ProControllerButtons.STICKL_DOWN,
            ClassicControllerButtons.STICKL_DOWN,
            WiimoteButtons.NUNCHUCK_DOWN,
                -> arrayOf(AxisInputMapping.Y_POS)

            VPadButtons.STICKL_LEFT,
            ProControllerButtons.STICKL_LEFT,
            ClassicControllerButtons.STICKL_LEFT,
            WiimoteButtons.NUNCHUCK_LEFT,
                -> arrayOf(AxisInputMapping.X_NEG)

            VPadButtons.STICKL_RIGHT,
            ProControllerButtons.STICKL_RIGHT,
            ClassicControllerButtons.STICKL_RIGHT,
            WiimoteButtons.NUNCHUCK_RIGHT,
                -> arrayOf(AxisInputMapping.X_POS)

            VPadButtons.STICKR_UP,
            ProControllerButtons.STICKR_UP,
            ClassicControllerButtons.STICKR_UP,
                -> arrayOf(AxisInputMapping.RY_NEG, AxisInputMapping.RZ_NEG)

            VPadButtons.STICKR_DOWN,
            ProControllerButtons.STICKR_DOWN,
            ClassicControllerButtons.STICKR_DOWN,
                -> arrayOf(AxisInputMapping.RY_POS, AxisInputMapping.RZ_POS)

            VPadButtons.STICKR_LEFT,
            ProControllerButtons.STICKR_LEFT,
            ClassicControllerButtons.STICKR_LEFT,
                -> arrayOf(AxisInputMapping.RX_NEG, AxisInputMapping.Z_NEG)

            VPadButtons.STICKR_RIGHT,
            ProControllerButtons.STICKR_RIGHT,
            ClassicControllerButtons.STICKR_RIGHT,
                -> arrayOf(AxisInputMapping.RX_POS, AxisInputMapping.Z_POS)

            VPadButtons.UP,
            ProControllerButtons.UP,
            ClassicControllerButtons.UP,
            WiimoteButtons.UP,
                -> arrayOf(AxisInputMapping.HAT_Y_NEG, ButtonInputMapping.DPAD_UP)

            VPadButtons.DOWN,
            ProControllerButtons.DOWN,
            ClassicControllerButtons.DOWN,
            WiimoteButtons.DOWN,
                -> arrayOf(AxisInputMapping.HAT_Y_POS, ButtonInputMapping.DPAD_DOWN)

            VPadButtons.LEFT,
            ProControllerButtons.LEFT,
            ClassicControllerButtons.LEFT,
            WiimoteButtons.LEFT,
                -> arrayOf(AxisInputMapping.HAT_X_NEG, ButtonInputMapping.DPAD_LEFT)

            VPadButtons.RIGHT,
            ProControllerButtons.RIGHT,
            ClassicControllerButtons.RIGHT,
            WiimoteButtons.RIGHT,
                -> arrayOf(AxisInputMapping.HAT_X_POS, ButtonInputMapping.DPAD_RIGHT)

            VPadButtons.STICKL,
            ProControllerButtons.STICKL,
                -> arrayOf(ButtonInputMapping.BUTTON_THUMBL)

            VPadButtons.STICKR,
            ProControllerButtons.STICKR,
                -> arrayOf(ButtonInputMapping.BUTTON_THUMBR)

            else -> arrayOf()
        }
    }

    companion object {
        private const val MIN_ABS_AXIS_VALUE = 0.33f
    }
}
