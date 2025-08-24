package com.projects.todos.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.projects.todos.R
import com.projects.todos.data.relation.TaskWithTag
import com.projects.todos.databinding.BottomSheetTaskDetailBinding
import com.projects.todos.utils.BottomSheetManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.toColorInt
import com.projects.todos.data.entity.TaskEntity

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
            return TaskDetailBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_TASK_WITH_TAG, taskWithTag)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetTaskDetailBinding.inflate(inflater, container, false)
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
        
        // Get task data from arguments
        taskWithTag = arguments?.getParcelable(ARG_TASK_WITH_TAG)
        
        setupUI()
        setupClickListeners()
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        // Notify the manager that this bottom sheet is dismissed
        BottomSheetManager.removeBottomSheet(TAG)
    }

    private fun setupUI() {
        taskWithTag?.let { taskWithTag ->
            val task = taskWithTag.task
            val tag = taskWithTag.tag

            binding.apply {
                // Set title
                taskDetailTitle.text = task.title
                
                // Set description
                taskDetailDescription.text = task.description
                
                // Set created date
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val createdDate = Date(task.createdAt)
                taskDetailCreatedDate.text = "Created on ${dateFormat.format(createdDate)}"
                
                // Set tag
                taskDetailTag.text = tag.name
                // Create custom background with tag color but same shape as task items
                val tagColor = tag.colorHex.toColorInt()
                val tagBackground = createTagBackground(tagColor)
                taskDetailTag.background = tagBackground
            }
        }
    }

    private fun setupClickListeners() {
        binding.taskDetailOverflow.setOnClickListener {
            showOverflowMenu()
        }
    }

    private fun showOverflowMenu() {
        val popupMenu = PopupMenu(requireContext(), binding.taskDetailOverflow)
        popupMenu.inflate(R.menu.menu_task_detail_overflow)
        
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit_task -> {
                    taskWithTag?.let { task ->
                        showEditTaskBottomSheet(task.task.id)
                    }
                    dismiss()
                    true
                }
                R.id.action_delete_task -> {
                    showDeleteConfirmationDialog()
                    true
                }
                else -> false
            }
        }
        
        popupMenu.show()
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                taskWithTag?.let { 
                    onDeleteTask?.invoke(it)
                    dismiss()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showEditTaskBottomSheet(taskId: Int) {
        val bottomSheet = com.projects.todos.ui.fragment.CreateTaskBottomSheetFragment.newInstanceForEdit(taskId)
        
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
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
