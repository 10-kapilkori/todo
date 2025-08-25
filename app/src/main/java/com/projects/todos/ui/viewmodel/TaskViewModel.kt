package com.projects.todos.ui.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projects.todos.data.DisplayItem
import com.projects.todos.data.getEffectiveDate
import com.projects.todos.data.entity.TagEntity
import com.projects.todos.data.relation.TaskWithTag
import com.projects.todos.data.repository.TagRepository
import com.projects.todos.data.repository.TaskRepository
import com.projects.todos.utils.AppLogger
import com.projects.todos.utils.NotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(
    private val taskRepository: TaskRepository,
    private val tagRepository: TagRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _expandedTagId = MutableStateFlow<Int?>(
        savedStateHandle.get<Int>("expanded_tag_id")
    )
    private val _tags = MutableStateFlow<List<TagEntity>>(emptyList())

    val tags: StateFlow<List<TagEntity>> = _tags.asStateFlow()
    val expandedTagId: StateFlow<Int?> = _expandedTagId.asStateFlow()

    val tasks: StateFlow<List<TaskWithTag>> = taskRepository.getAllTasksWithTags().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Flattened display list for accordion
    val displayItems: StateFlow<List<DisplayItem>> = combine(
        tasks,
        tags,
        _expandedTagId
    ) { tasks, tags, expandedTagId ->
        buildDisplayList(tasks, tags, expandedTagId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteTasks: StateFlow<List<TaskWithTag>> =
        taskRepository.getFavoriteTasksWithTags().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        AppLogger.i("TaskViewModel", AppLogger.getString(com.projects.todos.R.string.task_view_model_initialized))
        loadTags()
    }

    private fun loadTags() {
        AppLogger.methodEntry("TaskViewModel", "loadTags")
        viewModelScope.launch {
            try {
                tagRepository.getAllTags().collect { tags ->
                    _tags.value = tags
                    AppLogger.d("TaskViewModel", AppLogger.getString(com.projects.todos.R.string.loaded_tags_vm, tags.size))
                    // Don't automatically set default tag - let the Fragment handle it
                }
            } catch (e: Exception) {
                AppLogger.error("TaskViewModel", "loadTags", e)
                // Handle any errors
                e.printStackTrace()
            }
        }
        AppLogger.methodExit("TaskViewModel", "loadTags")
    }



    fun setExpandedTag(tagId: Int?) {
        AppLogger.methodEntry("TaskViewModel", "setExpandedTag", "tagId" to tagId)
        _expandedTagId.value = tagId
        // Persist to SavedStateHandle
        savedStateHandle["expanded_tag_id"] = tagId
        AppLogger.d("TaskViewModel", "Expanded tag set to: $tagId")
        AppLogger.methodExit("TaskViewModel", "setExpandedTag")
    }

    fun toggleTaskCompletion(taskId: Int, isCompleted: Boolean) {
        AppLogger.methodEntry("TaskViewModel", "toggleTaskCompletion", 
            "taskId" to taskId, 
            "isCompleted" to isCompleted
        )
        viewModelScope.launch {
            try {
                taskRepository.updateTaskCompletion(taskId, isCompleted)
                AppLogger.d("TaskViewModel", AppLogger.getString(com.projects.todos.R.string.task_completion_toggled_vm, taskId, isCompleted))
            } catch (e: Exception) {
                AppLogger.error("TaskViewModel", "toggleTaskCompletion", e, "taskId: $taskId")
            }
        }
        AppLogger.methodExit("TaskViewModel", "toggleTaskCompletion")
    }

    fun toggleTaskFavorite(taskId: Int, isFavorite: Boolean) {
        AppLogger.methodEntry("TaskViewModel", "toggleTaskFavorite", 
            "taskId" to taskId, 
            "isFavorite" to isFavorite
        )
        viewModelScope.launch {
            try {
                taskRepository.updateTaskFavorite(taskId, isFavorite)
                AppLogger.d("TaskViewModel", AppLogger.getString(com.projects.todos.R.string.task_favorite_toggled_vm, taskId, isFavorite))
            } catch (e: Exception) {
                AppLogger.error("TaskViewModel", "toggleTaskFavorite", e, "taskId: $taskId")
            }
        }
        AppLogger.methodExit("TaskViewModel", "toggleTaskFavorite")
    }

    fun deleteTask(taskId: Int, context: Context? = null) {
        AppLogger.methodEntry("TaskViewModel", "deleteTask", 
            "taskId" to taskId, 
            "context" to (context != null)
        )
        viewModelScope.launch {
            try {
                // Cancel notification before deleting the task
                context?.let { 
                    NotificationManager.cancelTaskNotification(it, taskId)
                    AppLogger.d("TaskViewModel", AppLogger.getString(com.projects.todos.R.string.notification_cancelled_vm, taskId))
                }
                taskRepository.deleteTaskById(taskId)
                AppLogger.d("TaskViewModel", AppLogger.getString(com.projects.todos.R.string.task_deleted_successfully, taskId))
            } catch (e: Exception) {
                AppLogger.error("TaskViewModel", "deleteTask", e, "taskId: $taskId")
            }
        }
        AppLogger.methodExit("TaskViewModel", "deleteTask")
    }

    fun createQuickTask(title: String): Boolean {
        AppLogger.methodEntry("TaskViewModel", "createQuickTask", "title" to title)
        
        if (title.trim().isEmpty()) {
            AppLogger.w("TaskViewModel", AppLogger.getString(com.projects.todos.R.string.cannot_create_empty_title))
            AppLogger.methodExit("TaskViewModel", "createQuickTask", false)
            return false
        }

        viewModelScope.launch {
            try {
                // Use General tag as default
                val generalTag = _tags.value.find { it.name == "General" }
                val tagId = generalTag?.id ?: return@launch

                val newTask = com.projects.todos.data.entity.TaskEntity(
                    title = title.trim(),
                    description = "",
                    tagId = tagId,
                    dueDateTime = null, // Quick tasks don't have due dates
                    isCompleted = false,
                    isFavorite = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                val taskId = taskRepository.insertTask(newTask)
                AppLogger.d("TaskViewModel", AppLogger.getString(com.projects.todos.R.string.quick_task_created, taskId, title.trim()))
            } catch (e: Exception) {
                AppLogger.error("TaskViewModel", "createQuickTask", e, "title: $title")
                e.printStackTrace()
            }
        }
        
        AppLogger.methodExit("TaskViewModel", "createQuickTask", true)
        return true
    }



    /**
     * Build the flattened display list for the accordion
     */
    private fun buildDisplayList(
        tasks: List<TaskWithTag>,
        tags: List<TagEntity>,
        expandedTagId: Int?
    ): List<DisplayItem> {
        val displayItems = mutableListOf<DisplayItem>()

        // Show accordion with headers for all tags
        tags.forEach { tag ->
            val tagTasks = tasks.filter { it.task.tagId == tag.id }
            val isExpanded = expandedTagId == tag.id
            
            // Add header
            displayItems.add(
                DisplayItem.TagHeaderItem(
                    tagId = tag.id,
                    tagName = tag.name,
                    count = tagTasks.size,
                    isExpanded = isExpanded,
                    colorHex = tag.colorHex
                )
            )
            
            // Add tasks if expanded
            if (isExpanded) {
                val sortedTasks = tagTasks.sortedByDescending { it.getEffectiveDate() }
                sortedTasks.forEach { taskWithTag ->
                    displayItems.add(
                        DisplayItem.TaskItem(
                            taskId = taskWithTag.task.id,
                            title = taskWithTag.task.title,
                            effectiveDate = taskWithTag.getEffectiveDate(),
                            tagId = taskWithTag.task.tagId,
                            colorHex = tag.colorHex
                        )
                    )
                }
            }
        }

        return displayItems
    }
}
