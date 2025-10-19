package com.austinauyeung.nyuma.c9.settings.ui.grid

import KeyCaptureOverlay
import android.view.KeyEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.austinauyeung.nyuma.c9.R
import com.austinauyeung.nyuma.c9.core.constants.GridConstants
import com.austinauyeung.nyuma.c9.grid.domain.GridLineVisibility
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings
import com.austinauyeung.nyuma.c9.settings.ui.ClearKeyPreferenceItem
import com.austinauyeung.nyuma.c9.settings.ui.ColorPickerDialog
import com.austinauyeung.nyuma.c9.settings.ui.DropdownPreferenceItem
import com.austinauyeung.nyuma.c9.settings.ui.NoteItem
import com.austinauyeung.nyuma.c9.settings.ui.PreferenceCategory
import com.austinauyeung.nyuma.c9.settings.ui.SetKeyPreferenceItem
import com.austinauyeung.nyuma.c9.settings.ui.SettingsState
import com.austinauyeung.nyuma.c9.settings.ui.SimplePreferenceItem
import com.austinauyeung.nyuma.c9.settings.ui.SliderPreferenceItem
import com.austinauyeung.nyuma.c9.settings.ui.SwitchPreferenceItem

/**
 * Grid cursor settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridSettingsScreen(
    settingsState: SettingsState,
    onNavigateBack: () -> Unit,
) {
    val uiState by settingsState.uiState.collectAsState()
    var showGridKeyCaptureOverlay by remember { mutableStateOf(false) }
    val reservedKeys =
        mapOf(
            KeyEvent.KEYCODE_1 to "Click cell 1",
            KeyEvent.KEYCODE_2 to "Click cell 2",
            KeyEvent.KEYCODE_3 to "Click cell 3",
            KeyEvent.KEYCODE_4 to "Click cell 4",
            KeyEvent.KEYCODE_5 to "Click cell 5",
            KeyEvent.KEYCODE_6 to "Click cell 6",
            KeyEvent.KEYCODE_7 to "Click cell 7",
            KeyEvent.KEYCODE_8 to "Click cell 8",
            KeyEvent.KEYCODE_9 to "Click cell 9",
            KeyEvent.KEYCODE_STAR to "Zoom out",
            KeyEvent.KEYCODE_0 to "Zoom in",
            KeyEvent.KEYCODE_POUND to "",
            KeyEvent.KEYCODE_DPAD_UP to "Scroll up",
            KeyEvent.KEYCODE_DPAD_DOWN to "Scroll down",
            KeyEvent.KEYCODE_DPAD_LEFT to "Scroll left",
            KeyEvent.KEYCODE_DPAD_RIGHT to "Scroll right",
            KeyEvent.KEYCODE_DPAD_CENTER to "Double tap",
        )

    var showBackgroundColorPickerDialog by remember { mutableStateOf(false) }
    var showLinesColorPickerDialog by remember { mutableStateOf(false) }
    var showNumbersColorPickerDialog by remember { mutableStateOf(false) }

    val currentKeyDescription =
        if (
            uiState.gridActivationKey != OverlaySettings.KEY_NONE &&
            !reservedKeys[uiState.gridActivationKey].isNullOrEmpty()
        ) {
            reservedKeys[uiState.gridActivationKey]
        } else {
            null
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grid Cursor") },
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
            PreferenceCategory(title = "Activation") {
                if (currentKeyDescription != null) {
                    NoteItem(
                        title = "\"$currentKeyDescription\" overridden and disabled",
                        icon = Icons.Default.Warning,
                        contentDescription = "Warning",
                        color = Color(0xFFFFF4E6),
                    )
                }

                SetKeyPreferenceItem(
                    title = "Set Activation Key",
                    currentKeyCode = uiState.gridActivationKey,
                    onCaptureKey = {
                        settingsState.requestHideAllOverlays()
                        showGridKeyCaptureOverlay = true
                    },
                )

                ClearKeyPreferenceItem(
                    mode = "grid cursor",
                    onClearKey = {
                        settingsState.requestHideAllOverlays()
                        settingsState.updateGridActivationKey(OverlaySettings.KEY_NONE)
                    },
                )

                if (showGridKeyCaptureOverlay) {
                    KeyCaptureOverlay(
                        restrictedKeys = setOf(uiState.cursorActivationKey),
                        reservedKeys = reservedKeys,
                        onKeySelected = { settingsState.updateGridActivationKey(it) },
                        onDismiss = { showGridKeyCaptureOverlay = false },
                        showToast = { message -> settingsState.showToast(message) },
                    )
                }
            }

            PreferenceCategory(title = "Behavior") {
                SliderPreferenceItem(
                    title = "Grid Levels",
                    value = uiState.gridLevels.toFloat(),
                    valueRange = GridConstants.MIN_LEVELS.toFloat()..GridConstants.MAX_LEVELS.toFloat(),
                    steps = 1,
                    valueText =
                    when (uiState.gridLevels) {
                        2 -> stringResource(R.string.settings_grid_levels_2)
                        3 -> stringResource(R.string.settings_grid_levels_3)
                        else -> stringResource(R.string.settings_grid_levels_4)
                    },
                    onValueChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(gridLevels = v.toInt())
                        }
                    },
                )
                SwitchPreferenceItem(
                    title = "Persistent Overlay",
                    subtitle = "Keep overlay visible after final selection",
                    checked = uiState.persistOverlay,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(persistOverlay = v)
                        }
                    },
                )
            }

            PreferenceCategory(title = "Appearance") {
                SimplePreferenceItem(
                    title = "Background Color",
                    subtitle = "Current ARGB hex value: #${uiState.gridCursorBackgroundHex}",
                    onClick = { showBackgroundColorPickerDialog = true }
                )

                if (showBackgroundColorPickerDialog) {
                    ColorPickerDialog(
                        initialColorHex = uiState.gridCursorBackgroundHex,
                        onColorSelected = { newColorHex ->
                            settingsState.updatePreference(newColorHex) { settings, v ->
                                settings.copy(gridCursorBackgroundHex = v)
                            }
                        },
                        onDismiss = { showBackgroundColorPickerDialog = false },
                        title = "Background Color"
                    )
                }

                SwitchPreferenceItem(
                    title = "Keep Current Grid Transparent",
                    subtitle = "Exclude current grid from background color",
                    checked = uiState.keepCurrentGridTransparent,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(keepCurrentGridTransparent = v)
                        }
                    },
                )

                SimplePreferenceItem(
                    title = "Lines Color",
                    subtitle = "Current ARGB hex value: #${uiState.gridCursorLinesHex}",
                    onClick = { showLinesColorPickerDialog = true }
                )

                if (showLinesColorPickerDialog) {
                    ColorPickerDialog(
                        initialColorHex = uiState.gridCursorLinesHex,
                        onColorSelected = { newColorHex ->
                            settingsState.updatePreference(newColorHex) { settings, v ->
                                settings.copy(gridCursorLinesHex = v)
                            }
                        },
                        onDismiss = { showLinesColorPickerDialog = false },
                        title = "Lines Color"
                    )
                }

                SliderPreferenceItem(
                    title = "Line Width",
                    value = uiState.gridCursorLineWidth.toFloat(),
                    valueRange = GridConstants.GRID_LINE_MIN_WIDTH.toFloat()..GridConstants.GRID_LINE_MAX_WIDTH.toFloat(),
                    steps = 3,
                    valueText = "${uiState.gridCursorLineWidth}",
                    onValueChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(gridCursorLineWidth = v.toInt())
                        }
                    },
                )

                DropdownPreferenceItem(
                    title = "Grid Lines",
                    subtitle = when (uiState.gridLineVisibility) {
                        GridLineVisibility.SHOW_ALL -> "Show all grid lines"
                        GridLineVisibility.FINAL_LEVEL_ONLY -> "Show grid lines in final subgrid only"
                        GridLineVisibility.HIDE_ALL -> "Hide all grid lines"
                    },
                    selectedOption = uiState.gridLineVisibility,
                    options = listOf(
                        GridLineVisibility.SHOW_ALL to "Show",
                        GridLineVisibility.FINAL_LEVEL_ONLY to "Final",
                        GridLineVisibility.HIDE_ALL to "Hide"
                    ),
                    onOptionSelected = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(gridLineVisibility = v)
                        }
                    },
                )

                SimplePreferenceItem(
                    title = "Numbers Color",
                    subtitle = "Current ARGB hex value: #${uiState.gridCursorNumbersHex}",
                    onClick = { showNumbersColorPickerDialog = true }
                )

                if (showNumbersColorPickerDialog) {
                    ColorPickerDialog(
                        initialColorHex = uiState.gridCursorNumbersHex,
                        onColorSelected = { newColorHex ->
                            settingsState.updatePreference(newColorHex) { settings, v ->
                                settings.copy(gridCursorNumbersHex = v)
                            }
                        },
                        onDismiss = { showNumbersColorPickerDialog = false },
                        title = "Numbers Color"
                    )
                }

                SwitchPreferenceItem(
                    title = "Hide Numbers",
                    subtitle = "Hide cell numbers in the grid",
                    checked = uiState.hideNumbers,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(hideNumbers = v)
                        }
                    },
                )

                SliderPreferenceItem(
                    title = "Numbers Font Size",
                    value = uiState.gridCursorFontSize.toFloat(),
                    valueRange = GridConstants.GRID_MIN_FONT_SIZE.toFloat()..GridConstants.GRID_MAX_FONT_SIZE.toFloat(),
                    steps = 5,
                    valueText = "${uiState.gridCursorFontSize}0%",
                    onValueChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(gridCursorFontSize = v.toInt())
                        }
                    },
                    enabled = !uiState.hideNumbers
                )
            }
        }
    }
}
