package com.example.focusreminder.platform.squad

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Realtime Firestore sync provider for Focus Squads.
 * Handles room creation with 6-character invite codes, joining,
 * and live snapshot updates.
 */
class FirebaseSquadSyncService private constructor(private val context: Context) {

    private val isFirebaseAvailable: Boolean
        get() = try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (_: Exception) {
            false
        }

    private val firestore: FirebaseFirestore?
        get() = if (isFirebaseAvailable) {
            try { FirebaseFirestore.getInstance() } catch (_: Exception) { null }
        } else null

    fun isConfigured(): Boolean = firestore != null

    suspend fun createSquad(
        squadName: String,
        createdById: String,
        initialMember: SquadMemberCloudDto
    ): Result<SquadCloudDto> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firebase is not configured yet."))
        return try {
            val squadId = "sq_" + UUID.randomUUID().toString().replace("-", "").take(10)
            val code = generateRoomCode()
            val squadDoc = SquadCloudDto(
                id = squadId,
                name = squadName.trim().ifEmpty { "Focus Squad" },
                inviteCode = code,
                createdById = createdById,
                createdAt = System.currentTimeMillis(),
                memberIds = listOf(createdById)
            )

            val squadRef = db.collection("squads").document(squadId)
            squadRef.set(squadDoc).await()

            // Put creator into members subcollection
            squadRef.collection("members").document(createdById).set(initialMember).await()

            Result.success(squadDoc)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinSquad(
        inviteCode: String,
        userId: String,
        memberData: SquadMemberCloudDto
    ): Result<SquadCloudDto> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firebase is not configured yet."))
        return try {
            val cleanCode = inviteCode.trim().uppercase()
            val query = db.collection("squads")
                .whereEqualTo("inviteCode", cleanCode)
                .limit(1)
                .get()
                .await()

            if (query.isEmpty) {
                return Result.failure(IllegalArgumentException("Invalid squad code. Check and try again."))
            }

            val squadDoc = query.documents[0].toObject(SquadCloudDto::class.java)
                ?: return Result.failure(IllegalStateException("Failed to parse squad document."))

            if (!squadDoc.memberIds.contains(userId) && squadDoc.memberIds.size >= 5) {
                return Result.failure(IllegalStateException("This squad is already full (max 5 members)."))
            }

            val squadRef = db.collection("squads").document(squadDoc.id)
            val updatedMembers = (squadDoc.memberIds + userId).distinct()

            squadRef.update("memberIds", updatedMembers).await()
            squadRef.collection("members").document(userId).set(memberData, SetOptions.merge()).await()

            Result.success(squadDoc.copy(memberIds = updatedMembers))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncMemberMetrics(
        squadId: String,
        memberData: SquadMemberCloudDto
    ): Result<Unit> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firebase is not configured yet."))
        return try {
            db.collection("squads")
                .document(squadId)
                .collection("members")
                .document(memberData.userId)
                .set(memberData, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeSquadMembers(squadId: String): Flow<List<SquadMemberCloudDto>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("squads")
            .document(squadId)
            .collection("members")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(SquadMemberCloudDto::class.java) }
                    trySend(list)
                }
            }

        awaitClose { listener.remove() }
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    companion object {
        @Volatile
        private var instance: FirebaseSquadSyncService? = null

        fun getInstance(context: Context): FirebaseSquadSyncService {
            return instance ?: synchronized(this) {
                instance ?: FirebaseSquadSyncService(context.applicationContext).also { instance = it }
            }
        }
    }
}
