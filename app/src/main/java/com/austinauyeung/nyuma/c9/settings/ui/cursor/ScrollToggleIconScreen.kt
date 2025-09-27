package com.austinauyeung.nyuma.c9.settings.ui.cursor

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.austinauyeung.nyuma.c9.cursor.domain.ControlScheme
import com.austinauyeung.nyuma.c9.cursor.domain.IconAlignment
import com.austinauyeung.nyuma.c9.settings.ui.DropdownPreferenceItem
import com.austinauyeung.nyuma.c9.settings.ui.PreferenceCategory
import com.austinauyeung.nyuma.c9.settings.ui.SettingsState
import com.austinauyeung.nyuma.c9.settings.ui.SimplePreferenceItem
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrollToggleIconScreen(
    settingsState: SettingsState,
    onNavigateBack: () -> Unit
) {
    val uiState by settingsState.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val inToggleControlScheme = (uiState.controlScheme == ControlScheme.DPAD_TOGGLE || uiState.controlScheme == ControlScheme.NUMPAD_TOGGLE)
    val clearScrollToggleIcon = {
        settingsState.updatePreference(null) { settings, v ->
            settings.copy(scrollToggleImagePath = v)
        }
    }
    val scrollToggleImagePicker = rememberUnifiedImagePickerLauncher(
        coroutineScope = coroutineScope,
        context = context,
        oldPath = uiState.scrollToggleImagePath,
        onCleared = clearScrollToggleIcon,
        updatePreference = { path ->
            settingsState.updatePreference(path) { settings, v -> settings.copy(scrollToggleImagePath = v) }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scroll Toggle Icon") },
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
            PreferenceCategory(title = "Configure") {
                SimplePreferenceItem(
                    title = if (!uiState.scrollToggleImagePath.isNullOrEmpty() && File(uiState.scrollToggleImagePath!!).exists() ) "Change Scroll Toggle Icon" else "Set Scroll Toggle Icon",
                    subtitle = "Supported formats: png, gif, jpg, bmp, webp",
                    onClick = { scrollToggleImagePicker.launch(context) },
                    enabled = uiState.useCustomCursorIcon && inToggleControlScheme
                )
                DropdownPreferenceItem(
                    title = "Scroll Toggle Icon Alignment",
                    subtitle =
                        when (uiState.scrollToggleImageAlignment) {
                            IconAlignment.TOP_LEFT -> "Align to top-left of icon"
                            IconAlignment.CENTER -> "Align to center of icon"
                        },
                    selectedOption = uiState.scrollToggleImageAlignment,
                    options =
                        listOf(
                            IconAlignment.TOP_LEFT to "Top left",
                            IconAlignment.CENTER to "Center"
                        ),
                    onOptionSelected = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(scrollToggleImageAlignment = v)
                        }
                    },
                    enabled = uiState.useCustomCursorIcon && inToggleControlScheme
                )
                SimplePreferenceItem(
                    title = "Clear Scroll Toggle Icon",
                    subtitle = "Fallback to default screen border indicator",
                    onClick = { clearImage(uiState.scrollToggleImagePath, clearScrollToggleIcon) },
                    enabled = uiState.useCustomCursorIcon && inToggleControlScheme
                )
            }
        }
    }
}