package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.FavoriteModel
import com.example.model.StorageVolumeModel

@Composable
fun DrawerContent(
    storageVolumes: List<StorageVolumeModel>,
    favorites: List<FavoriteModel>,
    onStorageVolumeClick: (StorageVolumeModel) -> Unit,
    onFavoriteClick: (FavoriteModel) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(modifier = modifier) {
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            item {
                Text(
                    text = "Umístění",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                )
            }
            items(storageVolumes) { volume ->
                DrawerItem(
                    title = volume.name,
                    icon = if (volume.isPrimary) Icons.Rounded.Home else if (volume.isRemovable) Icons.Rounded.Add else Icons.Rounded.Info,
                    onClick = { onStorageVolumeClick(volume) }
                )
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Text(
                    text = "Oblíbené",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
                )
            }
            if (favorites.isEmpty()) {
                item {
                    Text(
                        text = "Žádné oblíbené",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(favorites) { favorite ->
                    DrawerItem(
                        title = favorite.name,
                        icon = Icons.Rounded.Favorite,
                        onClick = { onFavoriteClick(favorite) },
                        enabled = favorite.isAvailable
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        HorizontalDivider()
        DrawerItem(
            title = "Nastavení",
            icon = Icons.Rounded.Settings,
            onClick = onSettingsClick
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun DrawerItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val contentColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor
        )
    }
}
