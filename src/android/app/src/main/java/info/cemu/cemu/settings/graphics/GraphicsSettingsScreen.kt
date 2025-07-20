package info.cemu.cemu.settings.graphics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import info.cemu.cemu.core.components.Button
import info.cemu.cemu.core.components.ScreenContent
import info.cemu.cemu.core.components.SingleSelection
import info.cemu.cemu.core.components.Toggle
import info.cemu.cemu.core.translation.tr
import info.cemu.cemu.nativeinterface.NativeEmulation
import info.cemu.cemu.nativeinterface.NativeSettings

private val SCALING_FILTER_CHOICES = listOf(
    NativeSettings.SCALING_FILTER_BILINEAR_FILTER,
    NativeSettings.SCALING_FILTER_BICUBIC_FILTER,
    NativeSettings.SCALING_FILTER_BICUBIC_HERMITE_FILTER,
    NativeSettings.SCALING_FILTER_NEAREST_NEIGHBOR_FILTER
)

@Composable
fun GraphicsSettingsScreen(navigateBack: () -> Unit, goToCustomDriversSettings: () -> Unit) {
    val supportsLoadingCustomDrivers =
        rememberSaveable { NativeEmulation.supportsLoadingCustomDriver() }

    ScreenContent(
        appBarText = tr("Graphics settings"),
        navigateBack = navigateBack,
    ) {
        if (supportsLoadingCustomDrivers) {
            Button(
                label = tr("Custom drivers"),
                onClick = goToCustomDriversSettings
            )
        }
        Toggle(
            label = tr("Async shader compile"),
            description = tr("Enables async shader and pipeline compilation. Reduces stutter at the cost of objects not rendering for a short time.\nVulkan only"),
            initialCheckedState = NativeSettings::getAsyncShaderCompile,
            onCheckedChanged = NativeSettings::setAsyncShaderCompile,
        )
        SingleSelection(
            label = tr("VSync"),
            initialChoice = NativeSettings::getVsyncMode,
            onChoiceChanged = NativeSettings::setVsyncMode,
            choiceToString = { vsyncModeToString(it) },
            choices = listOf(
                NativeSettings.VSYNC_MODE_OFF,
                NativeSettings.VSYNC_MODE_DOUBLE_BUFFERING,
                NativeSettings.VSYNC_MODE_TRIPLE_BUFFERING
            ),
        )
        Toggle(
            label = tr("Accurate barriers"),
            description = tr("Disabling the accurate barriers option will lead to flickering graphics but may improve performance. It is highly recommended to leave it turned on"),
            initialCheckedState = NativeSettings::getAccurateBarriers,
            onCheckedChanged = NativeSettings::setAccurateBarriers,
        )
        SingleSelection(
            label = tr("Fullscreen scaling"),
            initialChoice = NativeSettings::getFullscreenScaling,
            onChoiceChanged = NativeSettings::setFullscreenScaling,
            choiceToString = { fullscreenScalingModeToString(it) },
            choices = listOf(
                NativeSettings.FULLSCREEN_SCALING_KEEP_ASPECT_RATIO,
                NativeSettings.FULLSCREEN_SCALING_STRETCH
            ),
        )
        SingleSelection(
            label = tr("Upscale filter"),
            initialChoice = NativeSettings::getUpscalingFilter,
            onChoiceChanged = NativeSettings::setUpscalingFilter,
            choiceToString = { scalingFilterToString(it) },
            choices = SCALING_FILTER_CHOICES,
        )
        SingleSelection(
            label = tr("Downscale filter"),
            initialChoice = NativeSettings::getDownscalingFilter,
            onChoiceChanged = NativeSettings::setDownscalingFilter,
            choiceToString = { scalingFilterToString(it) },
            choices = SCALING_FILTER_CHOICES,
        )
    }
}

private fun scalingFilterToString(scalingFilter: Int) = when (scalingFilter) {
    NativeSettings.SCALING_FILTER_BILINEAR_FILTER -> tr("Bilinear")
    NativeSettings.SCALING_FILTER_BICUBIC_FILTER -> tr("Bicubic")
    NativeSettings.SCALING_FILTER_BICUBIC_HERMITE_FILTER -> tr("Hermite")
    NativeSettings.SCALING_FILTER_NEAREST_NEIGHBOR_FILTER -> tr("Nearest neighbor")
    else -> throw IllegalArgumentException("Invalid scaling filter:  $scalingFilter")
}

private fun vsyncModeToString(vsyncMode: Int) = when (vsyncMode) {
    NativeSettings.VSYNC_MODE_OFF -> tr("Off")
    NativeSettings.VSYNC_MODE_DOUBLE_BUFFERING -> tr("Double buffering")
    NativeSettings.VSYNC_MODE_TRIPLE_BUFFERING -> tr("Triple buffering")
    else -> throw IllegalArgumentException("Invalid vsync mode: $vsyncMode")
}

private fun fullscreenScalingModeToString(fullscreenScaling: Int) = when (fullscreenScaling) {
    NativeSettings.FULLSCREEN_SCALING_KEEP_ASPECT_RATIO -> tr("Keep aspect ratio")
    NativeSettings.FULLSCREEN_SCALING_STRETCH -> tr("Stretch")
    else -> throw IllegalArgumentException("Invalid fullscreen scaling mode:  $fullscreenScaling")
}