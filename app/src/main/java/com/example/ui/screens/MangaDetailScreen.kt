package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.api.ChapterData
import com.example.data.api.MangaData
import com.example.ui.components.*
import com.example.viewmodel.DetailUiState
import com.example.viewmodel.MangaViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import android.content.Intent
import android.net.Uri

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MangaDetailScreen(
    mangaId: String,
    viewModel: MangaViewModel,
    onBackClick: () -> Unit,
    onReadChapterClick: (String) -> Unit,
    onGenreClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val detailState by viewModel.detailUiState.collectAsState()
    val titleLang by viewModel.titleLanguage.collectAsState()
    val isBookmarkedFlow by viewModel.bookmarks.collectAsState()
    val historyLogFlow by viewModel.history.collectAsState()
    val selectedLang by viewModel.selectedChapterLang.collectAsState()

    val isBookmarked = isBookmarkedFlow.any { it.mangaId == mangaId }
    val readChapterIds = historyLogFlow.filter { it.mangaId == mangaId }.map { it.chapterId }.toSet()

    LaunchedEffect(key1 = mangaId) {
        viewModel.loadMangaDetails(mangaId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Toggle Bookmark
                        val successState = detailState as? DetailUiState.Success
                        if (successState != null) {
                            val mainTitle = getMangaDisplayTitle(successState.manga, titleLang)
                            val cover = getMangaCoverUrl(successState.manga)
                            viewModel.toggleBookmark(mangaId, mainTitle, cover)
                        }
                    }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = detailState) {
                is DetailUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is DetailUiState.Error -> {
                    NetworkErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadMangaDetails(mangaId, forceRefresh = true) }
                    )
                }
                is DetailUiState.Success -> {
                    val manga = state.manga
                    val stats = state.stats
                    val allChapters = state.chapters
                    val availableLangs = state.availableLanguages

                    var isExpanded by remember { mutableStateOf(false) }
                    var tabIndex by remember { mutableStateOf(0) }
                    val tabTitles = listOf("Chapters", "Art", "Similar", "Comments")
                    var isDropdownOpen by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Blurred cover background
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(getMangaCoverUrl(manga))
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.12f)
                        )

                        // LazyColumn content
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            item {
                                // Header segment: Floating thumb + meta
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(getMangaCoverUrl(manga))
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Cover",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(130.dp, 185.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outline,
                                                RoundedCornerShape(8.dp)
                                            )
                                    )

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = getMangaDisplayTitle(manga, titleLang),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "By ${getMangaAuthor(manga)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Rating
                                        val rateValue = stats?.rating?.average ?: 8.1
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Rating",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = String.format("%.2f", rateValue),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "(${stats?.follows ?: 2500} follows)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Status info
                                        val statusRaw = manga.attributes.status ?: "ongoing"
                                        val statusColor = if (statusRaw.lowercase() == "ongoing") Color(0xFF32CD32) else Color(0xFFFF8C00)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(statusColor, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = statusRaw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            Spacer(modifier = Modifier.width(12.dp))
                                            
                                            Text(
                                                text = "Year: ${manga.attributes.year ?: "N/A"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Genres Scroll row
                                val genresRowState = rememberScrollState()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(genresRowState),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    manga.attributes.tags?.forEach { tag ->
                                        val name = tag.attributes?.name?.get("en") ?: ""
                                        if (name.isNotBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                        shape = RoundedCornerShape(20.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                                        RoundedCornerShape(20.dp)
                                                    )
                                                    .clickable { onGenreClick(tag.id) }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = name,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Expandable Synopsis text
                                val synopsis = manga.attributes.description?.get("en") ?: manga.attributes.description?.values?.firstOrNull() ?: "No description provided."
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize()
                                        .background(
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(12.dp)
                                        .clickable { isExpanded = !isExpanded }
                                ) {
                                    Text(
                                        text = "Synopsis",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = synopsis,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = if (isExpanded) 100 else 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isExpanded) "Show Less" else "Show More",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            // Sub tabs bar Chapters | Art | Similar | Comments
                            stickyHeader {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                                        .padding(vertical = 4.dp)
                                ) {
                                    TabRow(
                                        selectedTabIndex = tabIndex,
                                        containerColor = Color.Transparent,
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ) {
                                        tabTitles.forEachIndexed { idx, title ->
                                            Tab(
                                                selected = tabIndex == idx,
                                                onClick = { tabIndex = idx },
                                                text = { Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)) }
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Render dynamic content based on selected sub tab
                            when (tabIndex) {
                                0 -> {
                                    // Chapters tab
                                    
                                    // Language selection dropdown
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = MaterialTheme.colorScheme.surface,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.outline,
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { isDropdownOpen = true }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                                    .align(Alignment.CenterStart)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "Lang: ${selectedLang.uppercase()} " + getLanguageFlag(selectedLang),
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(
                                                        isDropdownOpen.let { if (it) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown },
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }

                                            DropdownMenu(
                                                expanded = isDropdownOpen,
                                                onDismissRequest = { isDropdownOpen = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("ALL LANGUAGES 🌐") },
                                                    onClick = {
                                                        viewModel.selectedChapterLang.value = "all"
                                                        isDropdownOpen = false
                                                    }
                                                )
                                                availableLangs.forEach { l ->
                                                    DropdownMenuItem(
                                                        text = { Text("${l.uppercase()} " + getLanguageFlag(l)) },
                                                        onClick = {
                                                            viewModel.selectedChapterLang.value = l
                                                            isDropdownOpen = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Print Chapters list of the Selected Language
                                    val filteredChapters = if (selectedLang.lowercase() == "all") {
                                        allChapters
                                    } else {
                                        allChapters.filter {
                                            it.attributes.translatedLanguage?.lowercase() == selectedLang.lowercase() 
                                        }
                                    }

                                    if (filteredChapters.isEmpty()) {
                                        item {
                                            Text(
                                                "No chapters found for this language selection.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(vertical = 12.dp)
                                            )
                                        }
                                    } else {
                                        // Group by chapter number
                                        val groupedChapters = mutableListOf<List<ChapterData>>()
                                        val chaptersByNumber = filteredChapters.groupBy { it.attributes.chapter ?: UUID.randomUUID().toString() }
                                        chaptersByNumber.values.forEach { group ->
                                            groupedChapters.add(group)
                                        }

                                        items(groupedChapters, key = { it.first().id }) { chapterList ->
                                            val primaryChapter = chapterList.first()
                                            val isRead = chapterList.any { c -> readChapterIds.contains(c.id) }
                                            ChapterGroupedRow(
                                                chapterList = chapterList,
                                                isRead = isRead,
                                                onReadClick = { id -> if (id != null) onReadChapterClick(id) },
                                                context = context
                                            )
                                        }
                                    }
                                }
                                1 -> {
                                    item {
                                        // Art / alternatives
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                AsyncImage(
                                                    model = getMangaCoverUrl(manga),
                                                    contentDescription = "Main Cover",
                                                    modifier = Modifier
                                                        .size(180.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    "Alternative Artworks",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    "Official cover illustrations from volumes.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                                2 -> {
                                    item {
                                        // Similar series placeholder
                                        Text(
                                            text = "Recommended Alternatives",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        Text(
                                            text = "If you enjoyed this manga, check out other series with tags like ${manga.attributes.tags?.firstOrNull()?.attributes?.name?.get("en") ?: "this"}.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                3 -> {
                                    item {
                                        // Comments placeholder
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No comments yet.\nBe the first to discuss this chapter!",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.alpha(0.6f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterGroupedRow(
    chapterList: List<ChapterData>,
    isRead: Boolean,
    onReadClick: (String?) -> Unit,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    var selectedGroupIndex by remember { mutableStateOf(0) }
    var isDropdownOpen by remember { mutableStateOf(false) }

    val chapter = chapterList[selectedGroupIndex]
    val title = chapter.attributes.title ?: "Chapter ${chapter.attributes.chapter ?: ""}"
    val chapNum = chapter.attributes.chapter ?: "0"
    val uploadDateRaw = chapter.attributes.publishAt
    val uploadDateStr = if (uploadDateRaw != null) {
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val date = parser.parse(uploadDateRaw)
            if (date != null) {
                SimpleDateFormat("MMM dd, yyyy", Locale.US).format(date)
            } else ""
        } catch (e: Exception) {
            ""
        }
    } else ""

    val groupRel = chapter.relationships?.find { it.type == "scanlation_group" }
    val groupName = groupRel?.attributes?.name ?: "No Group / Scanlator"

    val isExternal = chapter.attributes.pages == 0 && !chapter.attributes.externalUrl.isNullOrEmpty()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                if (isExternal) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(chapter.attributes.externalUrl))
                    context.startActivity(intent)
                } else {
                    onReadClick(chapter.id)
                }
            }
            .alpha(if (isRead) 0.5f else 1.0f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ch. $chapNum",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isRead) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Read logo",
                            tint = Color(0xFF32CD32),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (isExternal) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "External Link",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (title.isNotEmpty() && title != "Chapter $chapNum") {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (chapterList.size > 1) {
                    Box {
                        Text(
                            text = "Group: $groupName ▼ | $uploadDateStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable { isDropdownOpen = true }
                                .padding(vertical = 4.dp)
                        )
                        DropdownMenu(
                            expanded = isDropdownOpen,
                            onDismissRequest = { isDropdownOpen = false }
                        ) {
                            chapterList.forEachIndexed { index, cd ->
                                val grp = cd.relationships?.find { it.type == "scanlation_group" }
                                val gName = grp?.attributes?.name ?: "No Group"
                                DropdownMenuItem(
                                    text = { Text(gName) },
                                    onClick = {
                                        selectedGroupIndex = index
                                        isDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Group: $groupName | $uploadDateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = {
                    if (isExternal) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(chapter.attributes.externalUrl))
                        context.startActivity(intent)
                    } else {
                        onReadClick(chapter.id)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(if (isExternal) "Link" else "Read", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}
