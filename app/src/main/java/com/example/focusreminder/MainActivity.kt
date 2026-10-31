package com.example.focusreminder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.example.focusreminder.platform.UsagePermissionHelper
import com.example.focusreminder.platform.analytics.FirebaseAnalyticsHelper
import com.example.focusreminder.platform.update.AppUpdateManager
import com.example.focusreminder.service.UsageMonitorService
import com.example.focusreminder.theme.FocusReminderTheme
import com.example.focusreminder.ui.components.AppUpdateDialog

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseAnalyticsHelper.init(this)

        val currentVersionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.16.0"
        } catch (_: Exception) {
            "1.16.0"
        }

        // Request POST_NOTIFICATIONS on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        setContent {
            FocusReminderTheme {
                LaunchedEffect(Unit) {
                    AppUpdateManager.checkForUpdates(this@MainActivity, currentVersionName)
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()
                    AppUpdateDialog()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Automatically start the usage enforcement service if permissions are granted
        if (UsagePermissionHelper.hasUsageAccessPermission(this)) {
            UsageMonitorService.start(this)
        }
    }
}
