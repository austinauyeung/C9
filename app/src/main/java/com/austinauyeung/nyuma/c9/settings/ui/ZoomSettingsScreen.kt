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
fun ZoomSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zoom Options") },
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
                SliderPreferenceItem(
                    title = "Zoom Duration",
                    value = uiState.zoomDuration.toFloat(),
                    valueRange = GestureConstants.MIN_ZOOM_DURATION.toFloat()..GestureConstants.MAX_ZOOM_DURATION.toFloat(),
                    valueText = "${uiState.zoomDuration} ms",
                    onValueChange = { value ->
                        viewModel.updatePreference(value) { settings, v ->
                            settings.copy(zoomDuration = v.toLong())
                        }
                    },
                    steps = 3,
                )
            }
        }
    }
}