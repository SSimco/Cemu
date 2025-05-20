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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import info.cemu.cemu.about.AboutCemuRoute
import info.cemu.cemu.about.aboutCemuNavigation
import info.cemu.cemu.emulation.EmulationActivity
import info.cemu.cemu.provider.DocumentsProvider
import info.cemu.cemu.gamelist.GameListRoute
import info.cemu.cemu.gamelist.gameListNavigation
import info.cemu.cemu.graphicpacks.GraphicPacksRoute
import info.cemu.cemu.graphicpacks.graphicPacksNavigation
import info.cemu.cemu.guicore.components.ActivityContent
import info.cemu.cemu.nativeinterface.NativeGameTitles.Game
import info.cemu.cemu.nativeinterface.NativeSettings
import info.cemu.cemu.settings.SettingsRoute
import info.cemu.cemu.settings.settingsNavigation
import info.cemu.cemu.titlemanager.TitleManagerRoute
import info.cemu.cemu.titlemanager.titleManagerNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ActivityContent {
                MainNav()
            }
        }
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
            contentDescription = "More options"
        )
    }
    DropdownMenu(
        expanded = expandMenu,
        onDismissRequest = { expandMenu = false }
    ) {
        DropdownMenuItem(
            onClick = goToSettings,
            text = stringResource(R.string.settings)
        )
        DropdownMenuItem(
            onClick = goToGraphicPacks,
            text = stringResource(R.string.graphic_packs)
        )
        DropdownMenuItem(
            onClick = goToTitleManager,
            text = stringResource(R.string.title_manager)
        )
        DropdownMenuItem(
            onClick = { openCemuFolder(context) },
            text = stringResource(R.string.open_cemu_folder)
        )
        DropdownMenuItem(
            onClick = goToAboutCemu,
            text = stringResource(R.string.about_cemu),
        )
    }
}

private fun openCemuFolder(context: Context) {
    try {
        Intent(Intent.ACTION_VIEW).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            data = DocumentsContract.buildRootUri(
                DocumentsProvider.AUTHORITY,
                DocumentsProvider.ROOT_ID
            )
            addFlags(
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            context.startActivity(this@apply)
        }
    } catch (activityNotFoundException: ActivityNotFoundException) {
        Toast.makeText(context, R.string.failed_to_open_cemu_folder, Toast.LENGTH_LONG).show()
    }
}
