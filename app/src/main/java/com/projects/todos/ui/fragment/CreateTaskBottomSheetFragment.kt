package com.projects.todos.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.projects.todos.R
import com.projects.todos.data.database.TodoDatabase
import com.projects.todos.data.entity.TagEntity
import com.projects.todos.data.entity.TaskEntity
import com.projects.todos.data.repository.TagRepository
import com.projects.todos.data.repository.TaskRepository
import com.projects.todos.databinding.BottomSheetCreateTaskBinding
import com.projects.todos.utils.BottomSheetManager
import com.projects.todos.utils.KeyboardUtils
import com.projects.todos.utils.ThemeUtils
import kotlinx.coroutines.launch

class CreateTaskBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCreateTaskBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskRepository: TaskRepository
    private lateinit var tagRepository: TagRepository
    private var selectedTagId: Int? = null

    // Edit mode variables
    private var isEditMode = false
    private var taskToEdit: TaskEntity? = null

    // Callbacks for parent fragment/activity
    var onTaskCreated: ((TaskEntity) -> Unit)? = null
    var onTaskUpdated: ((TaskEntity) -> Unit)? = null

    companion object {
        private const val ARG_TASK_ID = "task_id"
        const val TAG = "CreateTaskBottomSheet"

        fun newInstance(): CreateTaskBottomSheetFragment {
            return CreateTaskBottomSheetFragment()
        }

        fun newInstanceForEdit(taskId: Int): CreateTaskBottomSheetFragment {
            return CreateTaskBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TASK_ID, taskId)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCreateTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configure bottom sheet behavior
        dialog?.let { dialog ->
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet)
                behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                behavior.isDraggable = true
                behavior.isHideable = true
            }
        }

        setupRepositories()
        setupClickListeners()
        setupImeActions()
        setupKeyboardHiding()
        checkEditMode()
        loadTags()
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        // Notify the manager that this bottom sheet is dismissed
        BottomSheetManager.removeBottomSheet(TAG)
    }

    private fun setupRepositories() {
        val database = TodoDatabase.getDatabase(requireContext())
        taskRepository = TaskRepository(database.taskDao())
        tagRepository = TagRepository(database.tagDao())
    }

    private fun setupImeActions() {
        // Title field - move to next field
        binding.titleEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.descriptionEditText.requestFocus()
                return@setOnEditorActionListener true
            }
            false
        }

        // Description field - submit form
        binding.descriptionEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                if (isEditMode) {
                    updateTask()
                } else {
                    createTask()
                }
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun setupKeyboardHiding() {
        // Set up root layout to handle outside touches
        binding.root.setOnClickListener {
            hideKeyboardAndClearFocus()
        }
    }

    private fun hideKeyboard() {
        KeyboardUtils.hideKeyboard(requireContext())
    }

    private fun hideKeyboardAndClearFocus() {
        // Hide keyboard and clear focus from both input fields in one operation
        KeyboardUtils.hideKeyboardAndClearFocus(requireContext(), binding.titleEditText, binding.descriptionEditText)
    }

    private fun clearFocus() {
        KeyboardUtils.clearFocus(binding.titleEditText)
        KeyboardUtils.clearFocus(binding.descriptionEditText)
    }

    private fun checkEditMode() {
        val taskId = arguments?.getInt(ARG_TASK_ID, -1)
        taskId?.let {
            if (it != -1) {
                isEditMode = true
                loadTaskForEdit(it)
            }
        }
    }

    private fun loadTaskForEdit(taskId: Int) {
        lifecycleScope.launch {
            try {
                val taskWithTag = taskRepository.getTaskWithTagById(taskId)
                if (taskWithTag != null) {
                    taskToEdit = taskWithTag.task
                    populateFields(taskWithTag)
                }
            } catch (e: Exception) {
                if (isAdded && _binding != null) {
                    showError("Failed to load task: ${e.message}")
                }
            }
        }
    }

    private fun populateFields(taskWithTag: com.projects.todos.data.relation.TaskWithTag) {
        if (!isAdded || _binding == null) return

        val task = taskWithTag.task
        taskWithTag.tag

        // Set title and description
        binding.titleEditText.setText(task.title)
        binding.descriptionEditText.setText(task.description)

        // Set selected tag
        selectedTagId = task.tagId

        // Update UI for edit mode
        binding.createButton.text = "Update"
        binding.titleEditText.hint = "Edit Task Title"

        // Update header title
        val titleText = if (task.title.length > 20) {
            "Edit: ${task.title.take(20)}..."
        } else {
            "Edit: ${task.title}"
        }
        binding.headerTitle.text = titleText
    }

    private fun setupClickListeners() {
        binding.closeButton.setOnClickListener {
            dismiss()
        }
        
        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.createButton.setOnClickListener {
            if (isEditMode) {
                updateTask()
            } else {
                createTask()
            }
        }
    }

    private fun loadTags() {
        if (isAdded && _binding != null) {
            lifecycleScope.launch {
                try {
                    tagRepository.getAllTags().collect { tags ->
                        if (isAdded && _binding != null) {
                            createTagChips(tags)
                        }
                    }
                } catch (e: Exception) {
                    showError("Failed to load tags: ${e.message}")
                }
            }
        }
    }

    private fun createTagChips(tags: List<TagEntity>) {
        if (!isAdded || _binding == null) return

        binding.tagChipGroup.removeAllViews()

        tags.forEach { tag ->
            val chip = createStyledChip(tag.name).apply {
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        // Apply theme colors
                        ThemeUtils.applyChipThemeColors(this, isChecked)
                        
                        // Uncheck all other chips
                        for (i in 0 until binding.tagChipGroup.childCount) {
                            val child = binding.tagChipGroup.getChildAt(i)
                            if (child is Chip && child != this) {
                                child.isChecked = false
                                ThemeUtils.applyChipThemeColors(child, false)
                            }
                        }
                        
                        selectedTagId = tag.id
                    } else {
                        // Prevent deselection - keep it checked without changing theme
                        this.isChecked = true
                    }
                }
            }
            binding.tagChipGroup.addView(chip)

            // Auto-select "General" tag by default (only in create mode)
            if (!isEditMode && tag.name == "General") {
                chip.isChecked = true
                selectedTagId = tag.id
                // Apply theme colors for the selected state
                ThemeUtils.applyChipThemeColors(chip, true)
            }

            // In edit mode, select the task's current tag
            if (isEditMode && selectedTagId == tag.id) {
                chip.isChecked = true
                // Apply theme colors for the selected state
                ThemeUtils.applyChipThemeColors(chip, true)
            }
        }
    }

    private fun createStyledChip(text: String): Chip {
        return ThemeUtils.createStyledChip(requireContext(), text)
    }

    private fun createTask() {
        if (!isAdded || _binding == null) return

        val title = binding.titleEditText.text.toString().trim()
        val description = binding.descriptionEditText.text.toString().trim()

        // Validation
        if (title.isEmpty()) {
            showError("Title cannot be empty")
            return
        }

        if (selectedTagId == null) {
            showError("Please select a tag")
            return
        }

        lifecycleScope.launch {
            try {
                val newTask = TaskEntity(
                    title = title,
                    description = description,
                    tagId = selectedTagId!!,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                val taskId = taskRepository.insertTask(newTask)
                val createdTask = newTask.copy(id = taskId.toInt())

                if (isAdded && _binding != null) {
                    showSuccess("Task created successfully")
                    onTaskCreated?.invoke(createdTask)
                    dismiss()
                }

            } catch (e: Exception) {
                if (isAdded && _binding != null) {
                    showError("Failed to create task: ${e.message}")
                }
            }
        }
    }

    private fun updateTask() {
        if (!isAdded || _binding == null || taskToEdit == null) return

        val title = binding.titleEditText.text.toString().trim()
        val description = binding.descriptionEditText.text.toString().trim()

        // Validation
        if (title.isEmpty()) {
            showError("Title cannot be empty")
            return
        }

        if (selectedTagId == null) {
            showError("Please select a tag")
            return
        }

        lifecycleScope.launch {
            try {
                val updatedTask = taskToEdit!!.copy(
                    title = title,
                    description = description,
                    tagId = selectedTagId!!,
                    updatedAt = System.currentTimeMillis()
                )

                taskRepository.updateTask(updatedTask)

                if (isAdded && _binding != null) {
                    showSuccess("Task updated successfully")
                    onTaskUpdated?.invoke(updatedTask)
                    dismiss()
                }

            } catch (e: Exception) {
                if (isAdded && _binding != null) {
                    showError("Failed to update task: ${e.message}")
                }
            }
        }
    }

    private fun showSuccess(message: String) {
        if (isAdded && _binding != null) {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showError(message: String) {
        if (isAdded && _binding != null) {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
