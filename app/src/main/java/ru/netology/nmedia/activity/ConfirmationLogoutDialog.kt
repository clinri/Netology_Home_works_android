package ru.netology.nmedia.activity

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.R
import ru.netology.nmedia.viewmodel.PostViewModel

class ConfirmationLogoutDialog : DialogFragment() {
    private val postViewModel: PostViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("Are you really?")
        builder.setNegativeButton("Cancel"){ dialog, _ ->
            dialog.dismiss()
        }
        builder.setPositiveButton("Logout") { _, _ ->
            postViewModel.logoutFromNewPostFragment()
        }
        postViewModel.backToFeedFragmentFromDialogConfirmation.observe(this){
            findNavController().navigate(
                R.id.action_confirmationLogOutDialog_to_feedFragment
            )
        }
        return builder.create()
    }
}