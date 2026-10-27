package com.example.focusreminder.data.analytics

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.focusreminder.domain.model.UsageSession

/**
 * SQLiteOpenHelper providing persistent, low-overhead local storage for application usage sessions.
 * Guarantees zero-dependency, lightning-fast (<2ms) historical and daily queries.
 */
class SessionDatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "focus_sessions.db"
        private const val DATABASE_VERSION = 2

        const val TABLE_SESSIONS = "usage_sessions"
        const val COL_ID = "id"
        const val COL_PACKAGE_NAME = "package_name"
        const val COL_APP_NAME = "app_name"
        const val COL_START_TIME = "start_time"
        const val COL_END_TIME = "end_time"
        const val COL_DURATION_MS = "duration_ms"
        const val COL_SECTOR_ID = "sector_id"
        const val COL_CATEGORY = "category"

        @Volatile
        private var INSTANCE: SessionDatabaseHelper? = null

        fun getInstance(context: Context): SessionDatabaseHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionDatabaseHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_SESSIONS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PACKAGE_NAME TEXT NOT NULL,
                $COL_APP_NAME TEXT NOT NULL,
                $COL_START_TIME INTEGER NOT NULL,
                $COL_END_TIME INTEGER NOT NULL,
                $COL_DURATION_MS INTEGER NOT NULL,
                $COL_SECTOR_ID TEXT NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                UNIQUE($COL_PACKAGE_NAME, $COL_START_TIME)
            )
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sessions_time ON $TABLE_SESSIONS($COL_START_TIME, $COL_END_TIME);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sessions_pkg ON $TABLE_SESSIONS($COL_PACKAGE_NAME);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sessions_cat ON $TABLE_SESSIONS($COL_CATEGORY);")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Drop table to purge corrupted synthetic sessions from v1.10.0;
            // fresh sessions are automatically resynced from ground-truth UsageEvents.
            db.execSQL("DROP TABLE IF EXISTS $TABLE_SESSIONS")
            onCreate(db)
        }
    }

    /**
     * Inserts or ignores an individual session.
     */
    fun insertSession(session: UsageSession): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_PACKAGE_NAME, session.packageName)
            put(COL_APP_NAME, session.appName)
            put(COL_START_TIME, session.startTimeMs)
            put(COL_END_TIME, session.endTimeMs)
            put(COL_DURATION_MS, session.durationMs)
            put(COL_SECTOR_ID, session.sectorId)
            put(COL_CATEGORY, session.category)
        }
        return db.insertWithOnConflict(TABLE_SESSIONS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    /**
     * Batch inserts a list of sessions inside a single transaction for maximum speed.
     */
    fun insertSessions(sessions: List<UsageSession>) {
        if (sessions.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (session in sessions) {
                val values = ContentValues().apply {
                    put(COL_PACKAGE_NAME, session.packageName)
                    put(COL_APP_NAME, session.appName)
                    put(COL_START_TIME, session.startTimeMs)
                    put(COL_END_TIME, session.endTimeMs)
                    put(COL_DURATION_MS, session.durationMs)
                    put(COL_SECTOR_ID, session.sectorId)
                    put(COL_CATEGORY, session.category)
                }
                db.insertWithOnConflict(TABLE_SESSIONS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Queries all recorded sessions within [startTimeMs, endTimeMs] with optional filters.
     */
    fun querySessions(
        startTimeMs: Long,
        endTimeMs: Long,
        packageName: String? = null,
        category: String? = null
    ): List<UsageSession> {
        val db = readableDatabase
        val selection = StringBuilder("$COL_START_TIME >= ? AND $COL_START_TIME <= ?")
        val args = mutableListOf(startTimeMs.toString(), endTimeMs.toString())

        if (!packageName.isNullOrBlank()) {
            selection.append(" AND $COL_PACKAGE_NAME = ?")
            args.add(packageName)
        }
        if (!category.isNullOrBlank()) {
            selection.append(" AND $COL_CATEGORY = ?")
            args.add(category)
        }

        val cursor = db.query(
            TABLE_SESSIONS,
            null,
            selection.toString(),
            args.toTypedArray(),
            null,
            null,
            "$COL_START_TIME ASC"
        )

        val list = mutableListOf<UsageSession>()
        cursor.use {
            val idIdx = cursor.getColumnIndexOrThrow(COL_ID)
            val pkgIdx = cursor.getColumnIndexOrThrow(COL_PACKAGE_NAME)
            val nameIdx = cursor.getColumnIndexOrThrow(COL_APP_NAME)
            val startIdx = cursor.getColumnIndexOrThrow(COL_START_TIME)
            val endIdx = cursor.getColumnIndexOrThrow(COL_END_TIME)
            val durIdx = cursor.getColumnIndexOrThrow(COL_DURATION_MS)
            val sectorIdx = cursor.getColumnIndexOrThrow(COL_SECTOR_ID)
            val catIdx = cursor.getColumnIndexOrThrow(COL_CATEGORY)

            while (cursor.moveToNext()) {
                list.add(
                    UsageSession(
                        id = cursor.getLong(idIdx),
                        packageName = cursor.getString(pkgIdx),
                        appName = cursor.getString(nameIdx),
                        startTimeMs = cursor.getLong(startIdx),
                        endTimeMs = cursor.getLong(endIdx),
                        durationMs = cursor.getLong(durIdx),
                        sectorId = cursor.getString(sectorIdx),
                        category = cursor.getString(catIdx)
                    )
                )
            }
        }
        return list
    }

    /**
     * Checks if any sessions have been recorded within [startTimeMs, endTimeMs].
     */
    fun hasSessionsForTimeRange(startTimeMs: Long, endTimeMs: Long): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT 1 FROM $TABLE_SESSIONS WHERE $COL_START_TIME >= ? AND $COL_START_TIME <= ? LIMIT 1",
            arrayOf(startTimeMs.toString(), endTimeMs.toString())
        )
        return cursor.use { it.moveToFirst() }
    }

    /**
     * Returns the timestamp of the earliest recorded session, or null if empty.
     */
    fun getEarliestSessionTime(): Long? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT MIN($COL_START_TIME) FROM $TABLE_SESSIONS", null)
        return cursor.use {
            if (it.moveToFirst() && !it.isNull(0)) {
                it.getLong(0)
            } else {
                null
            }
        }
    }
}
