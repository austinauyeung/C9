package com.austinauyeung.nyuma.c9.settings.ui.autohide

import androidx.compose.runtime.Composable
import com.austinauyeung.nyuma.c9.settings.ui.AppListScreen
import com.austinauyeung.nyuma.c9.settings.ui.SettingsState
import com.austinauyeung.nyuma.c9.settings.ui.SettingsUiState

@Composable
fun AutoHideAppsScreen(
    settingsState: SettingsState,
    onNavigateBack: () -> Unit
) {
    AppListScreen(settingsState, {it: SettingsUiState -> it.autoHideApps}, {settings, v -> settings.copy(autoHideApps = v)}, onNavigateBack)
}