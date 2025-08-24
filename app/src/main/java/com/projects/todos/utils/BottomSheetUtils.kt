package com.projects.todos.utils

import android.view.View
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Extension function to apply edge-to-edge insets to a BottomSheetDialogFragment
 * Call this in onViewCreated() of your BottomSheetDialogFragment
 */
fun BottomSheetDialogFragment.applyEdgeToEdgeInsets() {
    dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { bottomSheet ->
        // Set bottom padding to 0 for true edge-to-edge
        bottomSheet.updatePadding(bottom = 0)
    }
}

/**
 * Extension function to apply only IME insets to a BottomSheetDialogFragment
 * Use this when you want the bottom sheet to be above the navigation bar but below the IME
 */
fun BottomSheetDialogFragment.applyImeInsets() {
    dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { bottomSheet ->
        // Set bottom padding to 0 for true edge-to-edge
        bottomSheet.updatePadding(bottom = 0)
    }
}

/**
 * Extension function to apply system bar insets to a BottomSheetDialogFragment
 * Use this when you want the bottom sheet to respect system bars but not IME
 */
fun BottomSheetDialogFragment.applySystemBarInsets() {
    dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { bottomSheet ->
        // Set bottom padding to 0 for true edge-to-edge
        bottomSheet.updatePadding(bottom = 0)
    }
}

/**
 * Extension function to apply edge-to-edge insets to a regular Dialog
 * Call this after showing the dialog
 */
fun android.app.Dialog.applyEdgeToEdgeInsets() {
    window?.decorView?.let { decorView ->
        // Set bottom padding to 0 for true edge-to-edge
        decorView.updatePadding(bottom = 0)
    }
}
