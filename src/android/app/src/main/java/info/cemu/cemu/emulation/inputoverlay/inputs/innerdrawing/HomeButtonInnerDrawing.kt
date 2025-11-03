package info.cemu.cemu.emulation.inputoverlay.inputs.innerdrawing

import android.graphics.Path
import androidx.core.graphics.PathParser

class HomeButtonInnerDrawing : PathInnerDrawing() {
    override val canvasSize: Float = CANVAS_SIZE
    override val originalPath: Path
        get() = Path

    companion object {
        private const val CANVAS_SIZE = 960f
        private const val PATH_DATA =
            "M240-200h120v-240h240v240h120v-360L480-740 240-560v360Zm-80 80v-480l320-240 320 240v480H520v-240h-80v240H160Zm320-350Z"
        private val Path by lazy { PathParser.createPathFromPathData(PATH_DATA) }
    }
}