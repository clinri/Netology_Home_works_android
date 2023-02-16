package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.activity.NewPostFragment.Companion.textArg
import ru.netology.nmedia.databinding.FragmentPhotoBinding
import ru.netology.nmedia.view.load


class PhotoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentPhotoBinding.inflate(
            inflater,
            container,
            false
        )

        val urlPhoto = arguments?.textArg
        binding.imageView.load("${BuildConfig.BASE_URL}/media/${urlPhoto}")

        return binding.root
    }
}