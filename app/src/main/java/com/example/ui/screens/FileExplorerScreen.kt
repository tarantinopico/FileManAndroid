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
    if (digitGroups == 0) return "$sizeBytes B"
    return String.format(Locale.getDefault(), "%.1f %s", sizeBytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileExplorerScreen(
    state: FileManagerState,
    favorites: List<FavoriteModel>,
    clipboard: ClipboardState?,
    fileSettings: com.example.model.FileSettings,
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
                        IconButton(onClick = { onBatchCopy(state.files.filter { state.selectedFiles.contains(it.path) }) }) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Kopírovat vybrané")
                        }
                        IconButton(onClick = { onBatchMove(state.files.filter { state.selectedFiles.contains(it.path) }) }) {
                            Icon(Icons.Rounded.DriveFileMove, contentDescription = "Přesunout vybrané")
                        }
                        IconButton(onClick = onBatchDelete) {
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
                        IconButton(onClick = { onNavigate(state.currentPath) }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Obnovit")
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
            if (state.breadcrumbs.isNotEmpty() && !showSearch) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(state.breadcrumbs, key = { it.path }) { breadcrumb ->
                        Surface(
                            color = androidx.compose.ui.graphics.Color.Transparent,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = breadcrumb.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = { onNavigate(breadcrumb.path) }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                        if (breadcrumb != state.breadcrumbs.last()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                HorizontalDivider()
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
                            FileListItem(
                                file = file,
                                isFavorite = favorites.any { it.path == file.path },
                                format = format,
                                isSelected = selected,
                                isMultiSelectMode = isMultiSelect,
                                showExtensions = fileSettings.showFileExtensions,
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
                                onToggleFavorite = {
                                    if (favorites.any { it.path == file.path }) {
                                        onRemoveFavorite(file.path)
                                    } else {
                                        showFavoriteDialogFor = file
                                    }
                                },
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
    
    val bgColor by androidx.compose.animation.animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent
    )

    val dimens = com.example.ui.theme.LocalAppDimens.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.listItemHeight)
            .padding(horizontal = dimens.paddingMedium, vertical = 2.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .background(bgColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = dimens.paddingMedium, vertical = dimens.paddingMedium),
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
            val ext = file.name.substringAfterLast('.', "").lowercase()
            val isImage = !file.isDirectory && ext in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
            
            if (isImage) {
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
            val displayName = if (!file.isDirectory && !showExtensions) file.name.substringBeforeLast('.', file.name) else file.name
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val dateStr = format.format(Date(file.lastModified))
            
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
        
        if (!isMultiSelectMode) {
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Možnosti")
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
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

@Composable
fun FavoriteEditDialog(
    file: FileModel,
    onConfirm: (com.example.model.FavoriteModel) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(file.name) }
    var selectedIcon by remember { mutableStateOf(com.example.model.FavoriteIcon.STAR) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Přidat do oblíbených") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Název") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
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
                        onConfirm(com.example.model.FavoriteModel(file.path, name, true, selectedIcon))
                    } 
                }
            ) { Text("Uložit") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Zrušit") }
        }
    )
}
