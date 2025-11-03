package info.cemu.cemu.emulation

import android.view.KeyEvent
import android.view.MotionEvent
import info.cemu.cemu.common.android.inputevent.isFromPhysicalController
import info.cemu.cemu.common.input.GamepadInputHandler
import info.cemu.cemu.nativeinterface.NativeInput.onNativeAxis
import info.cemu.cemu.nativeinterface.NativeInput.onNativeKey

object InputHandler : GamepadInputHandler {
    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!event.isFromPhysicalController()) {
            return false
        }

        val device = event.device

        onNativeKey(
            device.descriptor,
            device.name,
            event.keyCode,
            event.action == KeyEvent.ACTION_DOWN
        )

        return true
    }

    override fun onMotionEvent(event: MotionEvent): Boolean {
        if (!event.isFromPhysicalController()) {
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
}