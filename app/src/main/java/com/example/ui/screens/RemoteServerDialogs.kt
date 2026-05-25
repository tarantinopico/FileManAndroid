package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.RemoteServerModel
import com.example.model.ServerType
import kotlinx.coroutines.launch

@Composable
fun RemoteServerDialog(
    initialServer: RemoteServerModel?,
    onConfirm: (RemoteServerModel) -> Unit,
    onDismiss: () -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialServer?.name ?: "") }
    var host by remember { mutableStateOf(initialServer?.host ?: "") }
    var port by remember { mutableStateOf(initialServer?.port?.toString() ?: "22") }
    var username by remember { mutableStateOf(initialServer?.username ?: "") }
    var authValue by remember { mutableStateOf(initialServer?.passwordOrKeyPath ?: "") }
    var remotePath by remember { mutableStateOf(initialServer?.remotePath ?: "/") }
    var serverType by remember { mutableStateOf(initialServer?.type ?: ServerType.SFTP) }
    var authModeIsPassword by remember { mutableStateOf(authValue.isEmpty() || !authValue.startsWith("/")) }
    
    var showPassword by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val keyPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                // Kopírování private key do interní složky pro snazší použití JSch
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val keyDir = java.io.File(context.filesDir, "keys")
                    if (!keyDir.exists()) keyDir.mkdirs()
                    val keyFile = java.io.File(keyDir, "key_${System.currentTimeMillis()}")
                    val outputStream = java.io.FileOutputStream(keyFile)
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                    authValue = keyFile.absolutePath
                    authModeIsPassword = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (initialServer != null) "Upravit server" else "Přidat server",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = serverType == ServerType.SFTP,
                        onClick = { serverType = ServerType.SFTP; if (port == "21") port = "22" },
                        label = { Text("SFTP") }
                    )
                    FilterChip(
                        selected = serverType == ServerType.FTP,
                        onClick = { serverType = ServerType.FTP; if (port == "22") port = "21" },
                        label = { Text("FTP") }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Název (např. Práce)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Host") },
                        singleLine = true,
                        modifier = Modifier.weight(0.7f)
                    )
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        singleLine = true,
                        modifier = Modifier.weight(0.3f)
                    )
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Uživatelské jméno") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                if (serverType == ServerType.SFTP) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                         FilterChip(
                            selected = authModeIsPassword,
                            onClick = { authModeIsPassword = true; authValue = "" },
                            label = { Text("Heslo") }
                        )
                        FilterChip(
                            selected = !authModeIsPassword,
                            onClick = { authModeIsPassword = false; authValue = "" },
                            label = { Text("Private Key") }
                        )
                    }
                } else {
                    authModeIsPassword = true
                }

                if (authModeIsPassword) {
                    OutlinedTextField(
                        value = authValue,
                        onValueChange = { authValue = it },
                        label = { Text("Heslo") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = "Zobrazit heslo"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                } else {
                    OutlinedTextField(
                        value = if (authValue.startsWith("/")) "...${authValue.takeLast(15)}" else "Nevybráno",
                        onValueChange = { },
                        label = { Text("Private Key") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { keyPickerLauncher.launch("*/*") }) {
                                Icon(Icons.Rounded.Key, contentDescription = "Vybrat klíč")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }

                OutlinedTextField(
                    value = remotePath,
                    onValueChange = { remotePath = it },
                    label = { Text("Výchozí složka") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (initialServer != null && onDelete != null) {
                        TextButton(
                            onClick = { onDelete(initialServer.id) },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Smazat")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Zrušit")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && host.isNotBlank() && port.toIntOrNull() != null && username.isNotBlank()) {
                                onConfirm(
                                    RemoteServerModel(
                                        id = initialServer?.id ?: "",
                                        type = serverType,
                                        name = name,
                                        host = host,
                                        port = port.toInt(),
                                        username = username,
                                        passwordOrKeyPath = authValue.takeIf { it.isNotEmpty() },
                                        remotePath = remotePath
                                    )
                                )
                            }
                        }
                    ) {
                        Text("Uložit")
                    }
                }
            }
        }
    }
}
