package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.FileModel
import com.example.model.TagModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TagsDialog(
    file: FileModel,
    availableTags: List<TagModel>,
    onDismiss: () -> Unit,
    onAddTagToFile: (TagModel) -> Unit,
    onRemoveTagFromFile: (TagModel) -> Unit,
    onCreateTag: (String, Int) -> Unit,
    onDeleteTag: (TagModel) -> Unit
) {
    var showCreateTag by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text("Spravovat štítky", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(file.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))

                val fileTagsIds = file.tags.map { it.id }.toSet()

                Text("Aktivní štítky", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (file.tags.isEmpty()) {
                    Text("Žádné štítky", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        file.tags.forEach { tag ->
                            InputChip(
                                selected = true,
                                onClick = { onRemoveTagFromFile(tag) },
                                label = { Text(tag.name) },
                                trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = "Odebrat", modifier = Modifier.size(16.dp)) },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = Color(tag.colorArgb).copy(alpha = 0.2f),
                                    selectedLabelColor = Color(tag.colorArgb).copy(alpha = 1f)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Dostupné štítky", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { showCreateTag = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Nový štítek")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(availableTags) { tag ->
                        val isAttached = fileTagsIds.contains(tag.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isAttached) onRemoveTagFromFile(tag) else onAddTagToFile(tag)
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Checkbox(checked = isAttached, onCheckedChange = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(tag.colorArgb).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(tag.name, style = MaterialTheme.typography.labelMedium, color = Color(tag.colorArgb))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { onDeleteTag(tag) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Smazat štítek", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) {
                        Text("Hotovo")
                    }
                }
            }
        }
    }

    if (showCreateTag) {
        var newTagName by remember { mutableStateOf("") }
        var selectedColor by remember { mutableStateOf(Color(0xFF1976D2)) }
        val colors = listOf(
            Color(0xFF1976D2), Color(0xFFD32F2F), Color(0xFF388E3C), Color(0xFFFBC02D),
            Color(0xFF8E24AA), Color(0xFFE64A19), Color(0xFF0097A7), Color(0xFF607D8B)
        )

        AlertDialog(
            onDismissRequest = { showCreateTag = false },
            title = { Text("Nový štítek") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        label = { Text("Název") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Barva", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(color)
                                    .clickable { selectedColor = color }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (color == selectedColor) {
                                    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha=0.5f), RoundedCornerShape(16.dp)))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTagName.isNotBlank()) {
                            onCreateTag(newTagName.trim(), selectedColor.toArgb())
                            showCreateTag = false
                        }
                    }
                ) {
                    Text("Vytvořit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTag = false }) {
                    Text("Zrušit")
                }
            }
        )
    }
}
