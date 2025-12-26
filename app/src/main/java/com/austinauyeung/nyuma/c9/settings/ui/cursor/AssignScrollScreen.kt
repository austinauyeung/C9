package com.austinauyeung.nyuma.c9.settings.ui.cursor

import KeyCaptureOverlay
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings
import com.austinauyeung.nyuma.c9.settings.ui.ClearKeyPreferenceItem
import com.austinauyeung.nyuma.c9.settings.ui.PreferenceCategory
import com.austinauyeung.nyuma.c9.settings.ui.SetKeyPreferenceItem
import com.austinauyeung.nyuma.c9.settings.ui.SettingsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignScrollScreen(
    settingsState: SettingsState,
    onNavigateBack: () -> Unit
) {
    val uiState by settingsState.uiState.collectAsState()
    var showScrollUpCaptureOverlay by remember { mutableStateOf(false) }
    var showScrollDownCaptureOverlay by remember { mutableStateOf(false) }
    var showScrollLeftCaptureOverlay by remember { mutableStateOf(false) }
    var showScrollRightCaptureOverlay by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assign Scroll Buttons") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
        ) {
            PreferenceCategory(title = "Reset") {
                ClearKeyPreferenceItem(
                    mode = "scroll buttons",
                    onClearKey = {
                        settingsState.updateScrollUpKey(OverlaySettings.KEY_NONE)
                        settingsState.updateScrollDownKey(OverlaySettings.KEY_NONE)
                        settingsState.updateScrollLeftKey(OverlaySettings.KEY_NONE)
                        settingsState.updateScrollRightKey(OverlaySettings.KEY_NONE)
                    },
                )
            }

            PreferenceCategory(title = "Assign") {
                SetKeyPreferenceItem(
                    title = "Set Scroll Up Key",
                    currentKeyCode = uiState.scrollUpKey,
                    onCaptureKey = {
                        settingsState.requestHideAllOverlays()
                        showScrollUpCaptureOverlay = true
                    },
                )

                if (showScrollUpCaptureOverlay) {
                    KeyCaptureOverlay(
                        restrictedKeys = setOf(
                            uiState.gridActivationKey,
                            uiState.cursorActivationKey,
                            uiState.scrollDownKey,
                            uiState.scrollLeftKey,
                            uiState.scrollRightKey
                        ),
                        reservedKeys = emptyMap(),
                        onKeySelected = { settingsState.updateScrollUpKey(it) },
                        onDismiss = { showScrollUpCaptureOverlay = false },
                        showToast = { message -> settingsState.showToast(message) },
                    )
                }

                SetKeyPreferenceItem(
                    title = "Set Scroll Down Key",
                    currentKeyCode = uiState.scrollDownKey,
                    onCaptureKey = {
                        settingsState.requestHideAllOverlays()
                        showScrollDownCaptureOverlay = true
                    },
                )

                if (showScrollDownCaptureOverlay) {
                    KeyCaptureOverlay(
                        restrictedKeys = setOf(
                            uiState.gridActivationKey,
                            uiState.cursorActivationKey,
                            uiState.scrollUpKey,
                            uiState.scrollLeftKey,
                            uiState.scrollRightKey
                        ),
                        reservedKeys = emptyMap(),
                        onKeySelected = { settingsState.updateScrollDownKey(it) },
                        onDismiss = { showScrollDownCaptureOverlay = false },
                        showToast = { message -> settingsState.showToast(message) },
                    )
                }

                SetKeyPreferenceItem(
                    title = "Set Scroll Left Key",
                    currentKeyCode = uiState.scrollLeftKey,
                    onCaptureKey = {
                        settingsState.requestHideAllOverlays()
                        showScrollLeftCaptureOverlay = true
                    },
                )

                if (showScrollLeftCaptureOverlay) {
                    KeyCaptureOverlay(
                        restrictedKeys = setOf(
                            uiState.gridActivationKey,
                            uiState.cursorActivationKey,
                            uiState.scrollUpKey,
                            uiState.scrollDownKey,
                            uiState.scrollRightKey
                        ),
                        reservedKeys = emptyMap(),
                        onKeySelected = { settingsState.updateScrollLeftKey(it) },
                        onDismiss = { showScrollLeftCaptureOverlay = false },
                        showToast = { message -> settingsState.showToast(message) },
                    )
                }

                SetKeyPreferenceItem(
                    title = "Set Scroll Right Key",
                    currentKeyCode = uiState.scrollRightKey,
                    onCaptureKey = {
                        settingsState.requestHideAllOverlays()
                        showScrollRightCaptureOverlay = true
                    },
                )

                if (showScrollRightCaptureOverlay) {
                    KeyCaptureOverlay(
                        restrictedKeys = setOf(
                            uiState.gridActivationKey,
                            uiState.cursorActivationKey,
                            uiState.scrollUpKey,
                            uiState.scrollDownKey,
                            uiState.scrollLeftKey
                        ),
                        reservedKeys = emptyMap(),
                        onKeySelected = { settingsState.updateScrollRightKey(it) },
                        onDismiss = { showScrollRightCaptureOverlay = false },
                        showToast = { message -> settingsState.showToast(message) },
                    )
                }
            }
        }
    }
}