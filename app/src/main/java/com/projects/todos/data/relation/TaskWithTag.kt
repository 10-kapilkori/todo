package com.projects.todos.data.relation

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import com.projects.todos.data.entity.TaskEntity
import com.projects.todos.data.entity.TagEntity
import kotlinx.parcelize.Parcelize

@Parcelize
data class TaskWithTag(
    @Embedded
    val task: TaskEntity,
    
    @Relation(
        parentColumn = "tagId",
        entityColumn = "id"
    )
    val tag: TagEntity
) : Parcelable
