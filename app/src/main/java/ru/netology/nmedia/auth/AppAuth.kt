package ru.netology.nmedia.auth

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ru.netology.nmedia.di.DependencyContainer
import ru.netology.nmedia.dto.PushToken
import ru.netology.nmedia.model.AuthModel

class AppAuth(context: Context) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val tokenKey = "token"
    private val idKey = "id"

    private val _authStateFlow: MutableStateFlow<AuthModel?>

    init {
        val token = prefs.getString(tokenKey, null)
        val id = prefs.getLong(idKey, 0)

        if (token == null || id == 0L) {
            _authStateFlow = MutableStateFlow(null)
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
        _authStateFlow.value = null
        prefs.edit {
            clear()
        }
        sendPushToken()
    }

    fun sendPushToken(tokenFirebase: String? = null) {
        CoroutineScope(Dispatchers.Default).launch {
            val tokenFireBaseNotNull =
                tokenFirebase ?: FirebaseMessaging.getInstance().token.await()
            try {
                DependencyContainer.getInstance().apiService.sendPushToken(PushToken(tokenFireBaseNotNull))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}