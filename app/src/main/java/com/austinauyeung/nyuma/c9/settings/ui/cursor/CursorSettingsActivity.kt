package com.austinauyeung.nyuma.c9.settings.ui.cursor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelProvider
import com.austinauyeung.nyuma.c9.C9
import com.austinauyeung.nyuma.c9.core.ui.AppTheme
import com.austinauyeung.nyuma.c9.settings.ui.SettingsState

/**
 * Standard cursor settings screen.
 */
class CursorSettingsActivity : ComponentActivity() {
    private lateinit var settingsState: SettingsState

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

        setContent {
            AppTheme {
                CursorSettingsScreen(
                    settingsState = settingsState,
                    onNavigateToCursorIcon = { startCustomActivity(this, CursorIconActivity::class.java) },
                    onNavigateToLocationClickableIcon = { startCustomActivity(this, LocationClickableIconActivity::class.java) },
                    onNavigateToScrollToggleIcon = { startCustomActivity(this, ScrollToggleIconActivity::class.java) },
                    onNavigateToClickableAppsScreen = { startCustomActivity(this, ClickableAppsActivity::class.java) },
                    onNavigateBack = {
                        finish()
                    },
                )
            }
        }
    }
}
