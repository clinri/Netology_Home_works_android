package ru.netology.nmedia.activity

import android.app.Activity
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toFile
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.ImagePicker.Companion.getError
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentNewPostBinding
import ru.netology.nmedia.util.AndroidUtils
import ru.netology.nmedia.util.StringArg
import ru.netology.nmedia.viewmodel.PostViewModel

class NewPostFragment : Fragment() {

    companion object {
        var Bundle.textArg: String? by StringArg
    }

    private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentNewPostBinding.inflate(
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
                            getError(it.data),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    Activity.RESULT_OK -> {
                        val uri = it.data?.data ?: return@registerForActivityResult
                        viewModel.changePhoto(uri, uri.toFile())
                    }
                }
            }

        arguments?.textArg
            ?.let(binding.edit::setText)

        val toolbar = requireActivity().findViewById<Toolbar>(R.id.custom_toolbar)

        toolbar.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_new_post, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                when (menuItem.itemId) {
                    R.id.new_post -> {
                        viewModel.changeContent(binding.edit.text.toString())
                        viewModel.save()
                        AndroidUtils.hideKeyboard(requireView())
                        true
                    }
                    else -> false
                }
        }, viewLifecycleOwner)

        binding.clearPhoto.setOnClickListener {
            viewModel.clearPhoto()
        }

        viewModel.postCreated.observe(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        binding.pickPhoto.setOnClickListener {
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

        binding.takePhoto.setOnClickListener {
            ImagePicker.with(this)
                .cameraOnly()
                .crop()
                .compress(2048)
                .createIntent(pickPhotoLauncher::launch)
        }

        viewModel.media.observe(viewLifecycleOwner) { mediaModel ->
            if (mediaModel?.uri == null) {
                binding.previewContainer.isVisible = false
                return@observe
            }
            binding.previewContainer.isVisible = true
            binding.preview.setImageURI(mediaModel.uri)
        }

        viewModel.toDialogConfirmationFromNewPostFragment.observe(viewLifecycleOwner){
            findNavController().navigate(
                R.id.action_newPostFragment_to_confirmationLogOutDialog
            )
        }

        return binding.root
    }
}