package com.projects.todos.ui

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.projects.todos.R
import com.projects.todos.data.UserPreferences
import com.projects.todos.databinding.ActivitySettingsBinding
import com.projects.todos.utils.AppLogger
import com.projects.todos.utils.setupKeyboardDismissOnOutsideClick
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set soft input mode for edge-to-edge friendly behavior
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppLogger.methodEntry("SettingsActivity", "onCreate")

        setupToolbar()
        setupUserPreferences()
        setupClickListeners()
        loadCurrentSettings()
        setupKeyboardDismiss()

        AppLogger.methodExit("SettingsActivity", "onCreate")
    }
    
    private fun setupKeyboardDismiss() {
        // Setup keyboard dismiss on outside click
        binding.root.setupKeyboardDismissOnOutsideClick()
    }

    private fun setupToolbar() {
        AppLogger.methodEntry("SettingsActivity", "setupToolbar")
        binding.toolbar.setNavigationOnClickListener {
            AppLogger.uiOperation("SettingsActivity", "Toolbar close clicked")
            finish()
        }
        AppLogger.methodExit("SettingsActivity", "setupToolbar")
    }

    private fun setupUserPreferences() {
        AppLogger.methodEntry("SettingsActivity", "setupUserPreferences")
        userPreferences = UserPreferences(this)
        AppLogger.methodExit("SettingsActivity", "setupUserPreferences")
    }

    private fun setupClickListeners() {
        AppLogger.methodEntry("SettingsActivity", "setupClickListeners")

        // Profile card click
        binding.profileCard.setOnClickListener {
            AppLogger.uiOperation("SettingsActivity", "Profile card clicked")
            showChangeNameDialog()
        }

        // Tags card click
        binding.tagsCard.setOnClickListener {
            AppLogger.uiOperation("SettingsActivity", "Tags card clicked")
            startTagManagementActivity()
        }

        // Notifications switch
        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            AppLogger.uiOperation("SettingsActivity", "Notifications switch changed: $isChecked")
            lifecycleScope.launch {
                userPreferences.setNotificationsEnabled(isChecked)
                val message = if (isChecked) {
                    getString(R.string.notifications_enabled)
                } else {
                    getString(R.string.notifications_disabled)
                }
                showSnackbar(message)
            }
        }

        // Reminder time button
        binding.reminderTimeButton.setOnClickListener {
            AppLogger.uiOperation("SettingsActivity", "Reminder time button clicked")
            showReminderTimeDialog()
        }

        AppLogger.methodExit("SettingsActivity", "setupClickListeners")
    }

    private fun loadCurrentSettings() {
        AppLogger.methodEntry("SettingsActivity", "loadCurrentSettings")
        lifecycleScope.launch {
            // Load current user name
            val userName = userPreferences.userName.first()
            binding.currentNameText.text = userName ?: getString(R.string.user_profile)

            // Load notifications setting
            val notificationsEnabled = userPreferences.isNotificationsEnabled.first()
            binding.notificationsSwitch.isChecked = notificationsEnabled

            // Load reminder offset
            val reminderOffset = userPreferences.defaultReminderOffsetMinutes.first()
            updateReminderTimeButton(reminderOffset)

            AppLogger.d("SettingsActivity", "Settings loaded: userName=$userName, notificationsEnabled=$notificationsEnabled, reminderOffset=$reminderOffset")
        }
        AppLogger.methodExit("SettingsActivity", "loadCurrentSettings")
    }

    private fun showChangeNameDialog() {
        AppLogger.methodEntry("SettingsActivity", "showChangeNameDialog")
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_name, null)
        val nameEditText = dialogView.findViewById<TextInputEditText>(R.id.nameEditText)

        // Pre-fill with current name
        lifecycleScope.launch {
            val currentName = userPreferences.userName.first()
            nameEditText.setText(currentName)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.change_name))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newName = nameEditText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    lifecycleScope.launch {
                        userPreferences.saveUserName(newName)
                        binding.currentNameText.text = newName
                        showSnackbar(getString(R.string.name_updated_successfully))
                        AppLogger.d("SettingsActivity", "Name updated: $newName")
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        // Handle Enter key press
        nameEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val newName = nameEditText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    lifecycleScope.launch {
                        userPreferences.saveUserName(newName)
                        binding.currentNameText.text = newName
                        showSnackbar(getString(R.string.name_updated_successfully))
                        AppLogger.d("SettingsActivity", "Name updated via Enter key: $newName")
                    }
                    dialog.dismiss()
                    return@setOnEditorActionListener true
                }
            }
            false
        }

        dialog.show()
        AppLogger.methodExit("SettingsActivity", "showChangeNameDialog")
    }

    private fun startTagManagementActivity() {
        AppLogger.methodEntry("SettingsActivity", "startTagManagementActivity")
        val intent = Intent(this, TagManagementActivity::class.java)
        startActivity(intent)
        AppLogger.methodExit("SettingsActivity", "startTagManagementActivity")
    }

    private fun showReminderTimeDialog() {
        AppLogger.methodEntry("SettingsActivity", "showReminderTimeDialog")
        
        val options = arrayOf(
            getString(R.string.reminder_5_min_before),
            getString(R.string.reminder_10_min_before),
            getString(R.string.reminder_15_min_before),
            getString(R.string.reminder_30_min_before),
            getString(R.string.reminder_1_hour_before),
            getString(R.string.reminder_1_day_before)
        )

        val reminderValues = intArrayOf(5, 10, 15, 30, 60, 1440) // minutes

        lifecycleScope.launch {
            val currentOffset = userPreferences.defaultReminderOffsetMinutes.first()
            val currentIndex = reminderValues.indexOf(currentOffset).takeIf { it >= 0 } ?: 0

            MaterialAlertDialogBuilder(this@SettingsActivity)
                .setTitle(getString(R.string.default_reminder_offset))
                .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                    val selectedMinutes = reminderValues[which]
                    lifecycleScope.launch {
                        userPreferences.setDefaultReminderOffsetMinutes(selectedMinutes)
                        updateReminderTimeButton(selectedMinutes)
                        showSnackbar(getString(R.string.reminder_time_updated))
                        AppLogger.d("SettingsActivity", "Reminder time updated: $selectedMinutes minutes")
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
        
        AppLogger.methodExit("SettingsActivity", "showReminderTimeDialog")
    }

    private fun updateReminderTimeButton(minutes: Int) {
        AppLogger.methodEntry("SettingsActivity", "updateReminderTimeButton", "minutes" to minutes)
        val text = when (minutes) {
            5 -> getString(R.string.reminder_5_min_before)
            10 -> getString(R.string.reminder_10_min_before)
            15 -> getString(R.string.reminder_15_min_before)
            30 -> getString(R.string.reminder_30_min_before)
            60 -> getString(R.string.reminder_1_hour_before)
            1440 -> getString(R.string.reminder_1_day_before)
            else -> getString(R.string.reminder_5_min_before)
        }
        binding.reminderTimeButton.text = text
        AppLogger.methodExit("SettingsActivity", "updateReminderTimeButton")
    }

    private fun showSnackbar(message: String) {
        AppLogger.uiOperation("SettingsActivity", "Show snackbar: $message")
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        AppLogger.methodEntry("SettingsActivity", "onResume")
        // Refresh settings when returning from TagManagementActivity
        loadCurrentSettings()
        AppLogger.methodExit("SettingsActivity", "onResume")
    }
}
