package com.projects.todos.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

object KeyboardUtils {
    
    /**
     * Hides the soft keyboard and clears focus in a single operation
     * @param context The context
     * @param view The view that currently has focus (optional)
     */
    fun hideKeyboard(context: Context, view: View? = null) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val targetView = view ?: getCurrentFocus(context)
        targetView?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }
    
    /**
     * Hides keyboard and clears focus from a specific view in one operation
     * @param context The context
     * @param view The view to clear focus from and hide keyboard for
     */
    fun hideKeyboardAndClearFocus(context: Context, view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        view.clearFocus()
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
    
    /**
     * Hides keyboard and clears focus from multiple views in one operation
     * @param context The context
     * @param views The views to clear focus from
     */
    fun hideKeyboardAndClearFocus(context: Context, vararg views: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // Clear focus from all views
        views.forEach { it.clearFocus() }

        // Hide keyboard using the first view or current focus
        val targetView = views.firstOrNull() ?: getCurrentFocus(context)
        targetView?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    /**
     * Shows the soft keyboard for a specific view
     * @param context The context
     * @param view The view to show keyboard for
     */
    fun showKeyboard(context: Context, view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        view.requestFocus()
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
    
    /**
     * Gets the currently focused view from the activity
     * @param context The context
     * @return The currently focused view or null
     */
    private fun getCurrentFocus(context: Context): View? {
        return if (context is android.app.Activity) {
            context.currentFocus
        } else {
            null
        }
    }
    
    /**
     * Clears focus from a view
     * @param view The view to clear focus from
     */
    fun clearFocus(view: View) {
        view.clearFocus()
    }
}
