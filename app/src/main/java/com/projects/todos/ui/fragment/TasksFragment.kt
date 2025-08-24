package com.projects.todos.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.projects.todos.R
import com.projects.todos.data.UserPreferences
import com.projects.todos.data.database.TodoDatabase
import com.projects.todos.data.entity.TagEntity
import com.projects.todos.data.relation.TaskWithTag
import com.projects.todos.data.repository.TagRepository
import com.projects.todos.data.repository.TaskRepository
import com.projects.todos.databinding.FragmentTasksBinding
import com.projects.todos.ui.adapter.TaskAdapter
import com.projects.todos.ui.viewmodel.TaskViewModel
import com.projects.todos.utils.AppLogger
import com.projects.todos.utils.BottomSheetManager
import com.projects.todos.utils.ThemeUtils
import com.projects.todos.utils.hideKeyboard
import kotlinx.coroutines.launch

class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskViewModel: TaskViewModel
    private lateinit var taskAdapter: TaskAdapter
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

        setupViewModel()
        setupRecyclerView()
        setupFab()
        setupQuickAdd()
        setupKeyboardHiding()
        observeData()
        
        AppLogger.methodExit("TasksFragment", "onViewCreated")
    }


    private fun setupViewModel() {
        AppLogger.methodEntry("TasksFragment", "setupViewModel")
        val database = TodoDatabase.getDatabase(requireContext())
        val taskRepository = TaskRepository(database.taskDao())
        val tagRepository = TagRepository(database.tagDao())
        taskViewModel = TaskViewModel(taskRepository, tagRepository)
        userPreferences = UserPreferences(requireContext())
        AppLogger.d("TasksFragment", getString(R.string.view_model_initialized))
        AppLogger.methodExit("TasksFragment", "setupViewModel")
    }

    private fun setupRecyclerView() {
        AppLogger.methodEntry("TasksFragment", "setupRecyclerView")
        taskAdapter = TaskAdapter()
        taskAdapter.setOnTaskCompletionChangedListener { taskId, isCompleted ->
            AppLogger.uiOperation("TasksFragment", getString(R.string.task_completion_changed, taskId, isCompleted))
            taskViewModel.toggleTaskCompletion(taskId, isCompleted)
        }
        taskAdapter.setOnTaskFavoriteChangedListener { taskId, isFavorite ->
            AppLogger.uiOperation("TasksFragment", getString(R.string.task_favorite_changed, taskId, isFavorite))
            taskViewModel.toggleTaskFavorite(taskId, isFavorite)
        }
        taskAdapter.setOnTaskClickedListener { taskWithTag ->
            AppLogger.uiOperation("TasksFragment", getString(R.string.task_clicked, taskWithTag.task.id))
            showTaskDetailBottomSheet(taskWithTag)
        }
        binding.tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = taskAdapter
            // Add item decoration for better spacing
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
        // Observe tags and create chips dynamically
        lifecycleScope.launch {
            taskViewModel.tags.collect { tags ->
                createTagChips(tags)
                AppLogger.d("TasksFragment", getString(R.string.tags_updated, tags.size))
            }
        }

        // Observe tasks and handle empty state
        lifecycleScope.launch {
            taskViewModel.tasks.collect { tasks ->
                taskAdapter.submitList(tasks)
                AppLogger.d("TasksFragment", getString(R.string.tasks_updated, tasks.size))
                // Force update empty state with a slight delay to ensure UI is ready
                binding.tasksRecyclerView.post {
                    updateEmptyState(tasks.isEmpty())
                }
            }
        }

        // Observe selected tag name for empty state messages and quick add hint
        lifecycleScope.launch {
            taskViewModel.selectedTagName.collect { tagName ->
                updateEmptyStateMessages(tagName)
                updateQuickAddHint(tagName)
                AppLogger.d("TasksFragment", getString(R.string.selected_tag_updated, tagName))
            }
        }
        AppLogger.methodExit("TasksFragment", "observeData")
    }

    private fun createTagChips(tags: List<TagEntity>) {
        AppLogger.methodEntry("TasksFragment", "createTagChips", "tagCount" to tags.size)
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
                    taskViewModel.setSelectedTag(null, null)
                    AppLogger.uiOperation("TasksFragment", getString(R.string.all_tag_selected))
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
                        
                        taskViewModel.setSelectedTag(tag.id, tag.name)
                        AppLogger.uiOperation("TasksFragment", getString(R.string.tag_selected_ui, tag.name, tag.id))
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
                taskViewModel.setSelectedTag(tag.id, tag.name)
                AppLogger.d("TasksFragment", getString(R.string.auto_selected_general_ui))
            }
        }
        AppLogger.methodExit("TasksFragment", "createTagChips")
    }

    private fun createStyledChip(text: String): Chip {
        return ThemeUtils.createStyledChip(requireContext(), text)
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
}
