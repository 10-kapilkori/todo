package com.projects.todos.ui.adapter

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.projects.todos.R
import com.projects.todos.data.relation.TaskWithTag
import com.projects.todos.databinding.ItemTaskBinding
import com.projects.todos.utils.AppLogger
import com.projects.todos.utils.AppLogger.getString
import com.projects.todos.utils.DateTimeUtils
import androidx.core.graphics.toColorInt

interface TaskAdapterCallback {
    fun onTaskCompletionChanged(taskId: Int, isCompleted: Boolean)
    fun onTaskFavoriteChanged(taskId: Int, isFavorite: Boolean)
    fun onTaskClicked(taskWithTag: TaskWithTag)
}

class TaskAdapter(
    private val callback: TaskAdapterCallback
) : ListAdapter<TaskWithTag, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    fun updateTaskCompletion(taskId: Int, isCompleted: Boolean) {
        AppLogger.methodEntry(
            "TaskAdapter", "updateTaskCompletion",
            "taskId" to taskId,
            "isCompleted" to isCompleted
        )
        val currentList = currentList.toMutableList()
        val index = currentList.indexOfFirst { it.task.id == taskId }
        if (index != -1) {
            val updatedTask = currentList[index].task.copy(isCompleted = isCompleted)
            currentList[index] = currentList[index].copy(task = updatedTask)
            submitList(currentList)
            AppLogger.d(
                "TaskAdapter",
                getString(R.string.task_completion_updated, taskId, isCompleted)
            )
        } else {
            AppLogger.w("TaskAdapter", getString(R.string.task_not_found_update, taskId))
        }
        AppLogger.methodExit("TaskAdapter", "updateTaskCompletion")
    }

    fun updateTaskFavorite(taskId: Int, isFavorite: Boolean) {
        AppLogger.methodEntry(
            "TaskAdapter", "updateTaskFavorite",
            "taskId" to taskId,
            "isFavorite" to isFavorite
        )
        val currentList = currentList.toMutableList()
        val index = currentList.indexOfFirst { it.task.id == taskId }
        if (index != -1) {
            val updatedTask = currentList[index].task.copy(isFavorite = isFavorite)
            currentList[index] = currentList[index].copy(task = updatedTask)
            submitList(currentList)
            AppLogger.d(
                "TaskAdapter",
                getString(R.string.task_favorite_updated, taskId, isFavorite)
            )
        } else {
            AppLogger.w("TaskAdapter", getString(R.string.task_not_found_favorite, taskId))
        }
        AppLogger.methodExit("TaskAdapter", "updateTaskFavorite")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        AppLogger.methodEntry("TaskAdapter", "onCreateViewHolder")
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        AppLogger.methodExit("TaskAdapter", "onCreateViewHolder")
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        AppLogger.methodEntry("TaskAdapter", "onBindViewHolder", "position" to position)
        holder.bind(getItem(position))
        AppLogger.methodExit("TaskAdapter", "onBindViewHolder")
    }

    override fun onViewAttachedToWindow(holder: TaskViewHolder) {
        super.onViewAttachedToWindow(holder)
        // Add entrance animation
        holder.itemView.startAnimation(
            AnimationUtils.loadAnimation(holder.itemView.context, R.anim.item_entrance)
        )
        AppLogger.d("TaskAdapter", getString(R.string.view_attached_animation))
    }

    override fun onViewDetachedFromWindow(holder: TaskViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.itemView.clearAnimation()
        AppLogger.d("TaskAdapter", getString(R.string.view_detached))
    }

    inner class TaskViewHolder(
        private val binding: ItemTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentTaskId: Int = -1

        fun bind(taskWithTag: TaskWithTag) {
            AppLogger.methodEntry("TaskViewHolder", "bind", "taskId" to taskWithTag.task.id)
            val task = taskWithTag.task
            val tag = taskWithTag.tag

            // Store current task ID to prevent wrong callbacks
            currentTaskId = task.id

            binding.apply {
                taskTitle.text = task.title
                taskDescription.text = task.description
                taskTag.text = tag.name
                
                // Use the tag's actual color from colorHex
                try {
                    val tagColor = tag.colorHex.toColorInt()
                    val tagBackground = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 12f * itemView.context.resources.displayMetrics.density
                        setColor(tagColor)
                    }
                    taskTag.background = tagBackground
                    
                    // Always use white text color for better contrast
                    taskTag.setTextColor(android.graphics.Color.WHITE)
                } catch (e: Exception) {
                    // Fallback to default background if color parsing fails
                    AppLogger.w(
                        "TaskViewHolder",
                        getString(R.string.failed_parse_color, tag.colorHex)
                    )
                    taskTag.background = ContextCompat.getDrawable(itemView.context, R.drawable.tag_background)
                    taskTag.setTextColor(android.graphics.Color.WHITE)
                }

                // Handle due date display
                task.dueDateTime?.let { dueDateTime ->
                    taskDueDate.text = getString(
                        R.string.due_format,
                        DateTimeUtils.getRelativeTimeString(dueDateTime)
                    )
                    taskDueDate.visibility = android.view.View.VISIBLE

                    // Set text color based on whether task is overdue
                    if (DateTimeUtils.isOverdue(dueDateTime) && !task.isCompleted) {
                        taskDueDate.setTextColor(
                            ContextCompat.getColor(
                                itemView.context,
                                R.color.error
                            )
                        )
                        AppLogger.d("TaskViewHolder", getString(R.string.task_overdue, task.id))
                    } else {
                        taskDueDate.setTextColor(
                            ContextCompat.getColor(
                                itemView.context,
                                android.R.color.darker_gray
                            )
                        )
                    }
                } ?: run {
                    taskDueDate.visibility = android.view.View.GONE
                }

                // Clear previous listeners to prevent wrong callbacks
                taskCheckbox.setOnCheckedChangeListener(null)
                taskFavorite.setOnClickListener(null)
                itemView.setOnClickListener(null)

                // Set completion status with animation
                taskCheckbox.isChecked = task.isCompleted
                updateCompletionUI(task.isCompleted)

                // Set favorite status
                taskFavorite.isSelected = task.isFavorite

                // Set up click listeners with haptic feedback
                taskCheckbox.setOnCheckedChangeListener { _, isChecked ->
                    // Only process if this is still the correct task
                    if (currentTaskId == task.id) {
                        // Haptic feedback
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            itemView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        } else {
                            @Suppress("DEPRECATION")
                            itemView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                        
                        // Animate the completion change
                        updateCompletionUI(isChecked)
                        AppLogger.uiOperation(
                            "TaskViewHolder",
                            getString(R.string.task_completion_toggled, task.id, isChecked)
                        )
                        callback.onTaskCompletionChanged(task.id, isChecked)
                    }
                }

                taskFavorite.setOnClickListener {
                    // Only process if this is still the correct task
                    if (currentTaskId == task.id) {
                        // Haptic feedback
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            itemView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        } else {
                            @Suppress("DEPRECATION")
                            itemView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                        
                        val newFavoriteState = !task.isFavorite
                        AppLogger.uiOperation(
                            "TaskViewHolder",
                            getString(R.string.task_favorite_toggled, task.id, newFavoriteState)
                        )
                        callback.onTaskFavoriteChanged(task.id, newFavoriteState)
                    }
                }

                // Set up item click listener (excluding checkbox and favorite button)
                itemView.setOnClickListener {
                    // Only process if this is still the correct task
                    if (currentTaskId == task.id) {
                        AppLogger.uiOperation(
                            "TaskViewHolder",
                            getString(R.string.task_clicked, task.id)
                        )
                        callback.onTaskClicked(taskWithTag)
                    }
                }
            }
            AppLogger.methodExit("TaskViewHolder", "bind")
        }

        private fun updateCompletionUI(isCompleted: Boolean) {
            AppLogger.methodEntry(
                "TaskViewHolder",
                "updateCompletionUI",
                "isCompleted" to isCompleted
            )
            binding.apply {
                if (isCompleted) {
                    // Animate strike-through effect
                    taskTitle.animate()
                        .alpha(0.6f)
                        .setDuration(200)
                        .start()
                    taskDescription.animate()
                        .alpha(0.6f)
                        .setDuration(200)
                        .start()
                    
                    taskTitle.paintFlags = taskTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                    taskDescription.paintFlags = taskDescription.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                    AppLogger.d("TaskViewHolder", getString(R.string.task_marked_completed))
                } else {
                    // Animate back to normal
                    taskTitle.animate()
                        .alpha(1.0f)
                        .setDuration(200)
                        .start()
                    taskDescription.animate()
                        .alpha(1.0f)
                        .setDuration(200)
                        .start()
                    
                    taskTitle.paintFlags = taskTitle.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    taskDescription.paintFlags = taskDescription.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    AppLogger.d("TaskViewHolder", getString(R.string.task_marked_incomplete))
                }
            }
            AppLogger.methodExit("TaskViewHolder", "updateCompletionUI")
        }
    }

    private class TaskDiffCallback : DiffUtil.ItemCallback<TaskWithTag>() {
        override fun areItemsTheSame(oldItem: TaskWithTag, newItem: TaskWithTag): Boolean {
            return oldItem.task.id == newItem.task.id
        }

        override fun areContentsTheSame(oldItem: TaskWithTag, newItem: TaskWithTag): Boolean {
            return oldItem.task.isCompleted == newItem.task.isCompleted &&
                   oldItem.task.isFavorite == newItem.task.isFavorite &&
                   oldItem.task.title == newItem.task.title &&
                   oldItem.task.description == newItem.task.description &&
                    oldItem.task.dueDateTime == newItem.task.dueDateTime &&
                   oldItem.tag.id == newItem.tag.id
        }
    }
}
