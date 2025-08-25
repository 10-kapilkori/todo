package com.projects.todos.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.projects.todos.data.entity.TagEntity
import com.projects.todos.data.entity.TemplateEntity

data class TemplateWithTag(
    @Embedded val template: TemplateEntity,
    @Relation(parentColumn = "tagId", entityColumn = "id")
    val tag: TagEntity
)
