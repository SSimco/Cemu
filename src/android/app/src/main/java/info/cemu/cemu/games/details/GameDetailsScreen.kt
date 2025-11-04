package info.cemu.cemu.games.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.cemu.cemu.common.ui.components.ScreenContent
import info.cemu.cemu.common.ui.localization.regionToString
import info.cemu.cemu.common.ui.localization.tr
import info.cemu.cemu.games.GameIcon
import info.cemu.cemu.nativeinterface.NativeGameTitles.Game
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun GameDetailsScreen(game: Game?, navigateBack: () -> Unit) {
    if (game == null)
        return

    ScreenContent(
        appBarText = tr("About title"),
        contentModifier = Modifier.padding(16.dp),
        contentVerticalArrangement = Arrangement.spacedBy(16.dp),
        navigateBack = navigateBack,
    ) {
        GameDetails(game)
    }
}

@Composable
fun GameDetails(game: Game) {
    GameIcon(
        game = game,
        modifier = Modifier.size(128.dp),
    )
    TitleDetailsEntry(entryName = tr("Title name"), entryData = game.name)
    TitleDetailsEntry(entryName = tr("Title ID"), entryData = game.titleId)
    TitleDetailsEntry(entryName = tr("Version"), entryData = game.version)
    TitleDetailsEntry(entryName = tr("DLC"), entryData = game.dlc)
    TitleDetailsEntry(
        entryName = tr("You've played"),
        entryData = getTimePlayed(game)
    )
    TitleDetailsEntry(
        entryName = tr("Last played"),
        entryData = getLastPlayedDate(game)
    )
    TitleDetailsEntry(
        entryName = tr("Region"),
        entryData = regionToString(game.region)
    )
    TitleDetailsEntry(
        entryName = tr("Path"),
        entryData = game.path
    )
}


private fun getTimePlayed(game: Game): String {
    if (game.minutesPlayed == 0) {
        return tr("Never played")
    }
    if (game.minutesPlayed < 60) {
        return tr("Minutes: {0}", game.minutesPlayed)
    }
    return tr(
        "Hours: {0} Minutes: {1}",
        game.minutesPlayed / 60,
        game.minutesPlayed % 60
    )
}

private val DateFormatter = DateTimeFormatter.ofLocalizedDate(
    FormatStyle.SHORT
)

private fun getLastPlayedDate(game: Game): String {
    if (game.lastPlayedYear.toInt() == 0) {
        return tr("Never played")
    }
    val lastPlayedDate = LocalDate.of(
        game.lastPlayedYear.toInt(),
        game.lastPlayedMonth.toInt(),
        game.lastPlayedDay.toInt()
    )
    return DateFormatter.format(lastPlayedDate)
}

@Composable
private fun <T> TitleDetailsEntry(entryName: String, entryData: T?) {
    Column {
        Text(
            text = entryName,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
        )
        Text(
            text = entryData?.toString() ?: "",
            fontSize = 16.sp,
        )
    }
}
