package info.cemu.cemu.emulation.inputoverlay.inputs.innerdrawing

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import info.cemu.cemu.emulation.inputoverlay.Colors
import kotlin.math.min

abstract class PathInnerDrawing : ButtonInnerDrawing {
    private var activeColor = 0
    private var inactiveColor = 0
    private val paint = Paint()
    private var path = Path()
    override fun draw(canvas: Canvas, state: Boolean) {
        paint.color = if (state) activeColor else inactiveColor
        canvas.drawPath(path, paint)
    }

    protected abstract val canvasSize: Float
    protected abstract val originalPath: Path

    override fun configure(boundingRect: Rect, alpha: Int) {
        activeColor = Colors.activeStroke(alpha)
        inactiveColor = Colors.inactiveStroke(alpha)

        path = Path(originalPath)

        val transformMatrix = Matrix()
        val rectSize = min(boundingRect.width(), boundingRect.height()) * 0.85f
        val scale = rectSize / canvasSize
        transformMatrix.setScale(scale, scale)
        transformMatrix.postTranslate(
            boundingRect.exactCenterX() - rectSize * 0.5f,
            boundingRect.exactCenterY() + rectSize * 0.5f
        )

        path.transform(transformMatrix)
    }
}