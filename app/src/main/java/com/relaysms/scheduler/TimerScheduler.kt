package com.relaysms.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.relaysms.data.AppPrefs

object TimerScheduler {
    private const val REQUEST_CODE = 7301

    fun scheduleClose(context: Context, endAtMillis: Long, closeCommand: String) {
        val prefs = AppPrefs(context)
        prefs.timerEndAt = endAtMillis
        prefs.timerCloseCommand = closeCommand

        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = closePendingIntent(context)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                alarmManager.canScheduleExactAlarms()
            ) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, endAtMillis, pendingIntent
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, endAtMillis, pendingIntent
                )
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, endAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, endAtMillis, pendingIntent
            )
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager?.cancel(closePendingIntent(context))
        AppPrefs(context).clearTimer()
    }

    fun restore(context: Context) {
        val prefs = AppPrefs(context)
        val endAt = prefs.timerEndAt
        val command = prefs.timerCloseCommand
        if (endAt <= 0L || command.isBlank()) return
        if (endAt <= System.currentTimeMillis()) {
            val intent = Intent(context, CloseTimerReceiver::class.java)
            context.sendBroadcast(intent)
        } else {
            scheduleClose(context, endAt, command)
        }
    }

    private fun closePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, CloseTimerReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
