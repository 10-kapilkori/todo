package com.projects.todos.data

import com.projects.todos.data.relation.TaskWithTag

/**
 * Sealed class representing items in the accordion RecyclerView
 */
sealed class DisplayItem {
    /**
     * Represents a tag header in the accordion
     */
    data class TagHeaderItem(
        val tagId: Int,
        val tagName: String,
        val count: Int,
        val isExpanded: Boolean,
        val colorHex: String
    ) : DisplayItem()

    /**
     * Represents a task item in the accordion
     */
    data class TaskItem(
        val taskId: Int,
        val title: String,
        val effectiveDate: Long,
        val tagId: Int,
        val colorHex: String
    ) : DisplayItem()
}

/**
 * Extension function to calculate effective date for sorting
 */
fun TaskWithTag.getEffectiveDate(): Long {
    return maxOf(task.updatedAt, task.createdAt)
}
