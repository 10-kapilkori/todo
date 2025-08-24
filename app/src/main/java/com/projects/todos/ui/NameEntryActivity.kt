package com.projects.todos.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.projects.todos.R
import com.projects.todos.data.UserPreferences
import com.projects.todos.data.database.TodoDatabase
import com.projects.todos.data.repository.TagRepository
import com.projects.todos.data.repository.TaskRepository
import com.projects.todos.data.service.DatabaseInitializationService
import com.projects.todos.databinding.ActivityNameEntryBinding
import com.projects.todos.utils.KeyboardUtils
import kotlinx.coroutines.launch

class NameEntryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityNameEntryBinding
    private lateinit var userPreferences: UserPreferences
    private lateinit var databaseInitializationService: DatabaseInitializationService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNameEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        userPreferences = UserPreferences(this)
        
        // Initialize database services
        val database = TodoDatabase.getDatabase(this)
        val tagRepository = TagRepository(database.tagDao())
        val taskRepository = TaskRepository(database.taskDao())
        databaseInitializationService = DatabaseInitializationService(tagRepository, taskRepository)
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.saveNameButton.setOnClickListener {
            saveUserName()
        }
        
        // Handle IME action (Done button on keyboard)
        binding.nameEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                saveUserName()
                return@setOnEditorActionListener true
            }
            false
        }
        
        // Set up root layout to handle outside touches
        binding.root.setOnClickListener {
            hideKeyboardAndClearFocus()
        }
        
        // Enable button when text is entered
        binding.nameEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                binding.saveNameButton.isEnabled = !s.isNullOrBlank()
            }
        })
    }
    
    private fun hideKeyboard() {
        KeyboardUtils.hideKeyboard(this)
    }
    
    private fun hideKeyboardAndClearFocus() {
        KeyboardUtils.hideKeyboardAndClearFocus(this, binding.nameEditText)
    }
    
    private fun clearFocus() {
        KeyboardUtils.clearFocus(binding.nameEditText)
    }
    
    private fun saveUserName() {
        val name = binding.nameEditText.text.toString().trim()
        
        if (name.isBlank()) {
            Toast.makeText(this, getString(R.string.name_required), Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                // Save user name
                userPreferences.saveUserName(name)
                
                // Initialize database with dummy data
                databaseInitializationService.initializeDatabaseWithDummyData(this@NameEntryActivity)
                
                navigateToPermissions()
            } catch (e: Exception) {
                Toast.makeText(this@NameEntryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun navigateToPermissions() {
        val intent = Intent(this, PermissionsActivity::class.java)
        startActivity(intent)
        finish()
    }
}
