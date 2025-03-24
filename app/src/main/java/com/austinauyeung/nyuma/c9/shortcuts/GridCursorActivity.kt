package com.austinauyeung.nyuma.c9.shortcuts

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.austinauyeung.nyuma.c9.accessibility.service.OverlayAccessibilityService

class GridCursorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OverlayAccessibilityService.activateGridCursor(this)
        finish()
    }
}