package com.example.focusreminder.platform.auth

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Manages user identity for the Focus Squad.
 * Uses Firebase Anonymous Authentication by default for zero-friction room joining,
 * and gracefully falls back to a persistent local UUID if Firebase credentials
 * (google-services.json) are not yet supplied.
 */
class AuthManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val isFirebaseAvailable: Boolean
        get() = try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (_: Exception) {
            false
        }

    private val auth: FirebaseAuth?
        get() = if (isFirebaseAvailable) {
            try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
        } else null

    fun getCurrentUserId(): String {
        val fbUid = auth?.currentUser?.uid
        if (!fbUid.isNullOrBlank()) {
            return fbUid
        }
        var localUid = prefs.getString("local_user_uid", null)
        if (localUid.isNullOrBlank()) {
            localUid = "usr_" + UUID.randomUUID().toString().take(12)
            prefs.edit().putString("local_user_uid", localUid).apply()
        }
        return localUid
    }

    fun getDisplayName(): String {
        return prefs.getString("user_display_name", null)
            ?: auth?.currentUser?.displayName
            ?: "You"
    }

    fun setDisplayName(name: String) {
        val trimmed = name.trim().take(16)
        prefs.edit().putString("user_display_name", trimmed).apply()
    }

    suspend fun ensureAuthenticated(): String = withContext(Dispatchers.IO) {
        val firebaseAuth = auth
        if (firebaseAuth != null) {
            val existing = firebaseAuth.currentUser
            if (existing != null) {
                return@withContext existing.uid
            }
            try {
                val result = firebaseAuth.signInAnonymously().await()
                val uid = result.user?.uid
                if (!uid.isNullOrBlank()) {
                    return@withContext uid
                }
            } catch (e: Exception) {
                // Network or config fallback
            }
        }
        return@withContext getCurrentUserId()
    }

    companion object {
        @Volatile
        private var instance: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
