package com.projects.todos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.projects.todos.data.repository.TaskRepository
import com.projects.todos.data.repository.TagRepository

class TaskViewModelFactory(
    private val taskRepository: TaskRepository,
    private val tagRepository: TagRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            return TaskViewModel(taskRepository, tagRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
