package com.example.minlishapp.core.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.minlishapp.MainActivity

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sharedPrefs = context.getSharedPreferences("minlish_prefs", Context.MODE_PRIVATE)
        val isEnabled = sharedPrefs.getBoolean("daily_reminder_enabled", true)
        
        if (isEnabled) {
            showNotification(context)
            // Reschedule for the next day
            val dailyReminderTime = sharedPrefs.getString("daily_reminder_time", "09:00") ?: "09:00"
            ReminderManager.scheduleDailyReminder(context, dailyReminderTime)
        }
    }

    private fun showNotification(context: Context) {
        val channelId = "minlish_study_reminders"
        val notificationId = 1001

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Nhắc nhở học tập hàng ngày",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kênh thông báo nhắc nhở duy trì chuỗi Streak học từ vựng MinLish."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Đã đến giờ học MinLish rồi! 🔥")
            .setContentText("Dành ra 5 phút mỗi ngày để duy trì chuỗi Streak và chinh phục từ vựng mới nào!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
