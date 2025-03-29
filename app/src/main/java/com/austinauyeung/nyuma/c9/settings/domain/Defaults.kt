package com.austinauyeung.nyuma.c9.settings.domain

import android.view.KeyEvent
import com.austinauyeung.nyuma.c9.common.domain.GestureStyle
import com.austinauyeung.nyuma.c9.core.constants.CursorConstants
import com.austinauyeung.nyuma.c9.core.constants.GestureConstants
import com.austinauyeung.nyuma.c9.core.constants.GridConstants
import com.austinauyeung.nyuma.c9.grid.domain.GridLineVisibility

/**
 * Contains default values that can be modified by the user.
 */

// Maybe reference constants file directly
object Defaults {
    object Settings {
        const val GRID_LEVELS = GridConstants.DEFAULT_LEVELS
        const val OVERLAY_OPACITY = GridConstants.DEFAULT_OPACITY
        const val PERSIST_OVERLAY = GridConstants.PERSIST_OVERLAY
        const val HIDE_NUMBERS = GridConstants.HIDE_NUMBERS
        val GRID_LINE_VISIBILITY = GridLineVisibility.SHOW_ALL
        const val USE_NATURAL_SCROLLING = GestureConstants.USE_NATURAL_SCROLLING
        const val SHOW_GESTURE_VISUAL = GestureConstants.SHOW_GESTURE_VISUAL
        const val CURSOR_SPEED = CursorConstants.DEFAULT_SPEED
        const val CURSOR_ACCELERATION = CursorConstants.DEFAULT_ACCELERATION
        const val CURSOR_SIZE = CursorConstants.DEFAULT_SIZE
        const val CURSOR_ACCELERATION_THRESHOLD = CursorConstants.DEFAULT_ACCELERATION_THRESHOLD
        const val GRID_ACTIVATION_KEY = KeyEvent.KEYCODE_POUND
        const val CURSOR_ACTIVATION_KEY = KeyEvent.KEYCODE_STAR
        const val CURSOR_WRAP_AROUND = CursorConstants.CURSOR_WRAP_AROUND
        val CONTROL_SCHEME = ControlScheme.STANDARD
        val GESTURE_STYLE = GestureStyle.FIXED
        const val TOGGLE_HOLD = CursorConstants.TOGGLE_HOLD
        const val GESTURE_DURATION = GestureConstants.DEFAULT_GESTURE_DURATION
        const val SCROLL_MULTIPLIER = GestureConstants.DEFAULT_SCROLL_MULTIPLIER
        const val ALLOW_PASSTHROUGH = GestureConstants.ALLOW_PASSTHROUGH
        const val ENABLE_SHIZUKU_INTEGRATION = false
        const val HIDE_ON_TEXT_FIELD = false
        const val ROTATE_BUTTONS_WITH_ORIENTATION = false
    }
}
