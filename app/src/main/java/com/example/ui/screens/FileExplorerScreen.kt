package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import java.text.SimpleDateFormat
import java.util.*

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

fun formatSize(sizeBytes: Long): String {
    if (sizeBytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(sizeBytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.getDefault(), "%.1f %s", sizeBytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileExplorerScreen(
    state: FileManagerState,
    favorites: List<FavoriteModel>,
    clipboard: ClipboardState?,
    onNavigate: (String) -> Unit,
    onNavigateUp: () -> Unit,
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
    onToggleFavorite: (FileModel) -> Unit,
    onToggleSelection: (String) -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onSearchChange: (String) -> Unit,
    onBatchDelete: () -> Unit,
    onBatchZip: (String) -> Unit,
    onBatchEncrypt: (String) -> Unit,
    onDecryptFile: (FileModel, String) -> Unit,
    onUnzipFile: (FileModel) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showCreateMenu by remember { mutableStateOf(false) }
    var showInfoDialogFor by remember { mutableStateOf<FileModel?>(null) }
    var showZipDialog by remember { mutableStateOf(false) }
    var showEncryptDialog by remember { mutableStateOf(false) }
    var decryptTarget by remember { mutableStateOf<FileModel?>(null) }
    
    val isMultiSelect = state.selectedFiles.isNotEmpty()
    val isSearchMode = state.searchQuery.isNotEmpty()

    var showSearch by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isMultiSelect) {
                TopAppBar(
                    title = { Text("${state.selectedFiles.size} vybráno") },
                    navigationIcon = {
                        IconButton(onClick = onClearSelection) {
                            Icon(Icons.Rounded.Close, contentDescription = "Zrušit výběr")
                        }
                    },
                    actions = {
                        IconButton(onClick = onSelectAll) {
                            Icon(Icons.Rounded.SelectAll, contentDescription = "Vybrat vše")
                        }
                        IconButton(onClick = onBatchDelete) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Smazat vybrané")
                        }
                        Box {
                            var moreExpanded by remember { mutableStateOf(false) }
                            IconButton(onClick = { moreExpanded = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "Více")
                            }
                            DropdownMenu(expanded = moreExpanded, onDismissRequest = { moreExpanded = false }) {
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
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                )
            } else if (showSearch) {
                TopAppBar(
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
                TopAppBar(
                    title = { Text(state.breadcrumbs.lastOrNull()?.name ?: "Soubory") },
                    navigationIcon = {
                        if (state.parentPath != null) {
                            IconButton(onClick = onNavigateUp) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
                            }
                        } else {
                            IconButton(onClick = onMenuClick) {
                                Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Rounded.Search, contentDescription = "Hledat")
                        }
                        Box {
                            IconButton(onClick = { showCreateMenu = true }) {
                                Icon(Icons.Rounded.Add, contentDescription = "Přidat")
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
            if (state.breadcrumbs.size > 1 && !showSearch) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(state.breadcrumbs) { breadcrumb ->
                        Text(
                            text = breadcrumb.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = { onNavigate(breadcrumb.path) }
                                )
                                .padding(4.dp)
                        )
                        if (breadcrumb != state.breadcrumbs.last()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                HorizontalDivider()
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val filesToShow = if (state.searchQuery.isEmpty()) state.files else state.files.filter { it.name.contains(state.searchQuery, ignoreCase = true) }
                
                if (filesToShow.isEmpty() && !state.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (state.searchQuery.isNotEmpty()) "Žádné výsledky" else "Složka je prázdná", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    val format = remember { SimpleDateFormat("dd. MM. yyyy HH:mm", Locale.getDefault()) }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filesToShow, key = { it.path }) { file ->
                            val selected = state.selectedFiles.contains(file.path)
                            FileListItem(
                                file = file,
                                isFavorite = favorites.any { it.path == file.path },
                                format = format,
                                isSelected = selected,
                                isMultiSelectMode = isMultiSelect,
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
                                onDelete = { onDelete(file) },
                                onCopy = { onCopy(file) },
                                onMove = { onMove(file) },
                                onToggleFavorite = { onToggleFavorite(file) },
                                onShowInfo = { showInfoDialogFor = file },
                                onUnzip = { onUnzipFile(file) },
                                onDecrypt = { decryptTarget = file }
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
    
    if (showZipDialog) {
        InputDialog(title = "Vytvořit ZIP", initialValue = "archiv", onConfirm = { onBatchZip(it); showZipDialog = false }, onDismiss = { showZipDialog = false })
    }
    
    if (showEncryptDialog) {
        InputDialog(title = "Zadat heslo pro šifrování", initialValue = "", isPassword = true, onConfirm = { onBatchEncrypt(it); showEncryptDialog = false }, onDismiss = { showEncryptDialog = false })
    }
    
    if (decryptTarget != null) {
        InputDialog(title = "Zadat heslo pro dešifrování", initialValue = "", isPassword = true, onConfirm = { onDecryptFile(decryptTarget!!, it); decryptTarget = null }, onDismiss = { decryptTarget = null })
    }

    if (showInfoDialogFor != null) {
        val file = showInfoDialogFor!!
        val format = remember { SimpleDateFormat("dd. MM. yyyy HH:mm:ss", Locale.getDefault()) }
        AlertDialog(
            onDismissRequest = { showInfoDialogFor = null },
            title = { Text("Informace", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Název: ${file.name}", style = MaterialTheme.typography.bodyMedium)
                    Text("Typ: ${if (file.isDirectory) "Složka" else "Soubor"}", style = MaterialTheme.typography.bodyMedium)
                    if (!file.isDirectory) {
                        Text("Velikost: ${formatSize(file.size)} (${file.size} bajtů)", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("Upraveno: ${format.format(Date(file.lastModified))}", style = MaterialTheme.typography.bodyMedium)
                    Text("Cesta: ${file.path}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { showInfoDialogFor = null }) { Text("Zavřít") } }
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
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShowInfo: () -> Unit,
    onUnzip: () -> Unit,
    onDecrypt: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else androidx.compose.ui.graphics.Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isMultiSelectMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                modifier = Modifier.padding(end = 12.dp)
            )
        }
        
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = getIconForFile(file),
                contentDescription = if (file.isDirectory) "Složka" else "Soubor",
                tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val dateStr = format.format(Date(file.lastModified))
            val detailStr = if (file.isDirectory) dateStr else "${formatSize(file.size)} • $dateStr"
            Text(
                text = detailStr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                    if (file.isDirectory) {
                        DropdownMenuItem(
                            text = { Text(if (isFavorite) "Odebrat z oblíbených" else "Přidat do oblíbených") },
                            onClick = { expanded = false; onToggleFavorite() },
                            leadingIcon = {
                                Icon(if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, contentDescription = null)
                            }
                        )
                    }
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
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text("Potvrdit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit")
            }
        }
    )
}
