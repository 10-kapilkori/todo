package com.projects.todos.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.projects.todos.R
import com.projects.todos.data.database.TodoDatabase
import com.projects.todos.data.entity.TagEntity
import com.projects.todos.data.repository.TagRepository
import com.projects.todos.data.repository.TaskRepository
import com.projects.todos.data.repository.TemplateRepository
import com.projects.todos.data.relation.TemplateWithTag
import com.projects.todos.databinding.FragmentTemplatesBinding
import com.projects.todos.ui.adapter.TemplateAdapter
import com.projects.todos.ui.adapter.TemplateAdapterCallback
import com.projects.todos.ui.viewmodel.DeletionAction
import com.projects.todos.ui.viewmodel.TemplatesUiEvent
import com.projects.todos.ui.viewmodel.TemplatesViewModel
import com.projects.todos.ui.viewmodel.TemplatesViewModelFactory
import com.projects.todos.utils.AppLogger
import com.projects.todos.utils.BottomSheetManager
import com.projects.todos.utils.ThemeUtils
import kotlinx.coroutines.launch
import com.projects.todos.ui.fragment.TemplateDeletionDialogFragment

class TemplatesFragment : Fragment(), 
    EditTemplateCallback, 
    UseTemplateCallback, 
    TemplateDeletionDialogCallback,
    TemplateAdapterCallback {

    private var _binding: FragmentTemplatesBinding? = null
    private val binding get() = _binding!!

    private lateinit var templateAdapter: TemplateAdapter
    private lateinit var viewModel: TemplatesViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        AppLogger.methodEntry("TemplatesFragment", "onCreateView")
        _binding = FragmentTemplatesBinding.inflate(inflater, container, false)
        AppLogger.methodExit("TemplatesFragment", "onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppLogger.methodEntry("TemplatesFragment", "onViewCreated")

        setupViewModel()
        setupRecyclerView()
        setupFab()
        observeData()
        observeUiEvents()
        
        AppLogger.methodExit("TemplatesFragment", "onViewCreated")
    }

    private fun setupViewModel() {
        AppLogger.methodEntry("TemplatesFragment", "setupViewModel")
        val database = TodoDatabase.getDatabase(requireContext())
        val templateRepository = TemplateRepository(database.templateDao())
        val taskRepository = TaskRepository(database.taskDao())
        val tagRepository = TagRepository(database.tagDao())
        
        val factory = TemplatesViewModelFactory(templateRepository, taskRepository, tagRepository)
        viewModel = ViewModelProvider(this, factory)[TemplatesViewModel::class.java]
        
        AppLogger.d("TemplatesFragment", getString(R.string.view_model_initialized))
        AppLogger.methodExit("TemplatesFragment", "setupViewModel")
    }

    private fun setupRecyclerView() {
        AppLogger.methodEntry("TemplatesFragment", "setupRecyclerView")
        templateAdapter = TemplateAdapter(this)
        
        binding.templatesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = templateAdapter
            addItemDecoration(object : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: android.graphics.Rect,
                    view: View,
                    parent: androidx.recyclerview.widget.RecyclerView,
                    state: androidx.recyclerview.widget.RecyclerView.State
                ) {
                    outRect.top = 4
                    outRect.bottom = 4
                }
            })
        }
        AppLogger.methodExit("TemplatesFragment", "setupRecyclerView")
    }

    private fun setupFab() {
        binding.fabAddTemplate.setOnClickListener {
            AppLogger.uiOperation("TemplatesFragment", "FAB clicked")
            showCreateTemplateBottomSheet()
        }
    }

    private fun observeData() {
        // Observe tags and create chips dynamically
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tags.collect { tags ->
                createTagChips(tags)
                AppLogger.d("TemplatesFragment", "Tags updated: ${tags.size} tags")
            }
        }

        // Observe templates and handle empty state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.templatesFlow.collect { templates ->
                templateAdapter.submitList(templates)
                AppLogger.d("TemplatesFragment", "Templates updated: ${templates.size} templates")
                // Force update empty state with a slight delay to ensure UI is ready
                binding.templatesRecyclerView.post {
                    updateEmptyState(templates.isEmpty())
                }
            }
        }

        // Observe selected tag filter for empty state messages
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedTagFilter.collect { selectedTagId ->
                updateEmptyStateMessages(selectedTagId)
                AppLogger.d("TemplatesFragment", "Selected tag filter updated: $selectedTagId")
            }
        }
    }

    private fun createTagChips(tags: List<TagEntity>) {
        AppLogger.methodEntry("TemplatesFragment", "createTagChips", "tagCount" to tags.size)
        // Clear all existing chips
        val chipGroup = binding.filterChipGroup
        chipGroup.removeAllViews()

        // Create "All" chip first
        val allChip = createStyledChip(getString(R.string.all)).apply {
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Apply theme colors
                    ThemeUtils.applyChipThemeColors(this, true)
                    
                    // Uncheck all other chips when "All" is selected
                    for (i in 0 until chipGroup.childCount) {
                        val child = chipGroup.getChildAt(i)
                        if (child is Chip && child != this) {
                            child.isChecked = false
                            // Apply unselected theme to other chips
                            ThemeUtils.applyChipThemeColors(child, false)
                        }
                    }
                    viewModel.setTagFilter(null)
                    AppLogger.uiOperation("TemplatesFragment", "All tag selected")
                } else {
                    // Prevent deselection - keep it checked without changing theme
                    ThemeUtils.applyChipThemeColors(this, false)
                }
            }
        }
        chipGroup.addView(allChip)

        // Add tag chips
        tags.forEach { tag ->
            val chip = createStyledChip(tag.name).apply {
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        // Apply theme colors
                        ThemeUtils.applyChipThemeColors(this, true)
                        
                        // Uncheck "All" chip when a tag is selected
                        allChip.isChecked = false
                        ThemeUtils.applyChipThemeColors(allChip, false)
                        
                        // Uncheck all other tag chips
                        for (i in 0 until chipGroup.childCount) {
                            val child = chipGroup.getChildAt(i)
                            if (child is Chip && child != this && child != allChip) {
                                child.isChecked = false
                                ThemeUtils.applyChipThemeColors(child, false)
                            }
                        }
                        
                        viewModel.setTagFilter(tag.id)
                        AppLogger.uiOperation("TemplatesFragment", "Tag selected: ${tag.name} (ID: ${tag.id})")
                    } else {
                        // Allow deselection when another chip is selected
                        ThemeUtils.applyChipThemeColors(this, false)
                    }
                }
            }
            chipGroup.addView(chip)
            
            // Auto-select "General" chip by default
            if (tag.name == "General") {
                chip.isChecked = true
                viewModel.setTagFilter(tag.id)
                AppLogger.d("TemplatesFragment", "Auto-selected General tag")
            }
        }
        AppLogger.methodExit("TemplatesFragment", "createTagChips")
    }

    private fun createStyledChip(text: String): Chip {
        return ThemeUtils.createStyledChip(requireContext(), text)
    }

    private fun observeUiEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiEvent.collect { event ->
                event?.let { handleUiEvent(it) }
            }
        }
    }

    private fun handleUiEvent(event: TemplatesUiEvent) {
        when (event) {
            is TemplatesUiEvent.TemplateCreated -> {
                AppLogger.d("TemplatesFragment", "Template created: templateId=${event.templateId}")
                showSnackbar(getString(R.string.template_created_successfully))
            }
            is TemplatesUiEvent.TemplateUpdated -> {
                AppLogger.d("TemplatesFragment", "Template updated: templateId=${event.templateId}")
                showSnackbar(getString(R.string.template_updated_successfully))
            }
            is TemplatesUiEvent.TemplateDeleted -> {
                AppLogger.d("TemplatesFragment", "Template deleted: templateId=${event.templateId}")
                showSnackbar(getString(R.string.template_deleted_successfully))
            }
            is TemplatesUiEvent.TaskCreatedFromTemplate -> {
                AppLogger.d("TemplatesFragment", "Task created from template: taskId=${event.taskId}, templateTitle='${event.templateTitle}'")
                showSnackbar(
                    getString(R.string.task_created_from_template),
                    actionText = getString(R.string.view),
                    action = { /* Navigate to Tasks tab */ }
                )
                // Invoke the callback if set
                // Note: This would typically be handled by the parent activity or navigation
            }
            is TemplatesUiEvent.TemplateDeletionChoice -> {
                val message = when (event.action) {
                    DeletionAction.DELETE_ALL -> "Template and tasks deleted"
                    DeletionAction.DETACH -> "Template deleted, tasks kept"
                }
                AppLogger.d("TemplatesFragment", "Template deletion choice: templateId=${event.templateId}, action=${event.action}")
                showSnackbar(message)
            }
            is TemplatesUiEvent.ShowDeletionConfirmation -> {
                AppLogger.d("TemplatesFragment", "Show deletion confirmation: templateId=${event.templateId}, templateTitle='${event.templateTitle}', linkedTasksCount=${event.linkedTasksCount}")
                // Show the deletion confirmation dialog
                val dialog = TemplateDeletionDialogFragment.newInstance(
                    templateId = event.templateId,
                    templateTitle = event.templateTitle,
                    linkedTasksCount = event.linkedTasksCount,
                    callback = this
                )
                
                dialog.show(childFragmentManager, TemplateDeletionDialogFragment.TAG)
            }
            is TemplatesUiEvent.Error -> {
                AppLogger.e("TemplatesFragment", "Error: ${event.message}")
                showSnackbar(event.message)
            }
        }
        viewModel.clearUiEvent()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        AppLogger.methodEntry("TemplatesFragment", "updateEmptyState", "isEmpty" to isEmpty)
        // Cancel any ongoing animations to prevent conflicts
        binding.templatesRecyclerView.animate().cancel()
        binding.emptyStateLayout.animate().cancel()
        
        if (isEmpty) {
            // Show empty state
            if (binding.emptyStateLayout.visibility != View.VISIBLE) {
                binding.templatesRecyclerView.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction {
                        binding.templatesRecyclerView.visibility = View.GONE
                        binding.emptyStateLayout.visibility = View.VISIBLE
                        binding.emptyStateLayout.alpha = 0f
                        binding.emptyStateLayout.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .start()
                    }
                    .start()
            }
        } else {
            // Show templates list
            if (binding.templatesRecyclerView.visibility != View.VISIBLE) {
                binding.emptyStateLayout.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction {
                        binding.emptyStateLayout.visibility = View.GONE
                        binding.templatesRecyclerView.visibility = View.VISIBLE
                        binding.templatesRecyclerView.alpha = 0f
                        binding.templatesRecyclerView.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .start()
                    }
                    .start()
            }
        }
        AppLogger.methodExit("TemplatesFragment", "updateEmptyState")
    }

    private fun updateEmptyStateMessages(selectedTagId: Int?) {
        AppLogger.methodEntry("TemplatesFragment", "updateEmptyStateMessages", "selectedTagId" to selectedTagId)
        
        // For now, we'll use a simple approach. In a real implementation, you'd store the tags locally
        val title = when (selectedTagId) {
            null -> getString(R.string.no_templates_found)
            else -> getString(R.string.no_tag_templates_format, "this category")
        }

        val description = when (selectedTagId) {
            null -> getString(R.string.no_templates_available)
            else -> getString(R.string.no_templates_available)
        }

        binding.emptyStateTitle.text = title
        binding.emptyStateDescription.text = description
        AppLogger.d("TemplatesFragment", "Empty state messages updated: title='$title'")
        AppLogger.methodExit("TemplatesFragment", "updateEmptyStateMessages")
    }

    private fun showCreateTemplateBottomSheet() {
        val bottomSheet = EditTemplateBottomSheetFragment.newInstance(callback = this)
        
        BottomSheetManager.showBottomSheetIfNotActive(
            childFragmentManager,
            bottomSheet,
            EditTemplateBottomSheetFragment.TAG
        )
    }

    private fun showEditTemplateBottomSheet(templateWithTag: TemplateWithTag) {
        val bottomSheet = EditTemplateBottomSheetFragment.newInstance(
            templateToEdit = templateWithTag,
            callback = this
        )
        
        BottomSheetManager.showBottomSheetIfNotActive(
            childFragmentManager,
            bottomSheet,
            EditTemplateBottomSheetFragment.TAG
        )
    }

    private fun showUseTemplateBottomSheet(templateWithTag: TemplateWithTag) {
        val bottomSheet = UseTemplateBottomSheetFragment.newInstance(
            template = templateWithTag,
            callback = this
        )
        
        BottomSheetManager.showBottomSheetIfNotActive(
            childFragmentManager,
            bottomSheet,
            UseTemplateBottomSheetFragment.TAG
        )
    }

    private fun showSnackbar(
        message: String,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        actionText?.let { text ->
            action?.let { callback ->
                snackbar.setAction(text) { callback() }
            }
        }
        snackbar.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        BottomSheetManager.clearAllBottomSheets()
        _binding = null
    }

    // EditTemplateCallback implementation
    override fun onTemplateCreated(templateId: Long) {
        AppLogger.d("TemplatesFragment", "Template created successfully: templateId=$templateId")
    }

    override fun onTemplateUpdated(templateId: Int) {
        AppLogger.d("TemplatesFragment", "Template updated successfully: templateId=$templateId")
    }

    override fun onTemplateDeleted(templateId: Int) {
        AppLogger.d("TemplatesFragment", "Template deletion initiated: templateId=$templateId")
        // The actual deletion will be handled by the ViewModel's deleteTemplate method
    }

    // UseTemplateCallback implementation
    override fun onTaskCreated(taskId: Long) {
        if (taskId == -1L) {
            AppLogger.d("TemplatesFragment", "Task creation initiated from template")
        } else {
            AppLogger.d("TemplatesFragment", "Task created from template successfully: taskId=$taskId")
        }
    }

    // TemplateDeletionDialogCallback implementation
    override fun onDeletionConfirmed(templateId: Int, action: DeletionAction) {
        viewModel.handleTemplateDeletionChoice(templateId, action)
    }

    // TemplateAdapterCallback implementation
    override fun onTemplateClicked(templateWithTag: TemplateWithTag) {
        AppLogger.uiOperation("TemplatesFragment", "Template clicked: ${templateWithTag.template.id}")
        showEditTemplateBottomSheet(templateWithTag)
    }

    override fun onUseTemplate(templateWithTag: TemplateWithTag) {
        AppLogger.uiOperation("TemplatesFragment", "Use template clicked: ${templateWithTag.template.id}")
        showUseTemplateBottomSheet(templateWithTag)
    }

    override fun onMoreOptions(templateWithTag: TemplateWithTag) {
        AppLogger.uiOperation("TemplatesFragment", "More options clicked: ${templateWithTag.template.id}")
        // Directly initiate template deletion (will show confirmation dialog)
        viewModel.deleteTemplate(templateWithTag.template.id)
    }
}
