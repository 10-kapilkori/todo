package com.projects.todos

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.projects.todos.data.UserPreferences
import com.projects.todos.databinding.ActivityMainBinding
import com.projects.todos.ui.BaseActivity
import com.projects.todos.ui.SettingsActivity
import com.projects.todos.utils.setupKeyboardDismissOnOutsideClick
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set soft input mode for edge-to-edge friendly behavior
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupToolbar()
        setupGreeting()
        setupKeyboardDismiss()
    }

    private fun setupKeyboardDismiss() {
        // Setup keyboard dismiss on outside click
        binding.main.setupKeyboardDismissOnOutsideClick()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Connect BottomNavigationView with Navigation Controller
        binding.bottomNavigation.setupWithNavController(navController)
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }
    }

    private fun setupGreeting() {
        val userPreferences = UserPreferences(this@MainActivity)
        
        // Observe userName changes reactively
        lifecycleScope.launch {
            userPreferences.userName.collect { userName ->
                val greetingMessage = if (!userName.isNullOrBlank()) {
                    getString(R.string.welcome_format, userName)
                } else {
                    getString(R.string.welcome_default)
                }
                binding.toolbar.title = greetingMessage
            }
        }
    }
}