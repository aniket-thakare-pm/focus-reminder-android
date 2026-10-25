package com.example.focusreminder.platform

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Data model for a user-scheduled focus reminder or P0 sticky alert.
 */
data class ScheduledReminder(
    val id: Long,
    val title: String,
    val description: String?,
    val triggerTimeMs: Long,
    val isP0: Boolean,
    val isCompleted: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis()
)

/**
 * Persistent repository managing user-configured reminders.
 * Persists to SharedPreferences as JSON and provides a reactive StateFlow for Compose UI.
 */
class ReminderRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _remindersFlow = MutableStateFlow<List<ScheduledReminder>>(emptyList())
    val remindersFlow: StateFlow<List<ScheduledReminder>> = _remindersFlow.asStateFlow()

    init {
        loadReminders()
    }

    companion object {
        private const val PREFS_NAME = "scheduled_reminders_storage"
        private const val KEY_REMINDERS_JSON = "key_reminders_json"

        @Volatile
        private var INSTANCE: ReminderRepository? = null

        fun getInstance(context: Context): ReminderRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ReminderRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    @Synchronized
    private fun loadReminders() {
        val jsonStr = prefs.getString(KEY_REMINDERS_JSON, null)
        val list = mutableListOf<ScheduledReminder>()
        if (!jsonStr.isNullOrBlank()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        ScheduledReminder(
                            id = obj.getLong("id"),
                            title = obj.getString("title"),
                            description = obj.optString("description").takeIf { it.isNotEmpty() },
                            triggerTimeMs = obj.getLong("triggerTimeMs"),
                            isP0 = obj.getBoolean("isP0"),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            createdAtMs = obj.optLong("createdAtMs", System.currentTimeMillis())
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        _remindersFlow.value = list.sortedBy { it.triggerTimeMs }
    }

    @Synchronized
    private fun saveReminders(reminders: List<ScheduledReminder>) {
        val array = JSONArray()
        for (r in reminders) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("title", r.title)
                put("description", r.description ?: "")
                put("triggerTimeMs", r.triggerTimeMs)
                put("isP0", r.isP0)
                put("isCompleted", r.isCompleted)
                put("createdAtMs", r.createdAtMs)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_REMINDERS_JSON, array.toString()).apply()
        _remindersFlow.value = reminders.sortedBy { it.triggerTimeMs }
    }

    @Synchronized
    fun getAllReminders(): List<ScheduledReminder> {
        return _remindersFlow.value
    }

    @Synchronized
    fun getActiveReminders(): List<ScheduledReminder> {
        return _remindersFlow.value.filter { !it.isCompleted }
    }

    @Synchronized
    fun getNextUpcomingP0(): ScheduledReminder? {
        val now = System.currentTimeMillis()
        // Grace window: include reminders due in future or triggered within the last 2 minutes
        return _remindersFlow.value
            .filter { it.isP0 && !it.isCompleted && it.triggerTimeMs >= (now - 120_000L) }
            .minByOrNull { it.triggerTimeMs }
    }

    @Synchronized
    fun addReminder(reminder: ScheduledReminder) {
        val current = _remindersFlow.value.toMutableList()
        current.removeAll { it.id == reminder.id }
        current.add(reminder)
        saveReminders(current)
    }

    @Synchronized
    fun deleteReminder(id: Long) {
        val current = _remindersFlow.value.toMutableList()
        current.removeAll { it.id == id }
        saveReminders(current)
    }

    @Synchronized
    fun markCompleted(id: Long) {
        val current = _remindersFlow.value.map {
            if (it.id == id) it.copy(isCompleted = true) else it
        }
        saveReminders(current)
    }

    @Synchronized
    fun updateReminderTriggerTime(id: Long, newTriggerMs: Long) {
        val current = _remindersFlow.value.map {
            if (it.id == id) it.copy(triggerTimeMs = newTriggerMs, isCompleted = false) else it
        }
        saveReminders(current)
    }

    fun rescheduleAllActiveAlarms(context: Context) {
        val now = System.currentTimeMillis()
        getActiveReminders().forEach { reminder ->
            if (reminder.triggerTimeMs > now) {
                ReminderScheduler.scheduleReminder(
                    context = context,
                    reminderId = reminder.id,
                    title = reminder.title,
                    description = reminder.description,
                    triggerAtMs = reminder.triggerTimeMs,
                    isP0 = reminder.isP0
                )
            }
        }
    }
}
