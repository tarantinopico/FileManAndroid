package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.BackHandler
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.FileModel
import com.example.ui.state.ClipboardOperation
import com.example.ui.state.UiEvent
import com.example.viewmodel.FileManagerViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun MainScreen(viewModel: FileManagerViewModel) {
    val hasPermission by viewModel.hasPermission.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val storageVolumes by viewModel.storageVolumes.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is UiEvent.Error -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Long
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
                    "${context.packageName}.fileprovider",
                    File(file.path)
                )

                val extension = File(file.path).extension
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase(Locale.getDefault())) ?: "*/*"

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(intent, "Otevřít soubor")
                context.startActivity(chooser)
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Nelze otevřít soubor: ${e.localizedMessage}",
                        duration = SnackbarDuration.Short
                    )
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
                    }
                )
            }
        ) {
            val isFavorite = favorites.any { it.path == uiState.currentPath }
            FileExplorerScreen(
                state = uiState,
                favorites = favorites,
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
                    viewModel.toggleFavorite(file.path, file.name)
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
