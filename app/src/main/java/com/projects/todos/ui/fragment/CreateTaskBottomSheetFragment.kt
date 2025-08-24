package com.projects.todos.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.projects.todos.R
import com.projects.todos.data.database.TodoDatabase
import com.projects.todos.data.entity.TagEntity
import com.projects.todos.data.entity.TaskEntity
import com.projects.todos.data.repository.TagRepository
import com.projects.todos.data.repository.TaskRepository
import com.projects.todos.databinding.BottomSheetCreateTaskBinding
import com.projects.todos.utils.AppLogger
import com.projects.todos.utils.AppLogger.getString
import com.projects.todos.utils.BottomSheetManager
import com.projects.todos.utils.DateTimeUtils
import com.projects.todos.utils.KeyboardUtils
import com.projects.todos.utils.NotificationManager
import com.projects.todos.utils.ThemeUtils
import kotlinx.coroutines.launch
import java.util.Calendar

class CreateTaskBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCreateTaskBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskRepository: TaskRepository
    private lateinit var tagRepository: TagRepository
    private var selectedTagId: Int? = null

    // Edit mode variables
    private var isEditMode = false
    private var taskToEdit: TaskEntity? = null

    // Date and time variables
    private var selectedDate: Long? = null
    private var selectedTime: Int? = null // Hour in 24-hour format
    private var selectedMinute: Int? = null

    // Callbacks for parent fragment/activity
    var onTaskCreated: ((TaskEntity) -> Unit)? = null
    var onTaskUpdated: ((TaskEntity) -> Unit)? = null

    companion object {
        private const val ARG_TASK_ID = "task_id"
        const val TAG = "CreateTaskBottomSheet"

        fun newInstance(): CreateTaskBottomSheetFragment {
            AppLogger.d(
                "CreateTaskBottomSheetFragment",
                getString(R.string.creating_instance_creation)
            )
            return CreateTaskBottomSheetFragment()
        }

        fun newInstanceForEdit(taskId: Int): CreateTaskBottomSheetFragment {
            AppLogger.d(
                "CreateTaskBottomSheetFragment",
                getString(R.string.creating_instance_editing, taskId)
            )
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
        AppLogger.methodEntry("CreateTaskBottomSheetFragment", "onCreateView")
        _binding = BottomSheetCreateTaskBinding.inflate(inflater, container, false)
        AppLogger.methodExit("CreateTaskBottomSheetFragment", "onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppLogger.methodEntry("CreateTaskBottomSheetFragment", "onViewCreated")

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

        AppLogger.methodExit("CreateTaskBottomSheetFragment", "onViewCreated")
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        AppLogger.uiOperation(
            "CreateTaskBottomSheetFragment",
            getString(R.string.bottom_sheet_dismissed)
        )
        // Notify the manager that this bottom sheet is dismissed
        BottomSheetManager.removeBottomSheet(TAG)
    }

    private fun setupRepositories() {
        AppLogger.methodEntry("CreateTaskBottomSheetFragment", "setupRepositories")
        val database = TodoDatabase.getDatabase(requireContext())
        taskRepository = TaskRepository(database.taskDao())
        tagRepository = TagRepository(database.tagDao())
        AppLogger.methodExit("CreateTaskBottomSheetFragment", "setupRepositories")
    }

    private fun setupImeActions() {
        AppLogger.methodEntry("CreateTaskBottomSheetFragment", "setupImeActions")
        // Title field - move to next field
        binding.titleEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.descriptionEditText.requestFocus()
                AppLogger.uiOperation(
                    "CreateTaskBottomSheetFragment",
                    getString(R.string.ime_action_next)
                )
                return@setOnEditorActionListener true
            }
            false
        }

        // Description field - submit form
        binding.descriptionEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                AppLogger.uiOperation(
                    "CreateTaskBottomSheetFragment",
                    getString(R.string.ime_action_submit)
                )
                if (isEditMode) {
                    updateTask()
                } else {
                    createTask()
                }
                return@setOnEditorActionListener true
            }
            false
        }
        AppLogger.methodExit("CreateTaskBottomSheetFragment", "setupImeActions")
    }

    private fun setupKeyboardHiding() {
        AppLogger.methodEntry("CreateTaskBottomSheetFragment", "setupKeyboardHiding")
        // Set up root layout to handle outside touches
        binding.root.setOnClickListener {
            hideKeyboardAndClearFocus()
        }
        AppLogger.methodExit("CreateTaskBottomSheetFragment", "setupKeyboardHiding")
    }

    private fun hideKeyboard() {
        AppLogger.uiOperation("CreateTaskBottomSheetFragment", getString(R.string.hide_keyboard))
        KeyboardUtils.hideKeyboard(requireContext())
    }

    private fun hideKeyboardAndClearFocus() {
        AppLogger.uiOperation(
            "CreateTaskBottomSheetFragment",
            getString(R.string.hide_keyboard_clear_focus)
        )
        // Hide keyboard and clear focus from both input fields in one operation
        KeyboardUtils.hideKeyboardAndClearFocus(requireContext(), binding.titleEditText, binding.descriptionEditText)
    }

    private fun clearFocus() {
        AppLogger.uiOperation("CreateTaskBottomSheetFragment", getString(R.string.clear_focus))
        KeyboardUtils.clearFocus(binding.titleEditText)
        KeyboardUtils.clearFocus(binding.descriptionEditText)
    }

    private fun checkEditMode() {
        AppLogger.methodEntry("CreateTaskBottomSheetFragment", "checkEditMode")
        val taskId = arguments?.getInt(ARG_TASK_ID, -1)
        taskId?.let {
            if (it != -1) {
                isEditMode = true
                AppLogger.d("CreateTaskBottomSheetFragment", getString(R.string.task_not_found, it))
                loadTaskForEdit(it)
            }
        }
        AppLogger.methodExit("CreateTaskBottomSheetFragment", "checkEditMode")
    }

    private fun loadTaskForEdit(taskId: Int) {
        AppLogger.methodEntry(
            "CreateTaskBottomSheetFragment",
            "loadTaskForEdit",
            "taskId" to taskId
        )
        lifecycleScope.launch {
            try {
                val taskWithTag = taskRepository.getTaskWithTagById(taskId)
                if (taskWithTag != null) {
                    taskToEdit = taskWithTag.task
                    populateFields(taskWithTag)
                    AppLogger.d(
                        "CreateTaskBottomSheetFragment",
                        getString(R.string.task_loaded_editing, taskWithTag.task.title)
                    )
                } else {
                    AppLogger.w(
                        "CreateTaskBottomSheetFragment",
                        getString(R.string.task_not_found, taskId)
                    )
                }
            } catch (e: Exception) {
                if (isAdded && _binding != null) {
                    AppLogger.error(
                        "CreateTaskBottomSheetFragment",
                        "loadTaskForEdit",
                        e,
                        "taskId: $taskId"
                    )
                    showError(getString(R.string.failed_to_load_task, e.message))
                }
            }
        }
        AppLogger.methodExit("CreateTaskBottomSheetFragment", "loadTaskForEdit")
    }

    private fun populateFields(taskWithTag: com.projects.todos.data.relation.TaskWithTag) {
        AppLogger.methodEntry("CreateTaskBottomSheetFragment", "populateFields")
        if (!isAdded || _binding == null) return

        val task = taskWithTag.task
        taskWithTag.tag

        // Set title and description
        binding.titleEditText.setText(task.title)
        binding.descriptionEditText.setText(task.description)

        // Set selected tag
        selectedTagId = task.tagId

        // Set due date and time if exists
        task.dueDateTime?.let { dueDateTime ->
            val calendar = Calendar.getInstance().apply { timeInMillis = dueDateTime }
            selectedDate = dueDateTime
            selectedTime = calendar.get(Calendar.HOUR_OF_DAY)
            selectedMinute = calendar.get(Calendar.MINUTE)
            updateDueDateDisplay()
            AppLogger.d(
                "CreateTaskBottomSheetFragment",
                getString(R.string.due_date_loaded, dueDateTime)
            )
        }

        // Update UI for edit mode
        binding.createButton.text = getString(R.string.update)
        binding.titleEditText.hint = getString(R.string.edit_task_title)

        // Update header title
        val titleText = if (task.title.length > 20) {
            getString(R.string.edit_task) + ": ${task.title.take(20)}..."
        } else {
            getString(R.string.edit_task) + ": ${task.title}"
        }
        binding.headerTitle.text = titleText

        AppLogger.methodExit("CreateTaskBottomSheetFragment", "populateFields")
    }

    private fun setupClickListeners() {
        AppLogger.methodEntry("CreateTaskBottomSheetFragment", "setupClickListeners")
        binding.closeButton.setOnClickListener {
            AppLogger.uiOperation(
                "CreateTaskBottomSheetFragment",
                getString(R.string.close_button_clicked)
            )
            dismiss()
        }
        
        binding.cancelButton.setOnClickListener {
            AppLogger.uiOperation(
                "CreateTaskBottomSheetFragment",
                getString(R.string.cancel_button_clicked)
            )
            dismiss()
        }

        binding.createButton.setOnClickListener {
            AppLogger.uiOperation(
                "CreateTaskBottomSheetFragment",
                getString(R.string.create_update_button_clicked)
            )
            if (isEditMode) {
                updateTask()
            } else {
                createTask()
            }
        }

        // Date and time picker buttons
        binding.datePickerButton.setOnClickListener {
            AppLogger.uiOperation(
                "CreateTaskBottomSheetFragment",
                getString(R.string.date_picker_button_clicked)
            )
            showDatePicker()
        }

        binding.timePickerButton.setOnClickListener {
            AppLogger.uiOperation(
                "CreateTaskBottomSheetFragment",
                getString(R.string.time_picker_button_clicked)
            )
            showTimePicker()
        }

        binding.clearDueDateButton.setOnClickListener {
            AppLogger.uiOperation(
                "CreateTaskBottomSheetFragment",
                getString(R.string.clear_due_date_button_clicked)
            )
            clearDueDate()
        }
        AppLogger.methodExit("CreateTaskBottomSheetFragment", "setupClickListeners")
    }

    private fun showDatePicker() {
        AppLogger.methodEntry("CreateTaskBottomSheetFragment", "showDatePicker")
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.select_due_date))
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            selectedDate = selection
            AppLogger.dateTime(
                "CreateTaskBottomSheetFragment",
                getString(R.string.date_selected, selection)
            )
            updateDueDateDisplay()
        }

        datePicker.show(parentFragmentManager, "date_picker")
        AppLogger.methodExit("CreateTaskBottomSheetFragment", "showDatePicker")
    }

    private fun showTimePicker() {
        AppLogger.methodEntry("CreateTaskBottomSheetFragment", "showTimePicker")
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(12)
            .setMinute(0)
            .setTitleText(getString(R.string.select_due_time))
            .build()

        timePicker.addOnPositiveButtonClickListener {
            selectedTime = timePicker.hour
            selectedMinute = timePicker.minute
            AppLogger.d(
                "CreateTaskBottomSheetFragment",
                getString(R.string.time_selected, timePicker.hour, timePicker.minute)
            )
            updateDueDateDisplay()
        }

        timePicker.show(parentFragmentManager, "time_picker")
        AppLogger.methodExit("CreateTaskBottomSheetFragment", "showTimePicker")
    }

    private fun updateDueDateDisplay() {
        AppLogger.methodEntry("CreateTaskBottomSheetFragment", "updateDueDateDisplay")
        
        if (selectedDate != null) {
            // Show date even if only date is selected
            val calendar = Calendar.getInstance().apply {
                timeInMillis = selectedDate!!
                // If time is not selected, use current time as placeholder
                if (selectedTime == null || selectedMinute == null) {
                    val now = Calendar.getInstance()
                    set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, now.get(Calendar.MINUTE))
                } else {
                    set(Calendar.HOUR_OF_DAY, selectedTime!!)
                    set(Calendar.MINUTE, selectedMinute!!)
                }
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val displayText = if (selectedTime != null && selectedMinute != null) {
                // Both date and time are selected
                val dueDateTime = calendar.timeInMillis
                
                // Validate that the selected date/time is not in the past
                if (dueDateTime <= System.currentTimeMillis()) {
                    AppLogger.w(
                        "CreateTaskBottomSheetFragment",
                        getString(R.string.selected_date_past, dueDateTime)
                    )
                    showError(getString(R.string.select_future_date_time))
                    clearDueDate()
                    AppLogger.methodExit("CreateTaskBottomSheetFragment", "updateDueDateDisplay")
                    return
                }
                
                getString(R.string.due_format, DateTimeUtils.formatFullDateTime(dueDateTime))
            } else {
                // Only date is selected
                getString(R.string.due_date_only_format, DateTimeUtils.formatDate(selectedDate!!))
            }
            
            binding.dueDateText.text = displayText
            binding.dueDateContainer.visibility = View.VISIBLE
            
            // Update button states to show selection
            updateButtonStates()
            
            AppLogger.d(
                "CreateTaskBottomSheetFragment",
                getString(R.string.due_date_display_updated, selectedDate!!)
            )
        } else {
            binding.dueDateContainer.visibility = View.GONE
            updateButtonStates()
            AppLogger.d(
                "CreateTaskBottomSheetFragment",
                getString(R.string.due_date_display_cleared)
            )
        }
        AppLogger.methodExit("CreateTaskBottomSheetFragment", "updateDueDateDisplay")
    }
    
    private fun updateButtonStates() {
        // Update date picker button state
        if (selectedDate != null) {
            binding.datePickerButton.text = getString(R.string.change_date)
            binding.datePickerButton.setIconResource(R.drawable.ic_calendar)
        } else {
            binding.datePickerButton.text = getString(R.string.pick_date)
            binding.datePickerButton.setIconResource(R.drawable.ic_calendar)
        }
        
        // Update time picker button state
        if (selectedTime != null && selectedMinute != null) {
            val timeString = String.format("%02d:%02d", selectedTime!!, selectedMinute!!)
            binding.timePickerButton.text = getString(R.string.change_time_format, timeString)
        } else {
            binding.timePickerButton.text = getString(R.string.pick_time)
        }
    }

    private fun clearDueDate() {
        AppLogger.methodEntry("CreateTaskBottomSheetFragment", "clearDueDate")
        selectedDate = null
        selectedTime = null
        selectedMinute = null
        binding.dueDateContainer.visibility = View.GONE
        updateButtonStates()
        AppLogger.d("CreateTaskBottomSheetFragment", getString(R.string.due_date_cleared))
        AppLogger.methodExit("CreateTaskBottomSheetFragment", "clearDueDate")
    }

    private fun getDueDateTime(): Long? {
        AppLogger.methodEntry("CreateTaskBottomSheetFragment", "getDueDateTime")
        val result = if (selectedDate != null && selectedTime != null && selectedMinute != null) {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = selectedDate!!
                set(Calendar.HOUR_OF_DAY, selectedTime!!)
                set(Calendar.MINUTE, selectedMinute!!)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            calendar.timeInMillis
        } else {
            null
        }
        AppLogger.methodExit("CreateTaskBottomSheetFragment", "getDueDateTime", result)
        return result
    }

    private fun loadTags() {
        AppLogger.methodEntry("CreateTaskBottomSheetFragment", "loadTags")
        if (isAdded && _binding != null) {
            lifecycleScope.launch {
                try {
                    tagRepository.getAllTags().collect { tags ->
                        if (isAdded && _binding != null) {
                            createTagChips(tags)
                            AppLogger.d(
                                "CreateTaskBottomSheetFragment",
                                getString(R.string.loaded_tags_count, tags.size)
                            )
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.error("CreateTaskBottomSheetFragment", "loadTags", e)
                    showError(getString(R.string.failed_to_load_tags, e.message))
                }
            }
        }
        AppLogger.methodExit("CreateTaskBottomSheetFragment", "loadTags")
    }

    private fun createTagChips(tags: List<TagEntity>) {
        AppLogger.methodEntry(
            "CreateTaskBottomSheetFragment",
            "createTagChips",
            "tagCount" to tags.size
        )
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
                        AppLogger.d(
                            "CreateTaskBottomSheetFragment",
                            getString(R.string.tag_selected, tag.name, tag.id)
                        )
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
                AppLogger.d(
                    "CreateTaskBottomSheetFragment",
                    getString(R.string.auto_selected_general)
                )
            }

            // In edit mode, select the task's current tag
            if (isEditMode && selectedTagId == tag.id) {
                chip.isChecked = true
                // Apply theme colors for the selected state
                ThemeUtils.applyChipThemeColors(chip, true)
                AppLogger.d(
                    "CreateTaskBottomSheetFragment",
                    getString(R.string.selected_existing_tag, tag.name)
                )
            }
        }
        AppLogger.methodExit("CreateTaskBottomSheetFragment", "createTagChips")
    }

    private fun createStyledChip(text: String): Chip {
        return ThemeUtils.createStyledChip(requireContext(), text)
    }

    private fun createTask() {
        AppLogger.methodEntry("CreateTaskBottomSheetFragment", "createTask")
        if (!isAdded || _binding == null) return

        val title = binding.titleEditText.text.toString().trim()
        val description = binding.descriptionEditText.text.toString().trim()

        // Validation
        if (title.isEmpty()) {
            AppLogger.w("CreateTaskBottomSheetFragment", getString(R.string.title_cannot_be_empty))
            showError(getString(R.string.title_cannot_be_empty))
            AppLogger.methodExit("CreateTaskBottomSheetFragment", "createTask")
            return
        }

        if (selectedTagId == null) {
            AppLogger.w("CreateTaskBottomSheetFragment", getString(R.string.please_select_tag))
            showError(getString(R.string.please_select_tag))
            AppLogger.methodExit("CreateTaskBottomSheetFragment", "createTask")
            return
        }

        // Validate date and time selection
        if ((selectedDate != null && (selectedTime == null || selectedMinute == null)) ||
            (selectedTime != null && selectedDate == null) ||
            (selectedMinute != null && selectedDate == null)) {
            AppLogger.w("CreateTaskBottomSheetFragment", getString(R.string.both_date_time_required))
            showError(getString(R.string.both_date_time_required))
            AppLogger.methodExit("CreateTaskBottomSheetFragment", "createTask")
            return
        }

        lifecycleScope.launch {
            try {
                val dueDateTime = getDueDateTime()

                val newTask = TaskEntity(
                    title = title,
                    description = description,
                    tagId = selectedTagId!!,
                    dueDateTime = dueDateTime,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                val taskId = taskRepository.insertTask(newTask)
                val createdTask = newTask.copy(id = taskId.toInt())

                // Schedule notification if due date is set
                if (dueDateTime != null) {
                    NotificationManager.scheduleTaskNotification(requireContext(), createdTask)
                    AppLogger.d(
                        "CreateTaskBottomSheetFragment",
                        getString(R.string.notification_scheduled, taskId)
                    )
                }

                if (isAdded && _binding != null) {
                    showSuccess(getString(R.string.task_created_successfully))
                    onTaskCreated?.invoke(createdTask)
                    AppLogger.i(
                        "CreateTaskBottomSheetFragment",
                        getString(R.string.task_created_log, taskId, title)
                    )
                    dismiss()
                }

            } catch (e: Exception) {
                if (isAdded && _binding != null) {
                    AppLogger.error(
                        "CreateTaskBottomSheetFragment",
                        "createTask",
                        e,
                        "title: $title"
                    )
                    showError(getString(R.string.failed_to_create_task, e.message))
                }
            }
        }
        AppLogger.methodExit("CreateTaskBottomSheetFragment", "createTask")
    }

    private fun updateTask() {
        AppLogger.methodEntry("CreateTaskBottomSheetFragment", "updateTask")
        if (!isAdded || _binding == null || taskToEdit == null) return

        val title = binding.titleEditText.text.toString().trim()
        val description = binding.descriptionEditText.text.toString().trim()

        // Validation
        if (title.isEmpty()) {
            AppLogger.w("CreateTaskBottomSheetFragment", getString(R.string.title_cannot_be_empty))
            showError(getString(R.string.title_cannot_be_empty))
            AppLogger.methodExit("CreateTaskBottomSheetFragment", "updateTask")
            return
        }

        if (selectedTagId == null) {
            AppLogger.w("CreateTaskBottomSheetFragment", getString(R.string.please_select_tag))
            showError(getString(R.string.please_select_tag))
            AppLogger.methodExit("CreateTaskBottomSheetFragment", "updateTask")
            return
        }

        // Validate date and time selection
        if ((selectedDate != null && (selectedTime == null || selectedMinute == null)) ||
            (selectedTime != null && selectedDate == null) ||
            (selectedMinute != null && selectedDate == null)) {
            AppLogger.w("CreateTaskBottomSheetFragment", getString(R.string.both_date_time_required))
            showError(getString(R.string.both_date_time_required))
            AppLogger.methodExit("CreateTaskBottomSheetFragment", "updateTask")
            return
        }

        lifecycleScope.launch {
            try {
                val dueDateTime = getDueDateTime()

                val updatedTask = taskToEdit!!.copy(
                    title = title,
                    description = description,
                    tagId = selectedTagId!!,
                    dueDateTime = dueDateTime,
                    updatedAt = System.currentTimeMillis()
                )

                taskRepository.updateTask(updatedTask)

                // Reschedule notification if due date changed
                if (dueDateTime != null) {
                    NotificationManager.rescheduleTaskNotification(requireContext(), updatedTask)
                    AppLogger.d(
                        "CreateTaskBottomSheetFragment",
                        getString(R.string.notification_rescheduled, updatedTask.id)
                    )
                } else {
                    // Cancel notification if due date was removed
                    NotificationManager.cancelTaskNotification(requireContext(), updatedTask.id)
                    AppLogger.d(
                        "CreateTaskBottomSheetFragment",
                        getString(R.string.notification_cancelled, updatedTask.id)
                    )
                }

                if (isAdded && _binding != null) {
                    showSuccess(getString(R.string.task_updated_successfully))
                    onTaskUpdated?.invoke(updatedTask)
                    AppLogger.i(
                        "CreateTaskBottomSheetFragment",
                        getString(R.string.task_updated_log, updatedTask.id, title)
                    )
                    dismiss()
                }

            } catch (e: Exception) {
                if (isAdded && _binding != null) {
                    AppLogger.error(
                        "CreateTaskBottomSheetFragment",
                        "updateTask",
                        e,
                        "taskId: ${taskToEdit!!.id}"
                    )
                    showError(getString(R.string.failed_to_update_task, e.message))
                }
            }
        }
        AppLogger.methodExit("CreateTaskBottomSheetFragment", "updateTask")
    }

    private fun showSuccess(message: String) {
        if (isAdded && _binding != null) {
            AppLogger.uiOperation(
                "CreateTaskBottomSheetFragment",
                getString(R.string.show_success, message)
            )
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showError(message: String) {
        if (isAdded && _binding != null) {
            AppLogger.uiOperation(
                "CreateTaskBottomSheetFragment",
                getString(R.string.show_error, message)
            )
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AppLogger.d("CreateTaskBottomSheetFragment", getString(R.string.on_destroy_view))
        _binding = null
    }
}
