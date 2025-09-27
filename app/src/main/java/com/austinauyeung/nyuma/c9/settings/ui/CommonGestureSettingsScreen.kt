package com.austinauyeung.nyuma.c9.settings.ui

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
import androidx.compose.ui.Modifier
import com.austinauyeung.nyuma.c9.core.constants.GestureConstants
import com.austinauyeung.nyuma.c9.core.domain.GestureStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonGestureSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Common Gesture Options") },
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
            PreferenceCategory(title = "Behavior") {
                DropdownPreferenceItem(
                    title = "Gesture Style",
                    subtitle =
                    when (uiState.gestureStyle) {
                        GestureStyle.FIXED -> "Fixed distance, implementation 1"
                        GestureStyle.FIXED_2 -> "Fixed distance, implementation 2"
                        GestureStyle.INERTIA -> "Momentum-based"
                    },
                    selectedOption = uiState.gestureStyle,
                    options = if (!uiState.enableShizukuIntegration) listOf(
                        GestureStyle.FIXED to "Fixed 1",
                        GestureStyle.FIXED_2 to "Fixed 2",
                        GestureStyle.INERTIA to "Inertia",
                    ) else listOf(
                        GestureStyle.FIXED to "Fixed 1",
                        GestureStyle.INERTIA to "Inertia",
                    ),
                    onOptionSelected = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(gestureStyle = v)
                        }
                    },
                )
            }

            PreferenceCategory(title = "Visualization") {
                SwitchPreferenceItem(
                    title = "Gesture Visualization",
                    subtitle = "Show gestures on screen",
                    checked = uiState.showGestureVisualization,
                    onCheckedChange = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(showGestureVisualization = v)
                        }
                    },
                )

                SliderPreferenceItem(
                    title = "Gesture Visualization Size",
                    value = uiState.visualSize.toFloat(),
                    valueRange = GestureConstants.MIN_SIZE.toFloat()..GestureConstants.MAX_SIZE.toFloat(),
                    valueText = uiState.visualSize.toString(),
                    onValueChange = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(visualSize = v.toInt())
                        }
                    },
                    steps = 8,
                    enabled = uiState.showGestureVisualization
                )
            }
        }
    }
}