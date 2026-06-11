package com.example.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaDao {
    // Bookmarks
    @Query("SELECT * FROM bookmarks ORDER BY bookmarkedAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE mangaId = :mangaId LIMIT 1")
    suspend fun getBookmarkById(mangaId: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE mangaId = :mangaId")
    suspend fun deleteBookmark(mangaId: String)

    // History
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryById(id: Int)

    @Query("DELETE FROM history")
    suspend fun clearAllHistory()

    // Cache
    @Query("SELECT * FROM api_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun getCache(key: String): CacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: CacheEntity)

    @Query("DELETE FROM api_cache WHERE cacheKey = :key")
    suspend fun deleteCache(key: String)

    @Query("DELETE FROM api_cache WHERE cacheKey LIKE :pattern")
    suspend fun deleteCacheByPattern(pattern: String)

    @Query("DELETE FROM api_cache WHERE expiresAt < :currentTime")
    suspend fun deleteExpiredCache(currentTime: Long)

    @Query("DELETE FROM api_cache")
    suspend fun clearAllCache()

    // Genre Scores
    @Query("SELECT * FROM genre_scores ORDER BY score DESC")
    suspend fun getAllGenreScores(): List<GenreScoreEntity>

    @Query("SELECT * FROM genre_scores WHERE tagId = :tagId LIMIT 1")
    suspend fun getGenreScoreById(tagId: String): GenreScoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenreScore(score: GenreScoreEntity)

    @Query("DELETE FROM genre_scores")
    suspend fun clearGenreScores()
}

@Database(
    entities = [BookmarkEntity::class, HistoryEntity::class, CacheEntity::class, GenreScoreEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mangaDao(): MangaDao
}
