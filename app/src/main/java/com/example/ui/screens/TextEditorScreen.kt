package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.TextEditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(
    viewModel: TextEditorViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val editorSettings by viewModel.editorSettings.collectAsStateWithLifecycle(initialValue = com.example.model.EditorSettings())
    val syntaxMappings by viewModel.syntaxMappings.collectAsStateWithLifecycle(initialValue = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var expandedMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            if (event is com.example.viewmodel.UiEvent.ShowToast) {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val backAction = {
        if (state.hasUnsavedChanges) {
            showUnsavedDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler(onBack = backAction)

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            title = { Text("Zahodit změny?", style = MaterialTheme.typography.titleMedium) },
            text = { Text("Máte neuložené změny. Opravdu chcete odejít bez uložení?") },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    onNavigateBack()
                }) {
                    Text("Odejít", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedDialog = false }) {
                    Text("Zrušit")
                }
            }
        )
    }

    if (state.showGoToLineDialog) {
        var lineInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.setGoToLineDialogVisible(false) },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            title = { Text("Přejít na řádek", style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = lineInput,
                    onValueChange = { lineInput = it.filter { char -> char.isDigit() } },
                    label = { Text("Číslo řádku") },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    lineInput.toIntOrNull()?.let { viewModel.goToLine(it) }
                }) {
                    Text("Přejít", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setGoToLineDialogVisible(false) }) {
                    Text("Zrušit")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = state.name + if (state.hasUnsavedChanges) " *" else "",
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = backAction) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    actions = {
                        IconButton(onClick = viewModel::undo, enabled = state.canUndo) {
                            Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Zpět (Undo)", tint = if(state.canUndo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha=0.38f))
                        }
                        IconButton(onClick = viewModel::redo, enabled = state.canRedo) {
                            Icon(Icons.AutoMirrored.Rounded.Redo, contentDescription = "Znovu (Redo)", tint = if(state.canRedo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha=0.38f))
                        }
                        IconButton(onClick = viewModel::toggleFindReplace) {
                            Icon(Icons.Rounded.Search, contentDescription = "Hledat", tint = MaterialTheme.colorScheme.primary)
                        }
                        if (state.hasUnsavedChanges) {
                            IconButton(onClick = viewModel::saveFile, enabled = !state.isSaving) {
                                Icon(Icons.Rounded.Save, contentDescription = "Uložit", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Box {
                            IconButton(onClick = { expandedMenu = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "Více", tint = MaterialTheme.colorScheme.primary)
                            }
                            DropdownMenu(
                                expanded = expandedMenu,
                                onDismissRequest = { expandedMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Přejít na řádek") },
                                    onClick = {
                                        expandedMenu = false
                                        viewModel.setGoToLineDialogVisible(true)
                                    }
                                )
                            }
                        }
                    }
                )
                if (state.showFindReplace) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = state.findQuery,
                                    onValueChange = viewModel::onFindQueryChanged,
                                    placeholder = { Text("Hledat...") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    textStyle = TextStyle(fontSize = 14.sp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.findResults.isEmpty()) "0/0" else "${state.currentFindIndex + 1}/${state.findResults.size}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                IconButton(onClick = viewModel::findPrevious, enabled = state.findResults.isNotEmpty()) {
                                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Předchozí")
                                }
                                IconButton(onClick = viewModel::findNext, enabled = state.findResults.isNotEmpty()) {
                                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Další")
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = state.replaceQuery,
                                    onValueChange = viewModel::onReplaceQueryChanged,
                                    placeholder = { Text("Nahradit za...") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    textStyle = TextStyle(fontSize = 14.sp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = viewModel::replaceCurrent,
                                    enabled = state.findResults.isNotEmpty(),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Text("Nahradit")
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                TextButton(
                                    onClick = viewModel::replaceAll,
                                    enabled = state.findResults.isNotEmpty(),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Text("Vše")
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (editorSettings.editorToolbarEnabled && state.errorMessage == null && !state.isLoading) {
                BottomAppBar(
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.imePadding()
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    IconButton(onClick = {
                        val current = state.textFieldValue
                        // Insert tab at cursor
                        val textStr = current.text
                        val selection = current.selection
                        if (selection.collapsed) {
                            val newText = textStr.substring(0, selection.start) + "\t" + textStr.substring(selection.start)
                            val newSelection = androidx.compose.ui.text.TextRange(selection.start + 1)
                            viewModel.onContentChanged(androidx.compose.ui.text.input.TextFieldValue(newText, newSelection))
                        }
                    }) {
                        Icon(Icons.Rounded.SpaceBar, contentDescription = "Vložit tabulátor")
                    }
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val selText = state.textFieldValue.text.substring(state.textFieldValue.selection.start, state.textFieldValue.selection.end)
                        if (selText.isNotEmpty()) {
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("text", selText))
                        } else {
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("text", state.textFieldValue.text))
                        }
                    }) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Kopírovat")
                    }
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = clipboard.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            val text = clip.getItemAt(0).text?.toString() ?: ""
                            val current = state.textFieldValue
                            val txt = current.text
                            val newText = txt.substring(0, current.selection.start) + text + txt.substring(current.selection.end)
                            val newSelection = androidx.compose.ui.text.TextRange(current.selection.start + text.length)
                            viewModel.onContentChanged(androidx.compose.ui.text.input.TextFieldValue(newText, newSelection))
                        }
                    }) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = "Vložit")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    // Format lines length stats
                    Text("${state.textFieldValue.text.lines().size} řádků", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Načítám soubor...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Prosím čekejte.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else if (state.errorMessage != null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Rounded.ErrorOutline,
                        contentDescription = "Chyba",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.errorMessage!!,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val verticalScrollState = rememberScrollState()
                val horizontalScrollState = rememberScrollState()
                val text = state.textFieldValue.text
                
                // Calculate current line for active background highlighting
                val currentLineIndex = remember(state.textFieldValue.selection.start, text) {
                    if (text.isEmpty()) 0 else text.substring(0, minOf(state.textFieldValue.selection.start, text.length)).count { it == '\n' }
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(verticalScrollState)
                ) {
                    if (editorSettings.showLineNumbers) {
                        Column(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(horizontal = 8.dp, vertical = 12.dp)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.End
                        ) {
                            val lineCount = text.count { it == '\n' } + 1
                            for (i in 0 until lineCount) {
                                val isCurrentLine = i == currentLineIndex
                                Text(
                                    text = "${i + 1}",
                                    color = if (isCurrentLine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.End,
                                        fontWeight = if (isCurrentLine) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                    
                    val syntaxLangPair = syntaxMappings.find { it.extension == state.extension }
                    val langName = syntaxLangPair?.language?.name ?: state.extension
                    
                    BasicTextField(
                        value = state.textFieldValue,
                        onValueChange = viewModel::onContentChanged,
                        modifier = Modifier
                            .weight(1f)
                            .let { if (editorSettings.wordWrap) it else it.horizontalScroll(horizontalScrollState) }
                            .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 64.dp),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        visualTransformation = if (editorSettings.syntaxHighlightEnabled || editorSettings.activeLineHighlightEnabled) {
                            val activeColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            SyntaxVisualTransformation(
                                extension = langName, 
                                findQuery = state.findQuery, 
                                activeLineHighlightEnabled = editorSettings.activeLineHighlightEnabled, 
                                activeLineIndex = currentLineIndex, 
                                activeLineColor = activeColor
                            )
                        } else {
                            androidx.compose.ui.text.input.VisualTransformation.None
                        }
                    )
                }
            }
            if (state.isSaving) {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
