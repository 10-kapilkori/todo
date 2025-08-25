package com.projects.todos.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.projects.todos.data.database.TodoDatabase
import com.projects.todos.data.entity.TagEntity
import com.projects.todos.data.relation.TemplateWithTag
import com.projects.todos.data.repository.TagRepository
import com.projects.todos.data.repository.TaskRepository
import com.projects.todos.data.repository.TemplateRepository
import com.projects.todos.databinding.BottomSheetEditTemplateBinding
import com.projects.todos.ui.viewmodel.TemplatesViewModel
import com.projects.todos.ui.viewmodel.TemplatesViewModelFactory
import com.projects.todos.utils.AppLogger
import com.projects.todos.utils.BottomSheetManager
import kotlinx.coroutines.launch

interface EditTemplateCallback {
    fun onTemplateCreated(templateId: Long)
    fun onTemplateUpdated(templateId: Int)
    fun onTemplateDeleted(templateId: Int)
}

class EditTemplateBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetEditTemplateBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TemplatesViewModel
    private var callback: EditTemplateCallback? = null
    
    private var templateToEdit: TemplateWithTag? = null
    private var selectedTagId: Int = 1 // Default to General tag

    companion object {
        const val TAG = "EditTemplateBottomSheet"
        
        fun newInstance(
            templateToEdit: TemplateWithTag? = null,
            callback: EditTemplateCallback? = null
        ): EditTemplateBottomSheetFragment {
            return EditTemplateBottomSheetFragment().apply {
                this.templateToEdit = templateToEdit
                this.callback = callback
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetEditTemplateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModel()
        setupUI()
        setupListeners()
        loadTemplateData()
    }

    private fun setupViewModel() {
        val database = TodoDatabase.getDatabase(requireContext())
        val templateRepository = TemplateRepository(database.templateDao())
        val taskRepository = TaskRepository(database.taskDao())
        val tagRepository = TagRepository(database.tagDao())
        
        val factory = TemplatesViewModelFactory(templateRepository, taskRepository, tagRepository)
        viewModel = ViewModelProvider(this, factory)[TemplatesViewModel::class.java]
    }

    private fun setupUI() {
        if (templateToEdit != null) {
            binding.headerTitle.text = getString(com.projects.todos.R.string.edit_template)
            binding.saveButton.text = getString(com.projects.todos.R.string.update)
            // Show delete functionality for close button when editing
            binding.closeButton.contentDescription = getString(com.projects.todos.R.string.delete)
            binding.closeButton.setOnClickListener {
                deleteTemplate()
            }
        } else {
            binding.headerTitle.text = getString(com.projects.todos.R.string.create_template)
            binding.saveButton.text = getString(com.projects.todos.R.string.save)
            // Show close functionality for close button when creating
            binding.closeButton.contentDescription = getString(com.projects.todos.R.string.close)
            binding.closeButton.setOnClickListener {
                dismiss()
            }
        }
    }

    private fun setupListeners() {
        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.saveButton.setOnClickListener {
            saveTemplate()
        }
    }

    private fun loadTemplateData() {
        // Load tags and create chips dynamically
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tags.collect { tags ->
                createTagChips(tags)
            }
        }

        // Load existing template data if editing
        templateToEdit?.let { template ->
            binding.titleEditText.setText(template.template.title)
            binding.descriptionEditText.setText(template.template.description)
            selectedTagId = template.template.tagId
        }
    }

    private fun createTagChips(tags: List<TagEntity>) {
        val chipGroup = binding.tagChipGroup
        chipGroup.removeAllViews()

        tags.forEach { tag ->
            val chip = com.projects.todos.utils.ThemeUtils.createStyledChip(requireContext(), tag.name).apply {
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedTagId = tag.id
                        // Uncheck other chips
                        for (i in 0 until chipGroup.childCount) {
                            val child = chipGroup.getChildAt(i)
                            if (child is com.google.android.material.chip.Chip && child != this) {
                                child.isChecked = false
                            }
                        }
                    }
                }
            }
            chipGroup.addView(chip)

            // Auto-select the tag if editing or if it's General
            if (templateToEdit != null && tag.id == selectedTagId) {
                chip.isChecked = true
            } else if (templateToEdit == null && tag.name == "General") {
                chip.isChecked = true
                selectedTagId = tag.id
            }
        }
    }

    private fun saveTemplate() {
        val title = binding.titleEditText.text.toString().trim()
        val description = binding.descriptionEditText.text.toString().trim()

        if (title.isEmpty()) {
            binding.titleInputLayout.error = getString(com.projects.todos.R.string.template_title_required)
            return
        }

        binding.titleInputLayout.error = null

        if (templateToEdit != null) {
            // Update existing template
            viewModel.updateTemplate(
                templateToEdit!!.template.id,
                title,
                description,
                selectedTagId
            )
            callback?.onTemplateUpdated(templateToEdit!!.template.id)
        } else {
            // Create new template
            viewModel.createTemplate(title, description, selectedTagId)
        }

        dismiss()
    }

    private fun deleteTemplate() {
        templateToEdit?.let { template ->
            viewModel.deleteTemplate(template.template.id)
            callback?.onTemplateDeleted(template.template.id)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        // Remove from BottomSheetManager when dismissed
        BottomSheetManager.removeBottomSheet(TAG)
    }
}
