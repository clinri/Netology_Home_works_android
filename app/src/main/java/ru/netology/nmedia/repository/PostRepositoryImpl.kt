package ru.netology.nmedia.repository

import androidx.lifecycle.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import okio.IOException
import ru.netology.nmedia.api.*
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toDto
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError

class PostRepositoryImpl(private val dao: PostDao) : PostRepository {
    override val data = dao.getAllVisible().map(List<PostEntity>::toDto)
        .flowOn(Dispatchers.Default)

    override suspend fun getAll() {
        try {
            val response = PostsApi.service.getAll()
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

    override fun getNewerCount(latestId: Long): Flow<Int> = flow {
        while (true) {
            delay(10_000L)
            try {
                val response = PostsApi.service.getNewer(latestId)
                if (!response.isSuccessful) {
                    throw ApiError(response.code(), response.message())
                }

                val body = response.body() ?: throw ApiError(response.code(), response.message())
                dao.insert(body.toEntity().map {
                    it.copy(hidden = true)
                })
                emit(dao.getUnreadCount())
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                throw NetworkError
            } catch (e: Exception) {
                throw UnknownError
            }
        }
    }
        .flowOn(Dispatchers.Default)

    override suspend fun save(post: Post) {
        try {
            val response = PostsApi.service.save(post)
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

    override suspend fun removeById(id: Long) {
        try {
            //Сначала удаляем запись в локальной БД.
            dao.removeById(id)
            //После удаления из БД отправляем соответствующий запрос в API (HTTP).
            val response = PostsApi.service.removeById(id)
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
            val response = PostsApi.service.likeById(id)
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
            val response = PostsApi.service.dislikeById(id)
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
}
