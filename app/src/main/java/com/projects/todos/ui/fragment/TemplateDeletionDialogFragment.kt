package com.projects.todos.ui.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.projects.todos.R
import com.projects.todos.ui.viewmodel.DeletionAction

interface TemplateDeletionDialogCallback {
    fun onDeletionConfirmed(templateId: Int, action: DeletionAction)
}

class TemplateDeletionDialogFragment : DialogFragment() {

    private var callback: TemplateDeletionDialogCallback? = null
    private var templateId: Int = 0
    private var templateTitle: String = ""
    private var linkedTasksCount: Int = 0

    companion object {
        const val TAG = "TemplateDeletionDialog"
        
        fun newInstance(
            templateId: Int,
            templateTitle: String,
            linkedTasksCount: Int,
            callback: TemplateDeletionDialogCallback? = null
        ): TemplateDeletionDialogFragment {
            return TemplateDeletionDialogFragment().apply {
                this.templateId = templateId
                this.templateTitle = templateTitle
                this.linkedTasksCount = linkedTasksCount
                this.callback = callback
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val message = if (linkedTasksCount == 0) {
            getString(R.string.template_deletion_confirmation_no_tasks, templateTitle)
        } else {
            getString(R.string.template_deletion_confirmation_with_tasks, templateTitle, linkedTasksCount)
        }

        val positiveButtonText = if (linkedTasksCount == 0) {
            getString(R.string.delete)
        } else {
            getString(R.string.delete_template_and_tasks)
        }

        val negativeButtonText = getString(R.string.cancel)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_template))
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { _, _ ->
                val action = if (linkedTasksCount == 0) {
                    DeletionAction.DELETE_ALL
                } else {
                    DeletionAction.DELETE_ALL
                }
                callback?.onDeletionConfirmed(templateId, action)
            }
            .setNegativeButton(negativeButtonText) { _, _ ->
                // User cancelled
            }
            .create()
    }
}
