package com.projects.todos.utils

import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Utility class to manage bottom sheet states and prevent duplicate openings
 */
object BottomSheetManager {
    
    private val activeBottomSheets = mutableSetOf<String>()
    
    /**
     * Shows a bottom sheet if it's not already active
     * @param fragmentManager The fragment manager to show the bottom sheet with
     * @param bottomSheet The bottom sheet fragment to show
     * @param tag The tag for the bottom sheet
     * @return true if the bottom sheet was shown, false if it was already active
     */
    fun showBottomSheetIfNotActive(
        fragmentManager: FragmentManager,
        bottomSheet: BottomSheetDialogFragment,
        tag: String
    ): Boolean {
        return if (isBottomSheetActive(tag)) {
            false // Bottom sheet is already active
        } else {
            activeBottomSheets.add(tag)
            
            // Set animation for the bottom sheet
            bottomSheet.setStyle(
                BottomSheetDialogFragment.STYLE_NORMAL,
                com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog
            )
            
            bottomSheet.show(fragmentManager, tag)
            true // Bottom sheet was shown
        }
    }
    
    /**
     * Shows a bottom sheet with custom animation
     * @param fragmentManager The fragment manager to show the bottom sheet with
     * @param bottomSheet The bottom sheet fragment to show
     * @param tag The tag for the bottom sheet
     * @param animationStyle The animation style resource ID
     * @return true if the bottom sheet was shown, false if it was already active
     */
    fun showBottomSheetWithAnimation(
        fragmentManager: FragmentManager,
        bottomSheet: BottomSheetDialogFragment,
        tag: String,
        animationStyle: Int
    ): Boolean {
        return if (isBottomSheetActive(tag)) {
            false // Bottom sheet is already active
        } else {
            activeBottomSheets.add(tag)
            
            // Set custom animation for the bottom sheet
            bottomSheet.setStyle(
                BottomSheetDialogFragment.STYLE_NORMAL,
                animationStyle
            )
            
            bottomSheet.show(fragmentManager, tag)
            true // Bottom sheet was shown
        }
    }
    
    /**
     * Checks if a bottom sheet with the given tag is currently active
     * @param tag The tag to check
     * @return true if the bottom sheet is active, false otherwise
     */
    fun isBottomSheetActive(tag: String): Boolean {
        return activeBottomSheets.contains(tag)
    }
    
    /**
     * Removes a bottom sheet from the active list when it's dismissed
     * @param tag The tag of the bottom sheet to remove
     */
    fun removeBottomSheet(tag: String) {
        activeBottomSheets.remove(tag)
    }
    
    /**
     * Clears all active bottom sheets (useful for cleanup)
     */
    fun clearAllBottomSheets() {
        activeBottomSheets.clear()
    }
    
    /**
     * Gets the count of currently active bottom sheets
     * @return The number of active bottom sheets
     */
    fun getActiveBottomSheetCount(): Int {
        return activeBottomSheets.size
    }
}
