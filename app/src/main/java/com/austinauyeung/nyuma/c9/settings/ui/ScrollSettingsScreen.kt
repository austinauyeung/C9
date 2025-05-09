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
import com.austinauyeung.nyuma.c9.common.domain.GestureStyle
import com.austinauyeung.nyuma.c9.common.domain.ScreenEdgeBehavior
import com.austinauyeung.nyuma.c9.core.constants.CursorConstants
import com.austinauyeung.nyuma.c9.core.constants.GestureConstants
import kotlin.math.max
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrollSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scroll Options") },
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
                SwitchPreferenceItem(
                    title = "Natural Scrolling",
                    subtitle = "Use content-based scrolling instead of standard scrolling",
                    checked = uiState.useNaturalScrolling,
                    onCheckedChange = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(useNaturalScrolling = v)
                        }
                    }
                )

                SliderPreferenceItem(
                    title = "Scroll Duration",
                    value = uiState.scrollDuration.toFloat(),
                    valueRange = GestureConstants.MIN_SCROLL_DURATION.toFloat()..GestureConstants.MAX_SCROLL_DURATION.toFloat(),
                    valueText = "${uiState.scrollDuration} ms",
                    onValueChange = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(scrollDuration = v.toLong())
                        }
                    },
                    steps = 3,
                )

                SliderPreferenceItem(
                    title = "Scroll Distance",
                    value = uiState.scrollMultiplier,
                    valueRange = GestureConstants.MIN_SCROLL_MULTIPLIER..GestureConstants.MAX_SCROLL_MULTIPLIER,
                    valueText = "${round(uiState.scrollMultiplier * 100).toInt()}% of axis at most",
                    onValueChange = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(scrollMultiplier = v)
                        }
                    },
                    steps = 8,
                )

                SwitchPreferenceItem(
                    title = "Advanced Scrolling",
                    subtitle = "Enable advanced scrolling settings",
                    checked = uiState.useAdvancedScrolling,
                    onCheckedChange = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(useAdvancedScrolling = v)
                        }
                    }
                )
            }

            if (uiState.useAdvancedScrolling) {
                PreferenceCategory(title = "Continuous Scrolling (Accelerated)") {
                    SliderPreferenceItem(
                        title = "Continuous Scroll Duration",
                        value = uiState.continuousScrollDuration.toFloat(),
                        valueRange = GestureConstants.MIN_SCROLL_DURATION.toFloat()..uiState.scrollDuration.toFloat(),
                        valueText = "${uiState.continuousScrollDuration} ms",
                        onValueChange = { value ->
                            viewModel.updatePreference(value) { settings, v ->
                                settings.copy(continuousScrollDuration = v.toLong())
                            }
                        },
                        steps = max(
                            round((uiState.scrollDuration.toFloat() - GestureConstants.MIN_SCROLL_DURATION.toFloat()) / 100).toInt() - 1,
                            1
                        )
                    )

                    SliderPreferenceItem(
                        title = "Continuous Scroll Distance",
                        value = uiState.continuousScrollMultiplier,
                        valueRange = uiState.scrollMultiplier..GestureConstants.MAX_SCROLL_MULTIPLIER,
                        valueText = "${round(uiState.continuousScrollMultiplier * 100).toInt()}% of axis at most",
                        onValueChange = { value ->
                            viewModel.updatePreference(value) { settings, v ->
                                settings.copy(continuousScrollMultiplier = v)
                            }
                        },
                        steps = max(
                            round((GestureConstants.MAX_SCROLL_MULTIPLIER.toFloat() - uiState.scrollMultiplier.toFloat()) / 0.1).toInt() - 1,
                            1
                        )
                    )

                    SliderPreferenceItem(
                        title = "Acceleration Start",
                        value = uiState.continuousScrollAccelerationStart.toFloat(),
                        valueRange = GestureConstants.MIN_ACCELERATION_START.toFloat()..GestureConstants.MAX_MAX_ACCELERATION_START.toFloat(),
                        valueText = "${round(uiState.continuousScrollAccelerationStart / 100.0).toInt() * 100} ms",
                        onValueChange = { value ->
                            viewModel.updatePreference(value) { settings, v ->
                                settings.copy(continuousScrollAccelerationStart = v.toLong())
                            }
                        },
                        steps = 14,
                    )

                    SliderPreferenceItem(
                        title = "Acceleration Duration",
                        value = uiState.continuousScrollAccelerationDuration.toFloat(),
                        valueRange = GestureConstants.MIN_ACCELERATION_DURATION.toFloat()..GestureConstants.MAX_MAX_ACCELERATION_DURATION.toFloat(),
                        valueText = "${round(uiState.continuousScrollAccelerationDuration / 100.0).toInt() * 100} ms",
                        onValueChange = { value ->
                            viewModel.updatePreference(value) { settings, v ->
                                settings.copy(continuousScrollAccelerationDuration = v.toLong())
                            }
                        },
                        steps = 14,
                    )
                }

                PreferenceCategory(title = "Edge Scrolling (Decelerated)") {
                    SliderPreferenceItem(
                        title = "Edge Scroll Duration",
                        value = uiState.edgeScrollDuration.toFloat(),
                        valueRange = uiState.scrollDuration.toFloat()..GestureConstants.MAX_SCROLL_DURATION.toFloat(),
                        valueText = "${uiState.edgeScrollDuration} ms",
                        onValueChange = { value ->
                            viewModel.updatePreference(value) { settings, v ->
                                settings.copy(edgeScrollDuration = v.toLong())
                            }
                        },
                        steps = max(
                            round((GestureConstants.MAX_SCROLL_DURATION.toFloat() - uiState.scrollDuration.toFloat()) / 100).toInt() - 1,
                            1
                        ),
                        enabled = uiState.cursorEdgeBehavior == ScreenEdgeBehavior.AUTO_SCROLL
                    )

                    SliderPreferenceItem(
                        title = "Edge Scroll Distance",
                        value = uiState.edgeScrollMultiplier,
                        valueRange = GestureConstants.MIN_SCROLL_MULTIPLIER..uiState.scrollMultiplier,
                        valueText = "${round(uiState.edgeScrollMultiplier * 100).toInt()}% of axis at most",
                        onValueChange = { value ->
                            viewModel.updatePreference(value) { settings, v ->
                                settings.copy(edgeScrollMultiplier = v)
                            }
                        },
                        steps = max(
                            round((uiState.scrollMultiplier.toFloat() - GestureConstants.MIN_SCROLL_MULTIPLIER.toFloat()) / 0.1).toInt() - 1,
                            1
                        ),
                        enabled = uiState.cursorEdgeBehavior == ScreenEdgeBehavior.AUTO_SCROLL
                    )
                }
            }
        }
    }
}