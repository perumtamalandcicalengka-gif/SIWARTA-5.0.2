package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DueReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    runCatching { SiwartaNotificationHelper.evaluateAndPostScheduledReminder(context) }
  }
}
