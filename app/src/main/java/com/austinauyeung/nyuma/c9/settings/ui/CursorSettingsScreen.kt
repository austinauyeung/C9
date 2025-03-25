package com.austinauyeung.nyuma.c9.settings.ui

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
import com.austinauyeung.nyuma.c9.core.constants.CursorConstants
import com.austinauyeung.nyuma.c9.settings.domain.ControlScheme
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings

/**
 * Standard cursor settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CursorSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCursorKeyCaptureOverlay by remember { mutableStateOf(false) }
    var reservedKeys by remember { mutableStateOf(emptyMap<Int, String>()) }

    when (uiState.controlScheme) {
        ControlScheme.STANDARD -> {
            reservedKeys =
                mapOf(
                    KeyEvent.KEYCODE_1 to "Zoom out",
                    KeyEvent.KEYCODE_2 to "Scroll up",
                    KeyEvent.KEYCODE_3 to "Zoom in",
                    KeyEvent.KEYCODE_4 to "Scroll left",
                    KeyEvent.KEYCODE_5 to "",
                    KeyEvent.KEYCODE_6 to "Scroll right",
                    KeyEvent.KEYCODE_7 to "",
                    KeyEvent.KEYCODE_8 to "Scroll down",
                    KeyEvent.KEYCODE_9 to "",
                    KeyEvent.KEYCODE_STAR to "",
                    KeyEvent.KEYCODE_0 to "",
                    KeyEvent.KEYCODE_POUND to "",
                    KeyEvent.KEYCODE_DPAD_UP to "Cursor up",
                    KeyEvent.KEYCODE_DPAD_DOWN to "Cursor down",
                    KeyEvent.KEYCODE_DPAD_LEFT to "Cursor left",
                    KeyEvent.KEYCODE_DPAD_RIGHT to "Cursor right",
                    KeyEvent.KEYCODE_DPAD_CENTER to "Cursor select and double tap",
                )
        }

        ControlScheme.SWAPPED -> {
            reservedKeys =
                mapOf(
                    KeyEvent.KEYCODE_1 to "Zoom out",
                    KeyEvent.KEYCODE_2 to "Cursor up",
                    KeyEvent.KEYCODE_3 to "Zoom in",
                    KeyEvent.KEYCODE_4 to "Cursor left",
                    KeyEvent.KEYCODE_5 to "Cursor select and double tap",
                    KeyEvent.KEYCODE_6 to "Cursor right",
                    KeyEvent.KEYCODE_7 to "",
                    KeyEvent.KEYCODE_8 to "Cursor down",
                    KeyEvent.KEYCODE_9 to "",
                    KeyEvent.KEYCODE_STAR to "",
                    KeyEvent.KEYCODE_0 to "",
                    KeyEvent.KEYCODE_POUND to "",
                    KeyEvent.KEYCODE_DPAD_UP to "Scroll up",
                    KeyEvent.KEYCODE_DPAD_DOWN to "Scroll down",
                    KeyEvent.KEYCODE_DPAD_LEFT to "Scroll left",
                    KeyEvent.KEYCODE_DPAD_RIGHT to "Scroll right",
                    KeyEvent.KEYCODE_DPAD_CENTER to "",
                )
        }

        else -> {
            reservedKeys =
                mapOf(
                    KeyEvent.KEYCODE_1 to "",
                    KeyEvent.KEYCODE_2 to "",
                    KeyEvent.KEYCODE_3 to "",
                    KeyEvent.KEYCODE_4 to "",
                    KeyEvent.KEYCODE_5 to "",
                    KeyEvent.KEYCODE_6 to "",
                    KeyEvent.KEYCODE_7 to "",
                    KeyEvent.KEYCODE_8 to "",
                    KeyEvent.KEYCODE_9 to "",
                    KeyEvent.KEYCODE_STAR to "",
                    KeyEvent.KEYCODE_0 to "",
                    KeyEvent.KEYCODE_POUND to "",
                    KeyEvent.KEYCODE_DPAD_UP to "Cursor up and scroll up",
                    KeyEvent.KEYCODE_DPAD_DOWN to "Cursor down and scroll down",
                    KeyEvent.KEYCODE_DPAD_LEFT to "Cursor left and scroll left",
                    KeyEvent.KEYCODE_DPAD_RIGHT to "Cursor right and scroll right",
                    KeyEvent.KEYCODE_DPAD_CENTER to "Cursor select and double tap",
                )
        }
    }

    val currentKeyDescription =
        if (
            uiState.cursorActivationKey != OverlaySettings.KEY_NONE &&
            !reservedKeys[uiState.cursorActivationKey].isNullOrEmpty()
        ) {
            reservedKeys[uiState.cursorActivationKey]
        } else {
            null
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Standard Cursor") },
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
                    currentKeyCode = uiState.cursorActivationKey,
                    onCaptureKey = {
                        viewModel.requestHideAllOverlays()
                        showCursorKeyCaptureOverlay = true
                    },
                )

                ClearKeyPreferenceItem(
                    isEnabled = uiState.cursorActivationKey != OverlaySettings.KEY_NONE,
                    mode = "standard cursor",
                    onClearKey = {
                        viewModel.requestHideAllOverlays()
                        viewModel.updateCursorActivationKey(OverlaySettings.KEY_NONE)
                    },
                )

                if (showCursorKeyCaptureOverlay) {
                    KeyCaptureOverlay(
                        restrictedKeys = setOf(uiState.gridActivationKey),
                        reservedKeys = reservedKeys,
                        onKeySelected = { viewModel.updateCursorActivationKey(it) },
                        onDismiss = { showCursorKeyCaptureOverlay = false },
                        showToast = { message -> viewModel.showToast(message) },
                    )
                }
            }

            PreferenceCategory(title = "Behavior") {
                DropdownPreferenceItem(
                    title = "Control Scheme",
                    subtitle =
                    when (uiState.controlScheme) {
                        ControlScheme.STANDARD -> "D-pad moves, numpad scrolls"
                        ControlScheme.SWAPPED -> "D-pad scrolls, numpad moves"
                        else -> "D-pad scrolls and moves"
                    },
                    selectedOption = uiState.controlScheme,
                    options =
                    listOf(
                        ControlScheme.STANDARD to "Standard",
                        ControlScheme.SWAPPED to "Swapped",
                        ControlScheme.TOGGLE_MODE to "Toggle",
                    ),
                    onOptionSelected = { viewModel.updateControlScheme(it) },
                )

                SwitchPreferenceItem(
                    title = "Cursor Wrap Around",
                    subtitle = "Allow cursor to wrap around edges of the screen",
                    checked = uiState.cursorWrapAround,
                    onCheckedChange = { viewModel.updateCursorWrapAround(it) },
                )

//                SwitchPreferenceItem(
//                    title = "Long Press Hold",
//                    subtitle = "Press both action keys to toggle hold",
//                    checked = uiState.toggleHold,
//                    onCheckedChange = { viewModel.updateToggleHold(it) },
//                )
            }

            PreferenceCategory(title = "Appearance") {
                SliderPreferenceItem(
                    title = "Cursor Speed",
                    value = uiState.cursorSpeed.toFloat(),
                    valueRange = CursorConstants.MIN_SPEED.toFloat()..CursorConstants.MAX_SPEED.toFloat(),
                    valueText =
                    when (uiState.cursorSpeed) {
                        1 -> "Slowest"
                        2 -> "Slow"
                        3 -> "Medium"
                        4 -> "Fast"
                        else -> "Fastest"
                    },
                    onValueChange = { viewModel.updateCursorSpeed(it.toInt()) },
                    steps = 3,
                )

                SliderPreferenceItem(
                    title = "Cursor Acceleration",
                    value = uiState.cursorAcceleration.toFloat(),
                    valueRange = CursorConstants.MIN_ACCELERATION.toFloat()..CursorConstants.MAX_ACCELERATION.toFloat(),
                    valueText =
                    when (uiState.cursorAcceleration) {
                        1 -> "None"
                        2 -> "Light"
                        3 -> "Medium"
                        4 -> "Strong"
                        else -> "Maximum"
                    },
                    onValueChange = { viewModel.updateCursorAcceleration(it.toInt()) },
                    steps = 3,
                )

                SliderPreferenceItem(
                    title = "Cursor Acceleration Threshold",
                    value = uiState.cursorAccelerationThreshold.toFloat(),
                    valueRange = CursorConstants.MIN_ACCELERATION_THRESHOLD.toFloat()..CursorConstants.MAX_ACCELERATION_THRESHOLD.toFloat(),
                    valueText =
                    when (uiState.cursorAccelerationThreshold) {
                        100L -> "Fastest"
                        200L -> "Fast"
                        300L -> "Medium"
                        400L -> "Slow"
                        500L -> "Slowest"
                        else -> ""
                    },
                    onValueChange = { viewModel.updateCursorAccelerationThreshold(it.toLong()) },
                    steps = 3,
                )

                SliderPreferenceItem(
                    title = "Cursor Size",
                    value = uiState.cursorSize.toFloat(),
                    valueRange = CursorConstants.MIN_SIZE.toFloat()..CursorConstants.MAX_SIZE.toFloat(),
                    valueText =
                    when (uiState.cursorSize) {
                        1 -> "Smallest"
                        2 -> "Small"
                        3 -> "Medium"
                        4 -> "Large"
                        5 -> "Largest"
                        else -> ""
                    },
                    onValueChange = { viewModel.updateCursorSize(it.toInt()) },
                    steps = 3,
                )
            }
        }
    }
}
