package com.projects.todos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projects.todos.data.entity.TaskEntity
import com.projects.todos.data.entity.TemplateEntity
import com.projects.todos.data.repository.TagRepository
import com.projects.todos.data.repository.TaskRepository
import com.projects.todos.data.repository.TemplateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed class TemplatesUiEvent {
    data class TemplateCreated(val templateId: Long) : TemplatesUiEvent()
    data class TemplateUpdated(val templateId: Int) : TemplatesUiEvent()
    data class TemplateDeleted(val templateId: Int) : TemplatesUiEvent()
    data class TaskCreatedFromTemplate(val taskId: Long, val templateTitle: String) :
        TemplatesUiEvent()

    data class TemplateDeletionChoice(val templateId: Int, val action: DeletionAction) :
        TemplatesUiEvent()

    data class ShowDeletionConfirmation(val templateId: Int, val templateTitle: String, val linkedTasksCount: Int) :
        TemplatesUiEvent()

    data class Error(val message: String) : TemplatesUiEvent()
}

enum class DeletionAction {
    DELETE_ALL,
    DETACH
}

class TemplatesViewModel(
    private val templateRepository: TemplateRepository,
    private val taskRepository: TaskRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _uiEvent = MutableStateFlow<TemplatesUiEvent?>(null)
    val uiEvent: StateFlow<TemplatesUiEvent?> = _uiEvent.asStateFlow()

    private val _selectedTagFilter = MutableStateFlow<Int?>(null)
    val selectedTagFilter: StateFlow<Int?> = _selectedTagFilter.asStateFlow()

    private val allTemplatesFlow = templateRepository.getAllTemplatesWithTag()
    
    val templatesFlow = combine(allTemplatesFlow, _selectedTagFilter) { templates, selectedTagId ->
        if (selectedTagId == null) {
            templates
        } else {
            templates.filter { it.template.tagId == selectedTagId }
        }
    }
    
    val tags = tagRepository.getAllTags()

    fun setTagFilter(tagId: Int?) {
        _selectedTagFilter.value = tagId
    }

    fun createTemplate(title: String, description: String, tagId: Int) {
        viewModelScope.launch {
            try {
                val template = TemplateEntity(
                    title = title,
                    description = description,
                    tagId = tagId
                )
                val templateId = templateRepository.insertTemplate(template)
                _uiEvent.value = TemplatesUiEvent.TemplateCreated(templateId)
            } catch (e: Exception) {
                _uiEvent.value = TemplatesUiEvent.Error("Failed to create template: ${e.message}")
            }
        }
    }

    fun updateTemplate(templateId: Int, title: String, description: String, tagId: Int) {
        viewModelScope.launch {
            try {
                val existingTemplate = templateRepository.getTemplateById(templateId)
                existingTemplate?.let { template ->
                    val updatedTemplate = template.copy(
                        title = title,
                        description = description,
                        tagId = tagId,
                        updatedAt = System.currentTimeMillis()
                    )
                    templateRepository.updateTemplate(updatedTemplate)
                    _uiEvent.value = TemplatesUiEvent.TemplateUpdated(templateId)
                }
            } catch (e: Exception) {
                _uiEvent.value = TemplatesUiEvent.Error("Failed to update template: ${e.message}")
            }
        }
    }

    fun useTemplate(templateId: Int, dueDateTime: Long? = null): Long? {
        viewModelScope.launch {
            try {
                val template = templateRepository.getTemplateById(templateId)
                template?.let { temp ->
                    val task = TaskEntity(
                        title = temp.title,
                        description = temp.description,
                        tagId = temp.tagId,
                        templateId = temp.id,
                        dueDateTime = dueDateTime
                    )
                    val createdTaskId = taskRepository.insertTask(task)
                    _uiEvent.value = TemplatesUiEvent.TaskCreatedFromTemplate(
                        createdTaskId,
                        temp.title
                    )
                }
            } catch (e: Exception) {
                _uiEvent.value =
                    TemplatesUiEvent.Error("Failed to create task from template: ${e.message}")
            }
        }
        return null // We'll handle the return value through the UI event
    }

    fun handleTemplateDeletionChoice(templateId: Int, action: DeletionAction) {
        viewModelScope.launch {
            try {
                when (action) {
                    DeletionAction.DELETE_ALL -> {
                        // Delete all associated tasks first, then delete the template
                        taskRepository.deleteTasksByTemplateId(templateId)
                        templateRepository.deleteTemplateById(templateId)
                    }

                    DeletionAction.DETACH -> {
                        // Detach tasks from template (set templateId to -1), then delete the template
                        taskRepository.detachTasksFromTemplate(templateId)
                        templateRepository.deleteTemplateById(templateId)
                    }
                }
                _uiEvent.value = TemplatesUiEvent.TemplateDeletionChoice(templateId, action)
            } catch (e: Exception) {
                _uiEvent.value =
                    TemplatesUiEvent.Error("Failed to handle template deletion: ${e.message}")
            }
        }
    }

    fun deleteTemplate(templateId: Int) {
        viewModelScope.launch {
            try {
                val linkedTasksCount = getLinkedTasksCount(templateId)
                val template = templateRepository.getTemplateById(templateId)
                val templateTitle = template?.title ?: "Unknown Template"
                
                // Always emit event to show confirmation dialog
                _uiEvent.value = TemplatesUiEvent.ShowDeletionConfirmation(templateId, templateTitle, linkedTasksCount)
            } catch (e: Exception) {
                _uiEvent.value = TemplatesUiEvent.Error("Failed to prepare template deletion: ${e.message}")
            }
        }
    }

    suspend fun getLinkedTasksCount(templateId: Int): Int {
        return try {
            taskRepository.getTasksByTemplateId(templateId).size
        } catch (e: Exception) {
            0
        }
    }

    fun clearUiEvent() {
        _uiEvent.value = null
    }
}
