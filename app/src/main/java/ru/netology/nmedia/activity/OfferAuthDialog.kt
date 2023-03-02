package ru.netology.nmedia.activity

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.R

class OfferAuthDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("To perform the action, you need to authenticate")
        builder.setNegativeButton("Cancel"){ dialog, _ ->
            dialog.dismiss()
        }
        builder.setPositiveButton("Sing in") { _, _ ->
            findNavController().navigate(
                R.id.action_offerAuthDialog_to_authFragment
            )
        }
        return builder.create()
    }
}