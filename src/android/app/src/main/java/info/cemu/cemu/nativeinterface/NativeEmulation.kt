package info.cemu.cemu.nativeinterface

import android.view.Surface

object NativeEmulation {
    @JvmStatic
    external fun initializeEmulation()

    @JvmStatic
    external fun setDPI(dpi: Float)

    @JvmStatic
    external fun initializeSurface(isMainCanvas: Boolean)

    @JvmStatic
    external fun setSurface(surface: Surface?, isMainCanvas: Boolean)

    @JvmStatic
    external fun clearSurface(isMainCanvas: Boolean)

    @JvmStatic
    external fun setSurfaceSize(width: Int, height: Int, isMainCanvas: Boolean)

    @JvmStatic
    external fun initializeRenderer(surface: Surface?)

    object PrepareTitleResult {
        const val SUCCESSFUL: Int = 0
        const val ERROR_GAME_BASE_FILES_NOT_FOUND: Int = 1
        const val ERROR_NO_DISC_KEY: Int = 2
        const val ERROR_NO_TITLE_TIK: Int = 3
        const val ERROR_UNKNOWN: Int = 4
    }

    @JvmStatic
    external fun prepareTitle(launchPath: String?): Int

    @JvmStatic
    external fun launchTitle()

    @JvmStatic
    external fun initializeSystems()

    @JvmStatic
    external fun setReplaceTVWithPadView(swapped: Boolean)

    @JvmStatic
    external fun recreateRenderSurface(isMainCanvas: Boolean)

    @JvmStatic
    external fun supportsLoadingCustomDriver(): Boolean
}
