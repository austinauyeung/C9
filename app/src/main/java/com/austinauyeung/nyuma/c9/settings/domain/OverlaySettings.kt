package com.austinauyeung.nyuma.c9.settings.domain

import com.austinauyeung.nyuma.c9.common.domain.GestureStyle
import com.austinauyeung.nyuma.c9.core.constants.ApplicationConstants
import com.austinauyeung.nyuma.c9.core.constants.CursorConstants
import com.austinauyeung.nyuma.c9.core.constants.GridConstants

/**
 * Represents default user preferences.
 */
data class OverlaySettings(
    val gridLevels: Int = Defaults.Settings.GRID_LEVELS,
    val overlayOpacity: Int = Defaults.Settings.OVERLAY_OPACITY,
    val persistOverlay: Boolean = Defaults.Settings.PERSIST_OVERLAY,
    val hideNumbers: Boolean = Defaults.Settings.HIDE_NUMBERS,
    val useNaturalScrolling: Boolean = Defaults.Settings.USE_NATURAL_SCROLLING,
    val showGestureVisualization: Boolean = Defaults.Settings.SHOW_GESTURE_VISUAL,
    val cursorSpeed: Int = Defaults.Settings.CURSOR_SPEED,
    val cursorAcceleration: Int = Defaults.Settings.CURSOR_ACCELERATION,
    val cursorSize: Int = Defaults.Settings.CURSOR_SIZE,
    val gridActivationKey: Int = Defaults.Settings.GRID_ACTIVATION_KEY,
    val cursorActivationKey: Int = Defaults.Settings.CURSOR_ACTIVATION_KEY,
    val controlScheme: ControlScheme = Defaults.Settings.CONTROL_SCHEME,
    val cursorWrapAround: Boolean = Defaults.Settings.CURSOR_WRAP_AROUND,
    val gestureStyle: GestureStyle = Defaults.Settings.GESTURE_STYLE,
    val toggleHold: Boolean = Defaults.Settings.TOGGLE_HOLD,
    val gestureDuration: Long = Defaults.Settings.GESTURE_DURATION,
    val scrollMultiplier: Float = Defaults.Settings.SCROLL_MULTIPLIER
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

                if (overlayOpacity !in GridConstants.MIN_OPACITY..GridConstants.MAX_OPACITY) {
                    add("Overlay opacity must be between ${GridConstants.MIN_OPACITY} and ${GridConstants.MAX_OPACITY}")
                }

                if (cursorSpeed !in CursorConstants.MIN_SPEED..CursorConstants.MAX_SPEED) {
                    add("Cursor speed must be between ${CursorConstants.MIN_SPEED} and ${CursorConstants.MAX_SPEED}")
                }

                if (cursorAcceleration !in CursorConstants.MIN_ACCELERATION..CursorConstants.MAX_ACCELERATION) {
                    add("Cursor speed must be between ${CursorConstants.MIN_ACCELERATION} and ${CursorConstants.MAX_ACCELERATION}")
                }

                if (cursorSize !in CursorConstants.MIN_SIZE..CursorConstants.MAX_SIZE) {
                    add("Cursor size must be between ${CursorConstants.MIN_SIZE} and ${CursorConstants.MAX_SIZE}")
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
            cursorSize = cursorSpeed.coerceIn(CursorConstants.MIN_SIZE, CursorConstants.MAX_SIZE),
            gridActivationKey = if (isValidRemappableKey(gridActivationKey)) gridActivationKey else KEY_NONE,
            cursorActivationKey = if (isValidRemappableKey(cursorActivationKey)) cursorActivationKey else KEY_NONE,
        )
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
    )
}
