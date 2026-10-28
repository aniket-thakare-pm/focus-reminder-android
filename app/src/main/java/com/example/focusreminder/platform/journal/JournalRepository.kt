package com.example.focusreminder.platform.journal

import android.content.Context
import com.example.focusreminder.data.journal.JournalDatabaseHelper
import com.example.focusreminder.domain.model.JournalReflectionRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JournalRepository private constructor(context: Context) {

    private val dbHelper = JournalDatabaseHelper.getInstance(context)

    companion object {
        @Volatile
        private var INSTANCE: JournalRepository? = null

        fun getInstance(context: Context): JournalRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: JournalRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    suspend fun saveReflection(
        packageName: String,
        appName: String,
        requestedMinutes: Int,
        answers: List<String>
    ): Long = withContext(Dispatchers.IO) {
        val record = JournalReflectionRecord(
            timestampMs = System.currentTimeMillis(),
            packageName = packageName,
            appName = appName,
            requestedMinutes = requestedMinutes,
            answers = answers
        )
        dbHelper.insertEntry(record)
    }

    suspend fun getAllReflections(): List<JournalReflectionRecord> = withContext(Dispatchers.IO) {
        dbHelper.getAllEntries()
    }

    suspend fun deleteReflection(id: Long): Boolean = withContext(Dispatchers.IO) {
        dbHelper.deleteEntry(id) > 0
    }
}
