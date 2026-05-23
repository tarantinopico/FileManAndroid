package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
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
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(modifier = modifier) {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
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
                    icon = if (volume.isPrimary) Icons.Rounded.Smartphone else if (volume.isRemovable) Icons.Rounded.Usb else Icons.Rounded.SdStorage,
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
        }
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
