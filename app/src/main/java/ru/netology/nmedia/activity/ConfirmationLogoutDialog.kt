package ru.netology.nmedia.activity

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.R
import ru.netology.nmedia.viewmodel.PostViewModel

class ConfirmationLogoutDialog : DialogFragment() {
    private val postViewModel by viewModels<PostViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setPositiveButton("Logout") { dialog, _ ->
            postViewModel.logoutFromNewPostFragment()
            dialog.cancel()
        }
        builder.setNegativeButton("Cancel"){ dialog, _ ->
            dialog.cancel()
        }
        return builder.create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        postViewModel.backToFeedFragmentFromDialogConfirmation.observe(viewLifecycleOwner){
            findNavController().navigate(
                R.id.action_confirmationLogOutDialog_to_feedFragment
            )
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }
}