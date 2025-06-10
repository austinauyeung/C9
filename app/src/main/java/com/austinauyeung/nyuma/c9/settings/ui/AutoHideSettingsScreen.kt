package com.austinauyeung.nyuma.c9.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoHideSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAutoHideAppsScreen: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auto-Hide Cursor Options") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            PreferenceCategory(title = "Locations") {
                SwitchPreferenceItem(
                    title = "Text Fields",
                    subtitle = "Hide on keyboard open, restore on keyboard close",
                    checked = uiState.hideOnKeyboardOpen,
                    onCheckedChange = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(hideOnKeyboardOpen = v)
                        }
                    },
                )
                SwitchPreferenceItem(
                    title = "Lock Screen",
                    subtitle = "Hide on device lock, restore on device unlock",
                    checked = uiState.hideOnLockScreen,
                    onCheckedChange = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(hideOnLockScreen = v)
                        }
                    },
                )
                SimplePreferenceItem(
                    title = "Select Applications",
                    subtitle = "Auto-hide in specific apps",
                    onClick = onNavigateToAutoHideAppsScreen
                )
            }
        }
    }
}