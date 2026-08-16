package neunix.dailychunk.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun DailyChunkNavHost(initialDownloadId: Long?) {
    val navController = rememberNavController()
    val viewModel: DownloadViewModel = viewModel()

    LaunchedEffect(initialDownloadId) {
        if (initialDownloadId != null) {
            navController.navigate("details/$initialDownloadId")
        }
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onAddClick = { navController.navigate("add") },
                onOpenDetails = { id -> navController.navigate("details/$id") }
            )
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
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: -1L
            DetailsScreen(viewModel = viewModel, downloadId = id, onBack = { navController.popBackStack() })
        }
    }
}