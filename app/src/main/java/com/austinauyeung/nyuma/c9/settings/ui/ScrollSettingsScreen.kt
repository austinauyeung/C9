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
import com.austinauyeung.nyuma.c9.core.constants.GestureConstants
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
                title = { Text("Scroll Settings") },
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
            }
        }
    }
}