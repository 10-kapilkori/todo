package com.projects.todos.data.repository

import com.projects.todos.data.dao.TaskDao
import com.projects.todos.data.entity.TaskEntity
import com.projects.todos.data.relation.TaskWithTag
import com.projects.todos.utils.AppLogger
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    fun getAllTasksWithTags(): Flow<List<TaskWithTag>> {
        AppLogger.methodEntry("TaskRepository", "getAllTasksWithTags")
        return taskDao.getAllTasksWithTags().also {
            AppLogger.methodExit("TaskRepository", "getAllTasksWithTags", "Flow<List<TaskWithTag>>")
        }
    }

    fun getActiveTasksWithTags(): Flow<List<TaskWithTag>> = taskDao.getActiveTasksWithTags()
    
    fun getCompletedTasksWithTags(): Flow<List<TaskWithTag>> = taskDao.getCompletedTasksWithTags()
    
    fun getFavoriteTasksWithTags(): Flow<List<TaskWithTag>> {
        AppLogger.methodEntry("TaskRepository", "getFavoriteTasksWithTags")
        return taskDao.getFavoriteTasksWithTags().also {
            AppLogger.methodExit("TaskRepository", "getFavoriteTasksWithTags", "Flow<List<TaskWithTag>>")
        }
    }
    
    fun getTasksByTagId(tagId: Int): Flow<List<TaskWithTag>> = taskDao.getTasksByTagId(tagId)
    
    suspend fun getTaskById(taskId: Int): TaskEntity? = taskDao.getTaskById(taskId)
    
    suspend fun getTaskWithTagById(taskId: Int): TaskWithTag? {
        AppLogger.methodEntry("TaskRepository", "getTaskWithTagById", "taskId" to taskId)
        return try {
            val result = taskDao.getTaskWithTagById(taskId)
            AppLogger.dbOperation("TaskRepository", "SELECT", "tasks", taskId)
            AppLogger.methodExit("TaskRepository", "getTaskWithTagById", result)
            result
        } catch (e: Exception) {
            AppLogger.error("TaskRepository", "getTaskWithTagById", e, "taskId: $taskId")
            AppLogger.methodExit("TaskRepository", "getTaskWithTagById", null)
            null
        }
    }
    
    suspend fun insertTask(task: TaskEntity): Long {
        AppLogger.methodEntry("TaskRepository", "insertTask", 
            "title" to task.title, 
            "tagId" to task.tagId,
            "dueDateTime" to task.dueDateTime
        )
        return try {
            val result = taskDao.insertTask(task)
            AppLogger.dbOperation("TaskRepository", "INSERT", "tasks", result)
            AppLogger.methodExit("TaskRepository", "insertTask", result)
            result
        } catch (e: Exception) {
            AppLogger.error("TaskRepository", "insertTask", e, "task: ${task.title}")
            AppLogger.methodExit("TaskRepository", "insertTask", -1L)
            throw e
        }
    }
    
    suspend fun insertTasks(tasks: List<TaskEntity>) {
        AppLogger.methodEntry("TaskRepository", "insertTasks", "count" to tasks.size)
        try {
            taskDao.insertTasks(tasks)
            AppLogger.dbOperation("TaskRepository", "INSERT MULTIPLE", "tasks", tasks.size)
            AppLogger.methodExit("TaskRepository", "insertTasks")
        } catch (e: Exception) {
            AppLogger.error("TaskRepository", "insertTasks", e, "tasks count: ${tasks.size}")
            AppLogger.methodExit("TaskRepository", "insertTasks")
            throw e
        }
    }
    
    suspend fun updateTask(task: TaskEntity) {
        AppLogger.methodEntry("TaskRepository", "updateTask", 
            "id" to task.id, 
            "title" to task.title,
            "dueDateTime" to task.dueDateTime
        )
        try {
            taskDao.updateTask(task)
            AppLogger.dbOperation("TaskRepository", "UPDATE", "tasks", task.id)
            AppLogger.methodExit("TaskRepository", "updateTask")
        } catch (e: Exception) {
            AppLogger.error("TaskRepository", "updateTask", e, "taskId: ${task.id}")
            AppLogger.methodExit("TaskRepository", "updateTask")
            throw e
        }
    }
    
    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)
    
    suspend fun deleteTaskById(taskId: Int) {
        AppLogger.methodEntry("TaskRepository", "deleteTaskById", "taskId" to taskId)
        try {
            taskDao.deleteTaskById(taskId)
            AppLogger.dbOperation("TaskRepository", "DELETE", "tasks", taskId)
            AppLogger.methodExit("TaskRepository", "deleteTaskById")
        } catch (e: Exception) {
            AppLogger.error("TaskRepository", "deleteTaskById", e, "taskId: $taskId")
            AppLogger.methodExit("TaskRepository", "deleteTaskById")
            throw e
        }
    }
    
    suspend fun deleteAllTasks() = taskDao.deleteAllTasks()
    
    suspend fun updateTaskCompletion(taskId: Int, isCompleted: Boolean) {
        AppLogger.methodEntry("TaskRepository", "updateTaskCompletion", 
            "taskId" to taskId, 
            "isCompleted" to isCompleted
        )
        try {
            taskDao.updateTaskCompletion(taskId, isCompleted)
            AppLogger.dbOperation("TaskRepository", "UPDATE completion", "tasks", taskId)
            AppLogger.methodExit("TaskRepository", "updateTaskCompletion")
        } catch (e: Exception) {
            AppLogger.error("TaskRepository", "updateTaskCompletion", e, "taskId: $taskId, isCompleted: $isCompleted")
            AppLogger.methodExit("TaskRepository", "updateTaskCompletion")
            throw e
        }
    }
    
    suspend fun updateTaskFavorite(taskId: Int, isFavorite: Boolean) {
        AppLogger.methodEntry("TaskRepository", "updateTaskFavorite", 
            "taskId" to taskId, 
            "isFavorite" to isFavorite
        )
        try {
            taskDao.updateTaskFavorite(taskId, isFavorite)
            AppLogger.dbOperation("TaskRepository", "UPDATE favorite", "tasks", taskId)
            AppLogger.methodExit("TaskRepository", "updateTaskFavorite")
        } catch (e: Exception) {
            AppLogger.error("TaskRepository", "updateTaskFavorite", e, "taskId: $taskId, isFavorite: $isFavorite")
            AppLogger.methodExit("TaskRepository", "updateTaskFavorite")
            throw e
        }
    }
    
    suspend fun updateTasksTagId(oldTagId: Int, newTagId: Int) {
        AppLogger.methodEntry("TaskRepository", "updateTasksTagId", 
            "oldTagId" to oldTagId, 
            "newTagId" to newTagId
        )
        try {
            taskDao.updateTasksTagId(oldTagId, newTagId)
            AppLogger.dbOperation("TaskRepository", "UPDATE tagId", "tasks", "oldTagId: $oldTagId, newTagId: $newTagId")
            AppLogger.methodExit("TaskRepository", "updateTasksTagId")
        } catch (e: Exception) {
            AppLogger.error("TaskRepository", "updateTasksTagId", e, "oldTagId: $oldTagId, newTagId: $newTagId")
            AppLogger.methodExit("TaskRepository", "updateTasksTagId")
            throw e
        }
    }
}
