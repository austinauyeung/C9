package com.austinauyeung.nyuma.c9.shortcuts

import android.app.Activity
import android.os.Bundle
import com.austinauyeung.nyuma.c9.accessibility.service.OverlayAccessibilityService

class GridCursorActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OverlayAccessibilityService.activateGridCursor(this)
        finish()
    }
}