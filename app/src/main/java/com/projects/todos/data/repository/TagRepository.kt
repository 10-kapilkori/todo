package com.projects.todos.data.repository

import com.projects.todos.data.dao.TagDao
import com.projects.todos.data.entity.TagEntity
import kotlinx.coroutines.flow.Flow

class TagRepository(
    private val tagDao: TagDao
) {
    
    fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()
    
    suspend fun getTagById(tagId: Int): TagEntity? = tagDao.getTagById(tagId)
    
    suspend fun getTagByName(tagName: String): TagEntity? = tagDao.getTagByName(tagName)
    
    suspend fun insertTag(tag: TagEntity): Long = tagDao.insertTag(tag)
    
    suspend fun insertTags(tags: List<TagEntity>) = tagDao.insertTags(tags)
    
    suspend fun updateTag(tag: TagEntity) = tagDao.updateTag(tag)
    
    suspend fun deleteTag(tag: TagEntity) = tagDao.deleteTag(tag)
    
    suspend fun deleteAllTags() = tagDao.deleteAllTags()
}
