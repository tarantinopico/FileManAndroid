package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.ThemeMode
import com.example.viewmodel.FileManagerViewModel
import com.example.viewmodel.TextEditorViewModel
import java.net.URLEncoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(viewModel: FileManagerViewModel) {
    val navController = rememberNavController()
    NavHost(
        navController = navController, 
        startDestination = "main",
        enterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(300)) { it / 8 } },
        exitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(300)) { -it / 8 } },
        popEnterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(300)) { -it / 8 } },
        popExitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(300)) { it / 8 } }
    ) {
        composable("main") {
            val gitViewModel: com.example.viewmodel.GitViewModel = viewModel()
            MainScreen(
                viewModel = viewModel,
                gitViewModel = gitViewModel,
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToEditor = { path, name ->
                    val encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
                    val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
                    navController.navigate("editor/$encodedPath/$encodedName")
                },
                onNavigateToImage = { path, name ->
                    val encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
                    val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
                    navController.navigate("image/$encodedPath/$encodedName")
                },
                onNavigateToPdf = { path, name ->
                    val encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
                    val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
                    navController.navigate("pdf/$encodedPath/$encodedName")
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAbout = { navController.navigate("about") }
            )
        }
        composable("about") {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "editor/{path}/{name}",
            arguments = listOf(
                navArgument("path") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val path = URLDecoder.decode(backStackEntry.arguments?.getString("path") ?: "", StandardCharsets.UTF_8.toString())
            val name = URLDecoder.decode(backStackEntry.arguments?.getString("name") ?: "", StandardCharsets.UTF_8.toString())
            val editorViewModel: TextEditorViewModel = viewModel()
            LaunchedEffect(path, name) {
                editorViewModel.loadFile(path, name)
            }
            TextEditorScreen(
                viewModel = editorViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "image/{path}/{name}",
            arguments = listOf(
                navArgument("path") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val path = URLDecoder.decode(backStackEntry.arguments?.getString("path") ?: "", StandardCharsets.UTF_8.toString())
            val name = URLDecoder.decode(backStackEntry.arguments?.getString("name") ?: "", StandardCharsets.UTF_8.toString())
            ImageViewerScreen(
                imagePath = path,
                imageName = name,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "pdf/{path}/{name}",
            arguments = listOf(
                navArgument("path") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val path = URLDecoder.decode(backStackEntry.arguments?.getString("path") ?: "", StandardCharsets.UTF_8.toString())
            val name = URLDecoder.decode(backStackEntry.arguments?.getString("name") ?: "", StandardCharsets.UTF_8.toString())
            PdfViewerScreen(
                pdfPath = path,
                pdfName = name,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onRequestPermission()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Povolení k úložišti",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Aplikace vyžaduje přístup k veškerým souborům pro správu souborů a složek na zařízení.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:${context.packageName}")
                        launcher.launch(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        launcher.launch(intent)
                    }
                }
            }) {
                Text("Udělit povolení")
            }
        }
    }
}
