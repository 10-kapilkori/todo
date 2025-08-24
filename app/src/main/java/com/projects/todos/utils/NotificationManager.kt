package com.projects.todos.utils

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.projects.todos.R
import com.projects.todos.data.entity.TaskEntity
import java.util.concurrent.TimeUnit

object NotificationManager {
    
    fun scheduleTaskNotification(context: Context, task: TaskEntity) {
        AppLogger.methodEntry("NotificationManager", "scheduleTaskNotification", 
            "taskId" to task.id, 
            "title" to task.title,
            "dueDateTime" to task.dueDateTime
        )
        
        task.dueDateTime?.let { dueDateTime ->
            // Don't schedule if the time has already passed
            if (dueDateTime <= System.currentTimeMillis()) {
                AppLogger.w("NotificationManager", context.getString(R.string.cannot_schedule_past, dueDateTime))
                AppLogger.methodExit("NotificationManager", "scheduleTaskNotification")
                return
            }
            
            val delay = dueDateTime - System.currentTimeMillis()
            AppLogger.d("NotificationManager", context.getString(R.string.scheduling_delay, delay))
            
            val inputData = Data.Builder()
                .putInt(TaskNotificationWorker.KEY_TASK_ID, task.id)
                .putString(TaskNotificationWorker.KEY_TASK_TITLE, task.title)
                .putString(TaskNotificationWorker.KEY_TASK_DESCRIPTION, task.description)
                .build()
            
            val notificationWork = OneTimeWorkRequestBuilder<TaskNotificationWorker>()
                .setInputData(inputData)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("task_${task.id}")
                .build()
            
            WorkManager.getInstance(context).enqueue(notificationWork)
            AppLogger.notification("NotificationManager", context.getString(R.string.notification_scheduled_log, task.id))
            AppLogger.methodExit("NotificationManager", "scheduleTaskNotification")
        } ?: run {
            AppLogger.d("NotificationManager", context.getString(R.string.no_due_date_skip, task.id))
            AppLogger.methodExit("NotificationManager", "scheduleTaskNotification")
        }
    }
    
    fun cancelTaskNotification(context: Context, taskId: Int) {
        AppLogger.methodEntry("NotificationManager", "cancelTaskNotification", "taskId" to taskId)
        try {
            WorkManager.getInstance(context).cancelAllWorkByTag("task_$taskId")
            AppLogger.notification("NotificationManager", context.getString(R.string.notification_cancelled_log, taskId))
            AppLogger.methodExit("NotificationManager", "cancelTaskNotification")
        } catch (e: Exception) {
            AppLogger.error("NotificationManager", "cancelTaskNotification", e, "taskId: $taskId")
            AppLogger.methodExit("NotificationManager", "cancelTaskNotification")
        }
    }
    
    fun rescheduleTaskNotification(context: Context, task: TaskEntity) {
        AppLogger.methodEntry("NotificationManager", "rescheduleTaskNotification", 
            "taskId" to task.id,
            "dueDateTime" to task.dueDateTime
        )
        
        // Cancel existing notification first
        cancelTaskNotification(context, task.id)
        
        // Schedule new notification
        scheduleTaskNotification(context, task)
        
        AppLogger.notification("NotificationManager", context.getString(R.string.notification_rescheduled_log, task.id))
        AppLogger.methodExit("NotificationManager", "rescheduleTaskNotification")
    }
}
