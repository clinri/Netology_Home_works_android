package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.databinding.FragmentAuthBinding
import ru.netology.nmedia.di.DependencyContainer
import ru.netology.nmedia.util.AndroidUtils
import ru.netology.nmedia.viewmodel.AuthViewModel
import ru.netology.nmedia.viewmodel.ViewModelFactory

class AuthFragment : Fragment() {

    private val dependencyContainer = DependencyContainer.getInstance()

    private val viewModel: AuthViewModel by viewModels(
        ownerProducer = ::requireParentFragment,
        factoryProducer = {
            ViewModelFactory(
                dependencyContainer.repository,
                dependencyContainer.appAuth,
                dependencyContainer.apiService
            )
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        println("viewModel.authorized = ${viewModel.authorized}")
        val binding = FragmentAuthBinding.inflate(
            inflater,
            container,
            false
        )

        binding.loginButton.setOnClickListener {
            val login = binding.login.text.toString()
            val pass = binding.password.text.toString()
            viewModel.authByLoginAndPassword(login, pass)
            AndroidUtils.hideKeyboard(requireView())
        }

        viewModel.errorAuth.observe(viewLifecycleOwner) {
            Snackbar.make(
                binding.root,
                "Error authentication",
                Snackbar.LENGTH_LONG
            )
                .show()
        }

        viewModel.tryAuth.observe(viewLifecycleOwner) {
            if (viewModel.authorized) {
                findNavController().navigateUp()
            }
        }
        return binding.root
    }
}