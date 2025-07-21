package com.austinauyeung.nyuma.c9.settings.domain

import com.austinauyeung.nyuma.c9.common.domain.GestureStyle
import com.austinauyeung.nyuma.c9.common.domain.ScreenEdgeBehavior
import com.austinauyeung.nyuma.c9.core.constants.ApplicationConstants
import com.austinauyeung.nyuma.c9.core.constants.CursorConstants
import com.austinauyeung.nyuma.c9.core.constants.GestureConstants
import com.austinauyeung.nyuma.c9.core.constants.GridConstants
import com.austinauyeung.nyuma.c9.cursor.domain.ControlScheme
import com.austinauyeung.nyuma.c9.cursor.domain.IconAlignment
import com.austinauyeung.nyuma.c9.grid.domain.GridLineVisibility

/**
 * Represents default user preferences.
 */
data class OverlaySettings(
    val activationDuration: Long = Defaults.Settings.ACTIVATION_DURATION,
    val gridLevels: Int = Defaults.Settings.GRID_LEVELS,
    val overlayOpacity: Int = Defaults.Settings.OVERLAY_OPACITY,
    val persistOverlay: Boolean = Defaults.Settings.PERSIST_OVERLAY,
    val hideNumbers: Boolean = Defaults.Settings.HIDE_NUMBERS,
    val gridLineVisibility: GridLineVisibility = Defaults.Settings.GRID_LINE_VISIBILITY,
    val useNaturalScrolling: Boolean = Defaults.Settings.USE_NATURAL_SCROLLING,
    val showGestureVisualization: Boolean = Defaults.Settings.SHOW_GESTURE_VISUAL,
    val visualSize: Int = Defaults.Settings.VISUAL_SIZE,
    val cursorSpeed: Int = Defaults.Settings.CURSOR_SPEED,
    val cursorAcceleration: Int = Defaults.Settings.CURSOR_ACCELERATION,
    val cursorSize: Int = Defaults.Settings.CURSOR_SIZE,
    val cursorAccelerationStart: Long = Defaults.Settings.CURSOR_ACCELERATION_START,
    val cursorAccelerationDuration: Long = Defaults.Settings.CURSOR_ACCELERATION_DURATION,
    val gridActivationKey: Int = Defaults.Settings.GRID_ACTIVATION_KEY,
    val cursorActivationKey: Int = Defaults.Settings.CURSOR_ACTIVATION_KEY,
    val controlScheme: ControlScheme = Defaults.Settings.CONTROL_SCHEME,
    val cursorEdgeBehavior: ScreenEdgeBehavior = Defaults.Settings.CURSOR_EDGE_BEHAVIOR,
    val gestureStyle: GestureStyle = Defaults.Settings.GESTURE_STYLE,
    val toggleHold: Boolean = Defaults.Settings.TOGGLE_HOLD,
    val scrollDuration: Long = Defaults.Settings.SCROLL_DURATION,
    val scrollMultiplier: Float = Defaults.Settings.SCROLL_MULTIPLIER,
    val zoomDuration: Long = Defaults.Settings.ZOOM_DURATION,
    val zoomFactor: Float = Defaults.Settings.ZOOM_FACTOR,
    val allowPassthrough: Boolean = Defaults.Settings.ALLOW_PASSTHROUGH,
    val enableShizukuIntegration: Boolean = Defaults.Settings.ENABLE_SHIZUKU_INTEGRATION,
    val hideOnKeyboardOpen: Boolean = Defaults.Settings.HIDE_ON_KEYBOARD_OPEN,
    val hideOnLauncherOpen: Boolean = Defaults.Settings.HIDE_ON_LAUNCHER_OPEN,
    val hideOnLockScreen: Boolean = Defaults.Settings.HIDE_ON_LOCK_SCREEN,
    val rotateButtonsWithOrientation: Boolean = Defaults.Settings.ROTATE_BUTTONS_WITH_ORIENTATION,
    val roundedCursorCorners: Boolean = Defaults.Settings.ROUNDED_CURSOR_CORNERS,
    val usePhysicalSize: Boolean = Defaults.Settings.USE_PHYSICAL_SIZE,
    val standardCursorHex: String = Defaults.Settings.STANDARD_CURSOR_HEX,
    val standardCursorMatchBorder: Boolean = Defaults.Settings.STANDARD_CURSOR_MATCH_BORDER,
    val allowOverlappingGestures: Boolean = Defaults.Settings.ALLOW_OVERLAPPING_GESTURES,
    val forceSmootherGestures: Boolean = Defaults.Settings.FORCE_SMOOTHER_GESTURES,
    val cursorImagePath: String? = Defaults.Settings.CURSOR_IMAGE_PATH,
    val scrollToggleImagePath: String? = Defaults.Settings.SCROLL_TOGGLE_IMAGE_PATH,
    val useCustomCursorIcon: Boolean = Defaults.Settings.USE_CUSTOM_CURSOR_ICON,
    val cursorImageAlignment: IconAlignment = Defaults.Settings.CURSOR_IMAGE_ALIGNMENT,
    val scrollToggleImageAlignment: IconAlignment = Defaults.Settings.SCROLL_TOGGLE_IMAGE_ALIGNMENT,
    val useAdvancedScrolling: Boolean = Defaults.Settings.USE_ADVANCED_SCROLLING,
    val continuousScrollDuration: Long = Defaults.Settings.CONTINUOUS_SCROLL_DURATION,
    val continuousScrollMultiplier: Float = Defaults.Settings.CONTINUOUS_SCROLL_MULTIPLIER,
    val continuousScrollAccelerationStart: Long = Defaults.Settings.CONTINUOUS_SCROLL_ACCELERATION_START,
    val continuousScrollAccelerationDuration: Long = Defaults.Settings.CONTINUOUS_SCROLL_ACCELERATION_DURATION,
    val edgeScrollDuration: Long = Defaults.Settings.EDGE_SCROLL_DURATION,
    val edgeScrollMultiplier: Float = Defaults.Settings.EDGE_SCROLL_MULTIPLIER,
    val edgeScrollAccelerationStart: Long = Defaults.Settings.EDGE_SCROLL_ACCELERATION_START,
    val edgeScrollAccelerationDuration: Long = Defaults.Settings.EDGE_SCROLL_ACCELERATION_DURATION,
    val collectLogs: Boolean = Defaults.Settings.COLLECT_LOGS,
    val autoHideApps: Set<String> = Defaults.Settings.AUTO_HIDE_APPS,
    val showNotification: Boolean = Defaults.Settings.SHOW_NOTIFICATION,
    val applicationListType: AppListType = Defaults.Settings.APPLICATION_LIST_TYPE,
    val ignoreNumpad: Boolean = Defaults.Settings.IGNORE_NUMPAD
) {
    companion object {
        val DEFAULT = OverlaySettings()
        const val KEY_NONE = ApplicationConstants.OVERLAY_DISABLED
        val RESTRICTED_KEYS = emptySet<Int>()
    }

    private fun isValidRemappableKey(keyCode: Int): Boolean {
        if (keyCode == KEY_NONE) return true
        return keyCode !in RESTRICTED_KEYS
    }

    fun validate(): ValidationResult {
        val errors =
            buildList {
                if (gridLevels !in GridConstants.MIN_LEVELS..GridConstants.MAX_LEVELS) {
                    add("Grid levels must be between ${GridConstants.MIN_LEVELS} and ${GridConstants.MAX_LEVELS}")
                }

                if (!isValidRemappableKey(gridActivationKey)) {
                    add("Invalid grid activation key: $gridActivationKey")
                }

                if (!isValidRemappableKey(cursorActivationKey)) {
                    add("Invalid cursor activation key: $cursorActivationKey")
                }

                if (gridActivationKey != KEY_NONE && cursorActivationKey != KEY_NONE &&
                    gridActivationKey == cursorActivationKey
                ) {
                    add("Grid and cursor activation keys must be different")
                }

                if (continuousScrollDuration > scrollDuration) {
                    add("For acceleration, continuous scroll duration should be less than scroll duration")
                }

                if (continuousScrollMultiplier < scrollMultiplier) {
                    add("For acceleration, continuous scroll multiplier should be greater than scroll multiplier")
                }

                if (edgeScrollDuration < scrollDuration) {
                    add("For deceleration, edge scroll duration should be greater than scroll duration")
                }

                if (edgeScrollMultiplier > scrollMultiplier) {
                    add("For deceleration, edge scroll multiplier should be less than scroll multiplier")
                }
            }

        return ValidationResult(errors.isEmpty(), errors)
    }

    fun sanitized(): OverlaySettings {
        return copy(
            gridLevels = gridLevels.coerceIn(GridConstants.MIN_LEVELS, GridConstants.MAX_LEVELS),
            overlayOpacity = overlayOpacity.coerceIn(
                GridConstants.MIN_OPACITY,
                GridConstants.MAX_OPACITY
            ),
            cursorSpeed = cursorSpeed.coerceIn(
                CursorConstants.MIN_SPEED,
                CursorConstants.MAX_SPEED
            ),
            cursorAcceleration = cursorAcceleration.coerceIn(
                CursorConstants.MAX_ACCELERATION,
                CursorConstants.MAX_ACCELERATION
            ),
            cursorSize = cursorSize.coerceIn(CursorConstants.MIN_SIZE, CursorConstants.MAX_SIZE),
            gridActivationKey = if (isValidRemappableKey(gridActivationKey)) gridActivationKey else KEY_NONE,
            cursorActivationKey = if (isValidRemappableKey(cursorActivationKey)) cursorActivationKey else KEY_NONE,
            continuousScrollDuration = continuousScrollDuration.coerceIn(
                GestureConstants.MIN_SCROLL_DURATION,
                scrollDuration
            ),
            continuousScrollMultiplier = continuousScrollMultiplier.coerceIn(
                scrollMultiplier,
                GestureConstants.MAX_SCROLL_MULTIPLIER
            ),
            edgeScrollDuration = edgeScrollDuration.coerceIn(
                scrollDuration,
                GestureConstants.MAX_SCROLL_DURATION
            ),
            edgeScrollMultiplier = edgeScrollMultiplier.coerceIn(
                GestureConstants.MIN_SCROLL_MULTIPLIER,
                scrollMultiplier
            )
        )
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
    )
}
