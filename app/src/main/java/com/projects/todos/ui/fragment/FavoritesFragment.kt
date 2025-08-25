package com.projects.todos.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.projects.todos.data.relation.TaskWithTag
import com.projects.todos.databinding.FragmentFavoritesBinding
import com.projects.todos.ui.adapter.TaskAdapter
import com.projects.todos.ui.adapter.TaskAdapterCallback
import com.projects.todos.ui.fragment.CreateTaskBottomSheetFragment
import com.projects.todos.ui.fragment.TaskDetailBottomSheetFragment
import com.projects.todos.ui.viewmodel.TaskViewModel
import com.projects.todos.utils.BottomSheetManager

import kotlinx.coroutines.launch

class FavoritesFragment : Fragment(), TaskAdapterCallback {
    
    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    
    private val taskViewModel: TaskViewModel by activityViewModels()
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
        
        setupRecyclerView()
        setupFab()
        observeData()
    }
    
    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(this)
        
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
        viewLifecycleOwner.lifecycleScope.launch {
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

    // TaskAdapterCallback implementation
    override fun onTaskCompletionChanged(taskId: Int, isCompleted: Boolean) {
        taskViewModel.toggleTaskCompletion(taskId, isCompleted)
    }

    override fun onTaskFavoriteChanged(taskId: Int, isFavorite: Boolean) {
        taskViewModel.toggleTaskFavorite(taskId, isFavorite)
    }

    override fun onTaskClicked(taskWithTag: TaskWithTag) {
        showTaskDetailBottomSheet(taskWithTag)
    }
}
