package info.cemu.cemu.core.nativeenummapper

import info.cemu.cemu.core.translation.tr
import info.cemu.cemu.nativeinterface.NativeGameTitles

fun regionToString(region: Int): String = when (region) {
    NativeGameTitles.CONSOLE_REGION_JPN -> tr("Japan")
    NativeGameTitles.CONSOLE_REGION_USA -> tr("USA")
    NativeGameTitles.CONSOLE_REGION_EUR -> tr("Europe")
    NativeGameTitles.CONSOLE_REGION_AUS_DEPR -> tr("Australia")
    NativeGameTitles.CONSOLE_REGION_CHN -> tr("China")
    NativeGameTitles.CONSOLE_REGION_KOR -> tr("Korea")
    NativeGameTitles.CONSOLE_REGION_TWN -> tr("Taiwan")
    NativeGameTitles.CONSOLE_REGION_AUTO -> tr("Auto")
    else -> tr("Many")
}
