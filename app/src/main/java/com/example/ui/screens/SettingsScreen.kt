package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ViewCompact
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.EditorSettings
import com.example.model.SyntaxLanguage
import com.example.model.SyntaxMapping
import com.example.model.ThemeMode
import com.example.model.UiDensity
import com.example.viewmodel.FileManagerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FileManagerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val densityPreference by viewModel.densityPreference.collectAsStateWithLifecycle(initialValue = UiDensity.NORMAL)
    val editorSettings by viewModel.editorSettings.collectAsStateWithLifecycle(initialValue = EditorSettings())
    val fileSettings by viewModel.fileSettings.collectAsStateWithLifecycle()
    
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDensityDialog by remember { mutableStateOf(false) }
    var showEditorDialog by remember { mutableStateOf(false) }
    var showSyntaxScreen by remember { mutableStateOf(false) }
    var showFileDialog by remember { mutableStateOf(false) }

    if (showSyntaxScreen) {
        SyntaxSettingsScreen(viewModel, onNavigateBack = { showSyntaxScreen = false })
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Nastavení", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Vzhled aplikace") },
                        supportingContent = { Text(getThemeLabel(themePreference)) },
                        leadingContent = { Icon(Icons.Rounded.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        modifier = Modifier.clickable { showThemeDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))
                    ListItem(
                        headlineContent = { Text("Hustota zobrazení") },
                        supportingContent = { Text(getDensityLabel(densityPreference)) },
                        leadingContent = { Icon(Icons.Rounded.ViewCompact, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        modifier = Modifier.clickable { showDensityDialog = true }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Prohlížení souborů") },
                        supportingContent = { Text("Skryté soubory, přípony, řazení") },
                        leadingContent = { Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        modifier = Modifier.clickable { showFileDialog = true }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Editor a kód") },
                        supportingContent = { Text("Zalamování, čísla řádků") },
                        leadingContent = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        modifier = Modifier.clickable { showEditorDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))
                    ListItem(
                        headlineContent = { Text("Zvýraznění syntaxe") },
                        supportingContent = { Text("Spravovat mapování") },
                        leadingContent = { Icon(Icons.Rounded.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        modifier = Modifier.clickable { showSyntaxScreen = true }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                ListItem(
                    headlineContent = { Text("O aplikaci") },
                    leadingContent = { Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    modifier = Modifier.clickable { onNavigateToAbout() }
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            title = { Text("Vybrat vzhled", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    ThemeMode.values().forEach { mode ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (mode == themePreference),
                                    onClick = { 
                                        viewModel.setThemeMode(mode)
                                        showThemeDialog = false
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (mode == themePreference),
                                onClick = null
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(getThemeLabel(mode))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Zavřít")
                }
            }
        )
    }
    
    if (showDensityDialog) {
        AlertDialog(
            onDismissRequest = { showDensityDialog = false },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            title = { Text("Hustota zobrazení", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    UiDensity.values().forEach { density ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (density == densityPreference),
                                    onClick = { 
                                        viewModel.setUiDensity(density)
                                        showDensityDialog = false
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (density == densityPreference),
                                onClick = null
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(getDensityLabel(density))
                                Text(getDensityDescription(density), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDensityDialog = false }) {
                    Text("Zavřít")
                }
            }
        )
    }

    if (showEditorDialog) {
        AlertDialog(
            onDismissRequest = { showEditorDialog = false },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            title = { Text("Nastavení editoru", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.updateEditorSettings(editorSettings.copy(wordWrap = !editorSettings.wordWrap)) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Zalamování slov")
                            Text("Dlouhé řádky budou automaticky zalomeny", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = editorSettings.wordWrap, onCheckedChange = null)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.updateEditorSettings(editorSettings.copy(showLineNumbers = !editorSettings.showLineNumbers)) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Čísla řádků")
                            Text("Zobrazit čísla řádků na okraji editoru", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = editorSettings.showLineNumbers, onCheckedChange = null)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.updateEditorSettings(editorSettings.copy(syntaxHighlightEnabled = !editorSettings.syntaxHighlightEnabled)) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Zvýraznění syntaxe")
                            Text("Globálně zapne nebo vypne barvení kódu", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = editorSettings.syntaxHighlightEnabled, onCheckedChange = null)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEditorDialog = false }) {
                    Text("Zavřít")
                }
            }
        )
    }

    if (showFileDialog) {
        AlertDialog(
            onDismissRequest = { showFileDialog = false },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            title = { Text("Prohlížení souborů", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.updateFileSettings(fileSettings.copy(showHiddenFiles = !fileSettings.showHiddenFiles)) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Skryté soubory")
                            Text("Zobrazit soubory a složky začínající tečkou", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = fileSettings.showHiddenFiles, onCheckedChange = null)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.updateFileSettings(fileSettings.copy(showFileExtensions = !fileSettings.showFileExtensions)) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Přípony souborů")
                            Text("Zobrazovat za názvem souboru jeho typ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = fileSettings.showFileExtensions, onCheckedChange = null)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Výchozí řazení", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    com.example.model.SortOption.values().forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (option == fileSettings.sortOption),
                                    onClick = { 
                                        viewModel.updateFileSettings(fileSettings.copy(sortOption = option))
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (option == fileSettings.sortOption),
                                onClick = null
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(getSortLabel(option))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFileDialog = false }) {
                    Text("Zavřít")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyntaxSettingsScreen(
    viewModel: FileManagerViewModel,
    onNavigateBack: () -> Unit
) {
    val mappings by viewModel.syntaxMappings.collectAsStateWithLifecycle(initialValue = emptyList())
    var editingMapping by remember { mutableStateOf<SyntaxMapping?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Zvýraznění syntaxe", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetSyntaxMappings() }) {
                        Text("Reset", modifier = Modifier.padding(end = 8.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "Přidat příponu")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Text(
                text = "Přiřazení přípon k jazykům",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )
            
            mappings.forEach { mapping ->
                ListItem(
                    headlineContent = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(".${mapping.extension}", fontWeight = FontWeight.Bold)
                            val customColor = mapping.tagColorArgb?.let { androidx.compose.ui.graphics.Color(it) } ?: getColorForExtension(mapping.extension)
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(customColor.copy(alpha = 0.2f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Ukázka štítku",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = customColor.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    supportingContent = { Text(mapping.language.name) },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { 
                                editingMapping = mapping
                                showAddDialog = true
                            }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Upravit")
                            }
                            IconButton(onClick = { viewModel.removeSyntaxMapping(mapping.extension) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Smazat")
                            }
                        }
                    },
                    modifier = Modifier.clickable {
                        editingMapping = mapping
                        showAddDialog = true
                    }
                )
                HorizontalDivider()
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showAddDialog) {
        var ext by remember { mutableStateOf(editingMapping?.extension ?: "") }
        var lang by remember { mutableStateOf(editingMapping?.language ?: SyntaxLanguage.PLAIN) }
        var selectedColor by remember { mutableStateOf<androidx.compose.ui.graphics.Color?>(editingMapping?.tagColorArgb?.let { androidx.compose.ui.graphics.Color(it) }) }
        var showLangDropdown by remember { mutableStateOf(false) }
        
        val presetColors = listOf(
            androidx.compose.ui.graphics.Color(0xFFF44336), // Red
            androidx.compose.ui.graphics.Color(0xFFFF9800), // Orange
            androidx.compose.ui.graphics.Color(0xFF4CAF50), // Green
            androidx.compose.ui.graphics.Color(0xFF2196F3), // Blue
            androidx.compose.ui.graphics.Color(0xFF9C27B0), // Purple
            androidx.compose.ui.graphics.Color(0xFF009688)  // Teal
        )

        AlertDialog(
            onDismissRequest = { showAddDialog = false; editingMapping = null },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            title = { Text(if (editingMapping != null) "Upravit příponu" else "Přidat příponu", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    OutlinedTextField(
                        value = ext,
                        onValueChange = { ext = it },
                        label = { Text("Přípona (např. kt)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = editingMapping == null // Do not allow changing extension name while editing
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ExposedDropdownMenuBox(
                        expanded = showLangDropdown,
                        onExpandedChange = { showLangDropdown = !showLangDropdown }
                    ) {
                        OutlinedTextField(
                            value = lang.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Jazyk pro editor") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showLangDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = showLangDropdown,
                            onDismissRequest = { showLangDropdown = false }
                        ) {
                            SyntaxLanguage.values().forEach { sl ->
                                DropdownMenuItem(
                                    text = { Text(sl.name) },
                                    onClick = {
                                        lang = sl
                                        showLangDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Barva štítku (volitelné)", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            // Default / Auto color
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = if (selectedColor == null) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                                    .clickable { selectedColor = null },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Auto", style = MaterialTheme.typography.labelSmall, color = if (selectedColor == null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        items(presetColors.size) { index ->
                            val color = presetColors[index]
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(color, androidx.compose.foundation.shape.CircleShape)
                                    .clickable { selectedColor = color },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColor == color) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Vybráno", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (ext.isNotBlank()) {
                        viewModel.addSyntaxMapping(ext, lang, selectedColor?.toArgb())
                        showAddDialog = false
                        editingMapping = null
                    }
                }) {
                    Text("Uložit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; editingMapping = null }) {
                    Text("Zrušit")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("O aplikaci", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Správce souborů",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Verze 1.0",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Minimalistický manažer souborů vytvořený jako produkční ukázka v Kotlin Compose a Material 3.\nUmožňuje snadnou správu složek, oblíbených a externích úložišť.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

private fun getThemeLabel(mode: ThemeMode): String {
    return when (mode) {
        ThemeMode.SYSTEM -> "Výchozí nastavení systému"
        ThemeMode.LIGHT -> "Světlý motiv"
        ThemeMode.DARK -> "Tmavý motiv"
    }
}

private fun getDensityLabel(density: UiDensity): String {
    return when (density) {
        UiDensity.COMPACT -> "Nejmenší (Kompaktní)"
        UiDensity.NORMAL -> "Normální"
        UiDensity.LARGE -> "Velké"
        UiDensity.EXTRA_LARGE -> "Největší"
    }
}

private fun getDensityDescription(density: UiDensity): String {
    return when (density) {
        UiDensity.COMPACT -> "Minimum mezer, maximální množství informací na obrazovce."
        UiDensity.NORMAL -> "Vyvážený poměr mezi mezerami a informacemi."
        UiDensity.LARGE -> "Větší mezery, ikony i ovladatelnost."
        UiDensity.EXTRA_LARGE -> "Maximální čitelnost a snadné dotyky, vhodné pro velké displeje."
    }
}

private fun getSortLabel(option: com.example.model.SortOption): String {
    return when (option) {
        com.example.model.SortOption.NAME_ASC -> "Název (A-Z)"
        com.example.model.SortOption.NAME_DESC -> "Název (Z-A)"
        com.example.model.SortOption.SIZE_ASC -> "Velikost (Od nejmenšího)"
        com.example.model.SortOption.SIZE_DESC -> "Velikost (Od největšího)"
        com.example.model.SortOption.DATE_ASC -> "Datum (Od nejstaršího)"
        com.example.model.SortOption.DATE_DESC -> "Datum (Od nejnovějšího)"
        com.example.model.SortOption.TYPE_ASC -> "Typ (A-Z)"
        com.example.model.SortOption.TYPE_DESC -> "Typ (Z-A)"
    }
}
