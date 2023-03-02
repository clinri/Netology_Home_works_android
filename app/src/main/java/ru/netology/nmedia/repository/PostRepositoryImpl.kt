package ru.netology.nmedia.repository

import androidx.lifecycle.*
import arrow.core.Either
import arrow.core.left
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nmedia.api.*
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toDto
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.*
import ru.netology.nmedia.model.MediaModel
import java.io.IOException

class PostRepositoryImpl(private val dao: PostDao) : PostRepository {
    override val data = dao.getAllVisible().map(List<PostEntity>::toDto)
        .flowOn(Dispatchers.Default)

    override val newerCount: Flow<Int> = dao.getUnreadCount()

    override suspend fun getAll() {
        try {
            val response = RetrofitApi.service.getAll()
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            //загруженные данные не показываем, если раньше не показывались
            val visibleListIsEmpty = data.asLiveData().value?.isEmpty() ?: true
            if (visibleListIsEmpty) {
                dao.insert(body.toEntity())
            } else {
                val oldData = dao.getAllVisible().asLiveData().value
                dao.insert(body.toEntity().map {
                    it.copy(hidden = oldData?.find { oldPostEntity ->
                        oldPostEntity.id == it.id
                    }?.hidden ?: true)
                })
            }
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override fun requestNewer(latestId: Long): Flow<Either<Exception, Nothing>> = flow {
        while (true) {
            delay(10_000L)
            try {
                val response = RetrofitApi.service.getNewer(latestId)
                if (!response.isSuccessful) {
                    throw ApiError(response.code(), response.message())
                }
                val body = response.body() ?: throw ApiError(response.code(), response.message())
                dao.insert(body.toEntity().map {
                    it.copy(hidden = true)
                })
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                emit(NetworkError.left())
            } catch (e: Exception) {
                emit(UnknownError.left())
            }
        }
    }
        .flowOn(Dispatchers.Default)

    override suspend fun save(post: Post) {
        try {
            val response = RetrofitApi.service.save(post)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(PostEntity.fromDto(body))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun saveWithAttachment(post: Post, mediaModel: MediaModel) {
        try {
            val media = uploadMedia(mediaModel)
            //TODO: add support for other types
            val postWithAttachment =
                post.copy(attachment = Attachment(media.id, AttachmentType.IMAGE))
            save(postWithAttachment)
        } catch (e: AppError) {
            throw e
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }


    override suspend fun removeById(id: Long) {
        try {
            //Сначала удаляем запись в локальной БД.
            dao.removeById(id)
            //После удаления из БД отправляем соответствующий запрос в API (HTTP).
            val response = RetrofitApi.service.removeById(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun likeById(id: Long) {
        try {
            //Сначала модифицируем запись в локальной БД.
            dao.likeById(id)
            //После удаления из БД отправляем соответствующий запрос в API (HTTP).
            val response = RetrofitApi.service.likeById(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun dislikeById(id: Long) {
        try {
            //Сначала модифицируем запись в локальной БД.
            dao.likeById(id)
            //После удаления из БД отправляем соответствующий запрос в API (HTTP).
            val response = RetrofitApi.service.dislikeById(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun readAll() {
        try {
            dao.readAll()
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun uploadMedia(media: MediaModel): Media = try {
        val part = MultipartBody.Part.createFormData(
            name = "file",
            filename = media.file.name,
            body = media.file.asRequestBody()
        )
        val response = RetrofitApi.service.uploadMedia(part)
        if (!response.isSuccessful) {
            throw ApiError(response.code(), response.message())
        }

        response.body() ?: throw ApiError(response.code(), response.message())
    } catch (e: IOException) {
        throw NetworkError
    } catch (e: Exception) {
        throw UnknownError
    }
}
