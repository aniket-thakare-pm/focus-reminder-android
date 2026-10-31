package com.example.focusreminder.platform.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
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
}

/**
 * Manages checking for GitHub Releases.
 * When an update is available, routes user to the verified GitHub download page with zero special permissions.
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

            // Find APK asset
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
                downloadUrl = json.optString("html_url", "https://github.com/$GITHUB_REPO/releases/latest")
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
        } catch (_: Exception) {
            _updateStatus.value = UpdateStatus.Idle
            return@withContext null
        }
    }

    fun openUpdateInBrowser(context: Context, downloadUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    private fun isVersionNewer(candidate: String, current: String): Boolean {
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
