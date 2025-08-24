package com.projects.todos.ui.adapter

import android.content.Context
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

class TaskAdapter : ListAdapter<TaskWithTag, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    private var onTaskCompletionChanged: ((Int, Boolean) -> Unit)? = null
    private var onTaskFavoriteChanged: ((Int, Boolean) -> Unit)? = null
    private var onTaskClicked: ((TaskWithTag) -> Unit)? = null

    fun setOnTaskCompletionChangedListener(listener: (Int, Boolean) -> Unit) {
        onTaskCompletionChanged = listener
    }

    fun setOnTaskFavoriteChangedListener(listener: (Int, Boolean) -> Unit) {
        onTaskFavoriteChanged = listener
    }

    fun setOnTaskClickedListener(listener: (TaskWithTag) -> Unit) {
        onTaskClicked = listener
    }

    fun updateTaskCompletion(taskId: Int, isCompleted: Boolean) {
        val currentList = currentList.toMutableList()
        val index = currentList.indexOfFirst { it.task.id == taskId }
        if (index != -1) {
            val updatedTask = currentList[index].task.copy(isCompleted = isCompleted)
            currentList[index] = currentList[index].copy(task = updatedTask)
            submitList(currentList)
        }
    }

    fun updateTaskFavorite(taskId: Int, isFavorite: Boolean) {
        val currentList = currentList.toMutableList()
        val index = currentList.indexOfFirst { it.task.id == taskId }
        if (index != -1) {
            val updatedTask = currentList[index].task.copy(isFavorite = isFavorite)
            currentList[index] = currentList[index].copy(task = updatedTask)
            submitList(currentList)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewAttachedToWindow(holder: TaskViewHolder) {
        super.onViewAttachedToWindow(holder)
        // Add entrance animation
        holder.itemView.startAnimation(
            AnimationUtils.loadAnimation(holder.itemView.context, R.anim.item_entrance)
        )
    }

    override fun onViewDetachedFromWindow(holder: TaskViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.itemView.clearAnimation()
    }

    inner class TaskViewHolder(
        private val binding: ItemTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentTaskId: Int = -1

        fun bind(taskWithTag: TaskWithTag) {
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
                    val tagColor = android.graphics.Color.parseColor(tag.colorHex)
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
                    taskTag.background = ContextCompat.getDrawable(itemView.context, R.drawable.tag_background)
                    taskTag.setTextColor(android.graphics.Color.WHITE)
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
                        onTaskCompletionChanged?.invoke(task.id, isChecked)
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
                        onTaskFavoriteChanged?.invoke(task.id, newFavoriteState)
                    }
                }

                // Set up item click listener (excluding checkbox and favorite button)
                itemView.setOnClickListener {
                    // Only process if this is still the correct task
                    if (currentTaskId == task.id) {
                        onTaskClicked?.invoke(taskWithTag)
                    }
                }
            }
        }

        private fun updateCompletionUI(isCompleted: Boolean) {
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
                }
            }
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
                   oldItem.tag.id == newItem.tag.id
        }
    }
    
    private fun isColorDark(color: Int): Boolean {
        val red = android.graphics.Color.red(color)
        val green = android.graphics.Color.green(color)
        val blue = android.graphics.Color.blue(color)
        
        // Calculate relative luminance
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255
        return luminance < 0.5
    }
}
