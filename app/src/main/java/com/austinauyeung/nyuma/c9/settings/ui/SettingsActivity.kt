package com.austinauyeung.nyuma.c9.settings.ui

import android.content.Context
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
import com.austinauyeung.nyuma.c9.core.ui.AppTheme
import com.austinauyeung.nyuma.c9.settings.ui.autohide.AutoHideSettingsActivity
import com.austinauyeung.nyuma.c9.settings.ui.cursor.CursorSettingsActivity
import com.austinauyeung.nyuma.c9.settings.ui.gesture.CommonGestureSettingsActivity
import com.austinauyeung.nyuma.c9.settings.ui.gesture.ScrollSettingsActivity
import com.austinauyeung.nyuma.c9.settings.ui.gesture.ZoomSettingsActivity
import com.austinauyeung.nyuma.c9.settings.ui.grid.GridSettingsActivity

/**
 * Main settings screen.
 */
class SettingsActivity : ComponentActivity() {
    private lateinit var settingsState: SettingsState
    private lateinit var accessibilitySettingsObserver: ContentObserver

    private fun startCustomActivity(context: Context, activityClass: Class<*>) {
        val intent = Intent(context, activityClass)
        val options = ActivityOptionsCompat.makeBasic()
        context.startActivity(intent, options.toBundle())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory =
            SettingsState.Factory(
                C9.getInstance().settingsRepository,
            )
        settingsState = ViewModelProvider(this, factory)[SettingsState::class.java]
        settingsState.setToastFunction { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        registerAccessibilitySettingsObserver()
        checkAccessibilityServiceStatus()

        setContent {
            AppTheme {
                SettingsScreen(
                    settingsState = settingsState,
                    onNavigateToGridSettings = { startCustomActivity(this, GridSettingsActivity::class.java) },
                    onNavigateToCursorSettings = { startCustomActivity(this, CursorSettingsActivity::class.java) },
                    onNavigateToDebugOptions = { startCustomActivity(this, DebugOptionsActivity::class.java) },
                    onNavigateToAutoHideSettings = { startCustomActivity(this, AutoHideSettingsActivity::class.java) },
                    onNavigateToCommonGestureSettings = { startCustomActivity(this, CommonGestureSettingsActivity::class.java) },
                    onNavigateToScrollSettings = { startCustomActivity(this, ScrollSettingsActivity::class.java) },
                    onNavigateToZoomSettings = { startCustomActivity(this, ZoomSettingsActivity::class.java) }
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
        settingsState.updateAccessibilityServiceStatus(isServiceEnabled)
    }

    override fun onDestroy() {
        contentResolver.unregisterContentObserver(accessibilitySettingsObserver)
        super.onDestroy()
    }
}
