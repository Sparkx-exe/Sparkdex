package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MangaListResponse(
    val data: List<MangaData> = emptyList(),
    val limit: Int = 0,
    val offset: Int = 0,
    val total: Int = 0
)

@JsonClass(generateAdapter = true)
data class MangaResponse(
    val data: MangaData
)

@JsonClass(generateAdapter = true)
data class MangaData(
    val id: String,
    val type: String,
    val attributes: MangaAttributes,
    val relationships: List<Relationship>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class MangaAttributes(
    val title: Map<String, String>? = emptyMap(),
    val altTitles: List<Map<String, String>>? = emptyList(),
    val description: Map<String, String>? = emptyMap(),
    val status: String? = "unknown",
    val year: Int? = null,
    val tags: List<Tag>? = emptyList(),
    val state: String? = null
)

@JsonClass(generateAdapter = true)
data class Tag(
    val id: String,
    val type: String,
    val attributes: TagAttributes? = null
)

@JsonClass(generateAdapter = true)
data class TagAttributes(
    val name: Map<String, String>? = emptyMap(),
    val group: String? = ""
)

@JsonClass(generateAdapter = true)
data class Relationship(
    val id: String,
    val type: String,
    val attributes: RelationshipAttributes? = null
)

@JsonClass(generateAdapter = true)
data class RelationshipAttributes(
    val fileName: String? = null,
    val name: String? = null,
    val biography: Map<String, String>? = emptyMap()
)

@JsonClass(generateAdapter = true)
data class ChapterListResponse(
    val data: List<ChapterData> = emptyList(),
    val limit: Int = 0,
    val offset: Int = 0,
    val total: Int = 0
)

@JsonClass(generateAdapter = true)
data class ChapterResponse(
    val data: ChapterData
)

@JsonClass(generateAdapter = true)
data class ChapterData(
    val id: String,
    val type: String,
    val attributes: ChapterAttributes,
    val relationships: List<Relationship>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class ChapterAttributes(
    val volume: String? = null,
    val chapter: String? = null,
    val title: String? = null,
    val translatedLanguage: String? = null,
    val publishAt: String? = null,
    val pages: Int? = 0,
    val externalUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class ChapterPagesResponse(
    val baseUrl: String = "https://uploads.mangadex.org",
    val chapter: PageSaverData
)

@JsonClass(generateAdapter = true)
data class PageSaverData(
    val hash: String,
    val dataSaver: List<String> = emptyList(),
    val data: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MangaStatisticsResponse(
    val statistics: Map<String, MangaStats> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class MangaStats(
    val rating: RatingStats,
    val follows: Int? = 0
)

@JsonClass(generateAdapter = true)
data class RatingStats(
    val average: Double? = 0.0,
    val bayesian: Double? = 0.0
)

@JsonClass(generateAdapter = true)
data class TagListResponse(
    val data: List<Tag> = emptyList()
)
