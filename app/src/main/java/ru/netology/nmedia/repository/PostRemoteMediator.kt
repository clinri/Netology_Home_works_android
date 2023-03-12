package ru.netology.nmedia.repository

import androidx.paging.*
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.error.ApiError
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val apiService: ApiService,
    private val postDao: PostDao,
) : RemoteMediator<Int, PostEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        try {
            val result = when (loadType) {
                //пользователь делает свайп (для обновления списка)
                LoadType.REFRESH -> {
                    apiService.getLatest(state.config.pageSize)
                }
                //пользователь листает вниз
                LoadType.APPEND -> {
                    val id = state.lastItemOrNull()?.id ?: return MediatorResult.Success(false)
                    apiService.getBefore(id, state.config.pageSize)
                }
                //пользователь листает веерх
                LoadType.PREPEND -> {
                    val id = state.firstItemOrNull()?.id ?: return MediatorResult.Success(false)
                    apiService.getAfter(id, state.config.pageSize)
                }
            }
            if (!result.isSuccessful) {
                throw ApiError(result.code(), result.message())
            }

            val data = result.body() ?: throw ApiError(
                result.code(),
                result.message()
            )

            postDao.insert(data.map(PostEntity::fromDto))

            return MediatorResult.Success(data.isEmpty())
        } catch (e: IOException) {
            return MediatorResult.Error(e)
        }
    }
}