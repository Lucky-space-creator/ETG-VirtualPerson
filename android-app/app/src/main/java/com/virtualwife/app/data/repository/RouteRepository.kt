package com.virtualwife.app.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.virtualwife.app.data.local.PreferencesManager
import com.virtualwife.app.data.remote.RetrofitClient
import com.virtualwife.app.data.remote.dto.RouteDto
import com.virtualwife.app.data.remote.dto.SpotDto
import kotlinx.coroutines.flow.first

class RouteRepository(private val prefs: PreferencesManager) {

    /**
     * 根据用户兴趣推荐路线
     */
    suspend fun getRecommendedRoutes(): Result<List<RouteDto>> {
        return try {
            val interestTags = prefs.interestTags.first()
            val response = RetrofitClient.adminApi.getRoutes(pageNum = 1, pageSize = 50)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val routes = response.body()!!.data?.records ?: emptyList()

                // 根据用户兴趣匹配路线
                val recommended = if (interestTags.isNotEmpty()) {
                    routes.sortedByDescending { route ->
                        val routeTags = parseTags(route.interestTags)
                        routeTags.count { it in interestTags }
                    }
                } else {
                    routes
                }

                Result.success(recommended)
            } else {
                val msg = response.body()?.message ?: "获取路线失败"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseTags(tagsJson: String?): List<String> {
        if (tagsJson.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(tagsJson, type)
        } catch (e: Exception) {
            tagsJson.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }

    suspend fun getRoutes(
        pageNum: Int = 1,
        pageSize: Int = 10,
        keyword: String? = null
    ): Result<List<RouteDto>> {
        return try {
            // 使用adminApi（带认证）
            val response = RetrofitClient.adminApi.getRoutes(
                pageNum = pageNum,
                pageSize = pageSize,
                keyword = keyword
            )
            when {
                response.isSuccessful && response.body()?.isSuccess == true -> {
                    val routes = response.body()!!.data?.records ?: emptyList()
                    Result.success(routes)
                }
                else -> {
                    val msg = response.body()?.message ?: "获取路线失败"
                    Result.failure(Exception(msg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRoutesByInterest(interestTag: String): Result<List<RouteDto>> {
        return getRoutes(keyword = interestTag)
    }

    suspend fun getSpotsByRoute(routeId: Long): Result<List<SpotDto>> {
        return try {
            // 使用adminApi（带认证）
            val response = RetrofitClient.adminApi.getSpotsByRoute(routeId = routeId)
            when {
                response.isSuccessful && response.body()?.isSuccess == true -> {
                    val spots = response.body()!!.data?.records ?: emptyList()
                    Result.success(spots)
                }
                else -> {
                    val msg = response.body()?.message ?: "获取景点失败"
                    Result.failure(Exception(msg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveInterestTags(tags: Set<String>) {
        prefs.saveInterestTags(tags)
    }

    suspend fun getInterestTags(): Set<String> {
        return prefs.interestTags.first()
    }
}
