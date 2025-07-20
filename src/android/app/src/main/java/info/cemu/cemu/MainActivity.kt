package info.cemu.cemu

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import info.cemu.cemu.about.AboutCemuRoute
import info.cemu.cemu.about.aboutCemuNavigation
import info.cemu.cemu.core.components.ActivityContent
import info.cemu.cemu.core.translation.TranslatableContent
import info.cemu.cemu.core.translation.tr
import info.cemu.cemu.emulation.EmulationActivity
import info.cemu.cemu.gamelist.GameListRoute
import info.cemu.cemu.gamelist.gameListNavigation
import info.cemu.cemu.graphicpacks.GraphicPacksRoute
import info.cemu.cemu.graphicpacks.graphicPacksNavigation
import info.cemu.cemu.nativeinterface.NativeActiveSettings
import info.cemu.cemu.nativeinterface.NativeGameTitles.Game
import info.cemu.cemu.nativeinterface.NativeSettings
import info.cemu.cemu.provider.DocumentsProvider
import info.cemu.cemu.settings.SettingsRoute
import info.cemu.cemu.settings.settingsNavigation
import info.cemu.cemu.titlemanager.TitleManagerRoute
import info.cemu.cemu.titlemanager.titleManagerNavigation
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TranslatableContent {
                ActivityContent {
                    MainNav()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        NativeSettings.saveSettings()
    }

    override fun onPause() {
        super.onPause()
        NativeSettings.saveSettings()
    }
}

private fun startGame(context: Context, game: Game) {
    Intent(
        context,
        EmulationActivity::class.java
    ).apply {
        putExtra(EmulationActivity.EXTRA_LAUNCH_PATH, game.path)
        context.startActivity(this)
    }
}

@Composable
private fun MainNav() {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = GameListRoute,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None }
    ) {
        gameListNavigation(navController, startGame = { startGame(context, it) }) {
            GameListToolBarActionsMenu(
                goToSettings = { navController.navigate(SettingsRoute) },
                goToTitleManager = { navController.navigate(TitleManagerRoute) },
                goToGraphicPacks = { navController.navigate(GraphicPacksRoute) },
                goToAboutCemu = { navController.navigate(AboutCemuRoute) }
            )
        }
        settingsNavigation(navController)
        titleManagerNavigation(navController)
        graphicPacksNavigation(navController)
        aboutCemuNavigation(navController)
    }
}

@Composable
private fun GameListToolBarActionsMenu(
    goToSettings: () -> Unit,
    goToTitleManager: () -> Unit,
    goToGraphicPacks: () -> Unit,
    goToAboutCemu: () -> Unit,
) {
    var expandMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    @Composable
    fun DropdownMenuItem(onClick: () -> Unit, text: String) {
        DropdownMenuItem(
            onClick = {
                onClick()
                expandMenu = false
            },
            text = { Text(text) },
        )
    }
    IconButton(
        modifier = Modifier.padding(end = 8.dp),
        onClick = { expandMenu = true },
    ) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = null
        )
    }
    DropdownMenu(
        expanded = expandMenu,
        onDismissRequest = { expandMenu = false }
    ) {
        DropdownMenuItem(
            onClick = goToSettings,
            text = tr("Settings")
        )
        DropdownMenuItem(
            onClick = goToGraphicPacks,
            text = tr("Graphic packs")
        )
        DropdownMenuItem(
            onClick = goToTitleManager,
            text = tr("Title manager")
        )
        DropdownMenuItem(
            onClick = { openCemuFolder(context) },
            text = tr("Open Cemu folder")
        )
        DropdownMenuItem(
            onClick = { shareLogFile(context) },
            text = tr("Share log file"),
        )
        DropdownMenuItem(
            onClick = goToAboutCemu,
            text = tr("About Cemu"),
        )
    }
}

private fun shareLogFile(context: Context) {
    val logFileName = "log.txt"
    val logFile = File(NativeActiveSettings.getUserDataPath()).resolve(logFileName)

    if (!logFile.isFile) {
        Toast.makeText(context, tr("Log file doesn't exist"), Toast.LENGTH_LONG).show()
        return
    }

    val fileUri = DocumentsContract.buildDocumentUri(
        DocumentsProvider.AUTHORITY,
        DocumentsProvider.ROOT_ID + "/$logFileName"
    )

    val documentFile = DocumentFile.fromSingleUri(context, fileUri) ?: return

    val intent = Intent(Intent.ACTION_SEND)
        .setDataAndType(documentFile.uri, "text/plain")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .putExtra(Intent.EXTRA_STREAM, documentFile.uri)

    context.startActivity(Intent.createChooser(intent, null))
}

private fun openCemuFolder(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_DEFAULT)
            .addFlags(
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        intent.data = DocumentsContract.buildRootUri(
            DocumentsProvider.AUTHORITY,
            DocumentsProvider.ROOT_ID
        )
        context.startActivity(intent)
    } catch (activityNotFoundException: ActivityNotFoundException) {
        Toast.makeText(context, tr("Could not open Cemu folder"), Toast.LENGTH_LONG).show()
    }
}
