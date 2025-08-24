package com.projects.todos.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.projects.todos.R
import com.projects.todos.data.database.TodoDatabase
import com.projects.todos.data.entity.TagEntity
import com.projects.todos.data.repository.TagRepository
import com.projects.todos.data.repository.TaskRepository
import com.projects.todos.databinding.ActivityTagManagementBinding
import com.projects.todos.databinding.ItemTagManagementBinding
import com.projects.todos.databinding.ItemTagSelectorBinding
import com.projects.todos.utils.AppLogger
import com.projects.todos.utils.DateTimeUtils
import com.projects.todos.utils.setupKeyboardDismissOnOutsideClick
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TagManagementActivity : BaseActivity() {

    private lateinit var binding: ActivityTagManagementBinding
    private lateinit var tagRepository: TagRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var tagAdapter: TagAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set soft input mode for edge-to-edge friendly behavior
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        
        binding = ActivityTagManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppLogger.methodEntry("TagManagementActivity", "onCreate")

        setupToolbar()
        setupRepository()
        setupRecyclerView()
        setupFab()
        observeTags()
        setupKeyboardDismiss()

        AppLogger.methodExit("TagManagementActivity", "onCreate")
    }
    
    private fun setupKeyboardDismiss() {
        // Setup keyboard dismiss on outside click
        binding.root.setupKeyboardDismissOnOutsideClick()
    }
    
    private fun setupToolbar() {
        AppLogger.methodEntry("TagManagementActivity", "setupToolbar")
        binding.toolbar.setNavigationOnClickListener {
            AppLogger.uiOperation("TagManagementActivity", "Toolbar close clicked")
            finish()
        }
        AppLogger.methodExit("TagManagementActivity", "setupToolbar")
    }

    private fun setupRepository() {
        AppLogger.methodEntry("TagManagementActivity", "setupRepository")
        val database = TodoDatabase.getDatabase(this)
        tagRepository = TagRepository(database.tagDao())
        taskRepository = TaskRepository(database.taskDao())
        AppLogger.methodExit("TagManagementActivity", "setupRepository")
    }

    private fun setupRecyclerView() {
        AppLogger.methodEntry("TagManagementActivity", "setupRecyclerView")
        tagAdapter = TagAdapter(
            onEditClick = { tag -> showEditTagDialog(tag) },
            onDeleteClick = { tag -> showDeleteTagDialog(tag) },
            onLongPress = { tag -> showDeleteTagDialog(tag) }
        )
        
        binding.tagsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@TagManagementActivity)
            adapter = tagAdapter
        }
        AppLogger.methodExit("TagManagementActivity", "setupRecyclerView")
    }

    private fun setupFab() {
        AppLogger.methodEntry("TagManagementActivity", "setupFab")
        binding.fabAddTag.setOnClickListener {
            AppLogger.uiOperation("TagManagementActivity", "FAB clicked")
            showAddTagDialog()
        }
        AppLogger.methodExit("TagManagementActivity", "setupFab")
    }

    private fun observeTags() {
        AppLogger.methodEntry("TagManagementActivity", "observeTags")
        lifecycleScope.launch {
            tagRepository.getAllTags().collectLatest { tags ->
                tagAdapter.submitList(tags)
                updateEmptyState(tags.isEmpty())
                AppLogger.d("TagManagementActivity", "Tags updated: ${tags.size} tags")
            }
        }
        AppLogger.methodExit("TagManagementActivity", "observeTags")
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        AppLogger.methodEntry("TagManagementActivity", "updateEmptyState", "isEmpty" to isEmpty)
        if (isEmpty) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.tagsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.tagsRecyclerView.visibility = View.VISIBLE
        }
        AppLogger.methodExit("TagManagementActivity", "updateEmptyState")
    }

    private fun showAddTagDialog() {
        AppLogger.methodEntry("TagManagementActivity", "showAddTagDialog")
        showTagDialog(null)
        AppLogger.methodExit("TagManagementActivity", "showAddTagDialog")
    }

    private fun showEditTagDialog(tag: TagEntity) {
        AppLogger.methodEntry("TagManagementActivity", "showEditTagDialog", "tagId" to tag.id)
        showTagDialog(tag)
        AppLogger.methodExit("TagManagementActivity", "showEditTagDialog")
    }

    private fun showTagDialog(tag: TagEntity?) {
        AppLogger.methodEntry("TagManagementActivity", "showTagDialog", "tag" to tag?.id)
        
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_tag_edit, null)
        val nameEditText = dialogView.findViewById<TextInputEditText>(R.id.tagNameEditText)
        val colorButton = dialogView.findViewById<View>(R.id.colorButton)

        var selectedColor = "#FF5722" // Default color

        // Pre-fill with existing tag data if editing
        if (tag != null) {
            nameEditText.setText(tag.name)
            selectedColor = tag.colorHex
            colorButton.setBackgroundColor(Color.parseColor(selectedColor))
        } else {
            colorButton.setBackgroundColor(Color.parseColor(selectedColor))
        }

        // Color picker
        colorButton.setOnClickListener {
            showColorPickerDialog { color ->
                selectedColor = color
                colorButton.setBackgroundColor(Color.parseColor(color))
            }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (tag == null) getString(R.string.add_tag) else getString(R.string.edit_tag))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val tagName = nameEditText.text.toString().trim()
                if (tagName.isNotEmpty()) {
                    lifecycleScope.launch {
                        if (tag == null) {
                            // Add new tag
                            val newTag = TagEntity(
                                name = tagName,
                                colorHex = selectedColor
                            )
                            tagRepository.insertTag(newTag)
                            showSnackbar(getString(R.string.tag_added_successfully))
                            AppLogger.d("TagManagementActivity", "Tag added: $tagName")
                        } else {
                            // Update existing tag
                            val updatedTag = tag.copy(
                                name = tagName,
                                colorHex = selectedColor,
                                updatedAt = System.currentTimeMillis()
                            )
                            tagRepository.updateTag(updatedTag)
                            showSnackbar(getString(R.string.tag_updated_successfully))
                            AppLogger.d("TagManagementActivity", "Tag updated: $tagName")
                        }
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        // Handle Enter key press
        nameEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val tagName = nameEditText.text.toString().trim()
                if (tagName.isNotEmpty()) {
                    lifecycleScope.launch {
                        if (tag == null) {
                            val newTag = TagEntity(
                                name = tagName,
                                colorHex = selectedColor
                            )
                            tagRepository.insertTag(newTag)
                            showSnackbar(getString(R.string.tag_added_successfully))
                            AppLogger.d("TagManagementActivity", "Tag added via Enter key: $tagName")
                        } else {
                            val updatedTag = tag.copy(
                                name = tagName,
                                colorHex = selectedColor,
                                updatedAt = System.currentTimeMillis()
                            )
                            tagRepository.updateTag(updatedTag)
                            showSnackbar(getString(R.string.tag_updated_successfully))
                            AppLogger.d("TagManagementActivity", "Tag updated via Enter key: $tagName")
                        }
                    }
                    dialog.dismiss()
                    return@setOnEditorActionListener true
                }
            }
            false
        }

        dialog.show()
        AppLogger.methodExit("TagManagementActivity", "showTagDialog")
    }

    private fun showColorPickerDialog(onColorSelected: (String) -> Unit) {
        AppLogger.methodEntry("TagManagementActivity", "showColorPickerDialog")
        
        val colors = arrayOf(
            "#FF5722", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
            "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50",
            "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800"
        )

        val colorNames = arrayOf(
            "Red", "Pink", "Purple", "Deep Purple", "Indigo",
            "Blue", "Light Blue", "Cyan", "Teal", "Green",
            "Light Green", "Lime", "Yellow", "Amber", "Orange"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.select_tag_color))
            .setItems(colorNames) { _, which ->
                val selectedColor = colors[which]
                onColorSelected(selectedColor)
                AppLogger.d("TagManagementActivity", "Color selected: $selectedColor")
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
        
        AppLogger.methodExit("TagManagementActivity", "showColorPickerDialog")
    }

    private fun showDeleteTagDialog(tag: TagEntity) {
        AppLogger.methodEntry("TagManagementActivity", "showDeleteTagDialog", "tagId" to tag.id)
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Check if this is the last tag
                val tagCount = tagRepository.getTagCount()
                if (tagCount <= 1) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        showLastTagWarning()
                    }
                    return@launch
                }
                
                // Get task count for this tag
                val taskCount = tagRepository.getTaskCountForTag(tag.id)
                
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (taskCount == 0) {
                        // No tasks associated, safe to delete directly
                        showSimpleDeleteConfirmation(tag)
                    } else {
                        // Tasks associated, show safe deletion options
                        showSafeDeleteDialog(tag, taskCount)
                    }
                }
            } catch (e: Exception) {
                AppLogger.error("TagManagementActivity", "showDeleteTagDialog", e, "tagId: ${tag.id}")
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    showSnackbar("Error: ${e.message}")
                }
            }
        }
        
        AppLogger.methodExit("TagManagementActivity", "showDeleteTagDialog")
    }
    
    private fun showLastTagWarning() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.cannot_delete_last_tag))
            .setMessage(getString(R.string.cannot_delete_last_tag_message))
            .setPositiveButton(getString(R.string.ok), null)
            .show()
    }
    
    private fun showSimpleDeleteConfirmation(tag: TagEntity) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete))
            .setMessage(getString(R.string.delete_tag_confirmation))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        tagRepository.deleteTagById(tag.id)
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            showSnackbar(getString(R.string.tag_deleted_successfully))
                            AppLogger.d("TagManagementActivity", "Tag deleted: ${tag.name}")
                        }
                    } catch (e: Exception) {
                        AppLogger.error("TagManagementActivity", "deleteTag", e, "tagId: ${tag.id}")
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            showSnackbar("Error: ${e.message}")
                        }
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun showSafeDeleteDialog(tag: TagEntity, taskCount: Int) {
        val message = getString(R.string.delete_tag_safe_message, taskCount)
        
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_tag_safe_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.delete_tag_and_tasks)) { _, _ ->
                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        tagRepository.deleteTagById(tag.id)
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            showSnackbar(getString(R.string.tag_deleted_successfully))
                            AppLogger.d("TagManagementActivity", "Tag and tasks deleted: ${tag.name}")
                        }
                    } catch (e: Exception) {
                        AppLogger.error("TagManagementActivity", "deleteTagAndTasks", e, "tagId: ${tag.id}")
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            showSnackbar("Error: ${e.message}")
                        }
                    }
                }
            }
            .setNeutralButton(getString(R.string.reassign_tasks)) { _, _ ->
                AppLogger.d("TagManagementActivity", "Reassign tasks button clicked for tag: ${tag.name}")
                showTagSelectorDialog(tag)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun showTagSelectorDialog(tagToDelete: TagEntity) {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val allTags = tagRepository.getAllTagsSync()
                AppLogger.d("TagManagementActivity", "Retrieved ${allTags.size} tags from database")
                
                val availableTags = allTags.filter { it.id != tagToDelete.id }
                AppLogger.d("TagManagementActivity", "Available tags for reassignment: ${availableTags.size}")
                
                if (availableTags.isEmpty()) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        showSnackbar("No other tags available for reassignment")
                    }
                    return@launch
                }

                AppLogger.d(
                    "TagManagementActivity", "Tag names for dialog: ${
                    availableTags.joinToString(", ") { it.name }
                }")
                
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    // Create custom dialog with RecyclerView
                    val dialogView = LayoutInflater.from(this@TagManagementActivity).inflate(R.layout.dialog_tag_selector, null)
                    val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.tagSelectorRecyclerView)
                    
                    val dialog = MaterialAlertDialogBuilder(this@TagManagementActivity)
                        .setTitle(getString(R.string.select_new_tag))
                        .setMessage(getString(R.string.select_new_tag_message))
                        .setView(dialogView)
                        .setNegativeButton(getString(R.string.cancel), null)
                        .create()
                    
                    // Setup RecyclerView
                    recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@TagManagementActivity)
                    val tagSelectorAdapter = TagSelectorAdapter(availableTags) { selectedTag ->
                        AppLogger.d("TagManagementActivity", "Selected tag: ${selectedTag.name}")
                        reassignTasksAndDeleteTag(tagToDelete, selectedTag)
                        dialog.dismiss()
                    }
                    recyclerView.adapter = tagSelectorAdapter
                    
                    dialog.show()
                    AppLogger.d("TagManagementActivity", "Tag selector dialog shown successfully")
                }
            } catch (e: Exception) {
                AppLogger.error("TagManagementActivity", "showTagSelectorDialog", e, "tagToDelete: ${tagToDelete.id}")
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    showSnackbar("Error: ${e.message}")
                }
            }
        }
    }
    
    private fun reassignTasksAndDeleteTag(tagToDelete: TagEntity, newTag: TagEntity) {
        // Show loading indicator
        showSnackbar("Reassigning tasks...")
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                AppLogger.d("TagManagementActivity", "Starting task reassignment from ${tagToDelete.name} to ${newTag.name}")
                
                // Update all tasks to use the new tag
                taskRepository.updateTasksTagId(tagToDelete.id, newTag.id)
                AppLogger.d("TagManagementActivity", "Tasks updated successfully")
                
                // Delete the old tag
                tagRepository.deleteTagById(tagToDelete.id)
                AppLogger.d("TagManagementActivity", "Tag deleted successfully")
                
                // Switch back to main thread for UI updates
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    showSnackbar(getString(R.string.tasks_reassigned_successfully))
                    AppLogger.d("TagManagementActivity", "Tasks reassigned from ${tagToDelete.name} to ${newTag.name}")
                }
            } catch (e: Exception) {
                AppLogger.error("TagManagementActivity", "reassignTasksAndDeleteTag", e, 
                    "tagToDelete: ${tagToDelete.id}, newTag: ${newTag.id}")
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    showSnackbar("Error: ${e.message}")
                }
            }
        }
    }

    private fun showSnackbar(message: String) {
        AppLogger.uiOperation("TagManagementActivity", "Show snackbar: $message")
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    // Adapter for tags
    private inner class TagAdapter(
        private val onEditClick: (TagEntity) -> Unit,
        private val onDeleteClick: (TagEntity) -> Unit,
        private val onLongPress: (TagEntity) -> Unit
    ) : androidx.recyclerview.widget.ListAdapter<TagEntity, TagAdapter.TagViewHolder>(TagDiffCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
            val binding = ItemTagManagementBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return TagViewHolder(binding)
        }

        override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class TagViewHolder(private val binding: ItemTagManagementBinding) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

            fun bind(tag: TagEntity) {
                binding.tagNameText.text = tag.name
                binding.tagColorIndicator.setBackgroundColor(Color.parseColor(tag.colorHex))
                
                // Format date
                val dateText = if (tag.updatedAt > tag.createdAt) {
                    getString(R.string.updated_on) + " " + DateTimeUtils.formatDate(tag.updatedAt)
                } else {
                    getString(R.string.created_on) + " " + DateTimeUtils.formatDate(tag.createdAt)
                }
                binding.tagDateText.text = dateText

                binding.editButton.setOnClickListener {
                    onEditClick(tag)
                }

                binding.deleteButton.setOnClickListener {
                    onDeleteClick(tag)
                }
                
                // Long press on the entire item
                itemView.setOnLongClickListener {
                    onLongPress(tag)
                    true
                }
            }
        }
    }

    private class TagDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<TagEntity>() {
        override fun areItemsTheSame(oldItem: TagEntity, newItem: TagEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TagEntity, newItem: TagEntity): Boolean {
            return oldItem == newItem
        }
    }
    
    // Adapter for tag selector dialog
    private inner class TagSelectorAdapter(
        private val tags: List<TagEntity>,
        private val onTagSelected: (TagEntity) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<TagSelectorAdapter.TagSelectorViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagSelectorViewHolder {
            val binding = ItemTagSelectorBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return TagSelectorViewHolder(binding)
        }

        override fun onBindViewHolder(holder: TagSelectorViewHolder, position: Int) {
            holder.bind(tags[position])
        }

        override fun getItemCount(): Int = tags.size

        inner class TagSelectorViewHolder(private val binding: ItemTagSelectorBinding) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

            fun bind(tag: TagEntity) {
                binding.tagNameText.text = tag.name
                binding.tagColorIndicator.setBackgroundColor(Color.parseColor(tag.colorHex))

                itemView.setOnClickListener {
                    onTagSelected(tag)
                }
            }
        }
    }
}
