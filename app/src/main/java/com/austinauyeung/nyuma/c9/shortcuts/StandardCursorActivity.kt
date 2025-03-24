package com.austinauyeung.nyuma.c9.shortcuts

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.austinauyeung.nyuma.c9.accessibility.service.OverlayAccessibilityService

class StandardCursorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OverlayAccessibilityService.activateStandardCursor(this)
        finish()
    }
}