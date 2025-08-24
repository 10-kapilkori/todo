package com.projects.todos.utils

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.View
import com.google.android.material.R
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors

object ThemeUtils {

    /**
     * Creates a styled chip with proper theme colors
     */
    fun createStyledChip(context: Context, text: String): Chip {
        return Chip(context).apply {
            id = View.generateViewId()
            this.text = text
            isCheckable = true
            isClickable = true
            isFocusable = true
            isCheckedIconVisible = false

            // Apply initial theme colors
            applyChipThemeColors(this, false)
        }
    }

    /**
     * Applies theme colors to a chip based on its checked state
     */
    fun applyChipThemeColors(chip: Chip, isChecked: Boolean) {
        if (isChecked) {
            // Selected state: PrimaryContainer background with OnPrimaryContainer text
            val selectedColor = MaterialColors.getColor(
                chip,
                R.attr.colorPrimaryContainer
            )
            val selectedTextColor = MaterialColors.getColor(
                chip,
                R.attr.colorOnPrimaryContainer
            )
            chip.chipBackgroundColor = ColorStateList.valueOf(selectedColor)
            chip.setTextColor(selectedTextColor)
        } else {
            // Unselected state: Surface background with Primary text
            val unselectedColor =
                MaterialColors.getColor(chip, R.attr.colorSurface)
            val unselectedTextColor =
                MaterialColors.getColor(chip, R.attr.colorPrimary)
            chip.chipBackgroundColor = ColorStateList.valueOf(unselectedColor)
            chip.setTextColor(unselectedTextColor)
        }
    }

    /**
     * Gets a color from the current theme using a view
     */
    fun getThemeColor(view: View, colorAttr: Int): Int {
        return MaterialColors.getColor(view, colorAttr)
    }

    /**
     * Gets a color from the current theme using context (alternative method)
     */
    fun getThemeColor(context: Context, colorAttr: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(colorAttr, typedValue, true)
        return typedValue.data
    }

    /**
     * Creates a ColorStateList from a theme color
     */
    fun createColorStateList(context: Context, colorAttr: Int): ColorStateList {
        val color = getThemeColor(context, colorAttr)
        return ColorStateList.valueOf(color)
    }
}
