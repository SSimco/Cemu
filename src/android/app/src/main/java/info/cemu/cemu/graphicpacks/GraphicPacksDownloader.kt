package info.cemu.cemu.graphicpacks

import android.content.Context
import info.cemu.cemu.BuildConfig
import info.cemu.cemu.common.io.unzip
import info.cemu.cemu.nativeinterface.NativeGraphicPacks
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

enum class GraphicPacksDownloadStatus {
    CHECKING_VERSION,
    NO_UPDATES_AVAILABLE,
    DOWNLOADING,
    EXTRACTING,
    FINISHED_DOWNLOADING,
    ERROR,
    CANCELED
}

class GraphicPacksDownloader {
    private fun getCurrentVersion(graphicPacksDir: File): String? {
        val graphicPacksVersionFile =
            graphicPacksDir.toPath() / "downloadedGraphicPacks" / "version.txt"
        return try {
            graphicPacksVersionFile.readText()
        } catch (_: IOException) {
            null
        }
    }

    suspend fun download(
        context: Context,
        updateStatus: suspend (GraphicPacksDownloadStatus) -> Unit
    ) {
        val graphicPacksRootDir = context.getExternalFilesDir(null)
        if (graphicPacksRootDir == null) {
            updateStatus(GraphicPacksDownloadStatus.ERROR)
            return
        }

        val graphicPacksDirPath = graphicPacksRootDir.toPath() / "graphicPacks"
        checkForNewUpdate(graphicPacksDirPath.toFile(), updateStatus)
    }

    private suspend fun getUpdateUrl(): String {
        val queryUrl = "https://cemu.info/api2/query_graphicpack_url.php?" +
                "version=${BuildConfig.VERSION_NAME}" +
                "&t=${System.currentTimeMillis()}"

        val request = Request.Builder()
            .url(queryUrl)
            .build()

        Client.newCall(request).executeAsync().use { response ->
            if (response.isSuccessful) {
                val body = response.body.string().trim()
                if (body.startsWith("http")) {
                    return body
                }
            }
        }

        return "https://api.github.com/repos/cemu-project/cemu_graphic_packs/releases/latest"
    }

    private suspend fun checkForNewUpdate(
        graphicPacksDir: File,
        updateStatus: suspend (GraphicPacksDownloadStatus) -> Unit
    ) {
        updateStatus(GraphicPacksDownloadStatus.CHECKING_VERSION)
        val request = Request.Builder()
            .url(getUpdateUrl())
            .build()
        Client.newCall(request).executeAsync().use { response ->
            withContext(Dispatchers.IO) {
                if (!response.isSuccessful) {
                    updateStatus(GraphicPacksDownloadStatus.ERROR)
                    return@withContext
                }
                val json = JSONObject(response.body.string())
                val version = json.getString("name")
                if (getCurrentVersion(graphicPacksDir) == version) {
                    updateStatus(GraphicPacksDownloadStatus.NO_UPDATES_AVAILABLE)
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
        updateStatus(GraphicPacksDownloadStatus.DOWNLOADING)

        val request = Request.Builder()
            .url(downloadUrl)
            .build()

        Client.newCall(request).executeAsync().use { response ->
            withContext(Dispatchers.IO) {
                if (!response.isSuccessful) {
                    updateStatus(GraphicPacksDownloadStatus.ERROR)
                    return@withContext
                }

                updateStatus(GraphicPacksDownloadStatus.EXTRACTING)

                val graphicPacksTempDir = graphicPacksDir.resolve("downloadedGraphicPacksTemp")
                graphicPacksTempDir.deleteRecursively()
                unzip(
                    response.body.byteStream(),
                    graphicPacksTempDir.path
                )
                graphicPacksTempDir.resolve("version.txt").writeText(version)
                val downloadedGraphicPacksDir =
                    graphicPacksDir.resolve("downloadedGraphicPacks")
                downloadedGraphicPacksDir.deleteRecursively()
                graphicPacksTempDir.renameTo(downloadedGraphicPacksDir)
                NativeGraphicPacks.refreshGraphicPacks()

                updateStatus(GraphicPacksDownloadStatus.FINISHED_DOWNLOADING)
            }
        }
    }

    companion object {
        private val Client = OkHttpClient()
    }
}