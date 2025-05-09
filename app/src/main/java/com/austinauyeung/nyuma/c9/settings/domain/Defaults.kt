package com.austinauyeung.nyuma.c9.settings.domain

import android.view.KeyEvent
import com.austinauyeung.nyuma.c9.common.domain.GestureStyle
import com.austinauyeung.nyuma.c9.common.domain.ScreenEdgeBehavior
import com.austinauyeung.nyuma.c9.core.constants.ApplicationConstants
import com.austinauyeung.nyuma.c9.core.constants.CursorConstants
import com.austinauyeung.nyuma.c9.core.constants.GestureConstants
import com.austinauyeung.nyuma.c9.core.constants.GridConstants
import com.austinauyeung.nyuma.c9.cursor.domain.IconAlignment
import com.austinauyeung.nyuma.c9.grid.domain.GridLineVisibility

/**
 * Contains default values that can be modified by the user.
 */

// Maybe reference constants file directly
object Defaults {
    object Settings {
        const val ACTIVATION_DURATION = ApplicationConstants.DEFAULT_ACTIVATION_HOLD_DURATION
        const val GRID_LEVELS = GridConstants.DEFAULT_LEVELS
        const val OVERLAY_OPACITY = GridConstants.DEFAULT_OPACITY
        const val PERSIST_OVERLAY = GridConstants.PERSIST_OVERLAY
        const val HIDE_NUMBERS = GridConstants.HIDE_NUMBERS
        val GRID_LINE_VISIBILITY = GridLineVisibility.SHOW_ALL
        const val USE_NATURAL_SCROLLING = GestureConstants.USE_NATURAL_SCROLLING
        const val SHOW_GESTURE_VISUAL = GestureConstants.SHOW_GESTURE_VISUAL
        const val VISUAL_SIZE = GestureConstants.DEFAULT_SIZE
        const val CURSOR_SPEED = CursorConstants.DEFAULT_SPEED
        const val CURSOR_ACCELERATION = CursorConstants.DEFAULT_ACCELERATION
        const val CURSOR_SIZE = CursorConstants.DEFAULT_SIZE
        const val CURSOR_ACCELERATION_START = CursorConstants.DEFAULT_ACCELERATION_START
        const val CURSOR_ACCELERATION_DURATION = CursorConstants.DEFAULT_ACCELERATION_DURATION
        const val GRID_ACTIVATION_KEY = KeyEvent.KEYCODE_POUND
        const val CURSOR_ACTIVATION_KEY = KeyEvent.KEYCODE_STAR
        val CURSOR_EDGE_BEHAVIOR = ScreenEdgeBehavior.NONE
        val CONTROL_SCHEME = ControlScheme.STANDARD
        val GESTURE_STYLE = GestureStyle.FIXED
        const val TOGGLE_HOLD = CursorConstants.TOGGLE_HOLD
        const val SCROLL_DURATION = GestureConstants.DEFAULT_SCROLL_DURATION
        const val SCROLL_MULTIPLIER = GestureConstants.DEFAULT_SCROLL_MULTIPLIER
        const val ZOOM_DURATION = GestureConstants.DEFAULT_ZOOM_DURATION
        const val ALLOW_PASSTHROUGH = GestureConstants.ALLOW_PASSTHROUGH
        const val ENABLE_SHIZUKU_INTEGRATION = false
        const val HIDE_ON_KEYBOARD_OPEN = false
        const val HIDE_ON_LAUNCHER_OPEN = false
        const val HIDE_ON_LOCK_SCREEN = false
        const val ROTATE_BUTTONS_WITH_ORIENTATION = false
        const val ROUNDED_CURSOR_CORNERS = true
        const val USE_PHYSICAL_SIZE = true
        const val STANDARD_CURSOR_HEX = CursorConstants.STANDARD_CURSOR_HEX
        const val STANDARD_CURSOR_MATCH_BORDER = false
        const val ALLOW_OVERLAPPING_GESTURES = false
        const val FORCE_SMOOTHER_GESTURES = false
        val CURSOR_IMAGE_PATH = null
        val SCROLL_TOGGLE_IMAGE_PATH = null
        const val USE_CUSTOM_CURSOR_ICON = false
        val CURSOR_IMAGE_ALIGNMENT = IconAlignment.TOP_LEFT
        val SCROLL_TOGGLE_IMAGE_ALIGNMENT = IconAlignment.TOP_LEFT
    }
}
