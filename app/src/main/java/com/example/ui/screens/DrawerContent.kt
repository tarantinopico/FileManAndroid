package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.model.FavoriteModel
import com.example.model.StorageVolumeModel

@Composable
fun DrawerContent(
    storageVolumes: List<StorageVolumeModel>,
    favorites: List<FavoriteModel>,
    onStorageVolumeClick: (StorageVolumeModel) -> Unit,
    onFavoriteClick: (FavoriteModel) -> Unit,
    onEditFavorite: (FavoriteModel) -> Unit,
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
                    val iconVector = when(favorite.icon) {
                        com.example.model.FavoriteIcon.FOLDER -> Icons.Rounded.Folder
                        com.example.model.FavoriteIcon.STAR -> Icons.Rounded.Star
                        com.example.model.FavoriteIcon.PROJECT -> Icons.Rounded.Work
                        com.example.model.FavoriteIcon.DOWNLOAD -> Icons.Rounded.Download
                        com.example.model.FavoriteIcon.IMAGE -> Icons.Rounded.Image
                        com.example.model.FavoriteIcon.DOCUMENT -> Icons.Rounded.Description
                    }
                    DrawerItem(
                        title = favorite.name,
                        icon = iconVector,
                        onClick = { onFavoriteClick(favorite) },
                        onLongClick = { onEditFavorite(favorite) },
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DrawerItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val dimens = com.example.ui.theme.LocalAppDimens.current
    val contentColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    
    Box(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .let {
                if (onLongClick != null) {
                    it.combinedClickable(enabled = enabled, onClick = onClick, onLongClick = onLongClick)
                } else {
                    it.clickable(enabled = enabled, onClick = onClick)
                }
            }
            .heightIn(min = dimens.listItemHeight)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(dimens.iconSize)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
    }
}
