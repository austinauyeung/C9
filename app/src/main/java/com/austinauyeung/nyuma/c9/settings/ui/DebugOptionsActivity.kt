package com.austinauyeung.nyuma.c9.settings.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelProvider
import com.austinauyeung.nyuma.c9.C9
import com.austinauyeung.nyuma.c9.core.logs.LogActivity
import com.austinauyeung.nyuma.c9.core.ui.AppTheme

class DebugOptionsActivity : ComponentActivity() {
    private lateinit var settingsState: SettingsState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = SettingsState.Factory(C9.getInstance().settingsRepository)
        settingsState = ViewModelProvider(this, factory)[SettingsState::class.java]

        setContent {
            AppTheme {
                DebugOptionsScreen(
                    settingsState = settingsState,
                    onNavigateBack = {
                        finish()
                    },
                    onNavigateToLogScreen = {
                        val intent = Intent(this, LogActivity::class.java)
                        val options = ActivityOptionsCompat.makeBasic()
                        startActivity(intent, options.toBundle())
                    }
                )
            }
        }
    }
}