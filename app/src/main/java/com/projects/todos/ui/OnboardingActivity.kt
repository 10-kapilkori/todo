package com.projects.todos.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.projects.todos.R
import com.projects.todos.adapter.OnboardingAdapter
import com.projects.todos.data.OnboardingItem
import com.projects.todos.databinding.ActivityOnboardingBinding
import com.projects.todos.utils.JsonParser

class OnboardingActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var onboardingItems: List<OnboardingItem>
    private val dots = mutableListOf<ImageView>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupOnboarding()
    }
    
    private fun setupOnboarding() {
        // Parse onboarding items from JSON
        onboardingItems = JsonParser.parseOnboardingItems(this)
        
        if (onboardingItems.isEmpty()) {
            // Fallback to main activity if JSON parsing fails
            navigateToNameEntry()
            return
        }
        
        // Setup ViewPager2
        val adapter = OnboardingAdapter(onboardingItems) {
            navigateToNameEntry()
        }
        
        binding.onboardingViewPager.adapter = adapter
        binding.onboardingViewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        
        // Setup custom dots
        setupDots()
        
        // Setup ViewPager page change listener
        binding.onboardingViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
            }
        })
    }
    
    private fun setupDots() {
        // Clear existing dots
        binding.indicatorContainer.removeAllViews()
        dots.clear()
        
        // Create dots for each onboarding item
        for (i in onboardingItems.indices) {
            val dot = ImageView(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    width = (10 * resources.displayMetrics.density).toInt()
                    height = (10 * resources.displayMetrics.density).toInt()
                    marginStart = (4 * resources.displayMetrics.density).toInt()
                    marginEnd = (4 * resources.displayMetrics.density).toInt()
                    gravity = Gravity.CENTER
                }
                
                setImageResource(
                    if (i == 0) R.drawable.tab_indicator_selected 
                    else R.drawable.tab_indicator_unselected
                )
            }
            
            binding.indicatorContainer.addView(dot)
            dots.add(dot)
        }
    }
    
    private fun updateDots(selectedPosition: Int) {
        dots.forEachIndexed { index, dot ->
            dot.setImageResource(
                if (index == selectedPosition) R.drawable.tab_indicator_selected 
                else R.drawable.tab_indicator_unselected
            )
        }
    }
    
    private fun navigateToNameEntry() {
        val intent = Intent(this, NameEntryActivity::class.java)
        startActivity(intent)
        finish()
    }
}
