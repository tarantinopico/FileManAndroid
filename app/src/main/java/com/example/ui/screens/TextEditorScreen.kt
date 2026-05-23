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
            title = { Text("Zahodit změny?") },
            text = { Text("Máte neuložené změny. Opravdu chcete odejít bez uložení?") },
            confirmButton = {
                Button(onClick = {
                    showUnsavedDialog = false
                    onNavigateBack()
                }) {
                    Text("Odejít")
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
            title = { Text("Přejít na řádek") },
            text = {
                OutlinedTextField(
                    value = lineInput,
                    onValueChange = { lineInput = it.filter { char -> char.isDigit() } },
                    label = { Text("Číslo řádku") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    lineInput.toIntOrNull()?.let { viewModel.goToLine(it) }
                }) {
                    Text("Přejít")
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
                TopAppBar(
                    title = {
                        Text(
                            text = state.name + if (state.hasUnsavedChanges) " *" else "",
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = backAction) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
                        }
                    },
                    actions = {
                        IconButton(onClick = viewModel::undo, enabled = state.canUndo) {
                            Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Zpět (Undo)")
                        }
                        IconButton(onClick = viewModel::redo, enabled = state.canRedo) {
                            Icon(Icons.AutoMirrored.Rounded.Redo, contentDescription = "Znovu (Redo)")
                        }
                        IconButton(onClick = viewModel::toggleFindReplace) {
                            Icon(Icons.Rounded.Search, contentDescription = "Hledat")
                        }
                        if (state.hasUnsavedChanges) {
                            IconButton(onClick = viewModel::saveFile, enabled = !state.isSaving) {
                                Icon(Icons.Rounded.Save, contentDescription = "Uložit")
                            }
                        }
                        Box {
                            IconButton(onClick = { expandedMenu = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "Více")
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
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage!!,
                    modifier = Modifier.padding(16.dp).align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                val verticalScrollState = rememberScrollState()
                val horizontalScrollState = rememberScrollState()
                val text = state.textFieldValue.text
                val lines = text.count { it == '\n' } + 1
                val lineNumbersText = remember(lines) { (1..lines).joinToString("\n") }
                
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(verticalScrollState)
                ) {
                    if (editorSettings.showLineNumbers) {
                        Text(
                            text = lineNumbersText,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(horizontal = 8.dp, vertical = 12.dp)
                                .fillMaxHeight(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                textAlign = TextAlign.End
                            )
                        )
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
                        visualTransformation = if (editorSettings.syntaxHighlightEnabled) {
                            SyntaxVisualTransformation(langName, state.findQuery)
                        } else {
                            androidx.compose.ui.text.input.VisualTransformation.None
                        }
                    )
                }
            }
            if (state.isSaving) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
