package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.FileModel
import com.example.viewmodel.ClipboardOperation
import com.example.viewmodel.FileManagerViewModel
import kotlinx.coroutines.launch

import com.example.viewmodel.GitViewModel

@Composable
fun MainScreen(
    viewModel: FileManagerViewModel,
    gitViewModel: GitViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToEditor: (String, String) -> Unit,
    onNavigateToImage: (String, String) -> Unit
) {
    val hasPermission by viewModel.hasPermission.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gitUiState by gitViewModel.uiState.collectAsStateWithLifecycle()
    val storageVolumes by viewModel.storageVolumes.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle(initialValue = emptyList())
    val clipboard by viewModel.clipboard.collectAsStateWithLifecycle()
    val syntaxMappings by viewModel.syntaxMappings.collectAsStateWithLifecycle(initialValue = emptyList())
    val fileSettings by viewModel.fileSettings.collectAsStateWithLifecycle()
    val appPreferences by viewModel.appPreferences.collectAsStateWithLifecycle()

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
    
    LaunchedEffect(Unit) {
        gitViewModel.uiEvents.collect { event ->
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

    LaunchedEffect(uiState.currentPath) {
        if (uiState.currentPath.isNotEmpty()) {
            gitViewModel.loadGitStatus(uiState.currentPath)
        }
    }

    var showFavoriteEditDialogFor by remember { mutableStateOf<com.example.model.FavoriteModel?>(null) }
    var unsupportedFileFor by remember { mutableStateOf<FileModel?>(null) }

    if (unsupportedFileFor != null) {
        val file = unsupportedFileFor!!
        AlertDialog(
            onDismissRequest = { unsupportedFileFor = null },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Nelze otevřít soubor", style = MaterialTheme.typography.titleMedium) },
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nepodařilo se najít vhodnou aplikaci pro otevření tohoto souboru. Můžete soubor zkusit přečíst jako text, nebo zkopírovat jeho cestu.")
                    Text("Soubor: ${file.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    unsupportedFileFor = null
                    onNavigateToEditor(file.path, file.name)
                }) { Text("Zkusit jako text") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText("Cesta", file.path))
                        android.widget.Toast.makeText(context, "Cesta zkopírována", android.widget.Toast.LENGTH_SHORT).show()
                    }) { Text("Kopírovat cestu") }
                    TextButton(onClick = { unsupportedFileFor = null }) { Text("Zrušit") }
                }
            }
        )
    }

    if (showFavoriteEditDialogFor != null) {
        val fav = showFavoriteEditDialogFor!!
        var name by remember { mutableStateOf(fav.name) }
        var selectedIcon by remember { mutableStateOf(fav.icon) }
        
        AlertDialog(
            onDismissRequest = { showFavoriteEditDialogFor = null },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            title = { Text("Upravit oblíbenou", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Název") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    
                    Text("Ikona", style = MaterialTheme.typography.labelLarge)
                    
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val icons = com.example.model.FavoriteIcon.values()
                        items(icons.size) { index ->
                            val icon = icons[index]
                            val isSelected = icon == selectedIcon
                                val iconVector = when(icon) {
                                    com.example.model.FavoriteIcon.FOLDER -> Icons.Rounded.Folder
                                    com.example.model.FavoriteIcon.STAR -> Icons.Rounded.Star
                                    com.example.model.FavoriteIcon.PROJECT -> Icons.Rounded.Work
                                    com.example.model.FavoriteIcon.DOWNLOAD -> Icons.Rounded.Download
                                    com.example.model.FavoriteIcon.IMAGE -> Icons.Rounded.Image
                                    com.example.model.FavoriteIcon.DOCUMENT -> Icons.Rounded.Description
                                    com.example.model.FavoriteIcon.PIN -> Icons.Rounded.PushPin
                                }
                            
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                                    .clickable { selectedIcon = icon },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    iconVector,
                                    contentDescription = icon.name,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        if (name.isNotBlank()) {
                            viewModel.saveFavorite(fav.copy(name = name, icon = selectedIcon))
                            showFavoriteEditDialogFor = null
                        } 
                    }
                ) { Text("Uložit", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showFavoriteEditDialogFor = null }) { Text("Zrušit") }
            }
        )
    }

    if (hasPermission) {
        BackHandler(enabled = drawerState.isOpen || uiState.historyBackStack.isNotEmpty() || uiState.parentPath != null) {
            if (drawerState.isOpen) {
                scope.launch { drawerState.close() }
            } else if (uiState.historyBackStack.isNotEmpty()) {
                viewModel.navigateBack()
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
                
                val extension = file.name.substringAfterLast('.', "").lowercase()
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
                
                val isKnownTextExtension = syntaxMappings.any { it.extension == extension }
                val textExtensions = listOf("txt", "md", "csv", "xml", "json", "kt", "java", "py", "html", "css", "js", "log", "ini", "properties")
                val imageExtensions = listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
                
                if (mimeType.startsWith("image/") || extension in imageExtensions) {
                    onNavigateToImage(file.path, file.name)
                } else if (mimeType.startsWith("text/") || extension in textExtensions || isKnownTextExtension) {
                    onNavigateToEditor(file.path, file.name)
                } else {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: android.content.ActivityNotFoundException) {
                        unsupportedFileFor = file
                    } catch (e: Exception) {
                        unsupportedFileFor = file
                    }
                }
            } catch (e: Exception) {
                unsupportedFileFor = file
            }
        }


        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = appPreferences.drawerEnabled,
            drawerContent = {
                if (appPreferences.drawerEnabled) {
                    DrawerContent(
                        storageVolumes = storageVolumes,
                        favorites = favorites,
                        appPreferences = appPreferences,
                        onStorageVolumeClick = { volume ->
                            scope.launch { drawerState.close() }
                            viewModel.loadDirectory(volume.path)
                        },
                        onFavoriteClick = { favorite ->
                            scope.launch { drawerState.close() }
                            viewModel.loadDirectory(favorite.path)
                        },
                        onEditFavorite = { favorite ->
                            scope.launch { drawerState.close() }
                            showFavoriteEditDialogFor = favorite
                        },
                        onSettingsClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToSettings()
                        }
                    )
                }
            }
        ) {
            FileExplorerScreen(
                state = uiState,
                gitState = gitUiState,
                favorites = favorites,
                clipboard = clipboard,
                fileSettings = fileSettings,
                appPreferences = appPreferences,
                syntaxMappings = syntaxMappings,
                onNavigate = { path -> viewModel.loadDirectory(path) },
                onNavigateUp = { viewModel.navigateUp() },
                onNavigateBack = { viewModel.navigateBack() },
                onNavigateForward = { viewModel.navigateForward() },
                onCreateFolder = { name -> viewModel.createFolder(name) },
                onCreateFile = { name -> viewModel.createFile(name) },
                onDelete = { file -> viewModel.deleteItem(file) },
                onRename = { file, newName -> viewModel.renameItem(file, newName) },
                onCopy = { file -> viewModel.setClipboard(listOf(file), ClipboardOperation.COPY) },
                onMove = { file -> viewModel.setClipboard(listOf(file), ClipboardOperation.MOVE) },
                onPaste = { viewModel.pasteFromClipboard() },
                onCancelClipboard = { viewModel.cancelClipboard() },
                onOpenFile = onOpenFile,
                onMenuClick = { scope.launch { drawerState.open() } },
                onRemoveFavorite = { path -> viewModel.removeFavorite(path) },
                onSaveFavorite = { favorite -> viewModel.saveFavorite(favorite) },
                onToggleSelection = { path -> viewModel.toggleSelection(path) },
                onClearSelection = { viewModel.clearSelection() },
                onSelectAll = { viewModel.selectAll() },
                onSearchChange = { query -> viewModel.setSearchQuery(query) },
                onBatchDelete = { viewModel.deleteSelected() },
                onBatchCopy = { files -> viewModel.setClipboard(files, ClipboardOperation.COPY); viewModel.clearSelection() },
                onBatchMove = { files -> viewModel.setClipboard(files, ClipboardOperation.MOVE); viewModel.clearSelection() },
                onBatchShare = { files ->
                    val uris = java.util.ArrayList<android.net.Uri>()
                    for (file in files) {
                        uris.add(FileProvider.getUriForFile(context, "${context.packageName}.provider", java.io.File(file.path)))
                    }
                    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = "*/*"
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Sdílet"))
                    viewModel.clearSelection()
                },
                onBatchRename = { baseName -> viewModel.bulkRename(baseName) },
                onBatchZip = { zipName -> viewModel.zipSelected(zipName) },
                onBatchEncrypt = { password -> viewModel.encryptSelected(password) },
                onDecryptFile = { file, password -> viewModel.decryptSelected(file, password) },
                onUnzipFile = { file -> viewModel.unzipFile(file) },
                // Git Actions
                onGitInit = { gitViewModel.initRepository(uiState.currentPath) },
                onGitAddAll = { gitViewModel.addAll(uiState.currentPath) },
                onGitAdd = { file -> gitViewModel.addFile(uiState.currentPath, file.path) },
                onGitCommit = { message -> gitViewModel.commit(uiState.currentPath, message) },
                onGitPush = { gitViewModel.push(uiState.currentPath) },
                onGitPull = { gitViewModel.pull(uiState.currentPath) },
                onGitSetRemote = { remote -> gitViewModel.setRemote(uiState.currentPath, remote) },
                onGitCreateGithubRepo = { name, isPrivate -> gitViewModel.createAndLinkGithubRepo(uiState.currentPath, name, isPrivate) },
                snackbarHostState = snackbarHostState
            )
        }
    } else {
        PermissionScreen(
            onRequestPermission = { viewModel.checkPermission() }
        )
    }
}
