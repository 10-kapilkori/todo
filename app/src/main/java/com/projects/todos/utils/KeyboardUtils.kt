package com.projects.todos.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

/**
 * Hides the keyboard and clears focus from the current view
 */
fun Activity.hideKeyboard() {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    currentFocus?.let { view ->
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
    }
}

/**
 * Hides the keyboard for a specific view
 */
fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
    clearFocus()
}

/**
 * Shows the keyboard for an EditText
 */
fun EditText.showKeyboard() {
    requestFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

/**
 * Sets up click listener to hide keyboard when clicking outside of input fields
 * Call this on the root view of your layout
 */
fun View.setupKeyboardDismissOnOutsideClick() {
    setOnClickListener {
        hideKeyboard()
    }
    
    // Make sure the view is focusable and clickable
    isFocusableInTouchMode = true
    isClickable = true
}
