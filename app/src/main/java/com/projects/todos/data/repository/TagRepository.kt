package com.projects.todos.data.repository

import com.projects.todos.data.dao.TagDao
import com.projects.todos.data.entity.TagEntity
import com.projects.todos.utils.AppLogger
import kotlinx.coroutines.flow.Flow

class TagRepository(private val tagDao: TagDao) {

    fun getAllTags(): Flow<List<TagEntity>> {
        AppLogger.methodEntry("TagRepository", "getAllTags")
        return tagDao.getAllTags().also {
            AppLogger.methodExit("TagRepository", "getAllTags", "Flow<List<TagEntity>>")
        }
    }

    suspend fun getTagById(tagId: Int): TagEntity? {
        AppLogger.methodEntry("TagRepository", "getTagById", "tagId" to tagId)
        return try {
            val result = tagDao.getTagById(tagId)
            AppLogger.dbOperation("TagRepository", "SELECT", "tags", tagId)
            AppLogger.methodExit("TagRepository", "getTagById", result)
            result
        } catch (e: Exception) {
            AppLogger.error("TagRepository", "getTagById", e, "tagId: $tagId")
            AppLogger.methodExit("TagRepository", "getTagById", null)
            null
        }
    }

    suspend fun getTagByName(tagName: String): TagEntity? {
        AppLogger.methodEntry("TagRepository", "getTagByName", "tagName" to tagName)
        return try {
            val result = tagDao.getTagByName(tagName)
            AppLogger.dbOperation("TagRepository", "SELECT", "tags", "name: $tagName")
            AppLogger.methodExit("TagRepository", "getTagByName", result)
            result
        } catch (e: Exception) {
            AppLogger.error("TagRepository", "getTagByName", e, "tagName: $tagName")
            AppLogger.methodExit("TagRepository", "getTagByName", null)
            null
        }
    }

    suspend fun insertTag(tag: TagEntity): Long {
        AppLogger.methodEntry("TagRepository", "insertTag", 
            "name" to tag.name, 
            "colorHex" to tag.colorHex
        )
        return try {
            val result = tagDao.insertTag(tag)
            AppLogger.dbOperation("TagRepository", "INSERT", "tags", result)
            AppLogger.methodExit("TagRepository", "insertTag", result)
            result
        } catch (e: Exception) {
            AppLogger.error("TagRepository", "insertTag", e, "tag: ${tag.name}")
            AppLogger.methodExit("TagRepository", "insertTag", -1L)
            throw e
        }
    }

    suspend fun insertTags(tags: List<TagEntity>) {
        AppLogger.methodEntry("TagRepository", "insertTags", "count" to tags.size)
        try {
            tagDao.insertTags(tags)
            AppLogger.dbOperation("TagRepository", "INSERT MULTIPLE", "tags", tags.size)
            AppLogger.methodExit("TagRepository", "insertTags")
        } catch (e: Exception) {
            AppLogger.error("TagRepository", "insertTags", e, "tags count: ${tags.size}")
            AppLogger.methodExit("TagRepository", "insertTags")
            throw e
        }
    }

    suspend fun updateTag(tag: TagEntity) {
        AppLogger.methodEntry("TagRepository", "updateTag", 
            "id" to tag.id, 
            "name" to tag.name
        )
        try {
            tagDao.updateTag(tag)
            AppLogger.dbOperation("TagRepository", "UPDATE", "tags", tag.id)
            AppLogger.methodExit("TagRepository", "updateTag")
        } catch (e: Exception) {
            AppLogger.error("TagRepository", "updateTag", e, "tagId: ${tag.id}")
            AppLogger.methodExit("TagRepository", "updateTag")
            throw e
        }
    }

    suspend fun deleteTag(tag: TagEntity) {
        AppLogger.methodEntry("TagRepository", "deleteTag", 
            "id" to tag.id, 
            "name" to tag.name
        )
        try {
            tagDao.deleteTag(tag)
            AppLogger.dbOperation("TagRepository", "DELETE", "tags", tag.id)
            AppLogger.methodExit("TagRepository", "deleteTag")
        } catch (e: Exception) {
            AppLogger.error("TagRepository", "deleteTag", e, "tagId: ${tag.id}")
            AppLogger.methodExit("TagRepository", "deleteTag")
            throw e
        }
    }

    suspend fun deleteTagById(tagId: Int) {
        AppLogger.methodEntry("TagRepository", "deleteTagById", "tagId" to tagId)
        try {
            tagDao.deleteTagById(tagId)
            AppLogger.dbOperation("TagRepository", "DELETE", "tags", tagId)
            AppLogger.methodExit("TagRepository", "deleteTagById")
        } catch (e: Exception) {
            AppLogger.error("TagRepository", "deleteTagById", e, "tagId: $tagId")
            AppLogger.methodExit("TagRepository", "deleteTagById")
            throw e
        }
    }

    suspend fun deleteAllTags() {
        AppLogger.methodEntry("TagRepository", "deleteAllTags")
        try {
            tagDao.deleteAllTags()
            AppLogger.dbOperation("TagRepository", "DELETE ALL", "tags", null)
            AppLogger.methodExit("TagRepository", "deleteAllTags")
        } catch (e: Exception) {
            AppLogger.error("TagRepository", "deleteAllTags", e)
            AppLogger.methodExit("TagRepository", "deleteAllTags")
            throw e
        }
    }
    
    suspend fun getTaskCountForTag(tagId: Int): Int {
        AppLogger.methodEntry("TagRepository", "getTaskCountForTag", "tagId" to tagId)
        return try {
            val result = tagDao.getTaskCountForTag(tagId)
            AppLogger.dbOperation("TagRepository", "SELECT COUNT", "tasks for tag", tagId)
            AppLogger.methodExit("TagRepository", "getTaskCountForTag", result)
            result
        } catch (e: Exception) {
            AppLogger.error("TagRepository", "getTaskCountForTag", e, "tagId: $tagId")
            AppLogger.methodExit("TagRepository", "getTaskCountForTag", 0)
            0
        }
    }
    
    suspend fun getTagCount(): Int {
        AppLogger.methodEntry("TagRepository", "getTagCount")
        return try {
            val result = tagDao.getTagCount()
            AppLogger.dbOperation("TagRepository", "SELECT COUNT", "tags", null)
            AppLogger.methodExit("TagRepository", "getTagCount", result)
            result
        } catch (e: Exception) {
            AppLogger.error("TagRepository", "getTagCount", e)
            AppLogger.methodExit("TagRepository", "getTagCount", 0)
            0
        }
    }
    
    suspend fun getAllTagsSync(): List<TagEntity> {
        AppLogger.methodEntry("TagRepository", "getAllTagsSync")
        return try {
            val result = tagDao.getAllTagsSync()
            AppLogger.dbOperation("TagRepository", "SELECT ALL", "tags", result.size)
            AppLogger.d("TagRepository", "Retrieved tags: ${result.map { "${it.name} (ID: ${it.id})" }.joinToString(", ")}")
            AppLogger.methodExit("TagRepository", "getAllTagsSync", result.size)
            result
        } catch (e: Exception) {
            AppLogger.error("TagRepository", "getAllTagsSync", e)
            AppLogger.methodExit("TagRepository", "getAllTagsSync", emptyList<TagEntity>())
            emptyList()
        }
    }
}
