package info.cemu.cemu.core.nativeenummapper

import  info.cemu.cemu.core.translation.tr
import info.cemu.cemu.nativeinterface.NativeGameTitles.ConsoleRegion

fun regionToString(region: Int): String = when (region) {
    ConsoleRegion.JPN -> tr("Japan")
    ConsoleRegion.USA -> tr("USA")
    ConsoleRegion.EUR -> tr("Europe")
    ConsoleRegion.AUS_DEPR -> tr("Australia")
    ConsoleRegion.CHN -> tr("China")
    ConsoleRegion.KOR -> tr("Korea")
    ConsoleRegion.TWN -> tr("Taiwan")
    ConsoleRegion.AUTO -> tr("Auto")
    else -> tr("Many")
}
