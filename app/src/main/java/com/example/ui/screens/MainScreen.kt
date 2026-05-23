package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.FileModel
import com.example.viewmodel.ClipboardOperation
import com.example.viewmodel.FileManagerViewModel
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: FileManagerViewModel,
    onNavigateToSettings: () -> Unit
) {
    val hasPermission by viewModel.hasPermission.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val storageVolumes by viewModel.storageVolumes.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle(initialValue = emptyList())
    val clipboard by viewModel.clipboard.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is com.example.viewmodel.UiEvent.ShowToast -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    if (hasPermission) {
        BackHandler(enabled = drawerState.isOpen || uiState.parentPath != null) {
            if (drawerState.isOpen) {
                scope.launch { drawerState.close() }
            } else {
                viewModel.navigateUp()
            }
        }

        val onOpenFile: (FileModel) -> Unit = { file ->
            try {
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    java.io.File(file.path)
                )
                
                val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "*/*"
                
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Otevřít pomocí"))
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Nelze otevřít soubor")
                }
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                DrawerContent(
                    storageVolumes = storageVolumes,
                    favorites = favorites,
                    onStorageVolumeClick = { volume ->
                        scope.launch { drawerState.close() }
                        viewModel.loadDirectory(volume.path)
                    },
                    onFavoriteClick = { favorite ->
                        scope.launch { drawerState.close() }
                        viewModel.loadDirectory(favorite.path)
                    },
                    onSettingsClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    }
                )
            }
        ) {
            FileExplorerScreen(
                state = uiState,
                favorites = favorites,
                clipboard = clipboard,
                onNavigate = { path -> viewModel.loadDirectory(path) },
                onNavigateUp = { viewModel.navigateUp() },
                onCreateFolder = { name -> viewModel.createFolder(name) },
                onDelete = { file -> viewModel.deleteItem(file) },
                onRename = { file, newName -> viewModel.renameItem(file, newName) },
                onCopy = { file -> viewModel.setClipboard(file, ClipboardOperation.COPY) },
                onMove = { file -> viewModel.setClipboard(file, ClipboardOperation.MOVE) },
                onPaste = { viewModel.pasteFromClipboard() },
                onCancelClipboard = { viewModel.cancelClipboard() },
                onOpenFile = onOpenFile,
                onMenuClick = { scope.launch { drawerState.open() } },
                onToggleFavorite = { file ->
                    val isFav = favorites.any { it.path == file.path }
                    viewModel.toggleFavorite(file.path, file.name, isFav)
                },
                snackbarHostState = snackbarHostState
            )
        }
    } else {
        PermissionScreen(
            onRequestPermission = { viewModel.checkPermission() }
        )
    }
}
