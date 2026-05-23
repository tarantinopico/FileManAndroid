package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.example.ui.screens.AppNavigation
import com.example.ui.theme.FileManagerTheme
import com.example.viewmodel.FileManagerViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: FileManagerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themePreference = viewModel.themePreference.collectAsState(initial = com.example.model.ThemeMode.SYSTEM).value
            
            val isDarkTheme = when (themePreference) {
                com.example.model.ThemeMode.LIGHT -> false
                com.example.model.ThemeMode.DARK -> true
                com.example.model.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            FileManagerTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel)
                }
            }
        }
    }
}
