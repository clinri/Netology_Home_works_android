package ru.netology.nmedia.repository

import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.MediaModel

interface PostRepository {
    val data: Flow<List<Post>>
    fun getNewerCount(latestId: Long): Flow<Int>
    suspend fun getAll()
    suspend fun save(post: Post)
    suspend fun saveWithAttachment(post: Post, mediaModel: MediaModel)
    suspend fun removeById(id: Long)
    suspend fun likeById(id: Long)
    suspend fun dislikeById(id: Long)
    suspend fun readAll()
    suspend fun uploadMedia(media: MediaModel): Media
}
