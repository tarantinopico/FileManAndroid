package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.FavoriteModel
import com.example.model.FileModel
import com.example.viewmodel.ClipboardOperation
import com.example.viewmodel.ClipboardState
import com.example.viewmodel.FileManagerState
import com.example.viewmodel.GitUiState
import com.example.model.GitFileStatusType
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

fun getIconForFile(file: FileModel): ImageVector {
    if (file.isDirectory) return Icons.Rounded.Folder
    
    val ext = file.name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg" -> Icons.Rounded.Image
        "mp3", "wav", "ogg", "flac", "m4a", "aac" -> Icons.Rounded.AudioFile
        "mp4", "mkv", "avi", "mov", "webm" -> Icons.Rounded.VideoFile
        "pdf" -> Icons.Rounded.PictureAsPdf
        "zip", "rar", "7z", "tar", "gz" -> Icons.Rounded.FolderZip
        "txt", "md", "csv", "log" -> Icons.Rounded.Description
        "kt", "kts", "java", "xml", "json", "py", "js", "html", "css", "cpp", "c", "h", "gradle", "sh" -> Icons.Rounded.Code
        "apk", "aab" -> Icons.Rounded.Android
        "enc" -> Icons.Rounded.Lock
        else -> Icons.Rounded.InsertDriveFile
    }
}

fun getColorForExtension(extension: String): Color {
    val hash = extension.lowercase().hashCode()
    val hue = (hash % 360 + 360) % 360
    return Color.hsv(hue.toFloat(), 0.6f, 0.8f) // Pastel/distinct color based on extension string
}

fun formatSize(sizeBytes: Long): String {
    if (sizeBytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(sizeBytes.toDouble()) / Math.log10(1024.0)).toInt()
    if (digitGroups == 0) return "$sizeBytes B"
    return String.format(Locale.getDefault(), "%.1f %s", sizeBytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileExplorerScreen(
    state: FileManagerState,
    gitState: GitUiState,
    favorites: List<FavoriteModel>,
    clipboard: ClipboardState?,
    fileSettings: com.example.model.FileSettings,
    appPreferences: com.example.model.AppPreferences,
    syntaxMappings: List<com.example.model.SyntaxMapping>,
    onNavigate: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateForward: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onCreateFile: (String) -> Unit,
    onDelete: (FileModel) -> Unit,
    onRename: (FileModel, String) -> Unit,
    onCopy: (FileModel) -> Unit,
    onMove: (FileModel) -> Unit,
    onPaste: () -> Unit,
    onCancelClipboard: () -> Unit,
    onOpenFile: (FileModel) -> Unit,
    onMenuClick: () -> Unit,
    onRemoveFavorite: (String) -> Unit,
    onSaveFavorite: (FavoriteModel) -> Unit,
    onToggleSelection: (String) -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onSearchChange: (String) -> Unit,
    onBatchDelete: () -> Unit,
    onBatchCopy: (List<FileModel>) -> Unit,
    onBatchMove: (List<FileModel>) -> Unit,
    onBatchShare: (List<FileModel>) -> Unit,
    onBatchRename: (String) -> Unit,
    onBatchZip: (String) -> Unit,
    onBatchEncrypt: (String) -> Unit,
    onDecryptFile: (FileModel, String) -> Unit,
    onUnzipFile: (FileModel) -> Unit,
    onGitInit: () -> Unit,
    onGitAddAll: () -> Unit,
    onGitAdd: (FileModel) -> Unit,
    onGitCommit: (String) -> Unit,
    onGitPush: () -> Unit,
    onGitPull: () -> Unit,
    onGitSetRemote: (String) -> Unit,
    onGitCreateGithubRepo: (String, Boolean) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showCreateMenu by remember { mutableStateOf(false) }
    var showInfoDialogFor by remember { mutableStateOf<FileModel?>(null) }
    var showZipDialog by remember { mutableStateOf(false) }
    var showEncryptDialog by remember { mutableStateOf(false) }
    var showBulkRenameDialog by remember { mutableStateOf(false) }
    var showFavoriteDialogFor by remember { mutableStateOf<FileModel?>(null) }
    var decryptTarget by remember { mutableStateOf<FileModel?>(null) }
    var deleteTarget by remember { mutableStateOf<FileModel?>(null) }
    var batchDeleteConfirm by remember { mutableStateOf(false) }
    var showCommitDialog by remember { mutableStateOf(false) }
    var showSetRemoteDialog by remember { mutableStateOf(false) }
    var showGithubRepoDialog by remember { mutableStateOf(false) }
    
    val isMultiSelect = state.selectedFiles.isNotEmpty()
    val isSearchMode = state.searchQuery.isNotEmpty()
    val isRepo = gitState.repoStatus.isRepo

    var showSearch by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isMultiSelect) {
                CenterAlignedTopAppBar(
                    title = { Text("${state.selectedFiles.size} vybráno", style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = onClearSelection) {
                            Icon(Icons.Rounded.Close, contentDescription = "Zrušit výběr")
                        }
                    },
                    actions = {
                        IconButton(onClick = onSelectAll) {
                            Icon(Icons.Rounded.SelectAll, contentDescription = "Vybrat vše")
                        }
                        IconButton(onClick = { onBatchCopy(state.files.filter { state.selectedFiles.contains(it.path) }) }) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Kopírovat vybrané")
                        }
                        IconButton(onClick = { onBatchMove(state.files.filter { state.selectedFiles.contains(it.path) }) }) {
                            Icon(Icons.Rounded.DriveFileMove, contentDescription = "Přesunout vybrané")
                        }
                        IconButton(onClick = {
                            if (appPreferences.confirmDeletions) {
                                batchDeleteConfirm = true
                            } else {
                                onBatchDelete()
                            }
                        }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Smazat vybrané", tint = MaterialTheme.colorScheme.error)
                        }
                        Box {
                            var moreExpanded by remember { mutableStateOf(false) }
                            IconButton(onClick = { moreExpanded = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "Více")
                            }
                            DropdownMenu(expanded = moreExpanded, onDismissRequest = { moreExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Hromadně přejmenovat") },
                                    onClick = { moreExpanded = false; showBulkRenameDialog = true },
                                    leadingIcon = { Icon(Icons.Rounded.Edit, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Zabalit do ZIP") },
                                    onClick = { moreExpanded = false; showZipDialog = true },
                                    leadingIcon = { Icon(Icons.Rounded.FolderZip, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Zašifrovat (AES)") },
                                    onClick = { moreExpanded = false; showEncryptDialog = true },
                                    leadingIcon = { Icon(Icons.Rounded.Lock, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sdílet") },
                                    onClick = { 
                                        moreExpanded = false
                                        val selectedFiles = state.files.filter { state.selectedFiles.contains(it.path) && !it.isDirectory }
                                        if (selectedFiles.isNotEmpty()) onBatchShare(selectedFiles)
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.Share, null) }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                )
            } else if (showSearch) {
                CenterAlignedTopAppBar(
                    title = {
                        TextField(
                            value = state.searchQuery,
                            onValueChange = onSearchChange,
                            placeholder = { Text("Hledat...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { showSearch = false; onSearchChange("") }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Zrušit")
                        }
                    },
                    actions = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Rounded.Close, "Vymazat")
                            }
                        }
                    }
                )
            } else {
                CenterAlignedTopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(state.breadcrumbs.lastOrNull()?.name ?: "Soubory", style = MaterialTheme.typography.titleMedium)
                            if (isRepo) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (gitState.repoStatus.hasUncommittedChanges) Color(0xFFE6A23C).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = gitState.repoStatus.branchName ?: "Repo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (gitState.repoStatus.hasUncommittedChanges) Color(0xFFE6A23C) else MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        if (state.parentPath != null) {
                            IconButton(onClick = onNavigateUp) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět", tint = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            IconButton(onClick = onMenuClick) {
                                Icon(Icons.Rounded.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateBack, enabled = state.historyBackStack.isNotEmpty()) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět v historii", tint = if (state.historyBackStack.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onNavigateForward, enabled = state.historyForwardStack.isNotEmpty()) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Vpřed v historii", tint = if (state.historyForwardStack.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Rounded.Search, contentDescription = "Hledat", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onNavigate(state.currentPath) }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Obnovit", tint = MaterialTheme.colorScheme.primary)
                        }
                        Box {
                            IconButton(onClick = { showCreateMenu = true }) {
                                Icon(Icons.Rounded.Add, contentDescription = "Přidat", tint = MaterialTheme.colorScheme.primary)
                            }
                            DropdownMenu(
                                expanded = showCreateMenu,
                                onDismissRequest = { showCreateMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Nová složka") },
                                    onClick = { showCreateMenu = false; showCreateFolderDialog = true },
                                    leadingIcon = { Icon(Icons.Rounded.Folder, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Nový soubor") },
                                    onClick = { showCreateMenu = false; showCreateFileDialog = true },
                                    leadingIcon = { Icon(Icons.Rounded.InsertDriveFile, null) }
                                )
                            }
                        }
                        Box {
                            var moreExpanded by remember { mutableStateOf(false) }
                            IconButton(onClick = { moreExpanded = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "Víc")
                            }
                            DropdownMenu(expanded = moreExpanded, onDismissRequest = { moreExpanded = false }) {
                                if (isRepo) {
                                    DropdownMenuItem(
                                        text = { Text("Git: Commit") },
                                        onClick = { moreExpanded = false; showCommitDialog = true },
                                        leadingIcon = { Icon(Icons.Rounded.Commit, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Git: Add All") },
                                        onClick = { moreExpanded = false; onGitAddAll() },
                                        leadingIcon = { Icon(Icons.Rounded.LibraryAdd, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Git: Push") },
                                        onClick = { moreExpanded = false; onGitPush() },
                                        leadingIcon = { Icon(Icons.Rounded.CloudUpload, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Git: Pull") },
                                        onClick = { moreExpanded = false; onGitPull() },
                                        leadingIcon = { Icon(Icons.Rounded.CloudDownload, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Git: Nastavit Remote") },
                                        onClick = { moreExpanded = false; showSetRemoteDialog = true },
                                        leadingIcon = { Icon(Icons.Rounded.Link, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Git: Založit na GitHubu") },
                                        onClick = { moreExpanded = false; showGithubRepoDialog = true },
                                        leadingIcon = { Icon(Icons.Rounded.CloudCircle, null) }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("Inicializovat Git repo") },
                                        onClick = { moreExpanded = false; onGitInit() },
                                        leadingIcon = { Icon(Icons.Rounded.Code, null) }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (clipboard != null && !isMultiSelect) {
                BottomAppBar(
                    actions = {
                        TextButton(onClick = onCancelClipboard) {
                            Text("Zrušit")
                        }
                        Spacer(Modifier.weight(1f))
                        Button(onClick = onPaste) {
                            val opName = if (clipboard.operation == ClipboardOperation.COPY) "Kopírovat" else "Přesunout"
                            Text("$opName sem")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.breadcrumbs.isNotEmpty() && !showSearch) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(state.breadcrumbs, key = { it.path }) { breadcrumb ->
                        val isLast = breadcrumb == state.breadcrumbs.last()
                        Text(
                            text = breadcrumb.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isLast) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                            color = if (isLast) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                .combinedClickable(onClick = { onNavigate(breadcrumb.path) })
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                        if (!isLast) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val filesToShow = if (state.searchQuery.isEmpty()) state.files else state.files.filter { it.name.contains(state.searchQuery, ignoreCase = true) }
                
                if (filesToShow.isEmpty() && !state.isLoading) {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val isSearch = state.searchQuery.isNotEmpty()
                        Icon(
                            if (isSearch) Icons.Rounded.SearchOff else Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (isSearch) "Žádné výsledky pro \"${state.searchQuery}\"" else "Složka je prázdná",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val format = remember { SimpleDateFormat("dd. MM. yyyy HH:mm", Locale.getDefault()) }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filesToShow, key = { it.path }) { file ->
                            val selected = state.selectedFiles.contains(file.path)
                            val gitStatus = gitState.repoStatus.fileStatuses.find { 
                                val relative = java.io.File(file.path).relativeTo(java.io.File(state.currentPath)).path.replace("\\", "/")
                                it.path == relative || (file.isDirectory && it.path.startsWith("$relative/"))
                            }
                            
                            FileListItem(
                                file = file,
                                isFavorite = favorites.any { it.path == file.path },
                                format = format,
                                isSelected = selected,
                                isMultiSelectMode = isMultiSelect,
                                showExtensions = fileSettings.showFileExtensions,
                                syntaxMappings = syntaxMappings,
                                appPreferences = appPreferences,
                                gitStatus = gitStatus?.status,
                                onClick = {
                                    if (isMultiSelect) {
                                        onToggleSelection(file.path)
                                    } else {
                                        if (file.isDirectory) {
                                            onNavigate(file.path)
                                        } else {
                                            onOpenFile(file)
                                        }
                                    }
                                },
                                onLongClick = {
                                    if (!isMultiSelect) {
                                        onToggleSelection(file.path)
                                    }
                                },
                                onRename = { onRename(file, it) },
                                onDelete = {
                                    if (appPreferences.confirmDeletions) {
                                        deleteTarget = file
                                    } else {
                                        onDelete(file)
                                    }
                                },
                                onCopy = { onCopy(file) },
                                onMove = { onMove(file) },
                                onToggleFavorite = {
                                    if (favorites.any { it.path == file.path }) {
                                        onRemoveFavorite(file.path)
                                    } else {
                                        showFavoriteDialogFor = file
                                    }
                                },
                                onShowInfo = { showInfoDialogFor = file },
                                onUnzip = { onUnzipFile(file) },
                                onDecrypt = { decryptTarget = file },
                                onGitAdd = { onGitAdd(file) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(if (clipboard != null) 80.dp else 16.dp))
                        }
                    }
                }
                
                if (state.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
                }
            }
        }
    }

    if (showCreateFolderDialog) {
        InputDialog(title = "Nová složka", initialValue = "", onConfirm = { onCreateFolder(it); showCreateFolderDialog = false }, onDismiss = { showCreateFolderDialog = false })
    }

    if (showCreateFileDialog) {
        InputDialog(title = "Nový soubor", initialValue = "", onConfirm = { onCreateFile(it); showCreateFileDialog = false }, onDismiss = { showCreateFileDialog = false })
    }
    
    if (showCommitDialog) {
        InputDialog(title = "Git Commit", initialValue = "", onConfirm = { onGitCommit(it); showCommitDialog = false }, onDismiss = { showCommitDialog = false })
    }
    
    if (showSetRemoteDialog) {
        InputDialog(title = "Nastavit Remote URL", initialValue = "https://github.com/", onConfirm = { onGitSetRemote(it); showSetRemoteDialog = false }, onDismiss = { showSetRemoteDialog = false })
    }
    
    if (showGithubRepoDialog) {
        var repoName by remember { mutableStateOf(state.breadcrumbs.lastOrNull()?.name ?: "NoveRepo") }
        var isPrivate by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { showGithubRepoDialog = false },
            title = { Text("Založit na GitHubu") },
            text = {
                Column {
                    OutlinedTextField(
                        value = repoName,
                        onValueChange = { repoName = it },
                        label = { Text("Název repozitáře") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPrivate, onCheckedChange = { isPrivate = it })
                        Text("Soukromý repozitář")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onGitCreateGithubRepo(repoName, isPrivate); showGithubRepoDialog = false }) { Text("Vytvořit a propojit") }
            },
            dismissButton = {
                TextButton(onClick = { showGithubRepoDialog = false }) { Text("Zrušit") }
            }
        )
    }
    
    if (showZipDialog) {
        InputDialog(title = "Vytvořit ZIP", initialValue = "archiv", onConfirm = { onBatchZip(it); showZipDialog = false }, onDismiss = { showZipDialog = false })
    }
    
    if (showEncryptDialog) {
        InputDialog(title = "Zadat heslo pro šifrování", initialValue = "", isPassword = true, onConfirm = { onBatchEncrypt(it); showEncryptDialog = false }, onDismiss = { showEncryptDialog = false })
    }
    
    if (showBulkRenameDialog) {
        InputDialog(
            title = "Hromadně přejmenovat", 
            initialValue = "Novy_Nazev", 
            onConfirm = { 
                onBatchRename(it)
                showBulkRenameDialog = false 
            }, 
            onDismiss = { showBulkRenameDialog = false }
        )
    }
    
    if (decryptTarget != null) {
        InputDialog(title = "Zadat heslo pro dešifrování", initialValue = "", isPassword = true, onConfirm = { onDecryptFile(decryptTarget!!, it); decryptTarget = null }, onDismiss = { decryptTarget = null })
    }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Smazat soubor?") },
            text = { Text("Opravdu chcete smazat '${deleteTarget?.name}'?") },
            confirmButton = { TextButton(onClick = { deleteTarget?.let(onDelete); deleteTarget = null }) { Text("Smazat", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Zrušit") } }
        )
    }

    if (batchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { batchDeleteConfirm = false },
            title = { Text("Smazat vybrané?") },
            text = { Text("Opravdu chcete smazat všechny vybrané soubory (${state.selectedFiles.size})?") },
            confirmButton = { TextButton(onClick = { onBatchDelete(); batchDeleteConfirm = false }) { Text("Smazat", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { batchDeleteConfirm = false }) { Text("Zrušit") } }
        )
    }

    if (showFavoriteDialogFor != null) {
        FavoriteEditDialog(
            file = showFavoriteDialogFor!!,
            onConfirm = { favorite ->
                onSaveFavorite(favorite)
                showFavoriteDialogFor = null
            },
            onDismiss = { showFavoriteDialogFor = null }
        )
    }

    if (showInfoDialogFor != null) {
        val file = showInfoDialogFor!!
        val format = remember { SimpleDateFormat("dd. MM. yyyy HH:mm:ss", Locale.getDefault()) }
        val ext = file.name.substringAfterLast('.', "").takeIf { it != file.name }
        
        AlertDialog(
            onDismissRequest = { showInfoDialogFor = null },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            icon = { Icon(getIconForFile(file), contentDescription = null, modifier = Modifier.size(32.dp)) },
            title = { Text("Informace o souboru", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Column {
                        Text("Název", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(file.name, style = MaterialTheme.typography.bodyLarge)
                    }
                    Column {
                        Text("Typ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(if (file.isDirectory) "Složka" else "Soubor ${ext?.let { "(.${it.uppercase()})" } ?: ""}", style = MaterialTheme.typography.bodyLarge)
                    }
                    if (!file.isDirectory) {
                        Column {
                            Text("Velikost", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text("${formatSize(file.size)} (${file.size} bajtů)", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Column {
                        Text("Naposledy upraveno", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(format.format(Date(file.lastModified)), style = MaterialTheme.typography.bodyLarge)
                    }
                    Column {
                        Text("Absolutní cesta", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(file.path, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showInfoDialogFor = null }) { Text("Zavřít") } },
            dismissButton = {
                val context = androidx.compose.ui.platform.LocalContext.current
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Název", file.name))
                        android.widget.Toast.makeText(context, "Název zkopírován", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Kopírovat název")
                    }
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Cesta", file.path))
                        android.widget.Toast.makeText(context, "Cesta zkopírována", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Kopírovat cestu")
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    file: FileModel,
    isFavorite: Boolean,
    format: SimpleDateFormat,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    showExtensions: Boolean,
    syntaxMappings: List<com.example.model.SyntaxMapping>,
    appPreferences: com.example.model.AppPreferences,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShowInfo: () -> Unit,
    onUnzip: () -> Unit,
    onDecrypt: () -> Unit,
    onGitAdd: () -> Unit = {},
    gitStatus: GitFileStatusType? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    
    val bgColor by androidx.compose.animation.animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent
    )

    val dimens = com.example.ui.theme.LocalAppDimens.current
    val ext = file.name.substringAfterLast('.', "").lowercase()
    
    val itemHeight = if (appPreferences.compactListMode) dimens.listItemHeight * 0.75f else dimens.listItemHeight
    val vPadding = if (appPreferences.compactListMode) 4.dp else dimens.paddingMedium

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = itemHeight)
            .padding(horizontal = dimens.paddingMedium, vertical = 2.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .background(bgColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = dimens.paddingMedium, vertical = vPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isMultiSelectMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                modifier = Modifier.padding(end = dimens.paddingLarge)
            )
        }
        
        Box(modifier = Modifier.size(dimens.iconSize * 1.5f), contentAlignment = Alignment.Center) {
            val isImage = !file.isDirectory && ext in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
            
            if (isImage && appPreferences.imageThumbnailsEnabled) {
                coil.compose.AsyncImage(
                    model = java.io.File(file.path),
                    contentDescription = "Náhled",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.size(dimens.iconSize * 1.5f).clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                )
            } else {
                Icon(
                    imageVector = getIconForFile(file),
                    contentDescription = if (file.isDirectory) "Složka" else "Soubor",
                    tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(dimens.iconSize * 1.5f)
                )
            }
            
            if (isFavorite && file.isDirectory) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(MaterialTheme.colorScheme.tertiaryContainer, androidx.compose.foundation.shape.CircleShape)
                        .padding(2.dp)
                ) {
                    Icon(
                        Icons.Rounded.Star, 
                        contentDescription = "Oblíbené",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(dimens.iconSize * 0.6f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(dimens.paddingLarge))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // If showExtensions is true, we show the badge and hide the extension from the text.
                // If false, we show the full name and hide the badge.
                val showBadge = !file.isDirectory && file.name.contains('.') && ext.isNotEmpty() && showExtensions
                val displayName = if (showBadge) file.name.substringBeforeLast('.', file.name) else file.name
                
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                
                if (showBadge && appPreferences.showFileBadges) {
                    Spacer(modifier = Modifier.width(6.dp))
                    val customColor = syntaxMappings.find { it.extension.lowercase() == ext }?.tagColorArgb?.let { Color(it) } ?: getColorForExtension(ext)
                    Box(
                        modifier = Modifier
                            .background(customColor.copy(alpha = 0.2f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = ext,
                            style = MaterialTheme.typography.labelSmall,
                            color = customColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
                
                if (gitStatus != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    val gitColor = when(gitStatus) {
                        GitFileStatusType.MODIFIED -> Color(0xFFE6A23C)
                        GitFileStatusType.ADDED, GitFileStatusType.UNTRACKED -> Color(0xFF67C23A)
                        GitFileStatusType.REMOVED -> Color(0xFFF56C6C)
                        GitFileStatusType.CONFLICTING -> Color(0xFFF56C6C)
                        GitFileStatusType.UNCOMMITTED -> Color(0xFF909399)
                    }
                    val gitLabel = when(gitStatus) {
                        GitFileStatusType.MODIFIED -> "M"
                        GitFileStatusType.ADDED -> "A"
                        GitFileStatusType.UNTRACKED -> "U"
                        GitFileStatusType.REMOVED -> "D"
                        GitFileStatusType.CONFLICTING -> "C"
                        else -> "?"
                    }
                    Box(
                        modifier = Modifier
                            .background(gitColor.copy(alpha = 0.2f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = gitLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = gitColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
            val dateStr = format.format(Date(file.lastModified))
            
            if (appPreferences.detailPanelsEnabled) {
                // Format item details
                val detailStr = if (file.isDirectory) {
                    // If directory, show "Folder" and date
                    "Složka • $dateStr"
                } else {
                    "${formatSize(file.size)} • $dateStr"
                }
                
                Text(
                    text = detailStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        if (!isMultiSelectMode) {
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Možnosti")
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    if (gitStatus == GitFileStatusType.UNTRACKED || gitStatus == GitFileStatusType.MODIFIED || gitStatus == GitFileStatusType.CONFLICTING) {
                        DropdownMenuItem(
                            text = { Text("Git: Add") },
                            onClick = { expanded = false; onGitAdd() },
                            leadingIcon = { Icon(Icons.Rounded.AddBox, null) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(if (isFavorite) "Odebrat z oblíbených" else "Přidat do oblíbených") },
                        onClick = { expanded = false; onToggleFavorite() },
                        leadingIcon = {
                            Icon(if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, contentDescription = null)
                        }
                    )
                    if (file.name.endsWith(".zip", ignoreCase = true)) {
                        DropdownMenuItem(
                            text = { Text("Rozbalit zde") },
                            onClick = { expanded = false; onUnzip() },
                            leadingIcon = { Icon(Icons.Rounded.FolderZip, null) }
                        )
                    }
                    if (file.name.endsWith(".enc", ignoreCase = true)) {
                        DropdownMenuItem(
                            text = { Text("Dešifrovat") },
                            onClick = { expanded = false; onDecrypt() },
                            leadingIcon = { Icon(Icons.Rounded.LockOpen, null) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Přejmenovat") },
                        onClick = { expanded = false; showRenameDialog = true },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Kopírovat") },
                        onClick = { expanded = false; onCopy() },
                        leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Přesunout") },
                        onClick = { expanded = false; onMove() },
                        leadingIcon = { Icon(Icons.Rounded.DriveFileMove, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Informace") },
                        onClick = { expanded = false; onShowInfo() },
                        leadingIcon = { Icon(Icons.Rounded.Info, null) }
                    )
                    if (!file.isDirectory) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        DropdownMenuItem(
                            text = { Text("Sdílet") },
                            onClick = { 
                                expanded = false
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    java.io.File(file.path)
                                )
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "*/*"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "Sdílet"))
                            },
                            leadingIcon = { Icon(Icons.Rounded.Share, null) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Smazat") },
                        onClick = { expanded = false; onDelete() },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }

    if (showRenameDialog) {
        InputDialog(
            title = "Přejmenovat",
            initialValue = file.name,
            onConfirm = { name ->
                onRename(name)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }
}

@Composable
fun InputDialog(
    title: String,
    initialValue: String,
    isPassword: Boolean = false,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text("Potvrdit", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit")
            }
        }
    )
}

@Composable
fun FavoriteEditDialog(
    file: FileModel,
    onConfirm: (com.example.model.FavoriteModel) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(file.name) }
    var selectedIcon by remember { mutableStateOf(com.example.model.FavoriteIcon.STAR) }
    var isPinned by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        title = { Text("Přidat do oblíbených", style = MaterialTheme.typography.titleMedium) },
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
                
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { isPinned = !isPinned },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isPinned, onCheckedChange = { isPinned = it })
                    Text("Připnout nahoru", style = MaterialTheme.typography.bodyMedium)
                }
                
                Text("Ikona", style = MaterialTheme.typography.labelLarge)
                
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        onConfirm(com.example.model.FavoriteModel(file.path, name, true, selectedIcon, isPinned))
                    } 
                }
            ) { Text("Uložit", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Zrušit") }
        }
    )
}
