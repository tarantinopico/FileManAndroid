package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
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
    
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDensityDialog by remember { mutableStateOf(false) }
    var showEditorDialog by remember { mutableStateOf(false) }
    var showSyntaxScreen by remember { mutableStateOf(false) }

    if (showSyntaxScreen) {
        SyntaxSettingsScreen(viewModel, onNavigateBack = { showSyntaxScreen = false })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nastavení") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            ListItem(
                headlineContent = { Text("Vzhled aplikace") },
                supportingContent = { Text(getThemeLabel(themePreference)) },
                leadingContent = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                modifier = Modifier.clickable { showThemeDialog = true }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Hustota zobrazení (Kompaktnost)") },
                supportingContent = { Text(getDensityLabel(densityPreference)) },
                leadingContent = { Icon(Icons.Rounded.ViewCompact, contentDescription = null) },
                modifier = Modifier.clickable { showDensityDialog = true }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Editor a kód") },
                supportingContent = { Text("Zalamování řádků, čísla řádků") },
                leadingContent = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                modifier = Modifier.clickable { showEditorDialog = true }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Zvýraznění syntaxe") },
                supportingContent = { Text("Spravovat mapování přípon a jazyků") },
                leadingContent = { Icon(Icons.Rounded.Code, contentDescription = null) },
                modifier = Modifier.clickable { showSyntaxScreen = true }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("O aplikaci") },
                leadingContent = { Icon(Icons.Rounded.Info, contentDescription = null) },
                modifier = Modifier.clickable { onNavigateToAbout() }
            )
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Vybrat vzhled") },
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
            title = { Text("Hustota zobrazení") },
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
            title = { Text("Nastavení editoru") },
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyntaxSettingsScreen(
    viewModel: FileManagerViewModel,
    onNavigateBack: () -> Unit
) {
    val mappings by viewModel.syntaxMappings.collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zvýraznění syntaxe") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetSyntaxMappings() }) {
                        Text("Reset", modifier = Modifier.padding(end = 8.dp), color = MaterialTheme.colorScheme.primary)
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
                    headlineContent = { Text(".${mapping.extension}", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(mapping.language.name) },
                    trailingContent = {
                        IconButton(onClick = { viewModel.removeSyntaxMapping(mapping.extension) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Smazat")
                        }
                    }
                )
                HorizontalDivider()
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showAddDialog) {
        var ext by remember { mutableStateOf("") }
        var lang by remember { mutableStateOf(SyntaxLanguage.PLAIN) }
        var showLangDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Přidat příponu") },
            text = {
                Column {
                    OutlinedTextField(
                        value = ext,
                        onValueChange = { ext = it },
                        label = { Text("Přípona (např. kt)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
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
                            label = { Text("Jazyk") },
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
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (ext.isNotBlank()) {
                        viewModel.addSyntaxMapping(ext, lang)
                        showAddDialog = false
                    }
                }) {
                    Text("Přidat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
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
            TopAppBar(
                title = { Text("O aplikaci") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
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
