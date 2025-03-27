package com.austinauyeung.nyuma.c9.settings.ui

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.austinauyeung.nyuma.c9.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugOptionsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLogScreen: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showShizukuDialog by remember { mutableStateOf(false) }
    var showExperimentalDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug Options") },
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
            PreferenceCategory(title = "Shizuku") {
                val switchEnabled = (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) || BuildConfig.DEBUG
                SwitchPreferenceItem(
                    title = "Enable Shizuku Integration",
                    subtitle = "Required for certain Android 11 devices",
                    checked = uiState.enableShizukuIntegration,
                    onCheckedChange = { newValue ->
                        if (switchEnabled) {
                            if (newValue && !uiState.enableShizukuIntegration) {
                                showShizukuDialog = true
                            } else {
                                viewModel.updateEnableShizukuIntegration(newValue)
                            }
                        }
                    },
                    enabled = switchEnabled
                )
            }

            if (showShizukuDialog) {
                AlertDialog(
                    onDismissRequest = { showShizukuDialog = false },
                    title = { Text("Enable Shizuku Integration") },
                    text = {
                        Text("Only enable this if gestures do not work. Shizuku will be used to dispatch gestures. After enabling this setting, use the banner on the main page to verify Shizuku authorization.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.updateEnableShizukuIntegration(true)
                                showShizukuDialog = false
                            }
                        ) {
                            Text("Enable")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showShizukuDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            PreferenceCategory(title = "Logging") {
                NavigationItem(
                    title = "Log Screen",
                    subtitle = "View application logs",
                    onClick = onNavigateToLogScreen,
                )
            }

            PreferenceCategory(title = "Experimental") {
                SwitchPreferenceItem(
                    title = "Allow Passthrough",
                    subtitle = "Disable key press interception",
                    checked = uiState.allowPassthrough,
                    onCheckedChange = { newValue ->
                        if (newValue && !uiState.allowPassthrough) {
                            showExperimentalDialog = true
                        } else {
                            viewModel.updateAllowPassthrough(newValue)
                        }
                    },
                )
            }

            if (showExperimentalDialog) {
                AlertDialog(
                    onDismissRequest = { showExperimentalDialog = false },
                    title = { Text("Allow Passthrough") },
                    text = {
                        Text("All button presses will be forwarded to the underlying app. This may fix numpad backlight issues but cause unintended behavior with the underlying application.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.updateAllowPassthrough(true)
                                showExperimentalDialog = false
                            }
                        ) {
                            Text("Enable")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showExperimentalDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}