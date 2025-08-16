package info.cemu.cemu.emulation.inputoverlay.inputs.innerdrawing

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import info.cemu.cemu.emulation.inputoverlay.Colors
import kotlin.math.min

class StickClickInnerDrawing : ButtonInnerDrawing {
    private var activeColor = 0
    private var inactiveColor = 0
    private val paint = Paint().apply {
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }
    private var path = Path()

    override fun draw(canvas: Canvas, state: Boolean) {
        paint.color = if (state) activeColor else inactiveColor
        canvas.drawPath(path, paint)
    }

    override fun configure(boundingRect: Rect, alpha: Int) {
        activeColor = Colors.activeStroke(alpha)
        inactiveColor = Colors.inactiveStroke(alpha)
        val transformMatrix = Matrix()

        path = createArrowPath()
        transformMatrix.setTranslate(-0.15f, 0f)
        path.transform(transformMatrix)

        transformMatrix.reset()
        val arrowPath = createArrowPath()
        transformMatrix.setRotate(180f)
        transformMatrix.postTranslate(0.15f, 0f)
        arrowPath.transform(transformMatrix)
        path.addPath(arrowPath)

        transformMatrix.reset()
        val scale = min(boundingRect.width(), boundingRect.height()) * 0.5f
        transformMatrix.setScale(scale, scale)
        transformMatrix.postTranslate(
            boundingRect.exactCenterX(),
            boundingRect.exactCenterY()
        )
        path.transform(transformMatrix)
    }

    private fun createArrowPath(): Path {
        val arrowPath = Path()
        arrowPath.moveTo(-0.5f, 0.5f)
        arrowPath.lineTo(0f, 0f)
        arrowPath.lineTo(-0.5f, -0.5f)
        return arrowPath
    }
}