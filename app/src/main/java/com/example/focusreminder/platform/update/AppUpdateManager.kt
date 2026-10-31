package com.example.focusreminder.platform.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val tagName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val apkSize: Long
)

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class Available(val info: UpdateInfo) : UpdateStatus()
    data class Downloading(val progress: Float) : UpdateStatus()
    data class ReadyToInstall(val apkFile: File) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

/**
 * Manages OTA in-app updates directly from GitHub Releases.
 */
object AppUpdateManager {
    private const val GITHUB_REPO = "aniket-thakare-pm/focus-reminder-android"
    private const val RELEASES_API = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    fun dismissUpdate() {
        _updateStatus.value = UpdateStatus.Idle
    }

    suspend fun checkForUpdates(context: Context, currentVersionName: String): UpdateInfo? = withContext(Dispatchers.IO) {
        _updateStatus.value = UpdateStatus.Checking
        try {
            val url = URL(RELEASES_API)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "FocusReminderApp")
            }

            if (conn.responseCode != 200) {
                _updateStatus.value = UpdateStatus.Idle
                return@withContext null
            }

            val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonStr)

            val rawTagName = json.optString("tag_name", "").trim()
            val cleanTagName = rawTagName.removePrefix("v").trim()
            val body = json.optString("body", "Bug fixes and improvements.").trim()

            // Find APK asset in assets array
            var downloadUrl = ""
            var apkSize = 0L
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            if (downloadUrl.isEmpty()) {
                _updateStatus.value = UpdateStatus.Idle
                return@withContext null
            }

            // Compare versions
            val isNewer = isVersionNewer(candidate = cleanTagName, current = currentVersionName.removePrefix("v").trim())
            if (isNewer) {
                val info = UpdateInfo(
                    tagName = rawTagName,
                    releaseNotes = body,
                    downloadUrl = downloadUrl,
                    apkSize = apkSize
                )
                _updateStatus.value = UpdateStatus.Available(info)
                return@withContext info
            } else {
                _updateStatus.value = UpdateStatus.Idle
                return@withContext null
            }
        } catch (e: Exception) {
            _updateStatus.value = UpdateStatus.Idle
            return@withContext null
        }
    }

    suspend fun downloadAndInstallApk(context: Context, updateInfo: UpdateInfo) = withContext(Dispatchers.IO) {
        try {
            _updateStatus.value = UpdateStatus.Downloading(0f)

            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val destinationFile = File(updatesDir, "FocusReminder.apk")
            if (destinationFile.exists()) destinationFile.delete()

            // Handle potential HTTP 302 redirects from GitHub to AWS S3
            var currentUrl = updateInfo.downloadUrl
            var conn: HttpURLConnection
            var redirectCount = 0

            while (true) {
                conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "FocusReminderApp")
                }
                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM || code == 307 || code == 308) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    currentUrl = location
                    redirectCount++
                    if (redirectCount > 5) throw IllegalStateException("Too many redirects")
                } else if (code == 200) {
                    break
                } else {
                    throw IllegalStateException("Failed to download APK: HTTP $code")
                }
            }

            val totalBytes = conn.contentLength.toLong().let { if (it > 0) it else updateInfo.apkSize }
            var downloadedBytes = 0L
            var lastPercent = -1

            java.io.BufferedInputStream(conn.inputStream, 64 * 1024).use { input ->
                java.io.BufferedOutputStream(FileOutputStream(destinationFile), 64 * 1024).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            val progressPercent = ((downloadedBytes * 100L) / totalBytes).toInt()
                            if (progressPercent != lastPercent) {
                                lastPercent = progressPercent
                                _updateStatus.value = UpdateStatus.Downloading(progressPercent / 100f)
                            }
                        }
                    }
                    output.flush()
                }
            }

            _updateStatus.value = UpdateStatus.ReadyToInstall(destinationFile)
            withContext(Dispatchers.Main) {
                installApk(context, destinationFile)
            }
        } catch (e: Exception) {
            _updateStatus.value = UpdateStatus.Error(e.message ?: "Download failed")
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(installIntent)
        } catch (_: Exception) {
            // Error handling if unknown sources intent cannot launch
        }
    }

    private fun isVersionNewer(candidate: String, current: String): Boolean {
        // e.g. candidate: "1.17.0", current: "1.16.0"
        val cParts = candidate.split(".").mapNotNull { it.toIntOrNull() }
        val curParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(cParts.size, curParts.size)
        for (i in 0 until maxLen) {
            val c = cParts.getOrElse(i) { 0 }
            val cur = curParts.getOrElse(i) { 0 }
            if (c > cur) return true
            if (c < cur) return false
        }
        return false
    }
}
