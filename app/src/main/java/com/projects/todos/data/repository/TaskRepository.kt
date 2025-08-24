package com.projects.todos.data.repository

import com.projects.todos.data.dao.TaskDao
import com.projects.todos.data.entity.TaskEntity
import com.projects.todos.data.relation.TaskWithTag
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao
) {
    
    fun getAllTasksWithTags(): Flow<List<TaskWithTag>> = taskDao.getAllTasksWithTags()
    
    fun getActiveTasksWithTags(): Flow<List<TaskWithTag>> = taskDao.getActiveTasksWithTags()
    
    fun getCompletedTasksWithTags(): Flow<List<TaskWithTag>> = taskDao.getCompletedTasksWithTags()
    
    fun getFavoriteTasksWithTags(): Flow<List<TaskWithTag>> = taskDao.getFavoriteTasksWithTags()
    
    fun getTasksByTagId(tagId: Int): Flow<List<TaskWithTag>> = taskDao.getTasksByTagId(tagId)
    
    suspend fun getTaskById(taskId: Int): TaskEntity? = taskDao.getTaskById(taskId)
    
    suspend fun getTaskWithTagById(taskId: Int): TaskWithTag? = taskDao.getTaskWithTagById(taskId)
    
    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)
    
    suspend fun insertTasks(tasks: List<TaskEntity>) = taskDao.insertTasks(tasks)
    
    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)
    
    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)
    
    suspend fun deleteTaskById(taskId: Int) = taskDao.deleteTaskById(taskId)
    
    suspend fun deleteAllTasks() = taskDao.deleteAllTasks()
    
    suspend fun updateTaskCompletion(taskId: Int, isCompleted: Boolean) = 
        taskDao.updateTaskCompletion(taskId, isCompleted)
    
    suspend fun updateTaskFavorite(taskId: Int, isFavorite: Boolean) = 
        taskDao.updateTaskFavorite(taskId, isFavorite)
}
