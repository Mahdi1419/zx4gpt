package com.relaysms.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.relaysms.data.AppPrefs
import com.relaysms.sms.SmsSender
import com.relaysms.sms.SmsResult

class CloseTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = AppPrefs(context)
        val endAt = prefs.timerEndAt
        if (endAt == 0L) return

        // Ignore an outdated alarm if a newer timer replaced it.
        if (endAt > System.currentTimeMillis() + 1000L) {
            TimerScheduler.scheduleClose(context, endAt, prefs.timerCloseCommand)
            return
        }

        val command = prefs.timerCloseCommand
        val result = SmsSender(context).send(prefs.phoneNumber, command)
        if (result !is SmsResult.PermissionRequired) {
            prefs.clearTimer()
        }
    }
}
