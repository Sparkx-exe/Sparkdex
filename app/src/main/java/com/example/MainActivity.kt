package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.example.data.api.MangaDexService
import com.example.data.db.AppDatabase
import com.example.data.repository.MangaRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MangaViewModel
import com.example.viewmodel.ViewModelStateFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 0. Unlock 120Hz/High Refresh Rate
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.attributes = window.attributes.apply {
                preferredDisplayModeId = windowManager.defaultDisplay.supportedModes
                    .maxByOrNull { it.refreshRate }?.modeId ?: 0
            }
        }

        // 1. Initialize Network Client (OkHttp + Retrofit + Moshi)
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.mangadex.org/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val apiService = retrofit.create(MangaDexService::class.java)

        // 2. Initialize Database Caches
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "mangazen_db"
        )
        .fallbackToDestructiveMigration()
        .build()

        // 3. Initialize Domain Repository
        val repository = MangaRepository(apiService, db.mangaDao(), moshi, applicationContext)

        // 4. Retrieve Architectural ViewModel using Custom Factory
        val factory = ViewModelStateFactory(application, repository)
        val viewModel: MangaViewModel by viewModels { factory }

        setContent {
            val activeTheme by viewModel.isDarkTheme.collectAsState()

            MyApplicationTheme(themeMode = activeTheme) {
                MainAppContainer(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContainer(viewModel: MangaViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Verify if bottom nav bars should be shown (hide in detail chapters viewer reader)
    val showBottomBar = currentRoute in listOf("home", "search", "library", "history", "settings")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    val tabs = listOf(
                        NavigationTab("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
                        NavigationTab("search", "Search", Icons.Filled.Search, Icons.Outlined.Search),
                        NavigationTab("library", "Library", Icons.Filled.LibraryBooks, Icons.Outlined.LibraryBooks),
                        NavigationTab("history", "History", Icons.Filled.History, Icons.Outlined.History),
                        NavigationTab("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
                    )

                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
            enterTransition = { 
                androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(220)) +
                androidx.compose.animation.slideInHorizontally(initialOffsetX = { it / 8 }, animationSpec = androidx.compose.animation.core.tween(220)) 
            },
            exitTransition = { 
                androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(220)) +
                androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -it / 8 }, animationSpec = androidx.compose.animation.core.tween(220)) 
            },
            popEnterTransition = { 
                androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(220)) +
                androidx.compose.animation.slideInHorizontally(initialOffsetX = { -it / 8 }, animationSpec = androidx.compose.animation.core.tween(220))
            },
            popExitTransition = { 
                androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(220)) +
                androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it / 8 }, animationSpec = androidx.compose.animation.core.tween(220))
            }
        ) {
            // Screen 1: Home
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onMangaClick = { mangaId ->
                        navController.navigate("detail/$mangaId")
                    }
                )
            }

            // Screen 2: Search
            composable("search") {
                SearchScreen(
                    viewModel = viewModel,
                    onMangaClick = { mangaId ->
                        navController.navigate("detail/$mangaId")
                    }
                )
            }

            // Screen 3: Library
            composable("library") {
                LibraryScreen(
                    viewModel = viewModel,
                    onMangaClick = { mangaId ->
                        navController.navigate("detail/$mangaId")
                    }
                )
            }

            // Screen 4: History
            composable("history") {
                HistoryScreen(
                    viewModel = viewModel,
                    onMangaClick = { mangaId, chapterId ->
                        navController.navigate("reader/$mangaId/$chapterId")
                    }
                )
            }

            // Screen 5: Settings
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel
                )
            }

            // Screen 6: Detailed view
            composable(
                route = "detail/{mangaId}",
                arguments = listOf(navArgument("mangaId") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("mangaId") ?: ""
                MangaDetailScreen(
                    mangaId = id,
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onReadChapterClick = { chapterId ->
                        navController.navigate("reader/$id/$chapterId")
                    },
                    onGenreClick = { genreTagId ->
                        viewModel.searchTags.value = listOf(genreTagId)
                        viewModel.searchQuery.value = ""
                        navController.navigate("search") {
                            popUpTo("home") { saveState = true }
                        }
                    }
                )
            }

            // Screen 7: Active manga Reader pages
            composable(
                route = "reader/{mangaId}/{chapterId}",
                arguments = listOf(
                    navArgument("mangaId") { type = NavType.StringType },
                    navArgument("chapterId") { type = NavType.StringType }
                )
            ) { backStack ->
                val mId = backStack.arguments?.getString("mangaId") ?: ""
                val cId = backStack.arguments?.getString("chapterId") ?: ""
                ReaderScreen(
                    chapterId = cId,
                    mangaId = mId,
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onNavigateToChapter = { newChapId ->
                        navController.navigate("reader/$mId/$newChapId") {
                            popUpTo("reader/$mId/$cId") { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

data class NavigationTab(
    val route: String,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)
