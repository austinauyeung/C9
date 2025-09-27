package com.austinauyeung.nyuma.c9.settings.ui.cursor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.austinauyeung.nyuma.c9.C9
import com.austinauyeung.nyuma.c9.core.ui.AppTheme
import com.austinauyeung.nyuma.c9.settings.ui.SettingsState

/**
 * Auto-hide apps screen.
 */
class ClickableAppsActivity : ComponentActivity() {
    private lateinit var settingsState: SettingsState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory =
            SettingsState.Factory(
                C9.getInstance().settingsRepository,
            )
        settingsState = ViewModelProvider(this, factory)[SettingsState::class.java]

        setContent {
            AppTheme {
                ClickableAppsScreen(
                    settingsState = settingsState,
                    onNavigateBack = {
                        finish()
                    }
                )
            }
        }
    }
}
