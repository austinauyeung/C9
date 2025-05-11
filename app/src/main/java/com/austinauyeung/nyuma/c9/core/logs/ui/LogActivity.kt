package com.austinauyeung.nyuma.c9.core.logs.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.austinauyeung.nyuma.c9.common.ui.C9Theme

/**
 * Basic console for real-time logs.
 */
class LogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            C9Theme {
                LogScreen(
                    onNavigateBack = {
                        finish()
                    }
                )
            }
        }
    }
}