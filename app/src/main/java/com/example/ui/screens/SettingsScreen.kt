package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ViewCompact
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDensityDialog by remember { mutableStateOf(false) }

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
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
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
                Column {
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
                Column {
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
