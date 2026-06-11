package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.example.data.api.MangaData
import com.example.ui.components.*
import com.example.viewmodel.HomeUiState
import com.example.viewmodel.MangaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MangaViewModel,
    onMangaClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val homeState by viewModel.homeUiState.collectAsState()
    val titleLang by viewModel.titleLanguage.collectAsState()
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "refresh_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SparkDex",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (!isRefreshing) {
                                scope.launch {
                                    isRefreshing = true
                                    viewModel.loadHomeData(forceRefresh = true)
                                    delay(1000)
                                    isRefreshing = false
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Feed",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = if (isRefreshing) {
                                Modifier.rotate(rotation)
                            } else {
                                Modifier
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
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
            when (val state = homeState) {
                is HomeUiState.Loading -> {
                    // Shimmer Skeleton Skeletons
                    LoadingHomeShimmer()
                }
                is HomeUiState.Error -> {
                    NetworkErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadHomeData(forceRefresh = true) }
                    )
                }
                is HomeUiState.Success -> {
                    val carousel = state.carouselManga
                    val popular = state.popularManga
                    val updates = state.latestUpdates
                    val recommended = state.recommendedManga
                    val seasonal = state.seasonalManga

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        // 1. Hero Carousel
                        if (carousel.isNotEmpty()) {
                            item {
                                HeroCarousel(
                                    mangaList = carousel,
                                    titleLanguage = titleLang,
                                    onMangaClick = onMangaClick
                                )
                            }
                        }

                        // 2. Popular Today horizontal row
                        if (popular.isNotEmpty()) {
                            item {
                                SectionHeader(title = "Popular Today")
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(popular, key = { "popular_${it.id}" }) { manga ->
                                        MangaGridCard(
                                            manga = manga,
                                            titleLanguage = titleLang,
                                            onClick = { onMangaClick(manga.id) },
                                            rating = 8.5 // Simulated or stats rating
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Recommended For You
                        if (recommended.isNotEmpty()) {
                            item {
                                SectionHeader(title = "Recommended For You")
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(recommended, key = { "rec_${it.id}" }) { manga ->
                                        MangaGridCard(
                                            manga = manga,
                                            titleLanguage = titleLang,
                                            onClick = { onMangaClick(manga.id) }
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Seasonal Row
                        if (seasonal.isNotEmpty()) {
                            item {
                                SectionHeader(title = "Seasonal New Series")
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(seasonal, key = { "seasonal_${it.id}" }) { manga ->
                                        MangaGridCard(
                                            manga = manga,
                                            titleLanguage = titleLang,
                                            onClick = { onMangaClick(manga.id) }
                                        )
                                    }
                                }
                            }
                        }

                        // 5. Latest Updates list vertical with Infinite Scroll
                        if (updates.isNotEmpty()) {
                            item {
                                SectionHeader(title = "Latest Updates")
                            }

                            items(updates, key = { "update_${it.id}" }) { manga ->
                                LatestUpdateRow(
                                    manga = manga,
                                    titleLanguage = titleLang,
                                    onClick = { onMangaClick(manga.id) }
                                )

                                // Trigger loading more updates on reaching the last visible item
                                if (manga == updates.last()) {
                                    LaunchedEffect(Unit) {
                                        viewModel.loadMoreLatestUpdates()
                                    }
                                }
                            }

                            // Loading state indicator for pagination
                            if (viewModel.hasMoreLatest) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroCarousel(
    mangaList: List<MangaData>,
    titleLanguage: String,
    onMangaClick: (String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { mangaList.size })
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll effect (3 seconds interval)
    LaunchedEffect(key1 = pagerState.currentPage) {
        delay(3000L)
        val nextPage = (pagerState.currentPage + 1) % mangaList.size
        pagerState.animateScrollToPage(nextPage)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val manga = mangaList[page]
            val coverUrl = getMangaCoverUrl(manga)
            val title = getMangaDisplayTitle(manga, titleLanguage)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onMangaClick(manga.id) }
            ) {
                // Background cover with blur or card scale
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark Bottom Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                // Carousel details overlay at bottom
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    // Tag chips row
                    Row(
                        modifier = Modifier.horizontalScrollPadding(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        manga.attributes.tags?.take(2)?.forEach { tag ->
                            val tagName = tag.attributes?.name?.get("en") ?: ""
                            if (tagName.isNotBlank()) {
                                Text(
                                    text = tagName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onMangaClick(manga.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                "Read Now",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )
                        }
                    }
                }
            }
        }

        // Custom Dot indicators overlay
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            mangaList.indices.forEach { index ->
                val active = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (active) 12.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f))
                )
            }
        }
    }
}

@Composable
fun LatestUpdateRow(
    manga: MangaData,
    titleLanguage: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = getMangaDisplayTitle(manga, titleLanguage)
    val coverUrl = getMangaCoverUrl(manga)
    val author = getMangaAuthor(manga)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp, 80.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    
                    val lang = manga.attributes.status ?: "ongoing"
                    Text(
                        text = getLanguageFlag("en"), // default English translator tag
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Authored by $author",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "New Chapter Released",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "Just now",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Custom horizontal scrolling helper for Row padding in compose
@Composable
private fun Modifier.horizontalScrollPadding(): Modifier = this.padding(bottom = 2.dp)

@Composable
fun LoadingHomeShimmer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp)
    ) {
        ShimmerPlaceholder(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader(title = "Popular Today")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) {
                Column(modifier = Modifier.width(110.dp)) {
                    ShimmerPlaceholder(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ShimmerPlaceholder(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader(title = "Latest Updates")
        repeat(3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerPlaceholder(
                    modifier = Modifier
                        .size(60.dp, 80.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerPlaceholder(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ShimmerPlaceholder(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(14.dp)
                    )
                }
            }
        }
    }
}
