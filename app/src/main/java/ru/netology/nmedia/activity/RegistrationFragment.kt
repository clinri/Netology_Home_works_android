package ru.netology.nmedia.activity

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.databinding.FragmentRegistrationBinding
import ru.netology.nmedia.util.AndroidUtils
import ru.netology.nmedia.viewmodel.RegistrationViewModel

class RegistrationFragment : Fragment() {

    private val viewModel by viewModels<RegistrationViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentRegistrationBinding.inflate(
            inflater,
            container,
            false
        )
        val pickPhotoLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                when (it.resultCode) {
                    ImagePicker.RESULT_ERROR -> {
                        Snackbar.make(
                            binding.root,
                            ImagePicker.getError(it.data),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    Activity.RESULT_OK -> {
                        val uri = it.data?.data ?: return@registerForActivityResult
                        viewModel.changePhoto(uri, uri.toFile())
                    }
                }
            }

        binding.registrationButton.setOnClickListener {
            val login = binding.login.text.toString()
            val pass = binding.password.text.toString()
            val name = binding.name.text.toString()
            viewModel.registrationByLoginAndPasswordAndName(login, pass, name)
            AndroidUtils.hideKeyboard(requireView())
        }

        viewModel.errorRegistration.observe(viewLifecycleOwner) {
            Snackbar.make(
                binding.root,
                "Error authentication",
                Snackbar.LENGTH_LONG
            )
                .show()
        }

        viewModel.tryRegistration.observe(viewLifecycleOwner) {
            if (viewModel.authorized) {
                findNavController().navigateUp()
            }
        }

        viewModel.media.observe(viewLifecycleOwner) { mediaModel ->
            if (mediaModel?.uri == null) {
                binding.previewContainer.isVisible = false
                binding.addPhoto.isVisible = true
                return@observe
            }
            binding.previewContainer.isVisible = true
            binding.addPhoto.isVisible = false
            binding.preview.setImageURI(mediaModel.uri)
        }

        binding.clearPhoto.setOnClickListener {
            viewModel.clearPhoto()
        }

        binding.addPhoto.setOnClickListener {
            ImagePicker.with(this)
                .galleryOnly()
                .crop()
                .compress(2048)
                .galleryMimeTypes(
                    arrayOf(
                        "image/png",
                        "image/jpeg"
                    )
                )
                .createIntent(pickPhotoLauncher::launch)
        }

        return binding.root
    }
}