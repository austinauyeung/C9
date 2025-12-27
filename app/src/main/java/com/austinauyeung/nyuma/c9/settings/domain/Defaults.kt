package com.austinauyeung.nyuma.c9.settings.domain

import android.os.Build
import android.view.KeyEvent
import com.austinauyeung.nyuma.c9.core.constants.ApplicationConstants
import com.austinauyeung.nyuma.c9.core.constants.CursorConstants
import com.austinauyeung.nyuma.c9.core.constants.GestureConstants
import com.austinauyeung.nyuma.c9.core.constants.GridConstants
import com.austinauyeung.nyuma.c9.core.domain.GestureStyle
import com.austinauyeung.nyuma.c9.core.domain.ScreenEdgeBehavior
import com.austinauyeung.nyuma.c9.core.util.VersionUtil
import com.austinauyeung.nyuma.c9.cursor.domain.ControlScheme
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
        const val PERSIST_OVERLAY = GridConstants.PERSIST_OVERLAY
        const val HIDE_NUMBERS = GridConstants.HIDE_NUMBERS
        val GRID_LINE_VISIBILITY = GridLineVisibility.SHOW_ALL
        const val USE_NATURAL_SCROLLING = GestureConstants.USE_NATURAL_SCROLLING
        const val SHOW_GESTURE_VISUAL = GestureConstants.SHOW_GESTURE_VISUAL
        const val VISUAL_SIZE = GestureConstants.DEFAULT_SIZE
        const val CURSOR_SPEED = CursorConstants.DEFAULT_SPEED
        const val CURSOR_ACCELERATION = CursorConstants.DEFAULT_ACCELERATION
        const val CURSOR_SIZE = CursorConstants.DEFAULT_SIZE
        const val CURSOR_ACCELERATION_START = GestureConstants.DEFAULT_ACCELERATION_START
        const val CURSOR_ACCELERATION_DURATION = GestureConstants.DEFAULT_ACCELERATION_DURATION
        const val GRID_ACTIVATION_KEY = KeyEvent.KEYCODE_POUND
        const val CURSOR_ACTIVATION_KEY = KeyEvent.KEYCODE_STAR
        const val SCROLL_UP_KEY = KeyEvent.KEYCODE_2
        const val SCROLL_DOWN_KEY = KeyEvent.KEYCODE_8
        const val SCROLL_LEFT_KEY = KeyEvent.KEYCODE_4
        const val SCROLL_RIGHT_KEY = KeyEvent.KEYCODE_6
        val CURSOR_EDGE_BEHAVIOR = ScreenEdgeBehavior.NONE
        val CONTROL_SCHEME = ControlScheme.STANDARD
        val GESTURE_STYLE = GestureStyle.FIXED
        const val TOGGLE_HOLD = CursorConstants.TOGGLE_HOLD
        const val SCROLL_DURATION = GestureConstants.DEFAULT_SCROLL_DURATION
        const val SCROLL_MULTIPLIER = GestureConstants.DEFAULT_SCROLL_MULTIPLIER
        const val ZOOM_DURATION = GestureConstants.DEFAULT_ZOOM_DURATION
        const val ZOOM_FACTOR = GestureConstants.DEFAULT_ZOOM_DISTANCE_FACTOR
        const val ALLOW_PASSTHROUGH = GestureConstants.ALLOW_PASSTHROUGH
        val ENABLE_SHIZUKU_INTEGRATION = VersionUtil.belowVersion(Build.VERSION_CODES.O)
        const val OVERRIDE_ANDROID_7 = false
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
        val CLICKABLE_IMAGE_PATH = null
        val SCROLL_TOGGLE_IMAGE_PATH = null
        const val USE_CUSTOM_CURSOR_ICON = false
        val CURSOR_IMAGE_ALIGNMENT = IconAlignment.TOP_LEFT
        val CLICKABLE_IMAGE_ALIGNMENT = IconAlignment.TOP_LEFT
        val SCROLL_TOGGLE_IMAGE_ALIGNMENT = IconAlignment.TOP_LEFT
        const val USE_ADVANCED_SCROLLING = false
        const val COLLECT_LOGS = false
        val AUTO_HIDE_APPS = emptySet<String>()
        val CLICKABLE_APPS = emptySet<String>()
        const val SHOW_NOTIFICATION = false
        val APPLICATION_LIST_TYPE = AppListType.DENY_LIST
        val CLICKABLE_LIST_TYPE = AppListType.DENY_LIST
        const val IGNORE_NUMPAD = false
        const val CHECK_CLICKABLE = false
        const val KEEP_CURRENT_GRID_TRANSPARENT = true
        const val GRID_CURSOR_BACKGROUND_HEX = GridConstants.GRID_BACKGROUND_HEX
        const val GRID_CURSOR_LINES_HEX = GridConstants.GRID_LINES_HEX
        const val GRID_CURSOR_NUMBERS_HEX = GridConstants.GRID_NUMBERS_HEX
        const val GRID_CURSOR_LINE_WIDTH = GridConstants.GRID_LINE_WIDTH
        const val GRID_CURSOR_FONT_SIZE = GridConstants.GRID_FONT_SIZE

        const val CONTINUOUS_SCROLL_DURATION = GestureConstants.MIN_SCROLL_DURATION
        const val CONTINUOUS_SCROLL_MULTIPLIER = GestureConstants.MAX_SCROLL_MULTIPLIER
        const val CONTINUOUS_SCROLL_ACCELERATION_START = GestureConstants.DEFAULT_ACCELERATION_START
        const val CONTINUOUS_SCROLL_ACCELERATION_DURATION = GestureConstants.DEFAULT_ACCELERATION_DURATION

        const val EDGE_SCROLL_DURATION = GestureConstants.MAX_SCROLL_DURATION
        const val EDGE_SCROLL_MULTIPLIER = GestureConstants.MIN_SCROLL_MULTIPLIER
        const val EDGE_SCROLL_ACCELERATION_START = GestureConstants.DEFAULT_ACCELERATION_START
        const val EDGE_SCROLL_ACCELERATION_DURATION = GestureConstants.DEFAULT_ACCELERATION_DURATION

        const val DISABLE_TOUCHSCREEN = false
    }
}
