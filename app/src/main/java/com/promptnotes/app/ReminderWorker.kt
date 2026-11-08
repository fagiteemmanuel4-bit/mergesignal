package com.promptnotes.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE).orEmpty().ifBlank { "A note is waiting" }
        val body = inputData.getString(KEY_BODY).orEmpty().ifBlank { "Open Prompt Notes to continue where you left off." }
        createChannel(applicationContext)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return Result.success()
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_prompt_notes)
            .setContentTitle(title)
            .setContentText(body.take(120))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(inputData.getLong(KEY_ID, 1L).toInt(), notification)
        return Result.success()
    }

    companion object {
        private const val CHANNEL_ID = "note_reminders"
        private const val KEY_ID = "note_id"
        private const val KEY_TITLE = "note_title"
        private const val KEY_BODY = "note_body"

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Note reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Reminders you schedule for your notes"
                })
            }
        }

        fun schedule(context: Context, note: NoteEntity, atMillis: Long) {
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay((atMillis - System.currentTimeMillis()).coerceAtLeast(1_000L), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(KEY_ID to note.id, KEY_TITLE to note.title, KEY_BODY to note.content))
                .addTag("note-reminder-${note.id}")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("note-reminder-${note.id}", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
