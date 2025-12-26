package com.austinauyeung.nyuma.c9.settings.ui

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.Color
import com.austinauyeung.nyuma.c9.core.util.VersionUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugOptionsScreen(
    settingsState: SettingsState,
    onNavigateBack: () -> Unit
) {
    val uiState by settingsState.uiState.collectAsState()
    var showShizukuDialog by remember { mutableStateOf(false) }
    var showExperimentalDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer Options") },
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
//            PreferenceCategory(title = "Logging") {
//                SwitchPreferenceItem(
//                    title = "Collect Logs",
//                    subtitle = "Logs will be written to the log screen",
//                    checked = uiState.collectLogs,
//                    onCheckedChange = { value ->
//                        settingsState.updatePreference(value) { settings, v ->
//                            settings.copy(collectLogs = v)
//                        }
//                    }
//                )
//                SimplePreferenceItem(
//                    title = "Log Screen",
//                    subtitle = "View application logs",
//                    onClick = onNavigateToLogScreen,
//                )
//            }

            PreferenceCategory(title = "Shizuku") {
                SwitchPreferenceItem(
                    title = "Enable Shizuku Integration",
                    subtitle = "Required for certain Android devices",
                    checked = uiState.enableShizukuIntegration,
                    onCheckedChange = { newValue ->
                        if (newValue && !uiState.enableShizukuIntegration) {
                            showShizukuDialog = true
                        } else {
                            settingsState.updateEnableShizukuIntegration(newValue)
                        }
                    },
                    enabled = !VersionUtil.belowVersion(Build.VERSION_CODES.O)
                )

                SwitchPreferenceItem(
                    title = "Android 7 Override",
                    subtitle = "Removes Shizuku requirement but restricts gestures to clicks only",
                    checked = uiState.overrideAndroid7,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(overrideAndroid7 = v, enableShizukuIntegration = !v)
                        }
                    },
                    enabled = VersionUtil.belowVersion(Build.VERSION_CODES.O)
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
                                settingsState.updateEnableShizukuIntegration(true)
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

            PreferenceCategory(title = "Gestures") {
                SwitchPreferenceItem(
                    title = "Overlapping Gestures",
                    subtitle = "Allow manual scrolls and zooms to overlap",
                    checked = uiState.allowOverlappingGestures,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(allowOverlappingGestures = v)
                        }
                    },
                )
                SwitchPreferenceItem(
                    title = "Improve Non-Shizuku Gestures",
                    subtitle = "Currently recommended only for Android versions 8-10 as needed",
                    checked = uiState.forceSmootherGestures,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(forceSmootherGestures = v)
                        }
                    },
                )

                if (uiState.forceSmootherGestures && Build.VERSION.SDK_INT !in Build.VERSION_CODES.O..Build.VERSION_CODES.Q) {
                    NoteItem(
                        title = "Not recommended for this device",
                        icon = Icons.Default.Warning,
                        contentDescription = "Warning",
                        color = Color(0xFFFFF4E6),
                    )
                }
            }

            PreferenceCategory(title = "Display") {
                SwitchPreferenceItem(
                    title = "Use Physical Size",
                    subtitle = "Overlay cursor over the entire screen, including any navigation bars",
                    checked = uiState.usePhysicalSize,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(usePhysicalSize = v)
                        }
                    },
                )
            }

            PreferenceCategory(title = "Experimental") {
                SwitchPreferenceItem(
                    title = "Disable Touchscreen",
                    subtitle = "Currently disables touchscreen only between button presses",
                    checked = uiState.disableTouchscreen,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(disableTouchscreen = v)
                        }
                    },
                )

                SwitchPreferenceItem(
                    title = "Ignore Numpad",
                    subtitle = "Bypass all numpad behavior except for activation",
                    checked = uiState.ignoreNumpad,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(ignoreNumpad = v)
                        }
                    },
                )

                SwitchPreferenceItem(
                    title = "Allow Passthrough",
                    subtitle = "Disable key press interception",
                    checked = uiState.allowPassthrough,
                    onCheckedChange = { newValue ->
                        if (newValue && !uiState.allowPassthrough) {
                            showExperimentalDialog = true
                        } else {
                            settingsState.updateAllowPassthrough(newValue)
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
                                settingsState.updateAllowPassthrough(true)
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