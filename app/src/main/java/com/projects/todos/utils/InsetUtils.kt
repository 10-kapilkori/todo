package com.projects.todos.utils

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView

/**
 * Applies IME and system bottom insets to a RecyclerView
 */
fun RecyclerView.applyImeAndSystemBottomInset() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(
            WindowInsetsCompat.Type.ime() or WindowInsetsCompat.Type.systemBars()
        )
        
        view.setPadding(
            view.paddingLeft,
            view.paddingTop,
            view.paddingRight,
            view.paddingBottom + insets.bottom
        )
        
        WindowInsetsCompat.CONSUMED
    }
}

/**
 * Applies top system bar insets to a view (typically AppBarLayout)
 */
fun View.applyTopSystemBarInset() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        
        view.setPadding(
            view.paddingLeft,
            view.paddingTop + insets.top,
            view.paddingRight,
            view.paddingBottom
        )
        
        WindowInsetsCompat.CONSUMED
    }
}
