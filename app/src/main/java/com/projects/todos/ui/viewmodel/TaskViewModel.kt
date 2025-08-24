package com.projects.todos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projects.todos.data.entity.TagEntity
import com.projects.todos.data.relation.TaskWithTag
import com.projects.todos.data.repository.TagRepository
import com.projects.todos.data.repository.TaskRepository
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
        loadTags()
    }

    private fun loadTags() {
        viewModelScope.launch {
            try {
                tagRepository.getAllTags().collect { tags ->
                    _tags.value = tags
                    // Don't automatically set default tag - let the Fragment handle it
                }
            } catch (e: Exception) {
                // Handle any errors
                e.printStackTrace()
            }
        }
    }

    fun setSelectedTag(tagId: Int?, tagName: String? = null) {
        _selectedTagId.value = tagId
        _selectedTagName.value = tagName
    }

    fun toggleTaskCompletion(taskId: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            taskRepository.updateTaskCompletion(taskId, isCompleted)
        }
    }

    fun toggleTaskFavorite(taskId: Int, isFavorite: Boolean) {
        viewModelScope.launch {
            taskRepository.updateTaskFavorite(taskId, isFavorite)
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            taskRepository.deleteTaskById(taskId)
        }
    }

    fun createQuickTask(title: String): Boolean {
        if (title.trim().isEmpty()) {
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
                    isCompleted = false,
                    isFavorite = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                taskRepository.insertTask(newTask)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return true
    }

    /**
     * Get the currently selected tag ID
     */
    fun getSelectedTagId(): Int? {
        return _selectedTagId.value
    }
}
