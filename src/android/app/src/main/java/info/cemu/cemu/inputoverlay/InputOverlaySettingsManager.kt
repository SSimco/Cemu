package info.cemu.cemu.inputoverlay

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import android.util.DisplayMetrics.DENSITY_DEFAULT
import info.cemu.cemu.R
import kotlin.math.max
import kotlin.math.min

class OverlaySettings(
    var isVibrateOnTouchEnabled: Boolean,
    var isOverlayEnabled: Boolean,
    var controllerIndex: Int,
    var alpha: Int,
) {
    companion object {
        private const val DEFAULT_ALPHA: Int = 64
        private const val IS_VIBRATE_ON_TOUCH_ENABLED_KEY = "IS_VIBRATE_ON_TOUCH_ENABLED"
        private const val IS_OVERLAY_ENABLED_KEY = "IS_OVERLAY_ENABLED"
        private const val CONTROLLER_INDEX_KEY = "CONTROLLER_INDEX"
        private const val ALPHA_KEY = "ALPHA"
    }

    constructor(sharedPreferences: SharedPreferences) : this(
        isVibrateOnTouchEnabled = sharedPreferences.getBoolean(
            IS_VIBRATE_ON_TOUCH_ENABLED_KEY,
            false
        ),
        isOverlayEnabled = sharedPreferences.getBoolean(IS_OVERLAY_ENABLED_KEY, false),
        controllerIndex = sharedPreferences.getInt(CONTROLLER_INDEX_KEY, 0),
        alpha = sharedPreferences.getInt(ALPHA_KEY, DEFAULT_ALPHA),
    )

    fun save(sharedPreferences: SharedPreferences) {
        sharedPreferences.edit().apply {
            putBoolean(IS_VIBRATE_ON_TOUCH_ENABLED_KEY, isVibrateOnTouchEnabled)
            putBoolean(IS_OVERLAY_ENABLED_KEY, isOverlayEnabled)
            putInt(CONTROLLER_INDEX_KEY, controllerIndex)
            putInt(ALPHA_KEY, alpha)
            apply()
        }
    }
}

class InputOverlaySettingsManager(context: Context) {
    private val defaultInputConfigs =
        parseDefaultInputConfigs(context.resources.getXml(R.xml.input_overlay_default_configs))
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(INPUT_OVERLAY_SETTINGS_NAME, Context.MODE_PRIVATE)

    var overlaySettings: OverlaySettings
        get() = OverlaySettings(sharedPreferences)
        set(value) = value.save(sharedPreferences)

    fun getInputOverlayRectangle(
        input: OverlayInput,
        width: Int,
        height: Int,
        density: Int,
    ): Rect {
        return getRectangle(input) ?: getDefaultRectangle(input, width, height, density)
    }

    private fun getRectLeftConfigName(input: OverlayInput) = "${input.configName}_LEFT"
    private fun getRectTopConfigName(input: OverlayInput) = "${input.configName}_TOP"
    private fun getRectRightConfigName(input: OverlayInput) = "${input.configName}_RIGHT"
    private fun getRectBottomConfigName(input: OverlayInput) = "${input.configName}_BOTTOM"

    private fun getRectangle(input: OverlayInput): Rect? {
        val left = sharedPreferences.getInt(getRectLeftConfigName(input), -1)
        val top = sharedPreferences.getInt(getRectTopConfigName(input), -1)
        val right = sharedPreferences.getInt(getRectRightConfigName(input), -1)
        val bottom = sharedPreferences.getInt(getRectBottomConfigName(input), -1)
        if (left == -1 || top == -1 || right == -1 || bottom == -1) {
            return null
        }
        return Rect(left, top, right, bottom)
    }

    fun saveRectangle(input: OverlayInput, rect: Rect) {
        sharedPreferences.edit().apply {
            putInt(getRectLeftConfigName(input), rect.left)
            putInt(getRectTopConfigName(input), rect.top)
            putInt(getRectRightConfigName(input), rect.right)
            putInt(getRectBottomConfigName(input), rect.bottom)
            apply()
        }
    }


    fun clearSavedRectangle(input: OverlayInput) {
        sharedPreferences.edit().apply {
            remove(getRectLeftConfigName(input))
            remove(getRectTopConfigName(input))
            remove(getRectRightConfigName(input))
            remove(getRectBottomConfigName(input))
            apply()
        }
    }

    private fun getDefaultRectangle(
        input: OverlayInput,
        width: Int,
        height: Int,
        density: Int,
    ): Rect {
        fun Int.dpToPx() = (this * density) / DENSITY_DEFAULT
        val inputConfig = defaultInputConfigs[input.configName] ?: return Rect()
        val inputWidth = inputConfig.width.dpToPx()
        val horizontalPadding = inputConfig.paddingHorizontal.dpToPx()
        val verticalPadding = inputConfig.paddingVertical.dpToPx()
        val inputHeight = inputConfig.height.dpToPx()
        val top = min(
            max(if (inputConfig.alignBottom) height - inputHeight else 0, verticalPadding),
            height - verticalPadding - inputHeight
        )
        val left = min(
            max(if (inputConfig.alignEnd) width - inputWidth else 0, horizontalPadding),
            width - horizontalPadding - inputWidth
        )
        val right = left + inputWidth
        val bottom = top + inputHeight
        return Rect(
            left,
            top,
            right,
            bottom,
        )
    }

    companion object {
        private const val INPUT_OVERLAY_SETTINGS_NAME = "INPUT_OVERLAY_SETTINGS"
    }
}
