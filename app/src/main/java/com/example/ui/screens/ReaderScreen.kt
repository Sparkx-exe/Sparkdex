package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.util.Log
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.api.ChapterData
import com.example.ui.components.NetworkErrorState
import com.example.viewmodel.MangaViewModel
import com.example.viewmodel.ReaderUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    chapterId: String,
    mangaId: String,
    viewModel: MangaViewModel,
    onBackClick: () -> Unit,
    onNavigateToChapter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val readerState by viewModel.readerUiState.collectAsState()
    val activeMode by viewModel.readerMode.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Unlock auto rotate inside reader screen, restore on exit
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(key1 = chapterId) {
        viewModel.loadChapterPages(chapterId, mangaId)
    }

    var showOverlays by remember { mutableStateOf(true) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val state = readerState) {
            is ReaderUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is ReaderUiState.Error -> {
                NetworkErrorState(
                    message = state.message,
                    onRetry = { viewModel.loadChapterPages(chapterId, mangaId) }
                )
            }
            is ReaderUiState.Success -> {
                val pages = state.pages
                val rawHash = state.hash
                val baseUrl = state.baseUrl
                val chapterObj = state.chapter
                val nextChap = state.nextChapterId
                val prevChap = state.prevChapterId

                val pageUrls = pages.map { pageFile ->
                    "$baseUrl/data-saver/$rawHash/$pageFile"
                }

                // Interactive scale zoom levels
                var scale by remember { mutableFloatStateOf(1f) }

                // Save last progress on exit/dispose
                val lastPage = remember { mutableStateOf(0) }
                DisposableEffect(Unit) {
                    onDispose {
                        if (pageUrls.isNotEmpty()) {
                            val attributes = chapterObj.attributes
                            val name = "Chapter " + (attributes.chapter ?: "") + " - " + (attributes.title ?: "Untitled")
                            viewModel.saveProgress(
                                mangaId = mangaId,
                                mangaTitle = "Manga", // fallback title
                                mangaCover = "", // fallback cover
                                chapterId = chapterId,
                                chapterName = name,
                                page = lastPage.value + 1,
                                genres = emptyList() // tags resolved back in VM if bookmarked
                            )
                        }
                    }
                }

                // Tapper gesture
                val toggleBarsModifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showOverlays = !showOverlays },
                        onDoubleTap = {
                            scale = if (scale > 1f) 1f else 2.2f
                        }
                    )
                }

                // WEBTOON: Continuous Vertical Scroll Layout
                if (activeMode == "Webtoon") {
                    val listState = rememberLazyListState()
                    LaunchedEffect(listState.firstVisibleItemIndex) {
                        lastPage.value = listState.firstVisibleItemIndex
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(toggleBarsModifier),
                        contentPadding = PaddingValues(bottom = 120.dp, top = 50.dp)
                    ) {
                        itemsIndexed(pageUrls) { idx, url ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(url)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Page ${idx + 1}",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Last footer with chapter links
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "You completed this chapter!",
                                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.LightGray),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    if (prevChap != null) {
                                        Button(onClick = { onNavigateToChapter(prevChap) }) {
                                            Text("Prev Chapter")
                                        }
                                    }
                                    if (nextChap != null) {
                                        Button(onClick = { onNavigateToChapter(nextChap) }) {
                                            Text("Next Chapter")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom info overlay sheet (Current Page counter)
                    val firstVisible = listState.firstVisibleItemIndex
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${firstVisible + 1} / ${pages.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                } else {
                    // PAGED: Horizontal Page View Layout
                    val pagerState = rememberPagerState(pageCount = { pageUrls.size })
                    LaunchedEffect(pagerState.currentPage) {
                        lastPage.value = pagerState.currentPage
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(toggleBarsModifier),
                        contentAlignment = Alignment.Center
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { pageIdx ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(pageUrls[pageIdx])
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Page ${pageIdx + 1}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    // Bottom page counter slider/arrows
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        if (pagerState.currentPage > 0) {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    }
                                },
                                enabled = pagerState.currentPage > 0
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Prev", tint = Color.White)
                            }

                            Text(
                                text = "${pagerState.currentPage + 1} / ${pages.size}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            IconButton(
                                onClick = {
                                    scope.launch {
                                        if (pagerState.currentPage < pages.size - 1) {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    }
                                },
                                enabled = pagerState.currentPage < pages.size - 1
                            ) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Next", tint = Color.White)
                            }
                        }
                    }
                }

                // Top Floating Toolbar (Auto hiding)
                AnimatedVisibility(
                    visible = showOverlays,
                    enter = slideInVertically(initialOffsetY = { -it }),
                    exit = slideOutVertically(targetOffsetY = { -it }),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.85f))
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val chAttr = chapterObj.attributes
                                Text(
                                    "Chapter ${chAttr.chapter ?: "0"}",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    chAttr.title ?: "Scanning Pages",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { isSettingsOpen = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Brightness", tint = Color.White)
                            }
                        }
                    }
                }

                // Bottom sheet for Brightness Adjustment controls
                if (isSettingsOpen) {
                    ModalBottomSheet(
                        onDismissRequest = { isSettingsOpen = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        dragHandle = { BottomSheetDefaults.DragHandle() }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                        ) {
                            Text(
                                "Reader Controls",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Reader Mode display toggle
                            Text(
                                "Display Mode",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = activeMode == "Webtoon",
                                    onClick = { viewModel.setReaderModePreference("Webtoon") },
                                    label = { Text("Vertical Webtoon") }
                                )
                                FilterChip(
                                    selected = activeMode == "Paged",
                                    onClick = { viewModel.setReaderModePreference("Paged") },
                                    label = { Text("Paged Book") }
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}
