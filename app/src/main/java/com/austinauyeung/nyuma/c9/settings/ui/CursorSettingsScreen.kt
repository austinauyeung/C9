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
import com.austinauyeung.nyuma.c9.common.domain.ScreenEdgeBehavior
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
                    isEnabled = true,
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
                        ControlScheme.DPAD_TOGGLE -> "D-pad scrolls and moves"
                        else -> "Numpad scrolls and moves"
                    },
                    selectedOption = uiState.controlScheme,
                    options =
                    listOf(
                        ControlScheme.STANDARD to "Standard",
                        ControlScheme.SWAPPED to "Swapped",
                        ControlScheme.DPAD_TOGGLE to "D-pad",
                        ControlScheme.NUMPAD_TOGGLE to "Numpad"
                    ),
                    onOptionSelected = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(controlScheme = v)
                        }
                    },
                )

                DropdownPreferenceItem(
                    title = "Screen Edge Behavior",
                    subtitle =
                    when (uiState.cursorEdgeBehavior) {
                        ScreenEdgeBehavior.NONE -> "Cursor remains at edge"
                        ScreenEdgeBehavior.WRAP_AROUND -> "Cursor wraps to opposite side"
                        ScreenEdgeBehavior.AUTO_SCROLL -> "Cursor slowly scrolls in edge direction"
                    },
                    selectedOption = uiState.cursorEdgeBehavior,
                    options =
                    listOf(
                        ScreenEdgeBehavior.NONE to "None",
                        ScreenEdgeBehavior.WRAP_AROUND to "Wrap",
                        ScreenEdgeBehavior.AUTO_SCROLL to "Scroll"
                    ),
                    onOptionSelected = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(cursorEdgeBehavior = v)
                        }
                    },
                )

                SliderPreferenceItem(
                    title = "Cursor Speed",
                    value = uiState.cursorSpeed.toFloat(),
                    valueRange = CursorConstants.MIN_SPEED.toFloat()..CursorConstants.MAX_SPEED.toFloat(),
                    valueText = uiState.cursorSpeed.toString(),
                    onValueChange = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(cursorSpeed = v.toInt())
                        }
                    },
                    steps = 8,
                )

                SliderPreferenceItem(
                    title = "Cursor Acceleration",
                    value = uiState.cursorAcceleration.toFloat(),
                    valueRange = CursorConstants.MIN_ACCELERATION.toFloat()..CursorConstants.MAX_ACCELERATION.toFloat(),
                    valueText = "${uiState.cursorAcceleration}${if (uiState.cursorAcceleration == 1) " (no acceleration)" else ""}",
                    onValueChange = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(cursorAcceleration = v.toInt())
                        }
                    },
                    steps = 8,
                )

                SliderPreferenceItem(
                    title = "Cursor Acceleration Threshold",
                    value = uiState.cursorAccelerationThreshold.toFloat(),
                    valueRange = CursorConstants.MIN_ACCELERATION_THRESHOLD.toFloat()..CursorConstants.MAX_ACCELERATION_THRESHOLD.toFloat(),
                    valueText = "${uiState.cursorAccelerationThreshold} ms",
                    onValueChange = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(cursorAccelerationThreshold = v.toLong())
                        }
                    },
                    steps = 3,
                )

//                SwitchPreferenceItem(
//                    title = "Long Press Hold",
//                    subtitle = "Press both action keys to toggle hold",
//                    checked = uiState.toggleHold,
//                    onCheckedChange = { value ->
//                        viewModel.updatePreference(value) { settings, v ->
//                            settings.copy(toggleHold = v)
//                        }
//                    },
//                )
            }

            PreferenceCategory(title = "Appearance") {
                SliderPreferenceItem(
                    title = "Cursor Size",
                    value = uiState.cursorSize.toFloat(),
                    valueRange = CursorConstants.MIN_SIZE.toFloat()..CursorConstants.MAX_SIZE.toFloat(),
                    valueText = uiState.cursorSize.toString(),
                    onValueChange = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(cursorSize = v.toInt())
                        }
                    },
                    steps = 8,
                )

                SwitchPreferenceItem(
                    title = "Smooth Cursor Corners",
                    subtitle = "Round out the corners of the cursor",
                    checked = uiState.roundedCursorCorners,
                    onCheckedChange = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(roundedCursorCorners = v)
                        }
                    },
                )
            }
        }
    }
}
