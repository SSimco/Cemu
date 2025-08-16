package info.cemu.cemu.common.canvas

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

fun Canvas.fillCircleWithStroke(
    centerX: Float,
    centerY: Float,
    radius: Float,
    paint: Paint,
    fillColor: Int,
    strokeColor: Int,
) {
    paint.color = fillColor
    paint.style = Paint.Style.FILL
    drawCircle(centerX, centerY, radius, paint)
    paint.color = strokeColor
    paint.style = Paint.Style.STROKE
    drawCircle(centerX, centerY, radius, paint)
}

fun Canvas.fillRoundRectangleWithStroke(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    radius: Float,
    paint: Paint,
    fillColor: Int,
    strokeColor: Int,
) {
    fillRoundRectangleWithStroke(
        RectF(left, top, right, bottom),
        radius,
        paint,
        fillColor,
        strokeColor,
    )
}

fun Canvas.fillRoundRectangleWithStroke(
    rect: RectF,
    radius: Float,
    paint: Paint,
    fillColor: Int,
    strokeColor: Int,
) {
    paint.style = Paint.Style.FILL
    paint.color = fillColor
    drawRoundRect(rect, radius, radius, paint)
    paint.style = Paint.Style.STROKE
    paint.color = strokeColor
    drawRoundRect(rect, radius, radius, paint)
}