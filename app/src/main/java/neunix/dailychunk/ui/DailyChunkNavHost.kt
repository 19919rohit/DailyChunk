package neunix.dailychunk.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private data class BottomDest(val route: String, val label: String, val filled: androidx.compose.ui.graphics.vector.ImageVector, val outline: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomDestinations = listOf(
    BottomDest("home", "Home", Icons.Filled.CloudDownload, Icons.Outlined.CloudDownload),
    BottomDest("history", "History", Icons.Filled.History, Icons.Outlined.History),
    BottomDest("files", "Files", Icons.Filled.Folder, Icons.Outlined.Folder),
    BottomDest("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun DailyChunkNavHost(initialDownloadId: Long?) {
    val navController = rememberNavController()
    val viewModel: DownloadViewModel = viewModel()

    LaunchedEffect(initialDownloadId) {
        if (initialDownloadId != null) {
            navController.navigate("details/$initialDownloadId")
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = bottomDestinations.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomDestinations.forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(if (selected) dest.filled else dest.outline, contentDescription = dest.label) },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onAddClick = { navController.navigate("add") },
                    onOpenDetails = { id -> navController.navigate("details/$id") }
                )
            }
            composable("history") {
                HistoryScreen(viewModel = viewModel, onOpenDetails = { id -> navController.navigate("details/$id") })
            }
            composable("files") {
                FilesScreen(viewModel = viewModel)
            }
            composable("settings") {
                SettingsScreen(viewModel = viewModel)
            }
            composable("add") {
                AddDownloadScreen(
                    viewModel = viewModel,
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                "details/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: -1L
                DetailsScreen(viewModel = viewModel, downloadId = id, onBack = { navController.popBackStack() })
            }
        }
    }
}