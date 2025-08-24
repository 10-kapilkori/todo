package com.projects.todos.ui.fragment

import android.os.Bundle
import android.view.ContextThemeWrapper
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
import com.projects.todos.utils.BottomSheetManager
import com.projects.todos.utils.KeyboardUtils
import com.projects.todos.utils.ThemeUtils
import kotlinx.coroutines.flow.first
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
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupRecyclerView()
        setupWelcomeMessage()
        setupFab()
        setupQuickAdd()
        setupKeyboardHiding()
        observeData()
    }

    private fun setupViewModel() {
        val database = TodoDatabase.getDatabase(requireContext())
        val taskRepository = TaskRepository(database.taskDao())
        val tagRepository = TagRepository(database.tagDao())
        taskViewModel = TaskViewModel(taskRepository, tagRepository)
        userPreferences = UserPreferences(requireContext())
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter()
        taskAdapter.setOnTaskCompletionChangedListener { taskId, isCompleted ->
            taskViewModel.toggleTaskCompletion(taskId, isCompleted)
        }
        taskAdapter.setOnTaskFavoriteChangedListener { taskId, isFavorite ->
            taskViewModel.toggleTaskFavorite(taskId, isFavorite)
        }
        taskAdapter.setOnTaskClickedListener { taskWithTag ->
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
        }
    }

    private fun setupWelcomeMessage() {
        lifecycleScope.launch {
            val userName = userPreferences.userName.first()
            val welcomeMessage = if (!userName.isNullOrBlank()) {
                "Welcome, $userName! 🎉"
            } else {
                "Welcome to Todo! 🎉"
            }
            binding.welcomeText.text = welcomeMessage
        }
    }

    private fun setupFab() {
        binding.fabAddTask.setOnClickListener {
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
    }

    private fun setupQuickAdd() {
        // Handle Add button click
        binding.quickAddButton.setOnClickListener {
            addQuickTask()
        }

        // Handle Enter key press
        binding.quickAddEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addQuickTask()
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
        KeyboardUtils.hideKeyboardAndClearFocus(requireContext(), binding.quickAddEditText)
    }

    private fun clearFocus() {
        KeyboardUtils.clearFocus(binding.quickAddEditText)
    }

    private fun addQuickTask() {
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
                showQuickFeedback("Task added! ✨")
            }
        }
    }

    private fun showQuickFeedback(message: String) {
        val snackbar = com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        )
        snackbar.setAction("Undo") {
            // TODO: Implement undo functionality
        }
        snackbar.show()
    }

    private fun showCreateTaskBottomSheet() {
        val bottomSheet = CreateTaskBottomSheetFragment.newInstance()

        bottomSheet.onTaskCreated = { task ->
            // Task was created successfully
            // The ViewModel will automatically update the UI through the Flow
        }

        // Use BottomSheetManager to prevent duplicate openings
        BottomSheetManager.showBottomSheetIfNotActive(
            childFragmentManager,
            bottomSheet,
            CreateTaskBottomSheetFragment.TAG
        )
    }

    private fun observeData() {
        // Observe tags and create chips dynamically
        lifecycleScope.launch {
            taskViewModel.tags.collect { tags ->
                createTagChips(tags)
            }
        }

        // Observe tasks and handle empty state
        lifecycleScope.launch {
            taskViewModel.tasks.collect { tasks ->
                taskAdapter.submitList(tasks)
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
            }
        }
    }

    private fun createTagChips(tags: List<TagEntity>) {
        // Clear all existing chips
        val chipGroup = binding.filterChipGroup
        chipGroup.removeAllViews()

        // Create "All" chip first
        val allChip = createStyledChip("All").apply {
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
            }
        }
    }

    private fun createStyledChip(text: String): Chip {
        return ThemeUtils.createStyledChip(requireContext(), text)
    }

    private fun showTaskDetailBottomSheet(taskWithTag: TaskWithTag) {
        val bottomSheet = TaskDetailBottomSheetFragment.newInstance(taskWithTag)

        bottomSheet.onDeleteTask = { task ->
            taskViewModel.deleteTask(task.task.id)
        }

        bottomSheet.onTaskUpdated = { updatedTask ->
            // Task was updated successfully
            // The ViewModel will automatically update the UI through the Flow
        }

        // Use BottomSheetManager to prevent duplicate openings
        BottomSheetManager.showBottomSheetIfNotActive(
            childFragmentManager,
            bottomSheet,
            TaskDetailBottomSheetFragment.TAG
        )
    }

    private fun updateEmptyState(isEmpty: Boolean) {
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
            }
        }
    }

    private fun updateEmptyStateMessages(tagName: String?) {
        val title = when (tagName) {
            null -> "No tasks found"
            else -> "No $tagName tasks yet"
        }

        val description = when (tagName) {
            null -> "No tasks available yet."
            else -> "No tasks available in this category yet."
        }

        binding.emptyStateTitle.text = title
        binding.emptyStateDescription.text = description
    }

    private fun updateQuickAddHint(tagName: String?) {
        val hintText = when (tagName) {
            null -> "Type a task..."
            else -> "Add to $tagName..."
        }
        binding.quickAddEditText.hint = hintText
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up bottom sheets when fragment is destroyed
        BottomSheetManager.clearAllBottomSheets()
        _binding = null
    }
}
