package com.projects.todos.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.projects.todos.data.dao.TagDao
import com.projects.todos.data.dao.TaskDao
import com.projects.todos.data.dao.TemplateDao
import com.projects.todos.data.entity.TagEntity
import com.projects.todos.data.entity.TaskEntity
import com.projects.todos.data.entity.TemplateEntity

@Database(
    entities = [TagEntity::class, TaskEntity::class, TemplateEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TodoDatabase : RoomDatabase() {
    
    abstract fun tagDao(): TagDao
    abstract fun taskDao(): TaskDao
    abstract fun templateDao(): TemplateDao
    
    companion object {
        @Volatile
        private var INSTANCE: TodoDatabase? = null
        
        fun getDatabase(context: Context): TodoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TodoDatabase::class.java,
                    "todo_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
