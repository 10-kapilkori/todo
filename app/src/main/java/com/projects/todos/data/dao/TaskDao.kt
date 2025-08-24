package com.projects.todos.data.dao

import androidx.room.*
import com.projects.todos.data.entity.TaskEntity
import com.projects.todos.data.relation.TaskWithTag
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    
    @Transaction
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasksWithTags(): Flow<List<TaskWithTag>>
    
    @Transaction
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getActiveTasksWithTags(): Flow<List<TaskWithTag>>
    
    @Transaction
    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY updatedAt DESC")
    fun getCompletedTasksWithTags(): Flow<List<TaskWithTag>>
    
    @Transaction
    @Query("SELECT * FROM tasks WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteTasksWithTags(): Flow<List<TaskWithTag>>
    
    @Transaction
    @Query("SELECT * FROM tasks WHERE tagId = :tagId ORDER BY createdAt DESC")
    fun getTasksByTagId(tagId: Int): Flow<List<TaskWithTag>>
    
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): TaskEntity?
    
    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskWithTagById(taskId: Int): TaskWithTag?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)
    
    @Update
    suspend fun updateTask(task: TaskEntity)
    
    @Delete
    suspend fun deleteTask(task: TaskEntity)
    
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Int)
    
    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
    
    @Query("UPDATE tasks SET isCompleted = :isCompleted, updatedAt = :timestamp WHERE id = :taskId")
    suspend fun updateTaskCompletion(taskId: Int, isCompleted: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE tasks SET isFavorite = :isFavorite, updatedAt = :timestamp WHERE id = :taskId")
    suspend fun updateTaskFavorite(taskId: Int, isFavorite: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE tasks SET tagId = :newTagId, updatedAt = :timestamp WHERE tagId = :oldTagId")
    suspend fun updateTasksTagId(oldTagId: Int, newTagId: Int, timestamp: Long = System.currentTimeMillis())
}
