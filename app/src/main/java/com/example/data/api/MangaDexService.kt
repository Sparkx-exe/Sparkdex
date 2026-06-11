package com.example.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface MangaDexService {
    @GET("manga")
    suspend fun getMangaList(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("title") title: String? = null,
        @Query("includes[]") includes: List<String> = listOf("cover_art", "author"),
        @QueryMap order: Map<String, String> = emptyMap(),
        @Query("includedTags[]") includedTags: List<String>? = null,
        @Query("status[]") status: List<String>? = null,
        @Query("availableTranslatedLanguage[]") languages: List<String>? = null
    ): MangaListResponse

    @GET("manga/{id}")
    suspend fun getMangaDetails(
        @Path("id") id: String,
        @Query("includes[]") includes: List<String> = listOf("cover_art", "author")
    ): MangaResponse

    @GET("manga/{id}/feed")
    suspend fun getMangaFeed(
        @Path("id") id: String,
        @Query("limit") limit: Int = 96,
        @Query("offset") offset: Int = 0,
        @Query("includes[]") includes: List<String> = listOf("scanlation_group"),
        @Query("order[chapter]") chapterOrder: String = "asc",
        @Query("order[volume]") volumeOrder: String = "asc",
        @Query("translatedLanguage[]") translatedLanguages: List<String>? = null
    ): ChapterListResponse

    @GET("manga/tag")
    suspend fun getTagList(): TagListResponse

    @GET("at-home/server/{chapterId}")
    suspend fun getChapterPages(
        @Path("chapterId") chapterId: String
    ): ChapterPagesResponse

    @GET("statistics/manga/{id}")
    suspend fun getMangaStatistics(
        @Path("id") id: String
    ): MangaStatisticsResponse
}
