package info.cemu.cemu.settings.graphics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import info.cemu.cemu.R
import info.cemu.cemu.guicore.components.Button
import info.cemu.cemu.guicore.components.ScreenContent
import info.cemu.cemu.guicore.components.SingleSelection
import info.cemu.cemu.guicore.components.Toggle
import info.cemu.cemu.guicore.nativeenummapper.fullscreenScalingModeToStringId
import info.cemu.cemu.guicore.nativeenummapper.scalingFilterToStringId
import info.cemu.cemu.guicore.nativeenummapper.vsyncModeToStringId
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
        appBarText = stringResource(R.string.general_settings),
        navigateBack = navigateBack,
    ) {
        if (supportsLoadingCustomDrivers) {
            Button(
                label = stringResource(R.string.custom_drivers),
                onClick = goToCustomDriversSettings
            )
        }
        Toggle(
            label = stringResource(R.string.async_shader_compile),
            description = stringResource(R.string.async_shader_compile_description),
            initialCheckedState = NativeSettings::getAsyncShaderCompile,
            onCheckedChanged = NativeSettings::setAsyncShaderCompile,
        )
        SingleSelection(
            label = stringResource(R.string.vsync),
            initialChoice = NativeSettings::getVsyncMode,
            onChoiceChanged = NativeSettings::setVsyncMode,
            choiceToString = { stringResource(vsyncModeToStringId(it)) },
            choices = listOf(
                NativeSettings.VSYNC_MODE_OFF,
                NativeSettings.VSYNC_MODE_DOUBLE_BUFFERING,
                NativeSettings.VSYNC_MODE_TRIPLE_BUFFERING
            ),
        )
        Toggle(
            label = stringResource(R.string.accurate_barriers),
            description = stringResource(R.string.accurate_barriers_description),
            initialCheckedState = NativeSettings::getAccurateBarriers,
            onCheckedChanged = NativeSettings::setAccurateBarriers,
        )
        SingleSelection(
            label = stringResource(R.string.fullscreen_scaling),
            initialChoice = NativeSettings::getFullscreenScaling,
            onChoiceChanged = NativeSettings::setFullscreenScaling,
            choiceToString = { stringResource(fullscreenScalingModeToStringId(it)) },
            choices = listOf(
                NativeSettings.FULLSCREEN_SCALING_KEEP_ASPECT_RATIO,
                NativeSettings.FULLSCREEN_SCALING_STRETCH
            ),
        )
        SingleSelection(
            label = stringResource(R.string.upscale_filter),
            initialChoice = NativeSettings::getUpscalingFilter,
            onChoiceChanged = NativeSettings::setUpscalingFilter,
            choiceToString = { stringResource(scalingFilterToStringId(it)) },
            choices = SCALING_FILTER_CHOICES,
        )
        SingleSelection(
            label = stringResource(R.string.downscale_filter),
            initialChoice = NativeSettings::getDownscalingFilter,
            onChoiceChanged = NativeSettings::setDownscalingFilter,
            choiceToString = { stringResource(scalingFilterToStringId(it)) },
            choices = SCALING_FILTER_CHOICES,
        )
    }
}