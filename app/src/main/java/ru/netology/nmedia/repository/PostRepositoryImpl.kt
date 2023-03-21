package ru.netology.nmedia.repository

import android.util.Log
import androidx.paging.*
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.AppError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError
import ru.netology.nmedia.model.MediaModel
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val dao: PostDao,
    private val apiService: ApiService,
    private val postRemoteKeyDao: PostRemoteKeyDao,
    appDb: AppDb,
) : PostRepository {

    @OptIn(ExperimentalPagingApi::class)
    override val data: Flow<PagingData<Post>> = Pager(
        config = PagingConfig(pageSize = 5, enablePlaceholders = false),
        pagingSourceFactory = { dao.getPagingSource() },
        remoteMediator = PostRemoteMediator(
            apiService = apiService,
            postDao = dao,
            postRemoteKeyDao = postRemoteKeyDao,
            appDb = appDb
        )
    ).flow
        .map { it.map(PostEntity::toDto) }

//    override val newerCount: Flow<Int> = dao.getUnreadCount()

//    override suspend fun getAll() {
//        try {
//            val response = apiService.getAll()
//            if (!response.isSuccessful) {
//                throw ApiError(response.code(), response.message())
//            }
//            val data = response.body() ?: throw ApiError(response.code(), response.message())
            //загруженные данные не показываем, если раньше не показывались
//            val visibleListIsEmpty = data.asLiveData().value?.isEmpty() ?: true
//            if (visibleListIsEmpty) {
//                dao.insert(body.toEntity())
//            } else {
//                val oldData = dao.getAllVisible().asLiveData().value
//                dao.insert(body.toEntity().map {
//                    it.copy(hidden = oldData?.find { oldPostEntity ->
//                        oldPostEntity.id == it.id
//                    }?.hidden ?: true)
//                })
//            }
//            dao.insert(data.toEntity())
//        } catch (e: IOException) {
//            throw NetworkError
//        } catch (e: Exception) {
//            throw UnknownError
//        }
//    }

    override fun requestNewerCount(): Flow<Either<Exception, Int>> = flow {
        while (true) {
            delay(10_000L)
            try {
                val response = apiService.getNewerCount(postRemoteKeyDao.max() ?: 0L)
                if (!response.isSuccessful) {
                    throw ApiError(response.code(), response.message())
                }
                val body = response.body() ?: throw ApiError(response.code(), response.message())
                Log.d("newerCount", body.toString())
                emit(body.count.right())
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
            val response = apiService.save(post)
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
            val response = apiService.removeById(id)
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
            val response = apiService.likeById(id)
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
            val response = apiService.dislikeById(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

/*
    override suspend fun readAll() {
        try {
            dao.readAll()
        } catch (e: Exception) {
            throw UnknownError
        }
    }
*/

    override suspend fun uploadMedia(media: MediaModel): Media = try {
        val part = MultipartBody.Part.createFormData(
            name = "file",
            filename = media.file.name,
            body = media.file.asRequestBody()
        )
        val response = apiService.uploadMedia(part)
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
