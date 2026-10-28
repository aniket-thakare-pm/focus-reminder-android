package com.example.focusreminder.data.journal

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.focusreminder.domain.model.JournalReflectionRecord
import org.json.JSONArray

class JournalDatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "journal_reflections.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_JOURNAL = "journal_reflections"
        const val COL_ID = "id"
        const val COL_TIMESTAMP = "timestamp_ms"
        const val COL_PACKAGE_NAME = "package_name"
        const val COL_APP_NAME = "app_name"
        const val COL_REQUESTED_MINUTES = "requested_minutes"
        const val COL_ANSWERS_JSON = "answers_json"

        @Volatile
        private var INSTANCE: JournalDatabaseHelper? = null

        fun getInstance(context: Context): JournalDatabaseHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: JournalDatabaseHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_JOURNAL (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_PACKAGE_NAME TEXT NOT NULL,
                $COL_APP_NAME TEXT NOT NULL,
                $COL_REQUESTED_MINUTES INTEGER NOT NULL,
                $COL_ANSWERS_JSON TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_journal_time ON $TABLE_JOURNAL($COL_TIMESTAMP);")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_JOURNAL")
        onCreate(db)
    }

    fun insertEntry(entry: JournalReflectionRecord): Long {
        val db = writableDatabase
        val jsonArray = JSONArray(entry.answers)
        val values = ContentValues().apply {
            put(COL_TIMESTAMP, entry.timestampMs)
            put(COL_PACKAGE_NAME, entry.packageName)
            put(COL_APP_NAME, entry.appName)
            put(COL_REQUESTED_MINUTES, entry.requestedMinutes)
            put(COL_ANSWERS_JSON, jsonArray.toString())
        }
        return db.insert(TABLE_JOURNAL, null, values)
    }

    fun getAllEntries(): List<JournalReflectionRecord> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_JOURNAL,
            null,
            null,
            null,
            null,
            null,
            "$COL_TIMESTAMP DESC"
        )

        val list = mutableListOf<JournalReflectionRecord>()
        cursor.use {
            val idIdx = cursor.getColumnIndexOrThrow(COL_ID)
            val timeIdx = cursor.getColumnIndexOrThrow(COL_TIMESTAMP)
            val pkgIdx = cursor.getColumnIndexOrThrow(COL_PACKAGE_NAME)
            val appIdx = cursor.getColumnIndexOrThrow(COL_APP_NAME)
            val minIdx = cursor.getColumnIndexOrThrow(COL_REQUESTED_MINUTES)
            val ansIdx = cursor.getColumnIndexOrThrow(COL_ANSWERS_JSON)

            while (cursor.moveToNext()) {
                val answersRaw = cursor.getString(ansIdx)
                val answers = mutableListOf<String>()
                try {
                    val array = JSONArray(answersRaw)
                    for (i in 0 until array.length()) {
                        answers.add(array.getString(i))
                    }
                } catch (_: Exception) {
                    if (answersRaw.isNotBlank()) answers.add(answersRaw)
                }

                list.add(
                    JournalReflectionRecord(
                        id = cursor.getLong(idIdx),
                        timestampMs = cursor.getLong(timeIdx),
                        packageName = cursor.getString(pkgIdx),
                        appName = cursor.getString(appIdx),
                        requestedMinutes = cursor.getInt(minIdx),
                        answers = answers
                    )
                )
            }
        }
        return list
    }

    fun deleteEntry(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_JOURNAL, "$COL_ID = ?", arrayOf(id.toString()))
    }
}
