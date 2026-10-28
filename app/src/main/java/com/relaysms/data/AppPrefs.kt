package com.relaysms.data

import android.content.Context

class AppPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("relaysms", Context.MODE_PRIVATE)

    var phoneNumber: String
        get() = prefs.getString(KEY_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PHONE, value.trim()).apply()

    var openCommand: String
        get() = prefs.getString(KEY_OPEN, "OUT#1000") ?: "OUT#1000"
        set(value) = prefs.edit().putString(KEY_OPEN, value.trim()).apply()

    var closeCommand: String
        get() = prefs.getString(KEY_CLOSE, "OUT#0100") ?: "OUT#0100"
        set(value) = prefs.edit().putString(KEY_CLOSE, value.trim()).apply()

    var stopCommand: String
        get() = prefs.getString(KEY_STOP, "OUT#0000") ?: "OUT#0000"
        set(value) = prefs.edit().putString(KEY_STOP, value.trim()).apply()

    var timerEndAt: Long
        get() = prefs.getLong(KEY_END_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_END_AT, value).apply()

    var timerCloseCommand: String
        get() = prefs.getString(KEY_TIMER_CLOSE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TIMER_CLOSE, value).apply()

    fun clearTimer() {
        prefs.edit().remove(KEY_END_AT).remove(KEY_TIMER_CLOSE).apply()
    }

    companion object {
        private const val KEY_PHONE = "phone"
        private const val KEY_OPEN = "open_command"
        private const val KEY_CLOSE = "close_command"
        private const val KEY_STOP = "stop_command"
        private const val KEY_END_AT = "timer_end_at"
        private const val KEY_TIMER_CLOSE = "timer_close_command"
    }
}
