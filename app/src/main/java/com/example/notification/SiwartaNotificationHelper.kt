package com.example.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class ReminderScheduleConfig(
  val enabled: Boolean = true,
  val dueDay: Int = 10,
  val daysBefore: Int = 3,
  val wargaNoRumah: String = "",
  val wargaNama: String = "",
  val unpaidTitle: String = "",
  val unpaidMessage: String = "",
  val hasUnpaidDueSoon: Boolean = false,
  val periodeLabel: String = "",
  val lastAlarmDate: String = "",
)

object SiwartaNotificationHelper {
  const val CHANNEL_ID = "siwarta_payment_reminders"
  private const val CHANNEL_NAME = "Pengingat Pembayaran Iuran"
  private const val CHANNEL_DESC =
    "Notifikasi pengingat otomatis mendekati tanggal jatuh tempo iuran & tagihan air warga."
  private const val PREFS_NAME = "siwarta_reminder_prefs"
  private const val ALARM_REQUEST_CODE = 4010

  private const val KEY_ENABLED = "reminder_enabled"
  private const val KEY_DUE_DAY = "reminder_due_day"
  private const val KEY_DAYS_BEFORE = "reminder_days_before"
  private const val KEY_WARGA_BLOK = "reminder_warga_blok"
  private const val KEY_WARGA_NAMA = "reminder_warga_nama"
  private const val KEY_UNPAID_TITLE = "reminder_unpaid_title"
  private const val KEY_UNPAID_MESSAGE = "reminder_unpaid_message"
  private const val KEY_HAS_UNPAID = "reminder_has_unpaid"
  private const val KEY_PERIODE_LABEL = "reminder_periode_label"
  private const val KEY_LAST_ALARM_DATE = "reminder_last_alarm_date"

  fun ensureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      if (manager != null) {
        val channel =
          NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
            description = CHANNEL_DESC
            enableVibration(true)
          }
        manager.createNotificationChannel(channel)
      }
    }
  }

  fun isNotificationPermissionGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun showPaymentReminderNotification(
    context: Context,
    title: String,
    message: String,
    tagKey: String = "siwarta_due_reminder",
  ): Boolean {
    if (!isNotificationPermissionGranted(context)) return false
    ensureNotificationChannel(context)

    val launchIntent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("siwarta_open_reminder", tagKey)
      }
    val pendingIntent =
      PendingIntent.getActivity(
        context,
        tagKey.hashCode(),
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val safeTitle = title.ifBlank { "Pengingat Pembayaran Iuran SIWARTA" }
    val safeMessage =
      message.ifBlank {
        "Iuran bulanan Anda mendekati tanggal jatuh tempo. Segera lakukan pembayaran di Portal Warga."
      }

    val notification =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification_reminder)
        .setContentTitle(safeTitle)
        .setContentText(safeMessage)
        .setStyle(NotificationCompat.BigTextStyle().bigText(safeMessage))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    val notificationId = (tagKey.hashCode() and 0x7FFFFFFF).coerceAtLeast(1001)
    return runCatching {
        NotificationManagerCompat.from(context).notify(notificationId, notification)
        true
      }
      .getOrDefault(false)
  }

  fun saveReminderConfigAndSchedule(context: Context, config: ReminderScheduleConfig) {
    val clampedDueDay = config.dueDay.coerceIn(1, 28)
    val clampedDaysBefore = config.daysBefore.coerceIn(1, 14)

    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
      putBoolean(KEY_ENABLED, config.enabled)
      putInt(KEY_DUE_DAY, clampedDueDay)
      putInt(KEY_DAYS_BEFORE, clampedDaysBefore)
      putString(KEY_WARGA_BLOK, config.wargaNoRumah)
      putString(KEY_WARGA_NAMA, config.wargaNama)
      putString(KEY_UNPAID_TITLE, config.unpaidTitle)
      putString(KEY_UNPAID_MESSAGE, config.unpaidMessage)
      putBoolean(KEY_HAS_UNPAID, config.hasUnpaidDueSoon)
      putString(KEY_PERIODE_LABEL, config.periodeLabel)
      apply()
    }

    if (config.enabled && config.hasUnpaidDueSoon) {
      scheduleDailyInexactReminder(context)
    } else {
      cancelDailyReminder(context)
    }
  }

  fun getSavedReminderConfig(context: Context): ReminderScheduleConfig {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return ReminderScheduleConfig(
      enabled = prefs.getBoolean(KEY_ENABLED, true),
      dueDay = prefs.getInt(KEY_DUE_DAY, 10).coerceIn(1, 28),
      daysBefore = prefs.getInt(KEY_DAYS_BEFORE, 3).coerceIn(1, 14),
      wargaNoRumah = prefs.getString(KEY_WARGA_BLOK, "").orEmpty(),
      wargaNama = prefs.getString(KEY_WARGA_NAMA, "").orEmpty(),
      unpaidTitle = prefs.getString(KEY_UNPAID_TITLE, "").orEmpty(),
      unpaidMessage = prefs.getString(KEY_UNPAID_MESSAGE, "").orEmpty(),
      hasUnpaidDueSoon = prefs.getBoolean(KEY_HAS_UNPAID, false),
      periodeLabel = prefs.getString(KEY_PERIODE_LABEL, "").orEmpty(),
      lastAlarmDate = prefs.getString(KEY_LAST_ALARM_DATE, "").orEmpty(),
    )
  }

  fun scheduleDailyInexactReminder(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val pendingIntent = buildReceiverPendingIntent(context)

    val jakartaTz = TimeZone.getTimeZone("Asia/Jakarta")
    val nextTrigger =
      Calendar.getInstance(jakartaTz).apply {
        set(Calendar.HOUR_OF_DAY, 8)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) {
          add(Calendar.DAY_OF_YEAR, 1)
        }
      }

    runCatching {
      alarmManager.setInexactRepeating(
        AlarmManager.RTC_WAKEUP,
        nextTrigger.timeInMillis,
        AlarmManager.INTERVAL_DAY,
        pendingIntent,
      )
    }
  }

  fun cancelDailyReminder(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val pendingIntent = buildReceiverPendingIntent(context)
    runCatching { alarmManager.cancel(pendingIntent) }
  }

  fun evaluateAndPostScheduledReminder(context: Context): Boolean {
    val cfg = getSavedReminderConfig(context)
    if (!cfg.enabled || !cfg.hasUnpaidDueSoon) return false

    val jakartaTz = TimeZone.getTimeZone("Asia/Jakarta")
    val cal = Calendar.getInstance(jakartaTz)
    val currentDay = cal.get(Calendar.DAY_OF_MONTH)
    val todayIso =
      String.format(
        Locale.US,
        "%04d-%02d-%02d",
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        currentDay,
      )

    if (cfg.lastAlarmDate == todayIso) return false

    val daysLeft = cfg.dueDay - currentDay
    val withinReminderWindow = daysLeft <= cfg.daysBefore
    if (!withinReminderWindow) return false

    val title =
      cfg.unpaidTitle.ifBlank {
        when {
          daysLeft > 0 -> "Pengingat Iuran (H-$daysLeft Jatuh Tempo)"
          daysLeft == 0 -> "Hari Ini Jatuh Tempo Iuran"
          else -> "Tagihan Iuran Melewati Jatuh Tempo"
        }
      }
    val body =
      cfg.unpaidMessage.ifBlank {
        val blokLabel = if (cfg.wargaNoRumah.isNotBlank()) "Blok ${cfg.wargaNoRumah}" else "Anda"
        "Tagihan iuran $blokLabel untuk ${cfg.periodeLabel.ifBlank { "bulan ini" }} belum lunas. Segera lakukan konfirmasi pembayaran."
      }

    val posted =
      showPaymentReminderNotification(
        context = context,
        title = title,
        message = body,
        tagKey = "daily_${cfg.wargaNoRumah}_$todayIso",
      )

    if (posted) {
      context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_LAST_ALARM_DATE, todayIso)
        .apply()
    }
    return posted
  }

  private fun buildReceiverPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, DueReminderReceiver::class.java)
    return PendingIntent.getBroadcast(
      context,
      ALARM_REQUEST_CODE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }
}
