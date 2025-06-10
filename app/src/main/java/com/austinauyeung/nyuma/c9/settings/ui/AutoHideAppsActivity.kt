package com.austinauyeung.nyuma.c9.settings.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.austinauyeung.nyuma.c9.C9
import com.austinauyeung.nyuma.c9.common.ui.C9Theme

/**
 * Auto-hide apps screen.
 */
class AutoHideAppsActivity : ComponentActivity() {
    private lateinit var viewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory =
            SettingsViewModel.Factory(
                C9.getInstance().settingsRepository,
            )
        viewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]

        setContent {
            C9Theme {
                AutoHideAppsScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        finish()
                    }
                )
            }
        }
    }
}
