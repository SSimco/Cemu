package info.cemu.cemu.emulation.inputoverlay.inputs.innerdrawing

import android.graphics.Path
import androidx.core.graphics.PathParser

class BlowButtonInnerDrawing : PathInnerDrawing() {
    override val canvasSize: Float = CANVAS_SIZE
    override val originalPath: Path
        get() = PATH

    companion object {
        private const val CANVAS_SIZE = 960f
        private const val PATH_DATA =
            "M460-160q-50 0-85-35t-35-85h80q0 17 11.5 28.5T460-240q17 0 28.5-11.5T500-280q0-17-11.5-28.5T460-320H80v-80h380q50 0 85 35t35 85q0 50-35 85t-85 35ZM80-560v-80h540q26 0 43-17t17-43q0-26-17-43t-43-17q-26 0-43 17t-17 43h-80q0-59 40.5-99.5T620-840q59 0 99.5 40.5T760-700q0 59-40.5 99.5T620-560H80Zm660 320v-80q26 0 43-17t17-43q0-26-17-43t-43-17H80v-80h660q59 0 99.5 40.5T880-380q0 59-40.5 99.5T740-240Z"
        private val PATH by lazy { PathParser.createPathFromPathData(PATH_DATA) }
    }
}