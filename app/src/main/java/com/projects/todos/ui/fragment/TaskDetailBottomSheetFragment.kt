package com.projects.todos.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.projects.todos.R
import com.projects.todos.data.entity.TaskEntity
import com.projects.todos.data.relation.TaskWithTag
import com.projects.todos.databinding.BottomSheetTaskDetailBinding
import com.projects.todos.utils.AppLogger
import com.projects.todos.utils.BottomSheetManager
import com.projects.todos.utils.DateTimeUtils
import com.projects.todos.utils.ThemeUtils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskDetailBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTaskDetailBinding? = null
    private val binding get() = _binding!!
    
    private var taskWithTag: TaskWithTag? = null
    
    // Callbacks for parent activity/fragment
    var onDeleteTask: ((TaskWithTag) -> Unit)? = null
    var onTaskUpdated: ((TaskEntity) -> Unit)? = null

    companion object {
        private const val ARG_TASK_WITH_TAG = "task_with_tag"
        const val TAG = "TaskDetailBottomSheet"
        
        fun newInstance(taskWithTag: TaskWithTag): TaskDetailBottomSheetFragment {
            AppLogger.methodEntry("TaskDetailBottomSheetFragment", "newInstance")
            val fragment = TaskDetailBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_TASK_WITH_TAG, taskWithTag)
                }
            }
            AppLogger.methodExit("TaskDetailBottomSheetFragment", "newInstance")
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        AppLogger.methodEntry("TaskDetailBottomSheetFragment", "onCreateView")
        _binding = BottomSheetTaskDetailBinding.inflate(inflater, container, false)
        AppLogger.methodExit("TaskDetailBottomSheetFragment", "onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppLogger.methodEntry("TaskDetailBottomSheetFragment", "onViewCreated")

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
        
        // Get task data from arguments
        taskWithTag = arguments?.getParcelable(ARG_TASK_WITH_TAG)

        setupUI()
        setupClickListeners()
        AppLogger.methodExit("TaskDetailBottomSheetFragment", "onViewCreated")
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        AppLogger.uiOperation(
            "TaskDetailBottomSheetFragment",
            getString(R.string.bottom_sheet_dismissed)
        )
        // Notify the manager that this bottom sheet is dismissed
        BottomSheetManager.removeBottomSheet(TAG)
    }

    private fun setupUI() {
        AppLogger.methodEntry("TaskDetailBottomSheetFragment", "setupUI")
        taskWithTag?.let { taskWithTag ->
            val task = taskWithTag.task
            val tag = taskWithTag.tag

            binding.apply {
                // Set title
                taskDetailTitle.text = task.title
                
                // Set description
                taskDetailDescription.text = task.description

                // Set due date if exists
                task.dueDateTime?.let { dueDateTime ->
                    taskDetailDueDate.text = getString(
                        R.string.due_format,
                        DateTimeUtils.getRelativeTimeString(dueDateTime)
                    )
                    taskDetailDueDate.visibility = View.VISIBLE

                    // Set text color based on whether task is overdue
                    if (DateTimeUtils.isOverdue(dueDateTime) && !task.isCompleted) {
                        taskDetailDueDate.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.error
                            )
                        )
                        AppLogger.d(
                            "TaskDetailBottomSheetFragment",
                            getString(R.string.task_overdue, task.id)
                        )
                    } else {
                        taskDetailDueDate.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                android.R.color.darker_gray
                            )
                        )
                    }
                } ?: run {
                    taskDetailDueDate.visibility = View.GONE
                }

                // Set created date
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val createdDate = Date(task.createdAt)
                taskDetailCreatedDate.text =
                    getString(R.string.created_on_format, dateFormat.format(createdDate))
                
                // Set tag
                taskDetailTag.text = tag.name
                // Create custom background with tag color but same shape as task items
                val tagColor = tag.colorHex.toColorInt()
                val tagBackground = createTagBackground(tagColor)
                taskDetailTag.background = tagBackground
                taskDetailTag.setTextColor(
                    ThemeUtils.getThemeColor(
                        requireContext(),
                        com.google.android.material.R.attr.colorOnSurfaceInverse
                    )
                )
            }
        }
        AppLogger.methodExit("TaskDetailBottomSheetFragment", "setupUI")
    }

    private fun setupClickListeners() {
        AppLogger.methodEntry("TaskDetailBottomSheetFragment", "setupClickListeners")
        binding.taskDetailOverflow.setOnClickListener {
            showOverflowMenu()
        }
        AppLogger.methodExit("TaskDetailBottomSheetFragment", "setupClickListeners")
    }

    private fun showOverflowMenu() {
        AppLogger.methodEntry("TaskDetailBottomSheetFragment", "showOverflowMenu")
        val popupMenu = PopupMenu(requireContext(), binding.taskDetailOverflow)
        popupMenu.inflate(R.menu.menu_task_detail_overflow)
        
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit_task -> {
                    AppLogger.uiOperation(
                        "TaskDetailBottomSheetFragment",
                        getString(R.string.edit_task_action)
                    )
                    taskWithTag?.let { task ->
                        showEditTaskBottomSheet(task.task.id)
                    }
                    dismiss()
                    true
                }
                R.id.action_delete_task -> {
                    AppLogger.uiOperation(
                        "TaskDetailBottomSheetFragment",
                        getString(R.string.delete_task_action)
                    )
                    showDeleteConfirmationDialog()
                    true
                }
                else -> false
            }
        }
        
        popupMenu.show()
        AppLogger.methodExit("TaskDetailBottomSheetFragment", "showOverflowMenu")
    }

    private fun showDeleteConfirmationDialog() {
        AppLogger.methodEntry("TaskDetailBottomSheetFragment", "showDeleteConfirmationDialog")
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_task))
            .setMessage(getString(R.string.delete_task_confirmation))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                taskWithTag?.let { 
                    onDeleteTask?.invoke(it)
                    dismiss()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
        AppLogger.methodExit("TaskDetailBottomSheetFragment", "showDeleteConfirmationDialog")
    }
    
    private fun showEditTaskBottomSheet(taskId: Int) {
        AppLogger.methodEntry(
            "TaskDetailBottomSheetFragment",
            "showEditTaskBottomSheet",
            "taskId" to taskId
        )
        val bottomSheet = CreateTaskBottomSheetFragment.newInstanceForEdit(taskId)
        
        bottomSheet.onTaskUpdated = { updatedTask ->
            // Task was updated successfully
            onTaskUpdated?.invoke(updatedTask)
        }
        
        // Use BottomSheetManager to prevent duplicate openings
        BottomSheetManager.showBottomSheetIfNotActive(
            parentFragmentManager,
            bottomSheet,
            "EditTaskBottomSheet"
        )
        AppLogger.methodExit("TaskDetailBottomSheetFragment", "showEditTaskBottomSheet")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AppLogger.d("TaskDetailBottomSheetFragment", getString(R.string.on_destroy_view))
        _binding = null
    }
    
    /**
     * Creates a tag background drawable with the same shape as tag_background.xml
     * but with a custom color
     */
    private fun createTagBackground(color: Int): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = resources.getDimensionPixelSize(R.dimen.tag_corner_radius).toFloat()
            setColor(color)
        }
    }
}
