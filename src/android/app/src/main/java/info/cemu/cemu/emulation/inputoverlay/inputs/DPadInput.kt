package info.cemu.cemu.emulation.inputoverlay.inputs

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import info.cemu.cemu.emulation.inputoverlay.fillCircleWithStroke
import info.cemu.cemu.emulation.inputoverlay.fillRoundRectangleWithStroke
import info.cemu.cemu.emulation.inputoverlay.Colors
import info.cemu.cemu.emulation.inputoverlay.OverlayDpad
import kotlin.math.atan2
import kotlin.math.min

class DPadInput(
    private val onButtonStateChange: (button: OverlayDpad, state: Boolean) -> Unit,
    private val alpha: Int,
    rect: Rect,
) : Input(rect) {
    private var backgroundFillColor: Int = 0
    private var backgroundStrokeColor: Int = 0

    private val dpadUpRect = RectF()
    private val dpadDownRect = RectF()
    private val dpadLeftRect = RectF()
    private val dpadRightRect = RectF()

    private var dpadState: Int = NONE

    private var centerX: Float = 0f
    private var centerY: Float = 0f
    private var radius2: Float = 0f
    private var radius: Float = 0f
    private var currentPointerId: Int = -1

    override fun configure() {
        backgroundFillColor = Colors.backgroundFill(alpha)
        backgroundStrokeColor = Colors.backgroundStroke(alpha)
        configureColors(alpha)

        centerX = rect.exactCenterX()
        centerY = rect.exactCenterY()
        radius = min(rect.width(), rect.height()) * 0.5f
        radius2 = radius * radius
        val buttonSize = radius / 2
        val configureButtonRect = { circleXPos: Float, circleYPos: Float, rect: RectF ->
            val left = circleXPos - buttonSize * 0.5f
            val top = circleYPos - buttonSize * 0.5f
            rect.set(left, top, left + buttonSize, top + buttonSize)
        }
        val buttonCenterXYTranslate = 2f * radius / 3f
        configureButtonRect(
            centerX,
            centerY - buttonCenterXYTranslate,
            dpadUpRect
        )
        configureButtonRect(
            centerX,
            centerY + buttonCenterXYTranslate,
            dpadDownRect
        )
        configureButtonRect(
            centerX - buttonCenterXYTranslate,
            centerY,
            dpadLeftRect
        )
        configureButtonRect(
            centerX + buttonCenterXYTranslate,
            centerY,
            dpadRightRect
        )
    }

    init {
        configure()
    }

    private fun updateState(nextDpadState: Int) {
        if (nextDpadState == dpadState) return
        updateState(dpadState and nextDpadState.inv(), false)
        updateState(nextDpadState and dpadState.inv(), true)
        dpadState = nextDpadState
    }

    private fun updateState(dpadState: Int, pressed: Boolean) {
        if (dpadState == NONE) return
        if ((dpadState and UP) != 0) {
            onButtonStateChange(OverlayDpad.DPAD_UP, pressed)
        }
        if ((dpadState and DOWN) != 0) {
            onButtonStateChange(OverlayDpad.DPAD_DOWN, pressed)
        }
        if ((dpadState and LEFT) != 0) {
            onButtonStateChange(OverlayDpad.DPAD_LEFT, pressed)
        }
        if ((dpadState and RIGHT) != 0) {
            onButtonStateChange(OverlayDpad.DPAD_RIGHT, pressed)
        }
    }

    private fun getStateByPosition(x: Float, y: Float): Int {
        val dx = x - centerX
        val dy = y - centerY
        val norm2 = dx * dx + dy * dy

        if (norm2 <= radius2 * 0.1f) {
            return NONE
        }

        val angle = atan2(dy, dx)

        return when (angle) {
            in -0.875 * Math.PI..<-0.625 * Math.PI -> UP or LEFT
            in -0.625 * Math.PI..<-0.375 * Math.PI -> UP
            in -0.375 * Math.PI..<-0.125 * Math.PI -> UP or RIGHT
            in -0.125 * Math.PI..<0.125 * Math.PI -> RIGHT
            in 0.125 * Math.PI..<0.375 * Math.PI -> DOWN or RIGHT
            in 0.375 * Math.PI..<0.625 * Math.PI -> DOWN
            in 0.625 * Math.PI..<0.875 * Math.PI -> DOWN or LEFT
            else -> LEFT // in [-pi, -0.875*pi) or [0.875*pi, pi]
        }
    }

    override fun onTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (currentPointerId != -1) {
                    return false
                }
                val pointerIndex = event.actionIndex
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                val pointerId = event.getPointerId(pointerIndex)
                if (isInside(x, y)) {
                    currentPointerId = pointerId
                    updateState(getStateByPosition(x, y))
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val pointerId = event.getPointerId(event.actionIndex)
                if (pointerId == currentPointerId) {
                    currentPointerId = -1
                    updateState(NONE)
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (currentPointerId == -1) {
                    return false
                }
                for (i in 0 until event.pointerCount) {
                    if (currentPointerId != event.getPointerId(i)) {
                        continue
                    }
                    val x = event.getX(i)
                    val y = event.getY(i)
                    updateState(getStateByPosition(x, y))
                    return true
                }
            }
        }
        return false
    }

    override fun resetInput() {
        updateState(NONE)
    }

    override fun isInside(x: Float, y: Float): Boolean {
        val dx = x - centerX
        val dy = y - centerY
        return dx * dx + dy * dy <= radius2
    }

    override fun drawInput(canvas: Canvas) {
        canvas.fillCircleWithStroke(
            centerX,
            centerY,
            radius,
            paint,
            backgroundFillColor,
            backgroundStrokeColor
        )
        drawButton(canvas, dpadUpRect, dpadState and UP)
        drawButton(canvas, dpadDownRect, dpadState and DOWN)
        drawButton(canvas, dpadLeftRect, dpadState and LEFT)
        drawButton(canvas, dpadRightRect, dpadState and RIGHT)
    }

    private fun drawButton(canvas: Canvas, rect: RectF, state: Int) {
        val fillColor: Int
        val strokeColor: Int
        if (state != 0) {
            fillColor = activeFillColor
            strokeColor = activeStrokeColor
        } else {
            fillColor = inactiveFillColor
            strokeColor = inactiveStrokeColor
        }
        canvas.fillRoundRectangleWithStroke(rect, BUTTON_RADIUS, paint, fillColor, strokeColor)
    }

    companion object {
        private const val BUTTON_RADIUS = 5f
        private const val NONE = 0
        private const val UP = 1 shl 0
        private const val DOWN = 1 shl 1
        private const val LEFT = 1 shl 2
        private const val RIGHT = 1 shl 3
    }
}
