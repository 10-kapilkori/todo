package com.projects.todos.data.dao

import androidx.room.*
import com.projects.todos.data.entity.TemplateEntity
import com.projects.todos.data.relation.TemplateWithTag
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    
    @Transaction
    @Query("SELECT * FROM templates ORDER BY createdAt DESC")
    fun getAllTemplatesWithTag(): Flow<List<TemplateWithTag>>
    
    @Transaction
    @Query("SELECT * FROM templates WHERE tagId = :tagId ORDER BY createdAt DESC")
    fun getTemplatesByTagId(tagId: Int): Flow<List<TemplateWithTag>>
    
    @Query("SELECT * FROM templates WHERE id = :templateId")
    suspend fun getTemplateById(templateId: Int): TemplateEntity?
    
    @Transaction
    @Query("SELECT * FROM templates WHERE id = :templateId")
    suspend fun getTemplateWithTagById(templateId: Int): TemplateWithTag?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TemplateEntity): Long
    
    @Update
    suspend fun updateTemplate(template: TemplateEntity)
    
    @Delete
    suspend fun deleteTemplate(template: TemplateEntity)
    
    @Query("DELETE FROM templates WHERE id = :templateId")
    suspend fun deleteTemplateById(templateId: Int)
    
    @Query("DELETE FROM templates")
    suspend fun deleteAllTemplates()
}
