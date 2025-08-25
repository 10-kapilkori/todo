package com.projects.todos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.projects.todos.data.relation.TemplateWithTag
import com.projects.todos.databinding.ItemTemplateBinding
import com.projects.todos.utils.AppLogger
import com.projects.todos.utils.DateTimeUtils
import androidx.core.graphics.toColorInt
import com.projects.todos.R

interface TemplateAdapterCallback {
    fun onTemplateClicked(templateWithTag: TemplateWithTag)
    fun onUseTemplate(templateWithTag: TemplateWithTag)
    fun onMoreOptions(templateWithTag: TemplateWithTag)
}

class TemplateAdapter(
    private val callback: TemplateAdapterCallback
) : ListAdapter<TemplateWithTag, TemplateAdapter.TemplateViewHolder>(TemplateDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val binding = ItemTemplateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TemplateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TemplateViewHolder(
        private val binding: ItemTemplateBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(templateWithTag: TemplateWithTag) {
            val template = templateWithTag.template
            val tag = templateWithTag.tag

            binding.templateTitle.text = template.title
            
            // Show description if not empty
            if (template.description.isNotBlank()) {
                binding.templateDescription.text = template.description
                binding.templateDescription.visibility = android.view.View.VISIBLE
            } else {
                binding.templateDescription.visibility = android.view.View.GONE
            }

            // Set tag chip
            binding.templateTag.text = tag.name
            try {
                val tagColor = tag.colorHex.toColorInt()
                val tagBackground = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 12f * itemView.context.resources.displayMetrics.density
                    setColor(tagColor)
                }
                binding.templateTag.background = tagBackground
                binding.templateTag.setTextColor(android.graphics.Color.WHITE)
            } catch (e: Exception) {
                AppLogger.e("TemplateAdapter", "Failed to parse tag color: ${tag.colorHex}, e -> $e")
                binding.templateTag.background = ContextCompat.getDrawable(itemView.context, R.drawable.tag_background)
                binding.templateTag.setTextColor(android.graphics.Color.WHITE)
            }

            // Set click listeners
            binding.root.setOnClickListener {
                callback.onTemplateClicked(templateWithTag)
            }

            binding.btnUseTemplate.setOnClickListener {
                callback.onUseTemplate(templateWithTag)
            }

            binding.btnMore.setOnClickListener {
                callback.onMoreOptions(templateWithTag)
            }
        }
    }

    private class TemplateDiffCallback : DiffUtil.ItemCallback<TemplateWithTag>() {
        override fun areItemsTheSame(oldItem: TemplateWithTag, newItem: TemplateWithTag): Boolean {
            return oldItem.template.id == newItem.template.id
        }

        override fun areContentsTheSame(oldItem: TemplateWithTag, newItem: TemplateWithTag): Boolean {
            return oldItem.template == newItem.template && oldItem.tag == newItem.tag
        }
    }
}
