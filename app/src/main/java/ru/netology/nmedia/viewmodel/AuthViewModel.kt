package ru.netology.nmedia.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.model.AuthModel
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.IOException

class AuthViewModel : ViewModel() {
    val data: LiveData<AuthModel?> = AppAuth.getInstance()
        .authStateFlow
        .asLiveData(Dispatchers.Default)

    val authorized: Boolean
        get() {
            val id = AppAuth.getInstance().authStateFlow.value?.id ?: 0L
            return id != 0L
        }

    private var _tryAuth = SingleLiveEvent<Unit>()
    val tryAuth: LiveData<Unit>
        get() = _tryAuth

    private var _errorAuth = SingleLiveEvent<Unit>()
    val errorAuth: LiveData<Unit>
        get() = _errorAuth

    fun authByLoginAndPassword(login: String, password: String) {
        var result: AuthModel? = null
        viewModelScope.launch {
            try {
                val response = PostsApi.service.updateUser(login, password)
                if (!response.isSuccessful) {
                    throw ApiError(response.code(), response.message())
                }
                result = response.body() ?: let {
                    _errorAuth.value = Unit
                    result
                }
                result?.let { AppAuth.getInstance().setAuth(it.id, it.token) }
                _tryAuth.value = Unit
            } catch (e: IOException) {
                _errorAuth.value = Unit
            } catch (e: Exception) {
                _errorAuth.value = Unit
            }
        }
    }
}