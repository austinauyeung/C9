package com.austinauyeung.nyuma.c9.shortcuts

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.austinauyeung.nyuma.c9.C9
import com.austinauyeung.nyuma.c9.accessibility.service.OverlayAccessibilityService
import com.austinauyeung.nyuma.c9.core.util.Logger
import kotlinx.coroutines.launch

/**
 * Shortcuts to each cursor mode for button mappers.
 */
class ActivationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!C9.isAccessibilityServiceEnabled(this)) {
            finish()
            return
        }

        when (val cursorMode = getCursorModeFromIntent()) {
            "grid" -> {
                lifecycleScope.launch {
                    OverlayAccessibilityService.activateGridCursor(this@ActivationActivity)
                    finish()
                }
            }
            "standard" -> {
                lifecycleScope.launch {
                    OverlayAccessibilityService.activateStandardCursor(this@ActivationActivity)
                    finish()
                }
            }
            else -> {
                Logger.e("Unknown cursor mode: $cursorMode")
                finish()
            }
        }
    }

    private fun getCursorModeFromIntent(): String? {
        try {
            val modeFromIntent = intent.getStringExtra("cursorMode")
            if (!modeFromIntent.isNullOrEmpty()) {
                return modeFromIntent
            }

            val componentName = intent.component ?: return null
            val activityInfo = packageManager.getActivityInfo(
                componentName,
                PackageManager.GET_META_DATA
            )

            return activityInfo.metaData?.getString("cursorMode")
        } catch (e: Exception) {
            Logger.e("Error getting cursor mode from intent", e)
            return null
        }
    }
}