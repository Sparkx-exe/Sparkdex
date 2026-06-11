package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.api.ChapterData
import com.example.data.api.ChapterPagesResponse
import com.example.data.api.MangaData
import com.example.data.api.MangaDexService
import com.example.data.api.MangaStats
import com.example.data.api.Tag
import com.example.data.db.BookmarkEntity
import com.example.data.db.CacheEntity
import com.example.data.db.GenreScoreEntity
import com.example.data.db.HistoryEntity
import com.example.data.db.MangaDao
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MangaRepository(
    private val apiService: MangaDexService,
    private val dao: MangaDao,
    private val moshi: Moshi,
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mangazen_prefs", Context.MODE_PRIVATE)
    private val tag = "MangaRepository"
    private val cacheDuration = 10 * 60 * 1000L // 10 minutes TTL

    // Helper to serialize and cache API responses generically
    private suspend inline fun <reified T> fetchWithCache(
        cacheKey: String,
        forceRefresh: Boolean,
        crossinline apiCall: suspend () -> T
    ): T {
        if (!forceRefresh) {
            val cached = dao.getCache(cacheKey)
            if (cached != null && cached.expiresAt > System.currentTimeMillis()) {
                try {
                    val adapter = moshi.adapter(T::class.java)
                    val result = adapter.fromJson(cached.responseJson)
                    if (result != null) {
                        Log.d(tag, "Cache hit for key: $cacheKey")
                        return result
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Failed to parse cache for key: $cacheKey", e)
                }
            }
        }

        // Fetch fresh
        Log.d(tag, "Cache miss/expired or force refreshed for key: $cacheKey. Fetching fresh.")
        val response = apiCall()
        try {
            val adapter = moshi.adapter(T::class.java)
            val json = adapter.toJson(response)
            val expires = System.currentTimeMillis() + cacheDuration
            dao.insertCache(CacheEntity(cacheKey, json, expires))
        } catch (e: Exception) {
            Log.e(tag, "Failed to cache response for key: $cacheKey", e)
        }
        return response
    }

    // Daily date-based cache validation for "Popular Today"
    private fun checkPopularTodayDateRefresh(): Boolean {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val savedDate = prefs.getString("popular_today_date", "")
        if (todayStr != savedDate) {
            prefs.edit().putString("popular_today_date", todayStr).apply()
            return true // Date changed, invalid cache
        }
        return false
    }

    suspend fun getPopularManga(offset: Int, forceRefresh: Boolean = false): List<MangaData> = withContext(Dispatchers.IO) {
        val dateChanged = checkPopularTodayDateRefresh()
        val finalForce = forceRefresh || dateChanged
        val cacheKey = "popular_manga_offset_$offset"
        try {
            if (finalForce) {
                dao.deleteCache(cacheKey)
                if (offset == 0) {
                    dao.deleteCacheByPattern("popular_manga_offset_%")
                }
            }
            val response = fetchWithCache(cacheKey, finalForce) {
                val freshResponse = apiService.getMangaList(
                    limit = 80,
                    offset = offset,
                    order = mapOf("order[followedCount]" to "desc")
                )
                freshResponse.copy(data = freshResponse.data.shuffled())
            }
            response.data
        } catch (e: Exception) {
            Log.e(tag, "Error fetching popular manga", e)
            // Fallback to expired cache if available
            val cached = dao.getCache(cacheKey)
            if (cached != null) {
                try {
                    val adapter = moshi.adapter(com.example.data.api.MangaListResponse::class.java)
                    adapter.fromJson(cached.responseJson)?.data ?: throw e
                } catch (jsonEx: Exception) {
                    throw e
                }
            } else {
                throw e
            }
        }
    }

    suspend fun getLatestUpdates(offset: Int, forceRefresh: Boolean = false): List<MangaData> = withContext(Dispatchers.IO) {
        val cacheKey = "latest_manga_offset_$offset"
        // Generate ISO8601 for yesterday inside API
        val yesterdayMillis = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val yesterdayStr = dateFormat.format(Date(yesterdayMillis))
        
        try {
            if (forceRefresh) {
                dao.deleteCache(cacheKey)
                if (offset == 0) {
                    dao.deleteCacheByPattern("latest_manga_offset_%")
                }
            }
            val response = fetchWithCache(cacheKey, forceRefresh) {
                // Request details containing cover_art, ordered by updatedAt desc
                val orderMap = mapOf("order[latestUploadedChapter]" to "desc")
                apiService.getMangaList(
                    limit = 20,
                    offset = offset,
                    order = orderMap
                )
            }
            response.data
        } catch (e: Exception) {
            Log.e(tag, "Error fetching latest manga updates", e)
            val cached = dao.getCache(cacheKey)
            if (cached != null) {
                try {
                    val adapter = moshi.adapter(com.example.data.api.MangaListResponse::class.java)
                    adapter.fromJson(cached.responseJson)?.data ?: throw e
                } catch (jsonEx: Exception) {
                    throw e
                }
            } else {
                throw e
            }
        }
    }

    suspend fun getMangaDetails(id: String, forceRefresh: Boolean = false): MangaData = withContext(Dispatchers.IO) {
        val cacheKey = "manga_detail_$id"
        try {
            if (forceRefresh) {
                dao.deleteCache(cacheKey)
            }
            val response = fetchWithCache(cacheKey, forceRefresh) {
                apiService.getMangaDetails(id)
            }
            response.data
        } catch (e: Exception) {
            Log.e(tag, "Error fetching manga detail for $id", e)
            val cached = dao.getCache(cacheKey)
            if (cached != null) {
                try {
                    val adapter = moshi.adapter(com.example.data.api.MangaResponse::class.java)
                    adapter.fromJson(cached.responseJson)?.data ?: throw e
                } catch (jsonEx: Exception) {
                    throw e
                }
            } else {
                throw e
            }
        }
    }

    suspend fun getMangaFeed(id: String, language: String? = null, forceRefresh: Boolean = false): List<ChapterData> = withContext(Dispatchers.IO) {
        val finalLanguage = if (language?.lowercase() == "all") null else language
        val cacheKey = "manga_feed_${id}_lang_${finalLanguage ?: "all"}"
        try {
            if (forceRefresh) {
                dao.deleteCache(cacheKey)
            }
            val langsList = if (finalLanguage != null) listOf(finalLanguage) else null
            val response = fetchWithCache(cacheKey, forceRefresh) {
                val firstBatch = apiService.getMangaFeed(
                    id = id,
                    limit = 500,
                    offset = 0,
                    translatedLanguages = langsList
                )
                val allChapters = firstBatch.data.toMutableList()
                var currentOffset = firstBatch.data.size
                val totalChapters = firstBatch.total
                
                while (currentOffset < totalChapters) {
                    val nextBatch = apiService.getMangaFeed(
                        id = id,
                        limit = 500,
                        offset = currentOffset,
                        translatedLanguages = langsList
                    )
                    if (nextBatch.data.isEmpty()) break
                    allChapters.addAll(nextBatch.data)
                    currentOffset += nextBatch.data.size
                }
                
                firstBatch.copy(
                    data = allChapters,
                    limit = allChapters.size,
                    offset = 0,
                    total = totalChapters
                )
            }
            response.data
        } catch (e: Exception) {
            Log.e(tag, "Error fetching manga feed for $id", e)
            val cached = dao.getCache(cacheKey)
            if (cached != null) {
                try {
                    val adapter = moshi.adapter(com.example.data.api.ChapterListResponse::class.java)
                    adapter.fromJson(cached.responseJson)?.data ?: throw e
                } catch (jsonEx: Exception) {
                    throw e
                }
            } else {
                throw e
            }
        }
    }

    suspend fun getMangaStatistics(id: String, forceRefresh: Boolean = false): MangaStats? = withContext(Dispatchers.IO) {
        val cacheKey = "manga_stats_$id"
        try {
            if (forceRefresh) {
                dao.deleteCache(cacheKey)
            }
            val response = fetchWithCache(cacheKey, forceRefresh) {
                apiService.getMangaStatistics(id)
            }
            response.statistics[id]
        } catch (e: Exception) {
            Log.e(tag, "Error fetching manga stats for $id", e)
            null
        }
    }

    suspend fun getSeasonalManga(forceRefresh: Boolean = false): List<MangaData> = withContext(Dispatchers.IO) {
        val cacheKey = "manga_seasonal"
        try {
            if (forceRefresh) {
                dao.deleteCache(cacheKey)
            }
            val response = fetchWithCache(cacheKey, forceRefresh) {
                apiService.getMangaList(
                    limit = 10,
                    order = mapOf("order[createdAt]" to "desc")
                )
            }
            response.data
        } catch (e: Exception) {
            val cached = dao.getCache(cacheKey)
            if (cached != null) {
                try {
                    val adapter = moshi.adapter(com.example.data.api.MangaListResponse::class.java)
                    adapter.fromJson(cached.responseJson)?.data ?: emptyList()
                } catch (err: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    suspend fun searchManga(
        query: String,
        offset: Int = 0,
        tags: List<String>? = null,
        sortBy: String = "relevance",
        status: String = "All",
        languages: List<String>? = null
    ): List<MangaData> = withContext(Dispatchers.IO) {
        val orderMap = when (sortBy) {
            "relevance" -> mapOf("order[relevance]" to "desc")
            "latest" -> mapOf("order[latestUploadedChapter]" to "desc")
            "rating" -> mapOf("order[rating]" to "desc")
            "followed" -> mapOf("order[followedCount]" to "desc")
            "added" -> mapOf("order[createdAt]" to "desc")
            else -> mapOf("order[relevance]" to "desc")
        }

        val statusList = when (status) {
            "Ongoing" -> listOf("ongoing")
            "Completed" -> listOf("completed")
            "Hiatus" -> listOf("hiatus")
            else -> null
        }

        try {
            val response = apiService.getMangaList(
                limit = 20,
                offset = offset,
                title = query.ifBlank { null },
                includedTags = tags?.ifEmpty { null },
                order = orderMap,
                status = statusList,
                languages = languages?.ifEmpty { null }
            )
            response.data
        } catch (e: Exception) {
            Log.e(tag, "Error searching manga with query $query", e)
            emptyList()
        }
    }

    suspend fun getTags(forceRefresh: Boolean = false): List<Tag> = withContext(Dispatchers.IO) {
        val cacheKey = "genre_tags"
        try {
            if (forceRefresh) {
                dao.deleteCache(cacheKey)
            }
            val response = fetchWithCache(cacheKey, forceRefresh) {
                apiService.getTagList()
            }
            response.data
        } catch (e: Exception) {
            Log.e(tag, "Error loading tags", e)
            emptyList()
        }
    }

    suspend fun getChapterPages(chapterId: String): ChapterPagesResponse = withContext(Dispatchers.IO) {
        apiService.getChapterPages(chapterId)
    }

    // Genre score preference recommendations
    suspend fun getRecommendedManga(forceRefresh: Boolean = false): List<MangaData> = withContext(Dispatchers.IO) {
        val check30DaysReset = System.currentTimeMillis() - prefs.getLong("last_history_read_time", 0L) > (30 * 24 * 60 * 60 * 1000L)
        if (check30DaysReset) {
            dao.clearGenreScores()
        }

        val topScores = dao.getAllGenreScores().take(3)
        if (topScores.isEmpty()) {
            // Fallback to top followed count
            return@withContext getPopularManga(0, forceRefresh).take(10)
        }

        val cacheKey = "genre_scores_recommendations"
        try {
            if (forceRefresh) {
                dao.deleteCache(cacheKey)
            }
            val response = fetchWithCache(cacheKey, forceRefresh) {
                apiService.getMangaList(
                    limit = 10,
                    includedTags = topScores.map { it.tagId },
                    order = mapOf("order[relevance]" to "desc")
                )
            }
            response.data
        } catch (e: Exception) {
            getPopularManga(0, forceRefresh).take(10)
        }
    }

    // Bookmarks and histories interface
    fun getBookmarksFlow(): Flow<List<BookmarkEntity>> = dao.getAllBookmarks()

    suspend fun isBookmarked(mangaId: String): Boolean = withContext(Dispatchers.IO) {
        dao.getBookmarkById(mangaId) != null
    }

    suspend fun toggleBookmark(mangaId: String, title: String, coverUrl: String) = withContext(Dispatchers.IO) {
        val existing = dao.getBookmarkById(mangaId)
        if (existing != null) {
            dao.deleteBookmark(mangaId)
        } else {
            dao.insertBookmark(BookmarkEntity(mangaId, title, coverUrl))
        }
    }

    suspend fun updateBookmarkLastRead(mangaId: String, chapterName: String) = withContext(Dispatchers.IO) {
        val existing = dao.getBookmarkById(mangaId)
        if (existing != null) {
            dao.insertBookmark(existing.copy(lastReadChapter = chapterName))
        }
    }

    fun getHistoryFlow(): Flow<List<HistoryEntity>> = dao.getAllHistory()

    suspend fun saveReadingProgress(
        mangaId: String,
        mangaTitle: String,
        mangaCover: String,
        chapterId: String,
        chapterName: String,
        pageNumber: Int,
        genres: List<String>
    ) = withContext(Dispatchers.IO) {
        // Build History Entity
        Log.d(tag, "Saving reading history: $mangaTitle - $chapterName, Page $pageNumber")
        
        // Comma-separated list of tags
        val genresStr = genres.joinToString(",")

        val entity = HistoryEntity(
            mangaId = mangaId,
            mangaTitle = mangaTitle,
            mangaCover = mangaCover,
            chapterId = chapterId,
            chapterName = chapterName,
            pageNumber = pageNumber,
            timestamp = System.currentTimeMillis(),
            genres = genresStr
        )
        dao.insertHistory(entity)

        // Increment genre scores
        prefs.edit().putLong("last_history_read_time", System.currentTimeMillis()).apply()
        for (genre in genres) {
            if (genre.isNotBlank()) {
                val scoreEntity = dao.getGenreScoreById(genre)
                if (scoreEntity != null) {
                    dao.insertGenreScore(scoreEntity.copy(score = scoreEntity.score + 1, lastUpdatedAt = System.currentTimeMillis()))
                } else {
                    dao.insertGenreScore(GenreScoreEntity(tagId = genre, score = 1))
                }
            }
        }
    }

    suspend fun deleteHistoryItem(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteHistoryById(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        dao.clearAllHistory()
    }

    suspend fun clearAppCache(): Int = withContext(Dispatchers.IO) {
        dao.clearAllCache()
        // Count number of items left in DB or return dummy success
        0
    }
}
