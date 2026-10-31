package com.example.focusreminder.platform.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Lightweight privacy-conscious wrapper for Firebase Product Analytics.
 * Only logs high-level app interactions and feature adoption (no personal or sensitive data).
 */
object FirebaseAnalyticsHelper {
    private var analytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        if (analytics == null) {
            try {
                analytics = FirebaseAnalytics.getInstance(context.applicationContext)
            } catch (_: Exception) {
                // Graceful fallback if Firebase is not yet ready
            }
        }
    }

    fun logScreenView(screenName: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        }
        analytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    fun logSquadCreated(memberCount: Int) {
        val bundle = Bundle().apply {
            putInt("member_count", memberCount)
        }
        analytics?.logEvent("squad_created", bundle)
    }

    fun logSquadJoined() {
        analytics?.logEvent("squad_joined", null)
    }

    fun logSectorSaved(sectorCount: Int) {
        val bundle = Bundle().apply {
            putInt("sector_count", sectorCount)
        }
        analytics?.logEvent("sector_configured", bundle)
    }
}
