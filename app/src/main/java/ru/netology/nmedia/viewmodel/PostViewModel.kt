package ru.netology.nmedia.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import arrow.core.Either
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.MediaModel
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.File

private val empty = Post(
    id = 0,
    content = "",
    authorId = 0L,
    author = "",
    authorAvatar = "",
    likedByMe = false,
    likes = 0,
    published = ""
)

class PostViewModel(application: Application) : AndroidViewModel(application) {
    // упрощённый вариант
    private val repository: PostRepository =
        PostRepositoryImpl(AppDb.getInstance(context = application).postDao())

    @OptIn(ExperimentalCoroutinesApi::class)
    val data: LiveData<FeedModel> = AppAuth.getInstance().authStateFlow.flatMapLatest { authState ->
        repository.data
            .map { posts ->
                FeedModel(posts.map {
                    it.copy(ownedByMe = authState?.id == it.authorId)
                }, posts.isEmpty())
            }
    }.asLiveData(Dispatchers.Default)

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    val newerCount = repository.newerCount.asLiveData()

    private val _errorGetNewer = SingleLiveEvent<Unit>()
    val errorGetNewer: LiveData<Unit>
        get() = _errorGetNewer

    private val _media = MutableLiveData<MediaModel?>(null)
    val media: LiveData<MediaModel?>
        get() = _media

    private val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    private val _showOfferAuth = SingleLiveEvent<Unit>()
    val showOfferAuth: LiveData<Unit>
        get() = _showOfferAuth

    private val _showFragmentPostCreate = SingleLiveEvent<Unit>()
    val showFragmentPostCreate: LiveData<Unit>
        get() = _showFragmentPostCreate

    private val _backToFeedFragmentFromDialogConfirmation = SingleLiveEvent<Unit>()
    val backToFeedFragmentFromDialogConfirmation: LiveData<Unit>
        get() = _backToFeedFragmentFromDialogConfirmation

    private val _toDialogConfirmationFromNewPostFragment = SingleLiveEvent<Unit>()
    val toDialogConfirmationFromNewPostFragment: LiveData<Unit>
        get() = _toDialogConfirmationFromNewPostFragment

    private val _toDialogConfirmationFromFeedFragment = SingleLiveEvent<Unit>()
    val toDialogConfirmationFromFeedFragment: LiveData<Unit>
        get() = _toDialogConfirmationFromFeedFragment

    init {
        loadPosts()
        viewModelScope.launch {
            @OptIn(ExperimentalCoroutinesApi::class)
            repository.data.flatMapLatest { posts ->
                val latestPostId = posts.firstOrNull()?.id ?: 0L
                repository.requestNewer(latestPostId).mapNotNull { either ->
                    when (either) {
                        is Either.Left -> {
                            either.value
                        }
                        is Either.Right -> null
                    }
                }
            }.collect {
                // уведомление об ошибке при загрузке новых постов
                _errorGetNewer.value = Unit
            }
        }
    }

    fun changePhoto(uri: Uri, file: File) {
        _media.value = MediaModel(uri, file)
    }

    fun clearPhoto() {
        _media.value = null
    }

    fun clickOnButtonNewPosts() {
        AppAuth.getInstance().authStateFlow.value?.let {
            viewModelScope.launch {
                try {
                    repository.readAll()
                } catch (e: Exception) {
                    _dataState.value = FeedModelState(error = true)
                }
            }
        } ?: let {
            _showOfferAuth.value = Unit
        }
    }

    fun loadPosts() = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.getAll()
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun refreshPosts() = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(refreshing = true)
            repository.getAll()
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun save() {
        edited.value?.let { post ->
            _postCreated.value = Unit
            viewModelScope.launch {
                try {
                    when (val media = _media.value) {
                        null -> repository.save(post)
                        else -> repository.saveWithAttachment(post, media)
                    }
                    _dataState.value = FeedModelState()
                } catch (e: Exception) {
                    _dataState.value = FeedModelState(error = true)
                }
            }
        }
        edited.value = empty
        clearPhoto()
    }

    fun edit(post: Post) {
        edited.value = post
    }

    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) {
            return
        }
        edited.value = edited.value?.copy(content = text)
    }

    fun like(post: Post) {
        AppAuth.getInstance().authStateFlow.value?.let {
            viewModelScope.launch {
                try {
                    if (!post.likedByMe) {
                        repository.likeById(post.id)
                    } else {
                        repository.dislikeById(post.id)
                    }
                } catch (e: Exception) {
                    _dataState.value = FeedModelState(error = true)
                }
            }
        } ?: let {
            _showOfferAuth.value = Unit
        }
    }

    fun removeById(id: Long) = viewModelScope.launch {
        try {
            repository.removeById(id)
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun onFabClicked() {
        AppAuth.getInstance().authStateFlow.value?.let {
            _showFragmentPostCreate.value = Unit
        } ?: let {
            _showOfferAuth.value = Unit
        }
    }

    fun logoutFromNewPostFragment() {
        AppAuth.getInstance().removeAuth()
        _backToFeedFragmentFromDialogConfirmation.value = Unit
    }

    fun toDialogConfirmationFromNewPostFragment() {
        _toDialogConfirmationFromNewPostFragment.value = Unit
    }

    fun toDialogConfirmationFromFeedFragment() {
        _toDialogConfirmationFromFeedFragment.value = Unit
    }
}
