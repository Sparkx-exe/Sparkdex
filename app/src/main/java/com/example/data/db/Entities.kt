package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val mangaId: String,
    val title: String,
    val coverUrl: String,
    val lastReadChapter: String? = null,
    val bookmarkedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mangaId: String,
    val mangaTitle: String,
    val mangaCover: String,
    val chapterId: String,
    val chapterName: String,
    val pageNumber: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val genres: String // Comma separated tags/genres
)

@Entity(tableName = "api_cache")
data class CacheEntity(
    @PrimaryKey val cacheKey: String,
    val responseJson: String,
    val expiresAt: Long
)

@Entity(tableName = "genre_scores")
data class GenreScoreEntity(
    @PrimaryKey val tagId: String,
    val score: Int,
    val lastUpdatedAt: Long = System.currentTimeMillis()
)
