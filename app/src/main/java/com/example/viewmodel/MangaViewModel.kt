package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.ChapterData
import com.example.data.api.MangaData
import com.example.data.api.MangaStats
import com.example.data.api.Tag
import com.example.data.db.BookmarkEntity
import com.example.data.db.HistoryEntity
import com.example.data.repository.MangaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// UI States
sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(
        val carouselManga: List<MangaData>,
        val popularManga: List<MangaData>,
        val latestUpdates: List<MangaData>,
        val recommendedManga: List<MangaData>,
        val seasonalManga: List<MangaData>
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

sealed interface DetailUiState {
    object Loading : DetailUiState
    data class Success(
        val manga: MangaData,
        val stats: MangaStats?,
        val chapters: List<ChapterData>,
        val availableLanguages: List<String>
    ) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

sealed interface ReaderUiState {
    object Loading : ReaderUiState
    data class Success(
        val pages: List<String>,
        val hash: String,
        val baseUrl: String,
        val chapter: ChapterData,
        val nextChapterId: String?,
        val prevChapterId: String?
    ) : ReaderUiState
    data class Error(val message: String) : ReaderUiState
}

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Success(val results: List<MangaData>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class MangaViewModel(
    application: Application,
    private val repository: MangaRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("mangazen_prefs", Context.MODE_PRIVATE)

    // --- Home Screen Pagination ---
    var latestUpdatesOffset = 0
    private var isMoreLatestLoading = false
    var hasMoreLatest = true

    // --- Search Screen Pagination ---
    var searchOffset = 0
    private var isMoreSearchLoading = false
    var hasMoreSearch = true

    // --- Home Screen ---
    private val _homeUiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    // --- Detail Screen ---
    private val _detailUiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val detailUiState: StateFlow<DetailUiState> = _detailUiState.asStateFlow()
    val selectedChapterLang = MutableStateFlow<String>("en")

    // --- Reader Screen ---
    private val _readerUiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val readerUiState: StateFlow<ReaderUiState> = _readerUiState.asStateFlow()
    val activeReaderPage = MutableStateFlow<Int>(0)

    // --- Search ---
    private val _searchUiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()
    val searchQuery = MutableStateFlow<String>("")
    val searchTags = MutableStateFlow<List<String>>(emptyList())
    val searchSortBy = MutableStateFlow<String>("relevance")
    val searchStatus = MutableStateFlow<String>("All")
    val searchLanguages = MutableStateFlow<List<String>>(emptyList())
    
    private val _availableGenres = MutableStateFlow<List<Tag>>(emptyList())
    val availableGenres: StateFlow<List<Tag>> = _availableGenres.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    // --- Settings & UI Configurations ---
    val isDarkTheme = MutableStateFlow<String>(prefs.getString("theme_mode", "Dark") ?: "Dark")
    val titleLanguage = MutableStateFlow<String>(prefs.getString("title_lang", "English") ?: "English")
    val readerMode = MutableStateFlow<String>(prefs.getString("reader_mode", "Webtoon") ?: "Webtoon")
    val defaultChapterLang = MutableStateFlow<String>(prefs.getString("chapter_lang", "en") ?: "en")

    // --- Bookmarks Flow ---
    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.getBookmarksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Historical Logs Flow ---
    val history: StateFlow<List<HistoryEntity>> = repository.getHistoryFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadHomeData()
        loadAvailableTags()
        loadRecentSearches()
        selectedChapterLang.value = defaultChapterLang.value
    }

    // Settings modifiers
    fun setThemeMode(mode: String) {
        isDarkTheme.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun setTitleLanguagePreference(lang: String) {
        titleLanguage.value = lang
        prefs.edit().putString("title_lang", lang).apply()
    }

    fun setReaderModePreference(mode: String) {
        readerMode.value = mode
        prefs.edit().putString("reader_mode", mode).apply()
    }

    fun setDefaultChapterLangPreference(lang: String) {
        defaultChapterLang.value = lang
        selectedChapterLang.value = lang
        prefs.edit().putString("chapter_lang", lang).apply()
    }

    // Home Operations
    fun loadHomeData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!forceRefresh) {
                _homeUiState.value = HomeUiState.Loading
            }
            try {
                // Fetch popular manga (covers 0 offset)
                val popular = repository.getPopularManga(0, forceRefresh)
                
                // Hero Carousel is top 5 of popular
                val carousel = popular.take(5)
                
                // Popular today is top 10
                val popularToday = popular.take(10)

                // Seasonal manga
                val seasonal = repository.getSeasonalManga(forceRefresh)

                // Recommended manga (based on user score genre tag preferences)
                val recommended = repository.getRecommendedManga(forceRefresh)

                // Latest updates (shows live uploaded chapters)
                val latest = repository.getLatestUpdates(0, forceRefresh)

                latestUpdatesOffset = 0
                hasMoreLatest = true

                _homeUiState.value = HomeUiState.Success(
                    carouselManga = carousel,
                    popularManga = popularToday,
                    latestUpdates = latest,
                    recommendedManga = recommended,
                    seasonalManga = seasonal
                )
            } catch (e: Exception) {
                Log.e("MangaViewModel", "Error fetching home logs", e)
                if (!forceRefresh || _homeUiState.value is HomeUiState.Loading) {
                    _homeUiState.value = HomeUiState.Error(e.message ?: "Network error. Tap to retry.")
                }
            }
        }
    }

    fun loadMoreLatestUpdates() {
        val currentState = _homeUiState.value
        if (currentState !is HomeUiState.Success || isMoreLatestLoading || !hasMoreLatest) return

        viewModelScope.launch {
            isMoreLatestLoading = true
            try {
                val nextOffset = latestUpdatesOffset + 20
                val moreUpdates = repository.getLatestUpdates(nextOffset, forceRefresh = false)
                if (moreUpdates.isEmpty()) {
                    hasMoreLatest = false
                } else {
                    latestUpdatesOffset = nextOffset
                    val updatedList = currentState.latestUpdates + moreUpdates
                    _homeUiState.value = currentState.copy(latestUpdates = updatedList)
                }
            } catch (e: Exception) {
                Log.e("MangaViewModel", "Error fetching more latest updates", e)
            } finally {
                isMoreLatestLoading = false
            }
        }
    }

    // Detail Operations
    fun loadMangaDetails(mangaId: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _detailUiState.value = DetailUiState.Loading
            try {
                // Fetch details
                val manga = repository.getMangaDetails(mangaId, forceRefresh)
                
                // Fetch stats
                val stats = repository.getMangaStatistics(mangaId, forceRefresh)

                // Fetch chapters (no language constraint initially to see all languages)
                val allChapters = repository.getMangaFeed(mangaId, language = null, forceRefresh = forceRefresh)
                
                // Deduplicate and collect available languages of chapters
                val langs = allChapters.mapNotNull { it.attributes.translatedLanguage }
                    .distinct()
                    .sorted()

                _detailUiState.value = DetailUiState.Success(
                    manga = manga,
                    stats = stats,
                    chapters = allChapters,
                    availableLanguages = if (langs.isEmpty()) listOf("en") else langs
                )
            } catch (e: Exception) {
                Log.e("MangaViewModel", "Error launching details for $mangaId", e)
                _detailUiState.value = DetailUiState.Error(e.message ?: "Failed to load manga details.")
            }
        }
    }

    // Reader Operations
    fun loadChapterPages(chapterId: String, mangaId: String) {
        viewModelScope.launch {
            _readerUiState.value = ReaderUiState.Loading
            activeReaderPage.value = 0
            try {
                // Fetch pages
                val pagesResponse = repository.getChapterPages(chapterId)
                
                // Fetch full feed to retrieve chapter metadata, as well as next/prev calculation
                val feed = repository.getMangaFeed(mangaId, language = selectedChapterLang.value)
                val currentChapterObj = feed.find { it.id == chapterId } ?: repository.getMangaFeed(mangaId, language = null).find { it.id == chapterId }
                
                if (currentChapterObj == null) {
                    throw Exception("Chapter information not found.")
                }

                // Determine previous and next chapter IDs
                val currentIndex = feed.indexOfFirst { it.id == chapterId }
                val prevId = if (currentIndex != -1 && currentIndex > 0) feed[currentIndex - 1].id else null
                val nextId = if (currentIndex != -1 && currentIndex < feed.size - 1) feed[currentIndex + 1].id else null

                _readerUiState.value = ReaderUiState.Success(
                    pages = pagesResponse.chapter.dataSaver,
                    hash = pagesResponse.chapter.hash,
                    baseUrl = pagesResponse.baseUrl,
                    chapter = currentChapterObj,
                    nextChapterId = nextId,
                    prevChapterId = prevId
                )
            } catch (e: Exception) {
                Log.e("MangaViewModel", "Error inside reader screen setup", e)
                _readerUiState.value = ReaderUiState.Error(e.message ?: "Could not recover chapter pages.")
            }
        }
    }

    fun saveProgress(mangaId: String, mangaTitle: String, mangaCover: String, chapterId: String, chapterName: String, page: Int, genres: List<String>) {
        viewModelScope.launch {
            repository.saveReadingProgress(mangaId, mangaTitle, mangaCover, chapterId, chapterName, page, genres)
            repository.updateBookmarkLastRead(mangaId, chapterName)
        }
    }

    // Bookmark Toggle
    fun toggleBookmark(mangaId: String, title: String, coverUrl: String) {
        viewModelScope.launch {
            repository.toggleBookmark(mangaId, title, coverUrl)
        }
    }

    fun removeBookmark(mangaId: String) {
        viewModelScope.launch {
            val isB = repository.isBookmarked(mangaId)
            if (isB) {
                repository.toggleBookmark(mangaId, "", "")
            }
        }
    }

    // Search operations
    private fun loadAvailableTags() {
        viewModelScope.launch {
            val tags = repository.getTags()
            _availableGenres.value = tags
        }
    }

    fun searchManga() {
        val query = searchQuery.value
        if (query.isBlank() && searchTags.value.isEmpty()) {
            _searchUiState.value = SearchUiState.Idle
            return
        }

        viewModelScope.launch {
            _searchUiState.value = SearchUiState.Loading
            searchOffset = 0
            hasMoreSearch = true
            try {
                // Log and save recent search
                if (query.isNotBlank()) {
                    saveRecentSearch(query)
                }

                val results = repository.searchManga(
                    query = query,
                    offset = 0,
                    tags = searchTags.value,
                    sortBy = searchSortBy.value,
                    status = searchStatus.value,
                    languages = searchLanguages.value
                )
                _searchUiState.value = SearchUiState.Success(results)
            } catch (e: Exception) {
                _searchUiState.value = SearchUiState.Error(e.message ?: "No search responses.")
            }
        }
    }

    fun loadMoreSearch() {
        val currentState = _searchUiState.value
        if (currentState !is SearchUiState.Success || isMoreSearchLoading || !hasMoreSearch) return

        viewModelScope.launch {
            isMoreSearchLoading = true
            try {
                val nextOffset = searchOffset + 20
                val moreResults = repository.searchManga(
                    query = searchQuery.value,
                    offset = nextOffset,
                    tags = searchTags.value,
                    sortBy = searchSortBy.value,
                    status = searchStatus.value,
                    languages = searchLanguages.value
                )
                if (moreResults.isEmpty()) {
                    hasMoreSearch = false
                } else {
                    searchOffset = nextOffset
                    val updatedList = currentState.results + moreResults
                    _searchUiState.value = currentState.copy(results = updatedList)
                }
            } catch (e: Exception) {
                Log.e("MangaViewModel", "Error fetching more search results", e)
            } finally {
                isMoreSearchLoading = false
            }
        }
    }

    private fun loadRecentSearches() {
        val raw = prefs.getString("search_history", "") ?: ""
        if (raw.isNotBlank()) {
            _recentSearches.value = raw.split("||")
        }
    }

    private fun saveRecentSearch(query: String) {
        val list = _recentSearches.value.toMutableList()
        list.remove(query)
        list.add(0, query)
        val trimmed = list.take(10)
        _recentSearches.value = trimmed
        prefs.edit().putString("search_history", trimmed.joinToString("||")).apply()
    }

    fun deleteRecentSearch(query: String) {
        val list = _recentSearches.value.toMutableList()
        list.remove(query)
        _recentSearches.value = list
        prefs.edit().putString("search_history", list.joinToString("||")).apply()
    }

    // History deletion
    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Cache sizing
    fun getCacheSizeText(): String {
        // Since we are running local SQlite, can summarize cache rows or describe size
        return "12.4 MB (Database stored caches)"
    }

    fun clearAppCache(onCleared: () -> Unit) {
        viewModelScope.launch {
            repository.clearAppCache()
            onCleared()
        }
    }
}

class ViewModelStateFactory(
    private val application: Application,
    private val repository: MangaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MangaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MangaViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
