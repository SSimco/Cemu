package info.cemu.cemu.settings.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import info.cemu.cemu.core.components.Header
import info.cemu.cemu.core.components.ScreenContent
import info.cemu.cemu.core.components.SingleSelection
import info.cemu.cemu.core.components.Slider
import info.cemu.cemu.core.components.Toggle
import info.cemu.cemu.core.translation.tr
import info.cemu.cemu.nativeinterface.NativeSettings

private val OverlayPositionChoices = listOf(
    NativeSettings.OVERLAY_SCREEN_POSITION_DISABLED,
    NativeSettings.OVERLAY_SCREEN_POSITION_TOP_LEFT,
    NativeSettings.OVERLAY_SCREEN_POSITION_TOP_CENTER,
    NativeSettings.OVERLAY_SCREEN_POSITION_TOP_RIGHT,
    NativeSettings.OVERLAY_SCREEN_POSITION_BOTTOM_LEFT,
    NativeSettings.OVERLAY_SCREEN_POSITION_BOTTOM_CENTER,
    NativeSettings.OVERLAY_SCREEN_POSITION_BOTTOM_RIGHT
)

private const val OVERLAY_TEXT_SCALE_STEPS = 9

@Composable
fun OverlaySettingsScreen(navigateBack: () -> Unit) {
    var overlayPosition by rememberSaveable { mutableIntStateOf(NativeSettings.getOverlayPosition()) }
    var notificationsPosition by rememberSaveable { mutableIntStateOf(NativeSettings.getNotificationsPosition()) }
    ScreenContent(
        appBarText = tr("Overlay settings"),
        navigateBack = navigateBack,
    ) {
        Header(tr("Overlay"))
        SingleSelection(
            label = tr("Position"),
            choice = overlayPosition,
            choices = OverlayPositionChoices,
            choiceToString = { overlayScreenPositionToString(it) },
            onChoiceChanged = {
                overlayPosition = it
                NativeSettings.setOverlayPosition(it)
            },
        )
        if (overlayPosition != NativeSettings.OVERLAY_SCREEN_POSITION_DISABLED)
            OverlaySettings()
        Header(tr("Notifications"))
        SingleSelection(
            label = tr("Position"),
            choice = notificationsPosition,
            choices = OverlayPositionChoices,
            choiceToString = { overlayScreenPositionToString(it) },
            onChoiceChanged = {
                notificationsPosition = it
                NativeSettings.setNotificationsPosition(it)
            },
        )
        if (notificationsPosition != NativeSettings.OVERLAY_SCREEN_POSITION_DISABLED)
            NotificationSettings()
    }
}

@Composable
private fun OverlaySettings() {
    Slider(
        label = tr("Scale"),
        initialValue = NativeSettings::getOverlayTextScalePercentage,
        steps = OVERLAY_TEXT_SCALE_STEPS,
        valueFrom = NativeSettings.OVERLAY_TEXT_SCALE_MIN,
        valueTo = NativeSettings.OVERLAY_TEXT_SCALE_MAX,
        onValueChange = NativeSettings::setOverlayTextScalePercentage,
        labelFormatter = { "${it}%" }
    )
    Toggle(
        label = tr("FPS"),
        description = tr("The number of frames per second. Average over last 5 seconds"),
        initialCheckedState = NativeSettings::isOverlayFPSEnabled,
        onCheckedChanged = NativeSettings::setOverlayFPSEnabled,
    )
    Toggle(
        label = tr("Draw calls per frame"),
        description = tr("The number of draw calls per frame. Average over last 5 seconds"),
        initialCheckedState = NativeSettings::isOverlayDrawCallsPerFrameEnabled,
        onCheckedChanged = NativeSettings::setOverlayDrawCallsPerFrameEnabled,
    )
    Toggle(
        label = tr("CPU usage"),
        description = tr("CPU usage of Cemu in percent"),
        initialCheckedState = NativeSettings::isOverlayCPUUsageEnabled,
        onCheckedChanged = NativeSettings::setOverlayCPUUsageEnabled,
    )
    Toggle(
        label = tr("RAM usage"),
        description = tr("Cemu RAM usage in MB"),
        initialCheckedState = NativeSettings::isOverlayRAMUsageEnabled,
        onCheckedChanged = NativeSettings::setOverlayRAMUsageEnabled,
    )
    Toggle(
        label = tr("Debug"),
        description = tr("Displays internal debug information (Vulkan only)"),
        initialCheckedState = NativeSettings::isOverlayDebugEnabled,
        onCheckedChanged = NativeSettings::setOverlayDebugEnabled,
    )
}

@Composable
private fun NotificationSettings() {
    Slider(
        label = tr("Scale"),
        initialValue = NativeSettings::getNotificationsTextScalePercentage,
        steps = OVERLAY_TEXT_SCALE_STEPS,
        valueFrom = NativeSettings.OVERLAY_TEXT_SCALE_MIN,
        valueTo = NativeSettings.OVERLAY_TEXT_SCALE_MAX,
        onValueChange = NativeSettings::setNotificationsTextScalePercentage,
        labelFormatter = { "$it%" }
    )
    Toggle(
        label = tr("Controller profiles"),
        description = tr("Displays the active controller profile when starting a game"),
        initialCheckedState = NativeSettings::isNotificationControllerProfilesEnabled,
        onCheckedChanged = NativeSettings::setNotificationControllerProfilesEnabled,
    )
    Toggle(
        label = tr("Shader compiler"),
        description = tr("Shows a notification after shaders have been compiled"),
        initialCheckedState = NativeSettings::isNotificationShaderCompilerEnabled,
        onCheckedChanged = NativeSettings::setNotificationShaderCompilerEnabled,
    )
    Toggle(
        label = tr("Friend list"),
        description = tr("Shows friend list related data if online"),
        initialCheckedState = NativeSettings::isNotificationFriendListEnabled,
        onCheckedChanged = NativeSettings::setNotificationFriendListEnabled,
    )
}

private fun overlayScreenPositionToString(overlayScreenPosition: Int) = when (overlayScreenPosition) {
    NativeSettings.OVERLAY_SCREEN_POSITION_DISABLED -> tr("Disabled")
    NativeSettings.OVERLAY_SCREEN_POSITION_TOP_LEFT -> tr("Top left")
    NativeSettings.OVERLAY_SCREEN_POSITION_TOP_CENTER -> tr("Top center")
    NativeSettings.OVERLAY_SCREEN_POSITION_TOP_RIGHT -> tr("Top right")
    NativeSettings.OVERLAY_SCREEN_POSITION_BOTTOM_LEFT -> tr("Bottom left")
    NativeSettings.OVERLAY_SCREEN_POSITION_BOTTOM_CENTER -> tr("Bottom center")
    NativeSettings.OVERLAY_SCREEN_POSITION_BOTTOM_RIGHT -> tr("Bottom right")
    else -> throw IllegalArgumentException("Invalid overlay position: $overlayScreenPosition")
}