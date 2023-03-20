package ru.netology.nmedia.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.navigation.ui.navigateUp
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.NewPostFragment.Companion.textArg
import ru.netology.nmedia.viewmodel.AuthViewModel
import ru.netology.nmedia.viewmodel.PostViewModel
import javax.inject.Inject

@AndroidEntryPoint
class AppActivity : AppCompatActivity(R.layout.activity_app) {

    val postViewModel: PostViewModel by viewModels()
    val authViewModel: AuthViewModel by viewModels()
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var toolbar: Toolbar
    private var previousMenuProvider: MenuProvider? = null
    private lateinit var navController: NavController

    @Inject
    lateinit var fireBase: FirebaseMessaging
    @Inject
    lateinit var googleService:GoogleApiAvailability

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.let {
            if (it.action != Intent.ACTION_SEND) {
                return@let
            }

            val text = it.getStringExtra(Intent.EXTRA_TEXT)
            if (text?.isNotBlank() != true) {
                return@let
            }

            intent.removeExtra(Intent.EXTRA_TEXT)
            findNavController(R.id.nav_host_fragment)
                .navigate(
                    R.id.action_feedFragment_to_newPostFragment,
                    Bundle().apply {
                        textArg = text
                    }
                )
        }

        toolbar = findViewById(R.id.custom_toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupWithNavController(toolbar, navController, appBarConfiguration)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.photoFragment -> setToolbarDark()
                else -> setToolbarLight()
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.feedFragment, R.id.newPostFragment -> addMenuInMenuProvider()
                else -> previousMenuProvider?.let(toolbar::removeMenuProvider)
            }
        }

        checkGoogleApiAvailability()
    }

    private fun addMenuInMenuProvider() {
        authViewModel.data.observe(this) {
            previousMenuProvider?.let(toolbar::removeMenuProvider)
            toolbar.addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_auth, menu)
                    menu.setGroupVisible(R.id.unauthorized, !authViewModel.authorized)
                    menu.setGroupVisible(R.id.authorized, authViewModel.authorized)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                    when (menuItem.itemId) {
                        R.id.login -> {
                            findNavController(R.id.nav_host_fragment).navigate(
                                R.id.action_feedFragment_to_authFragment
                            )
                            true
                        }
                        R.id.register -> {
                            findNavController(R.id.nav_host_fragment).navigate(
                                R.id.action_feedFragment_to_registrationFragment
                            )
                            true
                        }
                        R.id.logout -> {
                            val listener =
                                NavController.OnDestinationChangedListener { _, destination, _ ->
                                    when (destination.id) {
                                        R.id.newPostFragment -> {
                                            postViewModel.toDialogConfirmationFromNewPostFragment()
                                        }
                                        R.id.feedFragment -> {
                                            postViewModel.toDialogConfirmationFromFeedFragment()
                                        }
                                    }
                                }
                            navController.addOnDestinationChangedListener(listener)
                            navController.removeOnDestinationChangedListener(listener)
                            true
                        }
                        else -> false
                    }
            }.also { previousMenuProvider = it })
        }
    }

    private fun setToolbarLight() {
        toolbar.setBackgroundColor(Color.CYAN)
        toolbar.setTitleTextColor(Color.BLACK)
    }

    private fun setToolbarDark() {
        toolbar.setBackgroundColor(Color.BLACK)
        toolbar.setTitleTextColor(Color.WHITE)
        toolbar.navigationIcon =
            AppCompatResources.getDrawable(this, R.drawable.ic_arrow_back_24dp)
    }

    private fun checkGoogleApiAvailability() {
        with(googleService) {
            val code = isGooglePlayServicesAvailable(this@AppActivity)
            if (code == ConnectionResult.SUCCESS) {
                return@with
            }
            if (isUserResolvableError(code)) {
                getErrorDialog(this@AppActivity, code, 9000)?.show()
                return
            }
            Toast.makeText(
                this@AppActivity,
                R.string.google_play_unavailable,
                Toast.LENGTH_LONG
            )
                .show()
        }

        fireBase.token.addOnSuccessListener {
            println(it)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navControllerNavigateUp = findNavController(R.id.nav_host_fragment)
        return navControllerNavigateUp.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}