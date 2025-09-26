package com.austinauyeung.nyuma.c9.settings.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelProvider
import com.austinauyeung.nyuma.c9.C9
import com.austinauyeung.nyuma.c9.core.ui.AppTheme
import com.austinauyeung.nyuma.c9.core.logs.LogActivity

class DebugOptionsActivity : ComponentActivity() {
    private lateinit var viewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = SettingsViewModel.Factory(C9.getInstance().settingsRepository)
        viewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]

        setContent {
            AppTheme {
                DebugOptionsScreen(
                    viewModel = viewModel,
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