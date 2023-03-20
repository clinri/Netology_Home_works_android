package ru.netology.nmedia.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.model.AuthModel
import ru.netology.nmedia.model.MediaModel
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.File
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val appAuth: AppAuth,
    private val api: ApiService,
) : ViewModel() {

    val authorized: Boolean
        get() {
            val id = appAuth.authStateFlow.value.id
            return id != 0L
        }

    private var _tryRegistration = SingleLiveEvent<Unit>()
    val tryRegistration: LiveData<Unit>
        get() = _tryRegistration

    private var _errorRegistration = SingleLiveEvent<Unit>()
    val errorRegistration: LiveData<Unit>
        get() = _errorRegistration

    private val _photo = MutableLiveData<MediaModel?>(null)
    val photo: LiveData<MediaModel?>
        get() = _photo

    fun changePhoto(uri: Uri, file: File) {
        _photo.value = MediaModel(uri, file)
    }

    fun clearPhoto() {
        _photo.value = null
    }

    fun registration(login: String, password: String, name: String) {
        when (val photoMediaModel = _photo.value) {
            null -> register(login, password, name)
            else -> registerWithPhoto(login, password, name, photoMediaModel)
        }
    }

    private fun register(
        login: String,
        password: String,
        name: String,
    ) {
        var result: AuthModel? = null
        viewModelScope.launch {
            try {
                val response = api.registrationUser(login, password, name)
                if (!response.isSuccessful) {
                    throw ApiError(response.code(), response.message())
                }
                result = response.body() ?: let {
                    _errorRegistration.value = Unit
                    result
                }
                result?.let { appAuth.setAuth(it.id, it.token ?: "") }
                _tryRegistration.value = Unit
            } catch (e: IOException) {
                _errorRegistration.value = Unit
            } catch (e: Exception) {
                _errorRegistration.value = Unit
            }
        }
    }

    private fun registerWithPhoto(
        login: String,
        password: String,
        name: String,
        photo: MediaModel
    ) {
        var result: AuthModel? = null
        viewModelScope.launch {
            try {
                val photoPart = MultipartBody.Part.createFormData(
                    name = "file",
                    filename = photo.file.name,
                    body = photo.file.asRequestBody()
                )
                val loginPart = login.toRequestBody("text/plain".toMediaType())
                val passwordPart = password.toRequestBody("text/plain".toMediaType())
                val namePart = name.toRequestBody("text/plain".toMediaType())
                val response = api.registerWithPhoto(
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
                result?.let { appAuth.setAuth(it.id, it.token ?: "") }
                _tryRegistration.value = Unit
            } catch (e: IOException) {
                _errorRegistration.value = Unit
            } catch (e: Exception) {
                _errorRegistration.value = Unit
            }
        }
    }
}