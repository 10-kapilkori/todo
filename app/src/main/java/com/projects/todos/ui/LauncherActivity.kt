package com.projects.todos.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.projects.todos.MainActivity
import com.projects.todos.data.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LauncherActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check onboarding status and navigate accordingly
        lifecycleScope.launch {
            val userPreferences = UserPreferences(this@LauncherActivity)
            val isOnboardingCompleted = userPreferences.isOnboardingCompleted.first()
            
            val intent = if (isOnboardingCompleted) {
                Intent(this@LauncherActivity, MainActivity::class.java)
            } else {
                Intent(this@LauncherActivity, OnboardingActivity::class.java)
            }
            
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
