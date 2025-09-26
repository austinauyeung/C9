package com.austinauyeung.nyuma.c9.shortcuts

import android.app.Activity
import android.os.Bundle
import com.austinauyeung.nyuma.c9.accessibility.AppAccessibilityService

class ResetGridActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppAccessibilityService.resetGrid(this)
        finish()
    }
}