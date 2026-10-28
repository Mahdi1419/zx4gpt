package com.relaysms

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.relaysms.data.AppPrefs
import com.relaysms.scheduler.TimerScheduler
import com.relaysms.sms.SmsResult
import com.relaysms.sms.SmsSender
import kotlinx.coroutines.delay
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TimerScheduler.restore(this)
        setContent {
            RelaySmsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RelaySmsApp()
                }
            }
        }
    }
}

@Composable
private fun RelaySmsTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF75D6B1),
        secondary = androidx.compose.ui.graphics.Color(0xFF9BB7FF),
        background = androidx.compose.ui.graphics.Color(0xFF101613),
        surface = androidx.compose.ui.graphics.Color(0xFF18211D),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFF24312B)
    )
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
private fun RelaySmsApp() {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    val smsSender = remember { SmsSender(context) }

    var phone by remember { mutableStateOf(prefs.phoneNumber) }
    var openCommand by remember { mutableStateOf(prefs.openCommand) }
    var closeCommand by remember { mutableStateOf(prefs.closeCommand) }
    var stopCommand by remember { mutableStateOf(prefs.stopCommand) }
    var minutes by remember { mutableIntStateOf(60) }
    var endAt by remember { mutableLongStateOf(prefs.timerEndAt) }
    var remaining by remember { mutableLongStateOf(0L) }
    var showSettings by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("آماده کنترل") }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        message = if (granted) "مجوز پیامک فعال شد؛ دوباره فرمان را اجرا کنید."
        else "برای کنترل پیامکی باید مجوز ارسال SMS را فعال کنید."
    }

    val saveSettings = {
        prefs.phoneNumber = phone
        prefs.openCommand = openCommand
        prefs.closeCommand = closeCommand
        prefs.stopCommand = stopCommand
    }

    fun sendCommand(command: String) {
        saveSettings()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            return
        }
        when (val result = smsSender.send(phone, command)) {
            SmsResult.SentToSystem -> message = "درخواست ارسال فرمان ثبت شد؛ دریافت آن تأیید نشده است."
            SmsResult.PermissionRequired -> message = "مجوز ارسال پیامک لازم است."
            is SmsResult.Error -> message = result.message
        }
    }

    LaunchedEffect(endAt) {
        while (true) {
            remaining = if (endAt > 0L) (endAt - System.currentTimeMillis()).coerceAtLeast(0L) else 0L
            delay(1000)
        }
    }

    val exactAlarmLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        message = "تنظیمات آلارم بررسی شد."
    }

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Icon(
                        Icons.Rounded.WaterDrop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(12.dp).size(30.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("RelaySMS", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("کنترلر پیامکی · ZX4 و رله‌ها", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { showSettings = !showSettings }) {
                    Icon(Icons.Rounded.Settings, contentDescription = "تنظیمات")
                }
            }

            ElevatedCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("کنترل شیر / رله", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("شماره سیم‌کارت کنترلر") },
                        placeholder = { Text("مثلاً 09xxxxxxxxx") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { sendCommand(openCommand) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.WaterDrop, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("باز کردن")
                        }
                        OutlinedButton(onClick = { sendCommand(closeCommand) }, modifier = Modifier.weight(1f)) {
                            Text("بستن")
                        }
                    }
                    OutlinedButton(
                        onClick = { sendCommand(stopCommand) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.StopCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("توقف موتور")
                    }
                }
            }

            ElevatedCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("تایمر بستن خودکار", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    }
                    Text("پس از باز کردن شیر، فرمان بستن پس از مدت انتخابی ارسال می‌شود.")
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { minutes = (minutes - 5).coerceAtLeast(5) }) { Text("−۵") }
                        Text("$minutes دقیقه", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = { minutes = (minutes + 5).coerceAtMost(1440) }) { Text("+۵") }
                    }
                    Button(
                        onClick = {
                            saveSettings()
                            if (phone.isBlank()) {
                                message = "ابتدا شماره کنترلر را وارد کنید."
                            } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                                != PackageManager.PERMISSION_GRANTED
                            ) {
                                smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                            } else {
                                val now = System.currentTimeMillis()
                                val target = now + minutes * 60_000L
                                TimerScheduler.scheduleClose(context, target, closeCommand)
                                endAt = target
                                sendCommand(openCommand)
                                message = "فرمان بازشدن درخواست شد؛ تایمر فعال شد."
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("باز کن و تایمر را شروع کن")
                    }
                    if (remaining > 0L) {
                        val mins = remaining / 60_000L
                        val secs = (remaining % 60_000L) / 1000L
                        Text(
                            "زمان باقی‌مانده: %02d:%02d".format(Locale.ROOT, mins, secs),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(
                            onClick = {
                                TimerScheduler.cancel(context)
                                endAt = 0L
                                remaining = 0L
                                message = "تایمر لغو شد؛ وضعیت شیر تغییر نکرد."
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Cancel, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("لغو تایمر")
                        }
                    } else {
                        Text("تایمر فعالی ثبت نشده است.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (showSettings) {
                ElevatedCard {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("تنظیمات فرمان‌ها", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = openCommand, onValueChange = { openCommand = it },
                            label = { Text("فرمان باز کردن") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = closeCommand, onValueChange = { closeCommand = it },
                            label = { Text("فرمان بستن") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = stopCommand, onValueChange = { stopCommand = it },
                            label = { Text("فرمان توقف") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(onClick = {
                            saveSettings()
                            message = "تنظیمات ذخیره شد."
                            showSettings = false
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("ذخیره تنظیمات")
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            OutlinedButton(
                                onClick = {
                                    val alarmManager = context.getSystemService(AlarmManager::class.java)
                                    if (alarmManager?.canScheduleExactAlarms() == false) {
                                        exactAlarmLauncher.launch(
                                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                        )
                                    } else {
                                        message = "اجازه آلارم دقیق فعال است."
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تنظیم مجوز آلارم دقیق")
                            }
                        }
                    }
                }
            }

            ElevatedCard {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("وضعیت برنامه", fontWeight = FontWeight.SemiBold)
                        Text(message, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "ارسال پیامک به معنی تأیید بسته‌شدن شیر نیست. برای کاربرد بدون نظارت، حفاظت سخت‌افزاری مستقل ضروری است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Text("RelaySMS 1.0 · اجرای محلی، بدون سرور", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
