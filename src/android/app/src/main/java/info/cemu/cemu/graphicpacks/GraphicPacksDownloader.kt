package info.cemu.cemu.graphicpacks

import android.content.Context
import info.cemu.cemu.nativeinterface.NativeGraphicPacks
import info.cemu.cemu.utils.unzip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import org.json.JSONObject
import java.io.File
import java.io.IOException
import kotlin.io.path.div
import kotlin.io.path.readText

sealed class GraphicPacksDownloadStatus {
    data object CheckingForUpdates : GraphicPacksDownloadStatus()
    data object NoUpdatesAvailable : GraphicPacksDownloadStatus()
    data object Downloading : GraphicPacksDownloadStatus()
    data object FinishedDownloading : GraphicPacksDownloadStatus()
    data object Error : GraphicPacksDownloadStatus()
    data object Canceled : GraphicPacksDownloadStatus()
}

class GraphicPacksDownloader {
    private fun getCurrentVersion(graphicPacksDir: File): String? {
        val graphicPacksVersionFile =
            graphicPacksDir.toPath() / "downloadedGraphicPacks" / "version.txt"
        return try {
            graphicPacksVersionFile.readText()
        } catch (ignored: IOException) {
            null
        }
    }

    suspend fun download(
        context: Context,
        updateStatus: suspend (GraphicPacksDownloadStatus) -> Unit
    ) {
        val graphicPacksRootDir = context.getExternalFilesDir(null)
        if (graphicPacksRootDir == null) {
            updateStatus(GraphicPacksDownloadStatus.Error)
            return
        }
        val graphicPacksDirPath = graphicPacksRootDir.toPath() / "graphicPacks"
        checkForNewUpdate(graphicPacksDirPath.toFile(), updateStatus)
    }

    private suspend fun checkForNewUpdate(
        graphicPacksDir: File,
        updateStatus: suspend (GraphicPacksDownloadStatus) -> Unit
    ) {
        updateStatus(GraphicPacksDownloadStatus.CheckingForUpdates)
        val request = Request.Builder()
            .url(GITHUB_RELEASES_API_URL)
            .build()
        Client.newCall(request).executeAsync().use { response ->
            withContext(Dispatchers.IO) {
                if (!response.isSuccessful) {
                    updateStatus(GraphicPacksDownloadStatus.Error)
                    return@withContext
                }
                val json = JSONObject(response.body.string())
                val version = json.getString("name")
                if (getCurrentVersion(graphicPacksDir) == version) {
                    updateStatus(GraphicPacksDownloadStatus.NoUpdatesAvailable)
                    return@withContext
                }
                val downloadUrl = json.getJSONArray("assets")
                    .getJSONObject(0)
                    .getString("browser_download_url")
                downloadNewUpdate(graphicPacksDir, downloadUrl, version, updateStatus)
            }
        }
    }

    private suspend fun downloadNewUpdate(
        graphicPacksDir: File,
        downloadUrl: String,
        version: String,
        updateStatus: suspend (GraphicPacksDownloadStatus) -> Unit
    ) {
        updateStatus(GraphicPacksDownloadStatus.Downloading)
        val request = Request.Builder()
            .url(downloadUrl)
            .build()
        Client.newCall(request).executeAsync().use { response ->
            withContext(Dispatchers.IO) {
                if (!response.isSuccessful) {
                    updateStatus(GraphicPacksDownloadStatus.Error)
                    return@withContext
                }
                val graphicPacksTempDir = graphicPacksDir.resolve("downloadedGraphicPacksTemp")
                graphicPacksTempDir.deleteRecursively()
                unzip(
                    response.body.byteStream(),
                    graphicPacksTempDir.path.toString()
                )
                graphicPacksTempDir.resolve("version.txt").writeText(version)
                val downloadedGraphicPacksDir =
                    graphicPacksDir.resolve("downloadedGraphicPacks")
                downloadedGraphicPacksDir.deleteRecursively()
                graphicPacksTempDir.renameTo(downloadedGraphicPacksDir)
                NativeGraphicPacks.refreshGraphicPacks()
                updateStatus(GraphicPacksDownloadStatus.FinishedDownloading)
            }
        }
    }

    companion object {
        private val Client = OkHttpClient()
        private const val GITHUB_RELEASES_API_URL =
            "https://api.github.com/repos/cemu-project/cemu_graphic_packs/releases/latest"
    }
}