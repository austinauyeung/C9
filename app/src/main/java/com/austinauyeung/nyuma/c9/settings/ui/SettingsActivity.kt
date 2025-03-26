package com.austinauyeung.nyuma.c9.settings.ui

import android.content.Intent
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelProvider
import com.austinauyeung.nyuma.c9.C9
import com.austinauyeung.nyuma.c9.common.ui.C9Theme
import com.austinauyeung.nyuma.c9.core.logs.ui.LogActivity

/**
 * Main settings screen.
 */
class SettingsActivity : ComponentActivity() {
    private lateinit var viewModel: SettingsViewModel
    private lateinit var accessibilitySettingsObserver: ContentObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory =
            SettingsViewModel.Factory(
                C9.getInstance().settingsRepository,
            )
        viewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]
        viewModel.setToastFunction { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        registerAccessibilitySettingsObserver()
        checkAccessibilityServiceStatus()

        setContent {
            C9Theme {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToGridSettings = {
                        val intent = Intent(this, GridSettingsActivity::class.java)
                        val options = ActivityOptionsCompat.makeBasic()
                        startActivity(intent, options.toBundle())
                    },
                    onNavigateToCursorSettings = {
                        val intent = Intent(this, CursorSettingsActivity::class.java)
                        val options = ActivityOptionsCompat.makeBasic()
                        startActivity(intent, options.toBundle())
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

    private fun registerAccessibilitySettingsObserver() {
        accessibilitySettingsObserver =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    checkAccessibilityServiceStatus()
                }
            }

        val uri = Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        contentResolver.registerContentObserver(uri, false, accessibilitySettingsObserver)
    }

    override fun onResume() {
        super.onResume()
        checkAccessibilityServiceStatus()
    }

    private fun checkAccessibilityServiceStatus() {
        val isServiceEnabled = C9.isAccessibilityServiceEnabled(this)
        viewModel.updateAccessibilityServiceStatus(isServiceEnabled)
    }

    override fun onDestroy() {
        contentResolver.unregisterContentObserver(accessibilitySettingsObserver)
        super.onDestroy()
    }
}
