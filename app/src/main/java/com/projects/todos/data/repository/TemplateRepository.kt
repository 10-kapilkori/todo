package com.projects.todos.data.repository

import com.projects.todos.data.dao.TemplateDao
import com.projects.todos.data.entity.TemplateEntity
import com.projects.todos.data.relation.TemplateWithTag
import kotlinx.coroutines.flow.Flow

class TemplateRepository(private val templateDao: TemplateDao) {
    
    fun getAllTemplatesWithTag(): Flow<List<TemplateWithTag>> = templateDao.getAllTemplatesWithTag()
    
    fun getTemplatesByTagId(tagId: Int): Flow<List<TemplateWithTag>> = templateDao.getTemplatesByTagId(tagId)
    
    suspend fun getTemplateById(templateId: Int): TemplateEntity? = templateDao.getTemplateById(templateId)
    
    suspend fun getTemplateWithTagById(templateId: Int): TemplateWithTag? = templateDao.getTemplateWithTagById(templateId)
    
    suspend fun insertTemplate(template: TemplateEntity): Long = templateDao.insertTemplate(template)
    
    suspend fun updateTemplate(template: TemplateEntity) = templateDao.updateTemplate(template)
    
    suspend fun deleteTemplate(template: TemplateEntity) = templateDao.deleteTemplate(template)
    
    suspend fun deleteTemplateById(templateId: Int) = templateDao.deleteTemplateById(templateId)
    
    suspend fun deleteAllTemplates() = templateDao.deleteAllTemplates()
}
