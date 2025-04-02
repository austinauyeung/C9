package com.austinauyeung.nyuma.c9.common.domain

enum class AutoHideDetection {
    NONE,                   // Do not auto-hide cursor
    RESTORE_ON_FOCUS_LOST,  // Hide on keyboard open, restore when text field loses focus
    RESTORE_ON_ENTER        // Hide on keyboard open, restore when enter is pressed
}