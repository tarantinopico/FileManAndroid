package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import com.example.model.FavoriteModel
import com.example.model.FileModel
import com.example.viewmodel.ClipboardOperation
import com.example.viewmodel.ClipboardState
import com.example.viewmodel.FileManagerState
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.vector.ImageVector

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
        "kt", "java", "xml", "json", "py", "js", "html", "css", "cpp", "c", "h" -> Icons.Rounded.Code
        "apk", "aab" -> Icons.Rounded.Android
        else -> Icons.Rounded.InsertDriveFile
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showCreateMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
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
                                onClick = {
                                    showCreateMenu = false
                                    showCreateFolderDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Folder, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Nový soubor") },
                                onClick = {
                                    showCreateMenu = false
                                    showCreateFileDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.InsertDriveFile, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (clipboard != null) {
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
            if (state.breadcrumbs.size > 1) {
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
                                .clickable { onNavigate(breadcrumb.path) }
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
                if (state.files.isEmpty() && !state.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Složka je prázdná", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    val format = remember { SimpleDateFormat("dd. MM. yyyy HH:mm", Locale.getDefault()) }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.files, key = { it.path }) { file ->
                            FileListItem(
                                file = file,
                                isFavorite = favorites.any { it.path == file.path },
                                format = format,
                                onClick = {
                                    if (file.isDirectory) {
                                        onNavigate(file.path)
                                    } else {
                                        onOpenFile(file)
                                    }
                                },
                                onRename = { onRename(file, it) },
                                onDelete = { onDelete(file) },
                                onCopy = { onCopy(file) },
                                onMove = { onMove(file) },
                                onToggleFavorite = { onToggleFavorite(file) }
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
        InputDialog(
            title = "Nová složka",
            initialValue = "",
            onConfirm = { name ->
                onCreateFolder(name)
                showCreateFolderDialog = false
            },
            onDismiss = { showCreateFolderDialog = false }
        )
    }

    if (showCreateFileDialog) {
        InputDialog(
            title = "Nový soubor",
            initialValue = "",
            onConfirm = { name ->
                onCreateFile(name)
                showCreateFileDialog = false
            },
            onDismiss = { showCreateFileDialog = false }
        )
    }
}

@Composable
fun FileListItem(
    file: FileModel,
    isFavorite: Boolean,
    format: SimpleDateFormat,
    onClick: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = getIconForFile(file),
                contentDescription = if (file.isDirectory) "Složka" else "Soubor",
                tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            if (isFavorite && file.isDirectory) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = "Oblíbené",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp).align(Alignment.BottomEnd)
                )
            }
        }
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
            val detailText = if (file.isDirectory) {
                format.format(Date(file.lastModified))
            } else {
                "${formatFileSize(file.size)} • ${format.format(Date(file.lastModified))}"
            }
            Text(
                text = detailText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Další možnosti")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (file.isDirectory) {
                    DropdownMenuItem(
                        text = { Text(if (isFavorite) "Odebrat z oblíbených" else "Přidat do oblíbených") },
                        onClick = {
                            expanded = false
                            onToggleFavorite()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Přejmenovat") },
                    onClick = {
                        expanded = false
                        showRenameDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("Kopírovat") },
                    onClick = {
                        expanded = false
                        onCopy()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Přesunout") },
                    onClick = {
                        expanded = false
                        onMove()
                    }
                )
                if (!file.isDirectory) {
                    DropdownMenuItem(
                        text = { Text("Sdílet") },
                        onClick = {
                            expanded = false
                            try {
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
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Nelze sdílet soubor", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Smazat", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        expanded = false
                        onDelete()
                    }
                )
            }
        }
    }

    if (showRenameDialog) {
        InputDialog(
            title = "Přejmenovat",
            initialValue = file.name,
            onConfirm = { newName ->
                onRename(newName)
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
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) onConfirm(text)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit")
            }
        }
    )
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
