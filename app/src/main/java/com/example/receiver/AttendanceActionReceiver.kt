package com.example.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.model.AttendanceRecord
import com.example.data.model.NotificationLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AttendanceActionReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        
        val courseId = intent.getIntExtra("courseId", -1)
        val periodId = intent.getIntExtra("periodId", -1)
        val date = intent.getStringExtra("date") ?: ""
        val status = intent.getStringExtra("status") ?: ""
        val notificationId = intent.getIntExtra("notificationId", -1)
        val periods = intent.getIntExtra("periods", 1)
        val units = intent.getIntExtra("units", 1)

        Log.d("AttendanceReceiver", "Received action: Course: $courseId, Status: $status, Date: $date")

        if (courseId == -1 || date.isEmpty() || status.isEmpty()) {
            pendingResult.finish()
            return
        }

        // Dismiss the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationId != -1) {
            notificationManager.cancel(notificationId)
        }

        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                
                // 1. Delete any existing record for this slot/date to avoid dupes
                db.attendanceDao().deleteRecordBySlotAndDate(periodId, date)

                // 2. Insert the new record
                val record = AttendanceRecord(
                    courseId = courseId,
                    date = date,
                    status = status,
                    periodIndex = periodId,
                    slotId = periodId,
                    periods = periods,
                    units = units,
                    isExtra = false
                )
                db.attendanceDao().insertRecord(record)

                // 3. Update notification log status to RESPONDED_X
                val logId = "${date}_$periodId"
                val existingLog = db.notificationLogDao().getLogById(logId)
                if (existingLog != null) {
                    val updatedLog = existingLog.copy(
                        status = "RESPONDED_$status"
                    )
                    db.notificationLogDao().insertLog(updatedLog)
                }

                Log.d("AttendanceReceiver", "Recorded attendance successfully in background!")
            } catch (e: Exception) {
                Log.e("AttendanceReceiver", "Error saving background attendance", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
