package com.austinauyeung.nyuma.c9.settings.ui

import android.content.Intent
import android.provider.Settings
import android.view.KeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.austinauyeung.nyuma.c9.BuildConfig
import com.austinauyeung.nyuma.c9.R
import com.austinauyeung.nyuma.c9.common.domain.GestureStyle
import com.austinauyeung.nyuma.c9.core.constants.GestureConstants
import com.austinauyeung.nyuma.c9.core.service.ShizukuServiceConnection
import com.austinauyeung.nyuma.c9.core.service.ShizukuStatus
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings
import kotlin.math.round

/**
 * Main settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToGridSettings: () -> Unit,
    onNavigateToCursorSettings: () -> Unit,
    onNavigateToDebugOptions: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
            if (BuildConfig.DEBUG) {
                NoteItem("Pre-Release Version", Icons.Default.Info, "Information")
            }

            PermissionStatusBanner(
                title = "Accessibility Service",
                status = uiState.isAccessibilityServiceEnabled,
                onClickAction = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
            )

            if (uiState.enableShizukuIntegration) {
                val shizukuStatus = ShizukuServiceConnection.statusFlow.collectAsState().value
                PermissionStatusBanner(
                    title = "Shizuku Service",
                    status = shizukuStatus == ShizukuStatus.READY,
                    onClickAction = {
                        when (shizukuStatus) {
                            ShizukuStatus.PERMISSION_REQUIRED -> ShizukuServiceConnection.requestPermission()
                            else -> {}
                        }
                    }
                )
            }

            PreferenceCategory(title = "Input Modes") {
                NavigationItem(
                    title = "Grid Cursor",
                    subtitle =
                    if (uiState.gridActivationKey == OverlaySettings.KEY_NONE) {
                        "Disabled"
                    } else {
                        "Enabled"
                    },
                    onClick = onNavigateToGridSettings,
                )

                NavigationItem(
                    title = "Standard Cursor",
                    subtitle =
                    if (uiState.cursorActivationKey == OverlaySettings.KEY_NONE) {
                        "Disabled"
                    } else {
                        "Enabled"
                    },
                    onClick = onNavigateToCursorSettings,
                )
            }

            PreferenceCategory(title = "Gestures") {
                SwitchPreferenceItem(
                    title = "Natural Scrolling",
                    subtitle = "Use content-based scrolling instead of standard scrolling",
                    checked = uiState.useNaturalScrolling,
                    onCheckedChange = { viewModel.updateNaturalScrolling(it) },
                )

                SwitchPreferenceItem(
                    title = "Gesture Visualization",
                    subtitle = "Show gestures on screen",
                    checked = uiState.showGestureVisualization,
                    onCheckedChange = { viewModel.updateGestureVisualization(it) },
                )

                DropdownPreferenceItem(
                    title = "Gesture Style",
                    subtitle =
                    when (uiState.gestureStyle) {
                        GestureStyle.FIXED -> "Fixed distance"
                        GestureStyle.INERTIA -> "Momentum-based"
                    },
                    selectedOption = uiState.gestureStyle,
                    options =
                    listOf(
                        GestureStyle.FIXED to "Fixed",
                        GestureStyle.INERTIA to "Inertia",
                    ),
                    onOptionSelected = { viewModel.updateGestureStyle(it) },
                )

                SliderPreferenceItem(
                    title = "Gesture Duration",
                    value = uiState.gestureDuration.toFloat(),
                    valueRange = GestureConstants.MIN_GESTURE_DURATION.toFloat()..GestureConstants.MAX_GESTURE_DURATION.toFloat(),
                    valueText =
                    when (uiState.gestureDuration) {
                        100L -> "Fastest"
                        200L -> "Fast"
                        300L -> "Medium"
                        400L -> "Slow"
                        500L -> "Slowest"
                        else -> ""
                    },
                    onValueChange = { viewModel.updateGestureDuration(it.toLong()) },
                    steps = 3,
                )

                SliderPreferenceItem(
                    title = "Scroll Distance",
                    value = uiState.scrollMultiplier,
                    valueRange = GestureConstants.MIN_SCROLL_MULTIPLIER..GestureConstants.MAX_SCROLL_MULTIPLIER,
                    valueText =
                    when (round(uiState.scrollMultiplier * 10) / 10) {
                        0.3f -> "Shortest"
                        0.4f -> "Short"
                        0.5f -> "Medium"
                        0.6f -> "Long"
                        0.7f -> "Longest"
                        else -> ""
                    },
                    onValueChange = { viewModel.updateScrollMultiplier(it) },
                    steps = 3,
                )
            }

            PreferenceCategory(title = "Behavior") {
                SwitchPreferenceItem(
                    title = "Auto-Hide in Text Fields",
                    subtitle = "Attempt to detect text fields and hide cursor",
                    checked = uiState.hideOnTextField,
                    onCheckedChange = { viewModel.updateHideOnTextField(it) },
                )
                SwitchPreferenceItem(
                    title = "Rotate Buttons With Orientation",
                    subtitle = "Rotate certain D-pad and numpad buttons with the screen",
                    checked = uiState.rotateButtonsWithOrientation,
                    onCheckedChange = { viewModel.updateRotateButtons(it) },
                )
            }

            PreferenceCategory(title = "Miscellaneous") {
                NavigationItem(
                    title = "Debug Options",
                    subtitle = "Logging and experimental features",
                    onClick = onNavigateToDebugOptions
                )
            }

            PreferenceCategory(title = "About") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                    ) {
                        Row(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(dimensionResource(R.dimen.padding_standard)),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Visit github.com/austinauyeung/C9 for instructions and updates.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(dimensionResource(R.dimen.padding_standard)),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Version: ${BuildConfig.VERSION_NAME}-${if (BuildConfig.DEBUG) "debug" else "release"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PreferenceCategory(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier =
            Modifier.padding(
                start = dimensionResource(R.dimen.padding_standard),
                top = dimensionResource(R.dimen.padding_standard),
                bottom = dimensionResource(R.dimen.padding_small),
            ),
        )
        content()
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_standard)))
    }
}

@Composable
private fun PreferenceItem(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_standard)),
        ) {
            Text(text = title)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun SliderPreferenceItem(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_standard)),
        ) {
            Text(text = title)
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
            )
        }
    }
}

@Composable
fun SwitchPreferenceItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_standard)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.padding(start = dimensionResource(R.dimen.padding_small)),
                enabled = enabled
            )
        }
    }
}

@Composable
fun <T> DropdownPreferenceItem(
    title: String,
    subtitle: String? = null,
    selectedOption: T,
    options: List<Pair<T, String>>,
    onOptionSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = options.find { it.first == selectedOption }?.second ?: "Select"

    Surface(
        onClick = { expanded = true },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_standard)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(
                        text = selectedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    options.forEach { (option, text) ->
                        DropdownMenuItem(
                            text = { Text(text) },
                            onClick = {
                                onOptionSelected(option)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusBanner(
    title: String,
    status: Boolean,
    onClickAction: () -> Unit,
    infoText: String? = null,
) {
    Column {
        Surface(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (status) Color(0xFFE6F3E6) else Color(0xFFFFF4E6),
            shape = RoundedCornerShape(8.dp),
            onClick = onClickAction,
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (status) Icons.Default.Check else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (status) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (status) "Permission Granted" else "Permission Required",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!status) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Configure",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (infoText != null) {
            Text(
                text = infoText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier =
                Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
fun SetKeyPreferenceItem(
    title: String,
    currentKeyCode: Int,
    onCaptureKey: () -> Unit,
) {
    val subtitle =
        if (currentKeyCode == OverlaySettings.KEY_NONE) {
            "Feature disabled (no key set)"
        } else {
            "Current: ${KeyEvent.keyCodeToString(currentKeyCode)}"
        }

    PreferenceItem(
        title = title,
        subtitle = subtitle,
        onClick = onCaptureKey,
    )
}

@Composable
fun ClearKeyPreferenceItem(
    title: String = "Clear Activation Key",
    mode: String,
    isEnabled: Boolean,
    onClearKey: () -> Unit,
) {
    PreferenceItem(
        title = title,
        subtitle = "Disables $mode mode",
        onClick = if (isEnabled) onClearKey else null,
    )
}

@Composable
fun NavigationItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_standard)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun NoteItem(
    title: String,
    icon: ImageVector,
    contentDescription: String,
    color: Color? = null,
) {
    Surface(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = color ?: Color.Transparent,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
            )
        }
    }
}
