package com.projects.todos.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(
    private val taskRepository: TaskRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _selectedTagId = MutableStateFlow<Int?>(null)
    private val _selectedTagName = MutableStateFlow<String?>(null)
    private val _tags = MutableStateFlow<List<TagEntity>>(emptyList())

    val tags: StateFlow<List<TagEntity>> = _tags.asStateFlow()
    val selectedTagName: StateFlow<String?> = _selectedTagName.asStateFlow()


    val tasks: StateFlow<List<TaskWithTag>> = combine(
        taskRepository.getAllTasksWithTags(),
        _selectedTagId
    ) { allTasks, selectedTagId ->
        when (selectedTagId) {
            null -> allTasks // Show all tasks
            else -> allTasks.filter { it.task.tagId == selectedTagId }
        }
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

    fun setSelectedTag(tagId: Int?, tagName: String? = null) {
        AppLogger.methodEntry("TaskViewModel", "setSelectedTag", 
            "tagId" to tagId, 
            "tagName" to tagName
        )
        _selectedTagId.value = tagId
        _selectedTagName.value = tagName
        AppLogger.d("TaskViewModel", AppLogger.getString(com.projects.todos.R.string.selected_tag_changed, tagName, tagId))
        AppLogger.methodExit("TaskViewModel", "setSelectedTag")
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
                // Use the currently selected tag ID, or default to General if none selected
                val tagId = _selectedTagId.value ?: run {
                    val generalTag = _tags.value.find { it.name == "General" }
                    generalTag?.id ?: return@launch
                }

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
     * Get the currently selected tag ID
     */
    fun getSelectedTagId(): Int? {
        val tagId = _selectedTagId.value
        AppLogger.d("TaskViewModel", AppLogger.getString(com.projects.todos.R.string.getting_selected_tag, tagId))
        return tagId
    }
}
