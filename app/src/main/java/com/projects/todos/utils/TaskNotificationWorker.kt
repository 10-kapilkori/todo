package com.projects.todos.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.projects.todos.R

class TaskNotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_TASK_TITLE = "task_title"
        const val KEY_TASK_DESCRIPTION = "task_description"
        const val CHANNEL_ID = "task_reminders"
        const val CHANNEL_NAME = "Task Reminders"
    }

    override fun doWork(): Result {
        val taskId = inputData.getInt(KEY_TASK_ID, -1)
        val taskTitle = inputData.getString(KEY_TASK_TITLE) ?: "Task"
        val taskDescription = inputData.getString(KEY_TASK_DESCRIPTION) ?: ""

        AppLogger.methodEntry("TaskNotificationWorker", "doWork", 
            "taskId" to taskId, 
            "taskTitle" to taskTitle
        )

        try {
            createNotificationChannel()
            showNotification(taskId, taskTitle, taskDescription)
            
            AppLogger.notification("TaskNotificationWorker", context.getString(R.string.notification_displayed_log, taskId))
            AppLogger.methodExit("TaskNotificationWorker", "doWork", "SUCCESS")
            return Result.success()
        } catch (e: Exception) {
            AppLogger.error("TaskNotificationWorker", "doWork", e, "taskId: $taskId")
            AppLogger.methodExit("TaskNotificationWorker", "doWork", "FAILURE")
            return Result.failure()
        }
    }

    private fun createNotificationChannel() {
        AppLogger.methodEntry("TaskNotificationWorker", "createNotificationChannel")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.task_reminders),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notifications_task_reminders)
                    enableLights(true)
                    enableVibration(true)
                }

                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
                
                AppLogger.d("TaskNotificationWorker", context.getString(R.string.notification_channel_created))
                AppLogger.methodExit("TaskNotificationWorker", "createNotificationChannel")
            } catch (e: Exception) {
                AppLogger.error("TaskNotificationWorker", "createNotificationChannel", e)
                AppLogger.methodExit("TaskNotificationWorker", "createNotificationChannel")
                throw e
            }
        } else {
            AppLogger.d("TaskNotificationWorker", context.getString(R.string.notification_channels_not_supported))
            AppLogger.methodExit("TaskNotificationWorker", "createNotificationChannel")
        }
    }

    private fun showNotification(taskId: Int, taskTitle: String, taskDescription: String) {
        AppLogger.methodEntry("TaskNotificationWorker", "showNotification", 
            "taskId" to taskId, 
            "taskTitle" to taskTitle
        )
        
        try {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_tasks)
                .setContentTitle(context.getString(R.string.task_due_format, taskTitle))
                .setContentText(if (taskDescription.isNotEmpty()) taskDescription else context.getString(R.string.task_due_now))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build()

            notificationManager.notify(taskId, notification)
            
            AppLogger.d("TaskNotificationWorker", context.getString(R.string.notification_displayed, taskId))
            AppLogger.methodExit("TaskNotificationWorker", "showNotification")
        } catch (e: Exception) {
            AppLogger.error("TaskNotificationWorker", "showNotification", e, "taskId: $taskId")
            AppLogger.methodExit("TaskNotificationWorker", "showNotification")
            throw e
        }
    }
}
