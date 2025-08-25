package com.projects.todos.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.projects.todos.R
import com.projects.todos.data.UserPreferences
import com.projects.todos.data.relation.TaskWithTag
import com.projects.todos.databinding.FragmentTasksBinding
import com.projects.todos.ui.adapter.SectionedTasksAdapter
import com.projects.todos.ui.adapter.SectionedTasksAdapterCallback
import com.projects.todos.ui.viewmodel.TaskViewModel
import com.projects.todos.utils.AppLogger
import com.projects.todos.utils.BottomSheetManager
import com.projects.todos.utils.ThemeUtils
import com.projects.todos.utils.hideKeyboard
import com.projects.todos.utils.applyImeAndSystemBottomInset
import kotlinx.coroutines.launch

class TasksFragment : Fragment(), SectionedTasksAdapterCallback {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    private val taskViewModel: TaskViewModel by activityViewModels()
    private lateinit var sectionedTasksAdapter: SectionedTasksAdapter
    private lateinit var userPreferences: UserPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        AppLogger.methodEntry("TasksFragment", "onCreateView")
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        AppLogger.methodExit("TasksFragment", "onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppLogger.methodEntry("TasksFragment", "onViewCreated")

        setupUserPreferences()
        setupRecyclerView()
        setupFab()
        setupQuickAdd()
        setupKeyboardHiding()
        setupInsets()
        observeData()
        
        AppLogger.methodExit("TasksFragment", "onViewCreated")
    }

    private fun setupUserPreferences() {
        AppLogger.methodEntry("TasksFragment", "setupUserPreferences")
        userPreferences = UserPreferences(requireContext())
        AppLogger.d("TasksFragment", getString(R.string.view_model_initialized))
        AppLogger.methodExit("TasksFragment", "setupUserPreferences")
    }

    private fun setupRecyclerView() {
        AppLogger.methodEntry("TasksFragment", "setupRecyclerView")
        sectionedTasksAdapter = SectionedTasksAdapter(this)
        
        binding.tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = sectionedTasksAdapter
            // Add item decoration for better spacing
            addItemDecoration(object : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: android.graphics.Rect,
                    view: View,
                    parent: androidx.recyclerview.widget.RecyclerView,
                    state: androidx.recyclerview.widget.RecyclerView.State
                ) {
                    outRect.top = 2
                    outRect.bottom = 2
                }
            })
        }
        
        // Set bottom padding after layout to ensure FAB height is available
        binding.tasksRecyclerView.post {
            val fabHeight = binding.fabAddTask.height
            val extraPadding = (40 * resources.displayMetrics.density).toInt() // 20dp in pixels
            val totalBottomPadding = fabHeight + extraPadding
            
            binding.tasksRecyclerView.setPadding(
                binding.tasksRecyclerView.paddingLeft,
                binding.tasksRecyclerView.paddingTop,
                binding.tasksRecyclerView.paddingRight,
                totalBottomPadding
            )
            binding.tasksRecyclerView.clipToPadding = false
            AppLogger.d("TasksFragment", getString(R.string.recycler_view_padding, totalBottomPadding))
        }
        AppLogger.methodExit("TasksFragment", "setupRecyclerView")
    }

    private fun setupInsets() {
        AppLogger.methodEntry("TasksFragment", "setupInsets")
        // Apply IME and system bottom insets to RecyclerView
        binding.tasksRecyclerView.applyImeAndSystemBottomInset()
        AppLogger.methodExit("TasksFragment", "setupInsets")
    }

    private fun setupFab() {
        AppLogger.methodEntry("TasksFragment", "setupFab")
        binding.fabAddTask.setOnClickListener {
            AppLogger.uiOperation("TasksFragment", getString(R.string.fab_clicked))
            // Add scale animation on click
            binding.fabAddTask.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction {
                    binding.fabAddTask.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .withEndAction {
                            showCreateTaskBottomSheet()
                        }
                        .start()
                }
                .start()
        }
        AppLogger.methodExit("TasksFragment", "setupFab")
    }

    private fun setupQuickAdd() {
        AppLogger.methodEntry("TasksFragment", "setupQuickAdd")
        // Handle Add button click
        binding.quickAddButton.setOnClickListener {
            AppLogger.uiOperation("TasksFragment", getString(R.string.quick_add_button_clicked))
            addQuickTask()
        }

        // Handle Enter key press
        binding.quickAddEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                AppLogger.uiOperation("TasksFragment", getString(R.string.quick_add_enter_pressed))
                addQuickTask()
                return@setOnEditorActionListener true
            }
            false
        }
        AppLogger.methodExit("TasksFragment", "setupQuickAdd")
    }

    private fun setupKeyboardHiding() {
        AppLogger.methodEntry("TasksFragment", "setupKeyboardHiding")
        // Set up root layout to handle outside touches
        binding.root.setOnClickListener {
            hideKeyboardAndClearFocus()
        }
        AppLogger.methodExit("TasksFragment", "setupKeyboardHiding")
    }

    private fun hideKeyboard() {
        AppLogger.uiOperation("TasksFragment", getString(R.string.hide_keyboard))
        requireActivity().hideKeyboard()
    }

    private fun hideKeyboardAndClearFocus() {
        AppLogger.uiOperation("TasksFragment", getString(R.string.hide_keyboard_clear_focus))
        binding.quickAddEditText.hideKeyboard()
    }

    private fun clearFocus() {
        AppLogger.uiOperation("TasksFragment", getString(R.string.clear_focus))
        binding.quickAddEditText.clearFocus()
    }

    private fun addQuickTask() {
        AppLogger.methodEntry("TasksFragment", "addQuickTask")
        val taskTitle = binding.quickAddEditText.text.toString().trim()

        if (taskTitle.isNotEmpty()) {
            val success = taskViewModel.createQuickTask(taskTitle)
            if (success) {
                // Clear the input field
                binding.quickAddEditText.text?.clear()
                // Hide keyboard
                hideKeyboard()
                // Clear focus
                clearFocus()
                // Show quick feedback with better styling
                showQuickFeedback(getString(R.string.task_added))
                AppLogger.d("TasksFragment", getString(R.string.quick_task_added, taskTitle))
            } else {
                AppLogger.w("TasksFragment", getString(R.string.failed_add_quick_task, taskTitle))
            }
        } else {
            AppLogger.w("TasksFragment", getString(R.string.cannot_add_empty_task))
        }
        AppLogger.methodExit("TasksFragment", "addQuickTask")
    }

    private fun showQuickFeedback(message: String) {
        AppLogger.uiOperation("TasksFragment", getString(R.string.show_quick_feedback, message))
        val snackbar = com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        )
        snackbar.setAction(getString(R.string.undo)) {
            // TODO: Implement undo functionality
            AppLogger.uiOperation("TasksFragment", getString(R.string.undo_clicked))
        }
        snackbar.show()
    }

    private fun showCreateTaskBottomSheet() {
        AppLogger.methodEntry("TasksFragment", "showCreateTaskBottomSheet")
        val bottomSheet = CreateTaskBottomSheetFragment.newInstance()

        bottomSheet.onTaskCreated = { task ->
            // Task was created successfully
            AppLogger.d("TasksFragment", getString(R.string.task_created_callback, task.id))
            // The ViewModel will automatically update the UI through the Flow
        }

        // Use BottomSheetManager to prevent duplicate openings
        BottomSheetManager.showBottomSheetIfNotActive(
            childFragmentManager,
            bottomSheet,
            CreateTaskBottomSheetFragment.TAG
        )
        AppLogger.methodExit("TasksFragment", "showCreateTaskBottomSheet")
    }

    private fun observeData() {
        AppLogger.methodEntry("TasksFragment", "observeData")

        // Observe display items and handle empty state
        viewLifecycleOwner.lifecycleScope.launch {
            taskViewModel.displayItems.collect { displayItems ->
                sectionedTasksAdapter.submitList(displayItems)
                AppLogger.d("TasksFragment", getString(R.string.tasks_updated, displayItems.size))
                // Force update empty state with a slight delay to ensure UI is ready
                binding.tasksRecyclerView.post {
                    updateEmptyState(displayItems.isEmpty())
                }
            }
        }

        // Update empty state messages and quick add hint (no tag filtering)
        updateEmptyStateMessages(null)
        updateQuickAddHint(null)
        
        AppLogger.methodExit("TasksFragment", "observeData")
    }

    private fun showTaskDetailBottomSheet(taskWithTag: TaskWithTag) {
        AppLogger.methodEntry("TasksFragment", "showTaskDetailBottomSheet", "taskId" to taskWithTag.task.id)
        val bottomSheet = TaskDetailBottomSheetFragment.newInstance(taskWithTag)

        bottomSheet.onDeleteTask = { task ->
            AppLogger.uiOperation("TasksFragment", getString(R.string.task_delete_requested, task.task.id))
            taskViewModel.deleteTask(task.task.id, requireContext())
        }

        bottomSheet.onTaskUpdated = { updatedTask ->
            // Task was updated successfully
            AppLogger.d("TasksFragment", getString(R.string.task_updated_callback, updatedTask.id))
            // The ViewModel will automatically update the UI through the Flow
        }

        // Use BottomSheetManager to prevent duplicate openings
        BottomSheetManager.showBottomSheetIfNotActive(
            childFragmentManager,
            bottomSheet,
            TaskDetailBottomSheetFragment.TAG
        )
        AppLogger.methodExit("TasksFragment", "showTaskDetailBottomSheet")
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        AppLogger.methodEntry("TasksFragment", "updateEmptyState", "isEmpty" to isEmpty)
        // Cancel any ongoing animations to prevent conflicts
        binding.tasksRecyclerView.animate().cancel()
        binding.emptyStateLayout.animate().cancel()
        
        if (isEmpty) {
            // Show empty state
            if (binding.emptyStateLayout.visibility != View.VISIBLE) {
                binding.tasksRecyclerView.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction {
                        binding.tasksRecyclerView.visibility = View.GONE
                        binding.emptyStateLayout.visibility = View.VISIBLE
                        binding.emptyStateLayout.alpha = 0f
                        binding.emptyStateLayout.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start()
                    }
                    .start()
                AppLogger.uiOperation("TasksFragment", getString(R.string.empty_state_shown))
            }
        } else {
            // Show task list
            if (binding.tasksRecyclerView.visibility != View.VISIBLE) {
                binding.emptyStateLayout.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction {
                        binding.emptyStateLayout.visibility = View.GONE
                        binding.tasksRecyclerView.visibility = View.VISIBLE
                        binding.tasksRecyclerView.alpha = 0f
                        binding.tasksRecyclerView.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start()
                    }
                    .start()
                AppLogger.uiOperation("TasksFragment", getString(R.string.task_list_shown))
            }
        }
        AppLogger.methodExit("TasksFragment", "updateEmptyState")
    }

    private fun updateEmptyStateMessages(tagName: String?) {
        AppLogger.methodEntry("TasksFragment", "updateEmptyStateMessages", "tagName" to tagName)
        val title = when (tagName) {
            null -> getString(R.string.no_tasks_found)
            else -> getString(R.string.no_tag_tasks_format, tagName)
        }

        val description = when (tagName) {
            null -> getString(R.string.no_tasks_available)
            else -> getString(R.string.no_tasks_category)
        }

        binding.emptyStateTitle.text = title
        binding.emptyStateDescription.text = description
        AppLogger.d("TasksFragment", getString(R.string.empty_state_messages_updated, title))
        AppLogger.methodExit("TasksFragment", "updateEmptyStateMessages")
    }

    private fun updateQuickAddHint(tagName: String?) {
        AppLogger.methodEntry("TasksFragment", "updateQuickAddHint", "tagName" to tagName)
        val hintText = when (tagName) {
            null -> getString(R.string.type_task)
            else -> getString(R.string.add_to_format, tagName)
        }
        binding.quickAddEditText.hint = hintText
        AppLogger.d("TasksFragment", getString(R.string.quick_add_hint_updated, hintText))
        AppLogger.methodExit("TasksFragment", "updateQuickAddHint")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AppLogger.d("TasksFragment", getString(R.string.on_destroy_view))
        // Clean up bottom sheets when fragment is destroyed
        BottomSheetManager.clearAllBottomSheets()
        _binding = null
    }

    // SectionedTasksAdapterCallback implementation
    override fun onTagHeaderClicked(tagId: Int, tagName: String) {
        AppLogger.uiOperation("TasksFragment", "Tag header clicked: $tagName ($tagId)")
        
        // Toggle accordion: if this tag is already expanded, collapse it; otherwise expand it
        val currentExpandedTagId = taskViewModel.expandedTagId.value
        val newExpandedTagId = if (currentExpandedTagId == tagId) null else tagId
        
        taskViewModel.setExpandedTag(newExpandedTagId)
        
        // Announce for accessibility
        val announcement = if (newExpandedTagId == null) "Collapsed" else "Expanded"
        @Suppress("DEPRECATION")
        binding.root.announceForAccessibility(announcement.toString())
    }

    override fun onTaskClicked(taskId: Int) {
        AppLogger.uiOperation("TasksFragment", getString(R.string.task_clicked, taskId))
        
        // Find the task and show detail bottom sheet
        viewLifecycleOwner.lifecycleScope.launch {
            taskViewModel.tasks.collect { tasks ->
                val taskWithTag = tasks.find { it.task.id == taskId }
                taskWithTag?.let { showTaskDetailBottomSheet(it) }
                return@collect
            }
        }
    }
}
