package com.relaysms.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

sealed class SmsResult {
    data object SentToSystem : SmsResult()
    data object PermissionRequired : SmsResult()
    data class Error(val message: String) : SmsResult()
}

class SmsSender(private val context: Context) {
    fun send(phone: String, message: String): SmsResult {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return SmsResult.PermissionRequired
        }
        if (phone.isBlank()) return SmsResult.Error("شماره کنترلر را وارد کنید.")
        if (message.isBlank()) return SmsResult.Error("متن فرمان خالی است.")

        return try {
            val manager = context.getSystemService(SmsManager::class.java)
                ?: return SmsResult.Error("سرویس پیامک در دسترس نیست.")
            manager.sendTextMessage(phone, null, message, null, null)
            SmsResult.SentToSystem
        } catch (e: Exception) {
            SmsResult.Error(e.message ?: "ارسال پیامک ناموفق بود.")
        }
    }
}
