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
    private val editorViewModel: com.example.viewmodel.TextEditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val themePreference = viewModel.themePreference.collectAsState(initial = com.example.model.ThemeMode.SYSTEM).value
            val densityPreference = viewModel.densityPreference.collectAsState(initial = com.example.model.UiDensity.NORMAL).value
            val appPrefs = viewModel.appPreferences.collectAsState().value
            
            FileManagerTheme(themeMode = themePreference, density = densityPreference, appPrefs = appPrefs) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel, editorViewModel)
                }
            }
        }
    }
}
