package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.database.AppDatabase
import com.example.data.model.NotificationLog
import com.example.receiver.AttendanceActionReceiver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PeriodNotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val courseId = inputData.getInt("courseId", -1)
        val courseName = inputData.getString("courseName") ?: "Class"
        val periodId = inputData.getInt("periodId", -1)
        val periods = inputData.getInt("periods", 1)
        val units = inputData.getInt("units", 1)
        
        Log.d("NotificationWorker", "Triggering worker for Course: $courseName ($courseId)")

        if (courseId == -1 || periodId == -1) {
            return Result.failure()
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = dateFormat.format(Date())

        val db = AppDatabase.getDatabase(context)

        // Check if notifications are enabled
        val nEnabled = db.settingDao().getSetting("notifications_enabled")?.value ?: "true"
        if (nEnabled != "true") {
            Log.d("NotificationWorker", "Notifications disabled in settings, skipping")
            return Result.success()
        }

        createNotificationChannel()

        val notificationId = (courseId * 100) + periodId
        val logId = "${dateStr}_$periodId"

        // Create Intents for action buttons
        // 1. Present Action Intent
        val presentIntent = Intent(context, AttendanceActionReceiver::class.java).apply {
            putExtra("courseId", courseId)
            putExtra("periodId", periodId)
            putExtra("date", dateStr)
            putExtra("status", "PRESENT")
            putExtra("periods", periods)
            putExtra("units", units)
            putExtra("notificationId", notificationId)
        }
        val presentPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            presentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Absent Action Intent
        val absentIntent = Intent(context, AttendanceActionReceiver::class.java).apply {
            putExtra("courseId", courseId)
            putExtra("periodId", periodId)
            putExtra("date", dateStr)
            putExtra("status", "ABSENT")
            putExtra("periods", periods)
            putExtra("units", units)
            putExtra("notificationId", notificationId)
        }
        val absentPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            absentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Cancelled Action Intent
        val cancelledIntent = Intent(context, AttendanceActionReceiver::class.java).apply {
            putExtra("courseId", courseId)
            putExtra("periodId", periodId)
            putExtra("date", dateStr)
            putExtra("status", "CANCELLED")
            putExtra("periods", periods)
            putExtra("units", units)
            putExtra("notificationId", notificationId)
        }
        val cancelledPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 3,
            cancelledIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Content click intent (opens app)
        val contentIntent = Intent(context, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Primary Builder
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Safe fallback icon
            .setContentTitle(courseName)
            .setContentText("Mark your attendance")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Present", presentPendingIntent)
            .addAction(0, "Absent", absentPendingIntent)
            .addAction(0, "Cancelled", cancelledPendingIntent)

        try {
            // Write to log DB first
            db.notificationLogDao().insertLog(
                NotificationLog(
                    id = logId,
                    date = dateStr,
                    periodId = periodId,
                    courseId = courseId,
                    status = "SENT"
                )
            )

            // Fire notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, builder.build())
            Log.d("NotificationWorker", "Notification triggered successfully!")
        } catch (e: SecurityException) {
            Log.e("NotificationWorker", "Permission block for notification firing", e)
        }

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Attendance Reminders"
            val descriptionText = "Fires at the end of classes from your timetable"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "period_ends_notif_channel"
    }
}
