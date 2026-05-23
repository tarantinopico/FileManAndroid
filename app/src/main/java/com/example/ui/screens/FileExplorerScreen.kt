package com.example.ui.screens

import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.FileModel
import com.example.ui.state.ClipboardOperation
import com.example.ui.state.FileManagerState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    state: FileManagerState,
    favorites: List<com.example.model.FavoriteModel>,
    onNavigate: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onCreateFolder: (String) -> Unit,
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = state.currentPath.substringAfterLast("/").ifEmpty { "Správce souborů" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        if (state.parentPath != null) {
                            IconButton(onClick = onNavigateUp) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Zpět"
                                )
                            }
                        } else {
                            IconButton(onClick = onMenuClick) {
                                Icon(
                                    imageVector = Icons.Rounded.Menu,
                                    contentDescription = "Menu"
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showCreateFolderDialog = true }) {
                            Icon(
                                imageVector = Icons.Rounded.CreateNewFolder,
                                contentDescription = "Nová složka"
                            )
                        }
                    }
                )
                if (state.breadcrumbs.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(state.breadcrumbs) { breadcrumb ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    onClick = { onNavigate(breadcrumb.path) },
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = breadcrumb.name,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                if (breadcrumb != state.breadcrumbs.last()) {
                                    Icon(
                                        imageVector = Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (state.clipboard != null) {
                BottomAppBar {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (state.clipboard.operation == ClipboardOperation.COPY) "Kopírovat sem?" else "Přesunout sem?",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Row {
                            TextButton(onClick = onCancelClipboard) {
                                Text("Zrušit")
                            }
                            Button(onClick = onPaste, modifier = Modifier.padding(start = 8.dp)) {
                                Text("Vložit")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading && state.files.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.errorMessage != null && state.files.isEmpty() -> {
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                state.files.isEmpty() -> {
                    Text(
                        text = "Tato složka je prázdná",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = if (state.clipboard != null) 80.dp else 8.dp, top = 8.dp)
                    ) {
                        items(
                            items = state.files,
                            key = { it.id }
                        ) { file ->
                            FileListItem(
                                file = file,
                                isFavorite = favorites.any { it.path == file.path },
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
                    }
                    
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showCreateFolderDialog) {
        InputDialog(
            title = "Nová složka",
            label = "Název složky",
            initialValue = "",
            confirmText = "Vytvořit",
            onConfirm = { name ->
                onCreateFolder(name)
                showCreateFolderDialog = false
            },
            onDismiss = { showCreateFolderDialog = false }
        )
    }
}

@Composable
fun FileListItem(
    file: FileModel,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    val format = remember { SimpleDateFormat("dd. MM. yyyy HH:mm", Locale.getDefault()) }
    var expanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (file.isDirectory) Icons.Rounded.Folder else Icons.Rounded.Description,
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
                .padding(start = 16.dp)
        ) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = format.format(Date(file.lastModified)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (!file.isDirectory) {
                    Text(
                        text = Formatter.formatShortFileSize(context, file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Možnosti"
                )
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
                DropdownMenuItem(
                    text = { Text("Smazat") },
                    onClick = {
                        expanded = false
                        showDeleteDialog = true
                    }
                )
            }
        }
    }

    if (showRenameDialog) {
        InputDialog(
            title = "Přejmenovat",
            label = "Nový název",
            initialValue = file.name,
            confirmText = "Přejmenovat",
            onConfirm = { newName ->
                onRename(newName)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Smazat položku") },
            text = { Text("Opravdu chcete smazat '${file.name}'? Tato akce je nevratná.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Smazat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Zrušit")
                }
            }
        )
    }
}

@Composable
fun InputDialog(
    title: String,
    label: String,
    initialValue: String,
    confirmText: String,
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
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank() && text != initialValue
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit")
            }
        }
    )
}
