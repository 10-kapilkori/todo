package com.projects.todos.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.projects.todos.R
import com.projects.todos.data.DisplayItem
import com.projects.todos.data.relation.TaskWithTag
import com.projects.todos.databinding.ItemTagHeaderBinding
import com.projects.todos.databinding.ItemTaskTitleOnlyBinding
import com.projects.todos.utils.AppLogger

interface SectionedTasksAdapterCallback {
    fun onTagHeaderClicked(tagId: Int, tagName: String)
    fun onTaskClicked(taskId: Int)
}

class SectionedTasksAdapter(
    private val callback: SectionedTasksAdapterCallback
) : ListAdapter<DisplayItem, RecyclerView.ViewHolder>(SectionedDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_TASK = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DisplayItem.TagHeaderItem -> VIEW_TYPE_HEADER
            is DisplayItem.TaskItem -> VIEW_TYPE_TASK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemTagHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                TagHeaderViewHolder(binding, callback)
            }
            VIEW_TYPE_TASK -> {
                val binding = ItemTaskTitleOnlyBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                TaskViewHolder(binding, callback)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is DisplayItem.TagHeaderItem -> (holder as TagHeaderViewHolder).bind(item)
            is DisplayItem.TaskItem -> (holder as TaskViewHolder).bind(item)
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        // Add entrance animation
        holder.itemView.startAnimation(
            AnimationUtils.loadAnimation(holder.itemView.context, R.anim.item_entrance)
        )
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.itemView.clearAnimation()
    }

    class TagHeaderViewHolder(
        private val binding: ItemTagHeaderBinding,
        private val callback: SectionedTasksAdapterCallback
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DisplayItem.TagHeaderItem) {
            binding.apply {
                tagName.text = item.tagName
                tagCount.text = item.count.toString()
                
                // Set colors from tag
                val color = try {
                    Color.parseColor(item.colorHex)
                } catch (e: IllegalArgumentException) {
                    Color.GRAY
                }
                
                tagIcon.setColorFilter(color)
                
                // Set content description for accessibility
                val state = if (item.isExpanded) "expanded" else "collapsed"
                root.contentDescription = "${item.tagName}, $state, ${item.count} items"
                
                // Rotate chevron based on expanded state with animation
                val targetRotation = if (item.isExpanded) 180f else 0f
                chevronIcon.animate()
                    .rotation(targetRotation)
                    .setDuration(200)
                    .start()
                
                // Show/hide empty text
                emptyText.visibility = if (item.isExpanded && item.count == 0) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
                
                // Set click listener
                root.setOnClickListener {
                    AppLogger.uiOperation("TagHeaderViewHolder", "Header clicked: ${item.tagName}")
                    callback.onTagHeaderClicked(item.tagId, item.tagName)
                }
            }
        }
    }

    class TaskViewHolder(
        private val binding: ItemTaskTitleOnlyBinding,
        private val callback: SectionedTasksAdapterCallback
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DisplayItem.TaskItem) {
            binding.apply {
                taskTitle.text = item.title
                
                // Set color dot from tag
                val color = try {
                    Color.parseColor(item.colorHex)
                } catch (e: IllegalArgumentException) {
                    Color.GRAY
                }
                colorDot.setBackgroundColor(color)
                
                // Set content description for accessibility
                root.contentDescription = "Task: ${item.title}"
                
                // Set click listener
                root.setOnClickListener {
                    AppLogger.uiOperation("TaskViewHolder", "Task clicked: ${item.taskId}")
                    callback.onTaskClicked(item.taskId)
                }
            }
        }
    }

    private class SectionedDiffCallback : DiffUtil.ItemCallback<DisplayItem>() {
        override fun areItemsTheSame(oldItem: DisplayItem, newItem: DisplayItem): Boolean {
            return when {
                oldItem is DisplayItem.TagHeaderItem && newItem is DisplayItem.TagHeaderItem ->
                    oldItem.tagId == newItem.tagId
                oldItem is DisplayItem.TaskItem && newItem is DisplayItem.TaskItem ->
                    oldItem.taskId == newItem.taskId
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: DisplayItem, newItem: DisplayItem): Boolean {
            return oldItem == newItem
        }
    }
}
