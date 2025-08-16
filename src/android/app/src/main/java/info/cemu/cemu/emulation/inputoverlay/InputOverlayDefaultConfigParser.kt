package info.cemu.cemu.emulation.inputoverlay

import info.cemu.cemu.common.string.toIntOrZero
import org.xmlpull.v1.XmlPullParser

data class InputConfig(
    val width: Int,
    val height: Int,
    val alignEnd: Boolean,
    val alignBottom: Boolean,
    val paddingHorizontal: Int,
    val paddingVertical: Int,
)

private const val INPUT_OVERLAY_CONFIG_TAG_NAME = "input-overlay-config"
private const val WIDTH_CONFIG_TAG_NAME = "width"
private const val SIZE_CONFIG_TAG_NAME = "size"
private const val HEIGHT_CONFIG_TAG_NAME = "height"
private const val ALIGN_END_CONFIG_TAG_NAME = "align-end"
private const val ALIGN_BOTTOM_CONFIG_TAG_NAME = "align-bottom"
private const val PADDING_HORIZONTAL_CONFIG_TAG_NAME = "padding-horizontal"
private const val PADDING_VERTICAL_CONFIG_TAG_NAME = "padding-vertical"
private const val NAME_CONFIG_TAG_NAME = "name"

private fun parseInputConfig(xmlPullParser: XmlPullParser): Pair<String, InputConfig>? {
    var name = ""
    var width = 0
    var height = 0
    var alignEnd = false
    var alignBottom = false
    var paddingHorizontal = 0
    var paddingVertical = 0
    var eventType = xmlPullParser.eventType
    var currentTag = ""
    while (eventType != XmlPullParser.END_DOCUMENT) {
        when (eventType) {
            XmlPullParser.END_TAG -> {
                if (xmlPullParser.name == INPUT_OVERLAY_CONFIG_TAG_NAME) {
                    return if (name.isBlank()) null
                    else name to InputConfig(
                        width = width,
                        height = height,
                        alignEnd = alignEnd,
                        alignBottom = alignBottom,
                        paddingHorizontal = paddingHorizontal,
                        paddingVertical = paddingVertical,
                    )
                }
            }

            XmlPullParser.START_TAG -> {
                currentTag = xmlPullParser.name
            }

            XmlPullParser.TEXT -> {
                val text = xmlPullParser.text
                when (currentTag) {
                    SIZE_CONFIG_TAG_NAME -> {
                        val size = text.toIntOrZero()
                        width = size
                        height = size
                    }

                    WIDTH_CONFIG_TAG_NAME -> width = text.toIntOrZero()
                    HEIGHT_CONFIG_TAG_NAME -> height = text.toIntOrZero()
                    ALIGN_END_CONFIG_TAG_NAME -> alignEnd = text.toBoolean()
                    ALIGN_BOTTOM_CONFIG_TAG_NAME -> alignBottom = text.toBoolean()
                    PADDING_HORIZONTAL_CONFIG_TAG_NAME -> paddingHorizontal = text.toIntOrZero()
                    PADDING_VERTICAL_CONFIG_TAG_NAME -> paddingVertical = text.toIntOrZero()
                    NAME_CONFIG_TAG_NAME -> name = text
                }
            }
        }
        eventType = xmlPullParser.next()
    }
    return null
}

fun parseDefaultInputConfigs(xmlPullParser: XmlPullParser): Map<String, InputConfig> {
    val inputConfigs = mutableMapOf<String, InputConfig>()
    var eventType = xmlPullParser.eventType
    while (eventType != XmlPullParser.END_DOCUMENT) {
        if (eventType == XmlPullParser.START_TAG && xmlPullParser.name == INPUT_OVERLAY_CONFIG_TAG_NAME) {
            val inputConfig = parseInputConfig(xmlPullParser)
            if (inputConfig != null) {
                inputConfigs[inputConfig.first] = inputConfig.second
            }
        }
        eventType = xmlPullParser.next()
    }
    return inputConfigs
}