package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.viewmodel.MangaViewModel
import com.example.viewmodel.SearchUiState
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MangaViewModel,
    onMangaClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchState by viewModel.searchUiState.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val searchTags by viewModel.searchTags.collectAsState()
    val selectedSort by viewModel.searchSortBy.collectAsState()
    val selectedStatus by viewModel.searchStatus.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val titleLang by viewModel.titleLanguage.collectAsState()
    val availableGenres by viewModel.availableGenres.collectAsState()

    var isFilterSheetOpen by remember { mutableStateOf(false) }

    // Auto-search Debounce: trigger search 400ms after user finishes typing
    LaunchedEffect(query, searchTags, selectedSort, selectedStatus) {
        if (query.isNotBlank() || searchTags.isNotEmpty()) {
            delay(400L)
            viewModel.searchManga()
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "Search",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = query,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = { Text("Search title, author, artist...") },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { isFilterSheetOpen = true },
                        modifier = Modifier
                            .size(54.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filters",
                            tint = if (searchTags.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = searchState) {
                is SearchUiState.Idle -> {
                    // Show recent searches when empty
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        if (recentSearches.isNotEmpty()) {
                            Text(
                                "Recent Searches",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recentSearches.take(6).forEach { recentQ ->
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.surface,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable { viewModel.searchQuery.value = recentQ }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = recentQ,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clickable { viewModel.deleteRecentSearch(recentQ) }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Friendly Empty screen tip
                            EmptyState(message = "Search your favorite manga by name, genre, status & tag filters!")
                        }
                    }
                }
                is SearchUiState.Loading -> {
                    // Loading shimmer grid
                    LoadingSearchShimmer()
                }
                is SearchUiState.Error -> {
                    NetworkErrorState(
                        message = state.message,
                        onRetry = { viewModel.searchManga() }
                    )
                }
                is SearchUiState.Success -> {
                    val list = state.results
                    if (list.isEmpty()) {
                        EmptyState(message = "No manga found. Try adjusting filter tags!")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(list, key = { it.id }) { manga ->
                                MangaGridCard(
                                    manga = manga,
                                    titleLanguage = titleLang,
                                    onClick = { onMangaClick(manga.id) },
                                    rating = 8.3,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Trigger infinite scroll loading of more search results
                                if (manga == list.last()) {
                                    LaunchedEffect(Unit) {
                                        viewModel.loadMoreSearch()
                                    }
                                }
                            }

                            // Show progress loader span at the bottom of the grid
                            if (viewModel.hasMoreSearch) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
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

            // Filters Drawer (Modal bottom sheet)
            if (isFilterSheetOpen) {
                ModalBottomSheet(
                    onDismissRequest = { isFilterSheetOpen = false },
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            "Filter Search Results",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // 1. Sort options
                        Text(
                            "Sort By",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        val sorts = listOf(
                            "relevance" to "Relevance",
                            "latest" to "Latest Upload",
                            "rating" to "Highest Rating",
                            "followed" to "Most Followed",
                            "added" to "Recently Added"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 2.dp)
                        ) {
                            sorts.forEach { (key, label) ->
                                val active = selectedSort == key
                                FilterChip(
                                    selected = active,
                                    onClick = { viewModel.searchSortBy.value = key },
                                    label = { Text(label) },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }

                        // 2. Status Options
                        Text(
                            "Status",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                        val states = listOf("All", "Ongoing", "Completed", "Hiatus")
                        Row {
                            states.forEach { s ->
                                FilterChip(
                                    selected = selectedStatus == s,
                                    onClick = { viewModel.searchStatus.value = s },
                                    label = { Text(s) },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }

                        // 3. Genre Multi-select tag chips
                        Text(
                            "Genres",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )

                        // Show available tags
                        if (availableGenres.isEmpty()) {
                            Text(
                                "Loading available genres...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp, min = 100.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                availableGenres.take(24).forEach { tag ->
                                    val name = tag.attributes?.name?.get("en") ?: ""
                                    if (name.isNotBlank()) {
                                        val active = searchTags.contains(tag.id)
                                        FilterChip(
                                            selected = active,
                                            onClick = {
                                                val list = searchTags.toMutableList()
                                                if (active) list.remove(tag.id) else list.add(tag.id)
                                                viewModel.searchTags.value = list
                                            },
                                            label = { Text(name, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                isFilterSheetOpen = false
                                viewModel.searchManga()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Apply Filters & Search", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingSearchShimmer() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(6, key = { "shimmer_$it" }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                ShimmerPlaceholder(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerPlaceholder(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(20.dp)
                )
            }
        }
    }
}
