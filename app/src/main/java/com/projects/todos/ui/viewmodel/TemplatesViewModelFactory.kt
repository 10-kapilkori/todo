package com.projects.todos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.projects.todos.data.repository.TagRepository
import com.projects.todos.data.repository.TaskRepository
import com.projects.todos.data.repository.TemplateRepository

class TemplatesViewModelFactory(
    private val templateRepository: TemplateRepository,
    private val taskRepository: TaskRepository,
    private val tagRepository: TagRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TemplatesViewModel::class.java)) {
            return TemplatesViewModel(templateRepository, taskRepository, tagRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
