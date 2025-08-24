package com.projects.todos.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.projects.todos.data.database.TodoDatabase
import com.projects.todos.data.relation.TaskWithTag
import com.projects.todos.data.repository.TaskRepository
import com.projects.todos.databinding.FragmentFavoritesBinding
import com.projects.todos.ui.adapter.TaskAdapter
import com.projects.todos.ui.fragment.CreateTaskBottomSheetFragment
import com.projects.todos.ui.fragment.TaskDetailBottomSheetFragment
import com.projects.todos.ui.viewmodel.TaskViewModel
import com.projects.todos.utils.BottomSheetManager
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {
    
    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var taskViewModel: TaskViewModel
    private lateinit var taskAdapter: TaskAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModel()
        setupRecyclerView()
        setupFab()
        observeData()
    }
    
    private fun setupViewModel() {
        val database = TodoDatabase.getDatabase(requireContext())
        val taskRepository = TaskRepository(database.taskDao())
        // We don't need TagRepository for favorites, but TaskViewModel requires it
        val tagRepository = com.projects.todos.data.repository.TagRepository(database.tagDao())
        taskViewModel = TaskViewModel(taskRepository, tagRepository)
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
        binding.favoritesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = taskAdapter
        }
    }
    
    private fun setupFab() {
        binding.fabAddTask.setOnClickListener {
            showCreateTaskBottomSheet()
        }
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
        // Observe favorite tasks
        lifecycleScope.launch {
            taskViewModel.favoriteTasks.collect { favoriteTasks ->
                taskAdapter.submitList(favoriteTasks)
            }
        }
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
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up bottom sheets when fragment is destroyed
        BottomSheetManager.clearAllBottomSheets()
        _binding = null
    }
}
