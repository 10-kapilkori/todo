package com.projects.todos.data.service

import android.content.Context
import com.google.gson.Gson
import com.projects.todos.R
import com.projects.todos.data.DummyData
import com.projects.todos.data.entity.TagEntity
import com.projects.todos.data.entity.TaskEntity
import com.projects.todos.data.repository.TagRepository
import com.projects.todos.data.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DatabaseInitializationService(
    private val tagRepository: TagRepository,
    private val taskRepository: TaskRepository
) {
    
    suspend fun initializeDatabaseWithDummyData(context: Context) = withContext(Dispatchers.IO) {
        try {
            // Parse dummy data from JSON
            val jsonString = context.resources.openRawResource(R.raw.dummy_data)
                .bufferedReader().use { it.readText() }
            
            val dummyData = Gson().fromJson(jsonString, DummyData::class.java)
            
            // Insert tags first
            val tagEntities = dummyData.tags.map { dummyTag ->
                TagEntity(
                    name = dummyTag.name,
                    colorHex = dummyTag.colorHex,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }
            
            tagRepository.insertTags(tagEntities)
            
            // Get tag IDs for task insertion
            val tagNameToIdMap = mutableMapOf<String, Int>()
            tagEntities.forEach { tag ->
                val insertedTag = tagRepository.getTagByName(tag.name)
                insertedTag?.let { tagNameToIdMap[tag.name] = it.id }
            }
            
            // Insert tasks with proper tag IDs
            val taskEntities = dummyData.tasks.mapNotNull { dummyTask ->
                val tagId = tagNameToIdMap[dummyTask.tagName]
                if (tagId != null) {
                    TaskEntity(
                        title = dummyTask.title,
                        description = dummyTask.description,
                        isCompleted = dummyTask.isCompleted,
                        isFavorite = dummyTask.isFavorite,
                        tagId = tagId,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                } else null
            }
            
            taskRepository.insertTasks(taskEntities)
            
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
