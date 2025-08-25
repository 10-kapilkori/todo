package com.projects.todos.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.projects.todos.data.database.TodoDatabase
import com.projects.todos.data.relation.TemplateWithTag
import com.projects.todos.data.repository.TagRepository
import com.projects.todos.data.repository.TaskRepository
import com.projects.todos.data.repository.TemplateRepository
import com.projects.todos.databinding.BottomSheetUseTemplateBinding
import com.projects.todos.ui.viewmodel.TemplatesViewModel
import com.projects.todos.ui.viewmodel.TemplatesViewModelFactory
import com.projects.todos.utils.AppLogger
import com.projects.todos.utils.BottomSheetManager
import com.projects.todos.utils.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.*

interface UseTemplateCallback {
    fun onTaskCreated(taskId: Long)
}

class UseTemplateBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetUseTemplateBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TemplatesViewModel
    private var callback: UseTemplateCallback? = null
    
    private var template: TemplateWithTag? = null
    private var selectedDate: Long? = null
    private var selectedTime: Int? = null // Hour in 24-hour format
    private var selectedMinute: Int? = null
    private var dueDateTime: Long? = null

    companion object {
        const val TAG = "UseTemplateBottomSheet"
        
        fun newInstance(
            template: TemplateWithTag,
            callback: UseTemplateCallback? = null
        ): UseTemplateBottomSheetFragment {
            return UseTemplateBottomSheetFragment().apply {
                this.template = template
                this.callback = callback
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetUseTemplateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModel()
        setupTemplateData()
        setupListeners()
    }

    private fun setupViewModel() {
        val database = TodoDatabase.getDatabase(requireContext())
        val templateRepository = TemplateRepository(database.templateDao())
        val taskRepository = TaskRepository(database.taskDao())
        val tagRepository = TagRepository(database.tagDao())
        
        val factory = TemplatesViewModelFactory(templateRepository, taskRepository, tagRepository)
        viewModel = ViewModelProvider(this, factory)[TemplatesViewModel::class.java]
    }

    private fun setupTemplateData() {
        template?.let { temp ->
            binding.templateTitle.text = temp.template.title
            
            if (temp.template.description.isNotBlank()) {
                binding.templateDescription.text = temp.template.description
                binding.templateDescription.visibility = View.VISIBLE
            } else {
                binding.templateDescription.visibility = View.GONE
            }

            binding.templateTag.text = temp.tag.name
            try {
                binding.templateTag.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor(temp.tag.colorHex)
                )
            } catch (e: Exception) {
                AppLogger.e("UseTemplateBottomSheet", "Failed to parse tag color: ${temp.tag.colorHex}")
            }
        }
    }

    private fun setupListeners() {
        binding.closeButton.setOnClickListener {
            dismiss()
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.datePickerButton.setOnClickListener {
            showDatePicker()
        }

        binding.timePickerButton.setOnClickListener {
            showTimePicker()
        }

        binding.clearDueDateButton.setOnClickListener {
            clearDueDateTime()
        }

        binding.createTaskButton.setOnClickListener {
            createTaskFromTemplate()
        }
    }

    private fun showDatePicker() {
        AppLogger.methodEntry("UseTemplateBottomSheetFragment", "showDatePicker")
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(com.projects.todos.R.string.select_due_date))
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            selectedDate = selection
            AppLogger.dateTime("UseTemplateBottomSheetFragment", "Date selected: $selection")
            updateDueDateTimeDisplay()
        }

        datePicker.show(parentFragmentManager, "date_picker")
        AppLogger.methodExit("UseTemplateBottomSheetFragment", "showDatePicker")
    }

    private fun showTimePicker() {
        AppLogger.methodEntry("UseTemplateBottomSheetFragment", "showTimePicker")
        
        // Set default time to current time + 5 minutes
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, 5)
        val defaultHour = calendar.get(Calendar.HOUR_OF_DAY)
        val defaultMinute = calendar.get(Calendar.MINUTE)
        
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(defaultHour)
            .setMinute(defaultMinute)
            .setTitleText(getString(com.projects.todos.R.string.select_due_time))
            .build()

        timePicker.addOnPositiveButtonClickListener {
            selectedTime = timePicker.hour
            selectedMinute = timePicker.minute
            AppLogger.d("UseTemplateBottomSheetFragment", "Time selected: ${timePicker.hour}:${timePicker.minute}")
            updateDueDateTimeDisplay()
        }

        timePicker.show(parentFragmentManager, "time_picker")
        AppLogger.methodExit("UseTemplateBottomSheetFragment", "showTimePicker")
    }

    private fun updateDueDateTimeDisplay() {
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
                dueDateTime = calendar.timeInMillis
                getString(com.projects.todos.R.string.due_format, DateTimeUtils.formatFullDateTime(dueDateTime!!))
            } else {
                // Only date is selected
                getString(com.projects.todos.R.string.due_date_only_format, DateTimeUtils.formatDate(selectedDate!!))
            }
            
            binding.dueDateText.text = displayText
            binding.dueDateContainer.visibility = View.VISIBLE
        }
    }

    private fun clearDueDateTime() {
        selectedDate = null
        selectedTime = null
        selectedMinute = null
        dueDateTime = null
        binding.dueDateContainer.visibility = View.GONE
    }

    private fun createTaskFromTemplate() {
        template?.let { temp ->
            viewModel.useTemplate(temp.template.id, dueDateTime)
            // The actual task ID will be communicated through the ViewModel's UI event
            // We'll use a placeholder ID for now, but the real ID will come from the UI event
            callback?.onTaskCreated(-1L) // -1 indicates the task is being created
        }
        dismiss()
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
