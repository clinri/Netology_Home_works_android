package ru.netology.nmedia.viewmodel

import android.net.Uri
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.model.AuthModel
import ru.netology.nmedia.model.MediaModel
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.File
import java.io.IOException

class RegistrationViewModel : ViewModel() {

    val authorized: Boolean
        get() {
            val id = AppAuth.getInstance().authStateFlow.value?.id ?: 0L
            return id != 0L
        }

    private var _tryRegistration = SingleLiveEvent<Unit>()
    val tryRegistration: LiveData<Unit>
        get() = _tryRegistration

    private var _errorRegistration = SingleLiveEvent<Unit>()
    val errorRegistration: LiveData<Unit>
        get() = _errorRegistration

    private val _media = MutableLiveData<MediaModel?>(null)
    val media: LiveData<MediaModel?>
        get() = _media

    fun changePhoto(uri: Uri, file: File) {
        _media.value = MediaModel(uri, file)
    }

    fun clearPhoto() {
        _media.value = null
    }

    fun registrationByLoginAndPasswordAndName(login: String, password: String, name: String) {
        when (val mediaModel = _media.value) {
            null -> registerWithoutAvatar(login, password, name)
            else -> registerWithAvatar(login, password, name, mediaModel)
        }
    }

    private fun registerWithoutAvatar(
        login: String,
        password: String,
        name: String,
    ) {
        var result: AuthModel? = null
        viewModelScope.launch {
            try {
                val response = PostsApi.service.registrationUser(login, password, name)
                if (!response.isSuccessful) {
                    throw ApiError(response.code(), response.message())
                }
                result = response.body() ?: let {
                    _errorRegistration.value = Unit
                    result
                }
                result?.let { AppAuth.getInstance().setAuth(it.id, it.token) }
                _tryRegistration.value = Unit
            } catch (e: IOException) {
                _errorRegistration.value = Unit
            } catch (e: Exception) {
                _errorRegistration.value = Unit
            }
        }
    }

    private fun registerWithAvatar(
        login: String,
        password: String,
        name: String,
        avatar: MediaModel
    ) {
        var result: AuthModel? = null
        viewModelScope.launch {
            try {
                val photoPart = MultipartBody.Part.createFormData(
                    name = "file",
                    filename = avatar.file.name,
                    body = avatar.file.asRequestBody()
                )
                val loginPart = login.toRequestBody("text/plain".toMediaType())
                val passwordPart = password.toRequestBody("text/plain".toMediaType())
                val namePart = name.toRequestBody("text/plain".toMediaType())
                val response = PostsApi.service.registerWithPhoto(
                    login =  loginPart,
                    pass = passwordPart,
                    name = namePart,
                    media = photoPart
                )
                if (!response.isSuccessful) {
                    throw ApiError(response.code(), response.message())
                }
                result = response.body() ?: let {
                    _errorRegistration.value = Unit
                    result
                }
                result?.let { AppAuth.getInstance().setAuthWithAvatar(it.id, it.token, it.photo!!) }
                _tryRegistration.value = Unit
            } catch (e: IOException) {
                _errorRegistration.value = Unit
            } catch (e: Exception) {
                _errorRegistration.value = Unit
            }
        }
    }
}