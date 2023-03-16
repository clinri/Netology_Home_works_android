package ru.netology.nmedia.repository

import androidx.paging.*
import androidx.room.withTransaction
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.PostRemoteKeyEntity
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val apiService: ApiService,
    private val postDao: PostDao,
    private val postRemoteKeyDao: PostRemoteKeyDao,
    private val appDb: AppDb,
) : RemoteMediator<Int, PostEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>,
    ): MediatorResult {
        try {
            val response = when (loadType) {
                //пользователь делает свайп (для обновления списка)
                // функционал как при попытке прокрутить вверх
                LoadType.REFRESH -> {
                    if (postDao.isEmpty()) {
                        apiService.getLatest(state.config.initialLoadSize)
                    } else {
                        val id = postRemoteKeyDao.max() ?: return MediatorResult.Success(
                            endOfPaginationReached = false
                        )
                        println("id = $id")
                        apiService.getAfter(id, state.config.pageSize)
                    }
                }
                //пользователь листает вниз
                LoadType.APPEND -> {
                    val id = postRemoteKeyDao.min() ?: return MediatorResult.Success(
                        endOfPaginationReached = false
                    )
                    apiService.getBefore(id, state.config.pageSize)
                }
                //пользователь листает веерх
                //отключено
                LoadType.PREPEND -> {
                    return MediatorResult.Success(
                        endOfPaginationReached = false
                    )
                }
            }
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val data = response.body() ?: throw ApiError(
                response.code(),
                response.message()
            )
            println("+++++++++===========")
            println(data.size)
            println(loadType.name)

            appDb.withTransaction {
                when (loadType) {
                    LoadType.REFRESH -> {
                        if (postDao.isEmpty()) {
                            postRemoteKeyDao.insert(
                                listOf(
                                    PostRemoteKeyEntity(
                                        type = PostRemoteKeyEntity.KeyType.AFTER,
                                        key = data.first().id
                                    ),
                                    PostRemoteKeyEntity(
                                        type = PostRemoteKeyEntity.KeyType.BEFORE,
                                        key = data.last().id
                                    )
                                )
                            )
                        } else {
                            postRemoteKeyDao.insert(
                                PostRemoteKeyEntity(
                                    type = PostRemoteKeyEntity.KeyType.AFTER,
                                    key = data.first().id
                                )
                            )
                        }
                    }
                    LoadType.APPEND -> {
                        postRemoteKeyDao.insert(
                            PostRemoteKeyEntity(
                                type = PostRemoteKeyEntity.KeyType.BEFORE,
                                key = data.last().id
                            )
                        )
                    }
                    else -> {}
                }
                postDao.insert(data.toEntity())
            }
            return MediatorResult.Success(endOfPaginationReached = data.isEmpty())
        } catch (e: Exception) {
            return MediatorResult.Error(e)
        }
    }
}