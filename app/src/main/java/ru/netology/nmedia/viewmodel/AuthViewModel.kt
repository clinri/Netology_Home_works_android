package ru.netology.nmedia.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.Dispatchers
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.model.AuthModel

class AuthViewModel : ViewModel() {
    val data: LiveData<AuthModel?> = AppAuth.getInstance()
        .data
        .asLiveData(Dispatchers.Default)

    val authorized: Boolean
        get() = data.value != null
}