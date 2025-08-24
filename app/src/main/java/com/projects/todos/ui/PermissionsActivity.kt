package com.projects.todos.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.projects.todos.MainActivity
import com.projects.todos.R
import com.projects.todos.data.UserPreferences
import com.projects.todos.databinding.ActivityPermissionsBinding
import kotlinx.coroutines.launch

class PermissionsActivity : BaseActivity() {

    private lateinit var binding: ActivityPermissionsBinding
    private lateinit var userPreferences: UserPreferences

    // Permission launcher - must be registered before activity starts
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        lifecycleScope.launch {
            userPreferences.setNotificationsPermissionGranted(isGranted)

            if (isGranted) {
                Toast.makeText(
                    this@PermissionsActivity,
                    "Notifications enabled!",
                    Toast.LENGTH_SHORT
                ).show()
                completeOnboarding()
            } else {
                showPermissionDeniedDialog()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set soft input mode for edge-to-edge friendly behavior
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        
        binding = ActivityPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userPreferences = UserPreferences(this)

        setupUI()
    }
    


    private fun setupUI() {
        binding.requestPermissionButton.setOnClickListener {
            requestNotificationPermission()
        }

        binding.skipPermissionButton.setOnClickListener {
            showSkipPermissionDialog()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Permission is automatically granted for older Android versions
            lifecycleScope.launch {
                userPreferences.setNotificationsPermissionGranted(true)
                Toast.makeText(
                    this@PermissionsActivity,
                    "Notifications enabled!",
                    Toast.LENGTH_SHORT
                ).show()
                completeOnboarding()
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_required_title))
            .setMessage(getString(R.string.permission_required_message))
            .setPositiveButton(getString(R.string.try_again)) { _, _ ->
                requestNotificationPermission()
            }
            .setNegativeButton(getString(R.string.skip)) { _, _ ->
                lifecycleScope.launch {
                    userPreferences.setNotificationsPermissionGranted(false)
                    completeOnboarding()
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun showSkipPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.skip_notifications_title))
            .setMessage(getString(R.string.skip_notifications_message))
            .setPositiveButton(getString(R.string.enable_now)) { _, _ ->
                requestNotificationPermission()
            }
            .setNegativeButton(getString(R.string.skip)) { _, _ ->
                lifecycleScope.launch {
                    userPreferences.setNotificationsPermissionGranted(false)
                    completeOnboarding()
                }
            }
            .setCancelable(true)
            .show()
    }

    private fun completeOnboarding() {
        lifecycleScope.launch {
            try {
                userPreferences.setOnboardingCompleted(true)
                navigateToMainActivity()
            } catch (e: Exception) {
                Toast.makeText(
                    this@PermissionsActivity,
                    "Error completing onboarding",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
