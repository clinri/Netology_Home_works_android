package ru.netology.nmedia.auth

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dto.PushToken
import ru.netology.nmedia.model.AuthModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppAuth @Inject constructor(
    @ApplicationContext
    private val context: Context
) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val tokenKey = "token"
    private val idKey = "id"

    private val _authStateFlow: MutableStateFlow<AuthModel>

    init {
        val token = prefs.getString(tokenKey, null)
        val id = prefs.getLong(idKey, 0)

        if (token == null || id == 0L) {
            _authStateFlow = MutableStateFlow(AuthModel())
            prefs.edit { clear() }
        } else {
            _authStateFlow = MutableStateFlow(AuthModel(id, token))
        }
        sendPushToken()
    }

    val authStateFlow = _authStateFlow.asStateFlow()

    @Synchronized
    fun setAuth(id: Long, token: String) {
        _authStateFlow.value = AuthModel(id, token)
        prefs.edit {
            putLong(idKey, id)
            putString(tokenKey, token)
            sendPushToken()
        }
    }

    @Synchronized
    fun removeAuth() {
        _authStateFlow.value = AuthModel()
        prefs.edit {
            clear()
        }
        sendPushToken()
    }

    @InstallIn(SingletonComponent::class)
    @EntryPoint
    interface AppAuthEntryPoint{
        fun getApiServices():ApiService
    }

    fun sendPushToken(tokenFirebase: String? = null) {
        CoroutineScope(Dispatchers.Default).launch {
            val tokenFireBaseNotNull =
                tokenFirebase ?: FirebaseMessaging.getInstance().token.await()
            try {
                val entryPoint = EntryPointAccessors.fromApplication(context,AppAuthEntryPoint::class.java)
                entryPoint.getApiServices().sendPushToken(
                    PushToken(
                        tokenFireBaseNotNull
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}