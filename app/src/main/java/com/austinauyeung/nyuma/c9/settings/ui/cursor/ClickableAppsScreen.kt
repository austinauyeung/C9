package com.austinauyeung.nyuma.c9.settings.ui.cursor

import androidx.compose.runtime.Composable
import com.austinauyeung.nyuma.c9.settings.ui.AppListScreen
import com.austinauyeung.nyuma.c9.settings.ui.SettingsState
import com.austinauyeung.nyuma.c9.settings.ui.SettingsUiState

@Composable
fun ClickableAppsScreen(
    settingsState: SettingsState,
    onNavigateBack: () -> Unit
) {
    AppListScreen(settingsState, {it: SettingsUiState -> it.clickableApps}, {settings, v -> settings.copy(clickableApps = v)}, onNavigateBack)
}