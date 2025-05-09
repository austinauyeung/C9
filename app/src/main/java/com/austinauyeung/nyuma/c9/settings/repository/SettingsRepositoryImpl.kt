package com.austinauyeung.nyuma.c9.settings.repository

import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.austinauyeung.nyuma.c9.core.logs.Logger
import com.austinauyeung.nyuma.c9.core.util.VersionUtil
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * Persists user settings.
 */
class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    companion object {
        private val ACTIVATION_DURATION = longPreferencesKey("activation_duration")
        private val GRID_LEVELS = intPreferencesKey("grid_levels")
        private val OVERLAY_OPACITY = intPreferencesKey("overlay_opacity")
        private val PERSIST_OVERLAY = booleanPreferencesKey("persist_overlay")
        private val HIDE_NUMBERS = booleanPreferencesKey("hide_numbers")
        private val GRID_LINE_VISIBILITY = stringPreferencesKey("grid_line_visibility")
        private val USE_NATURAL_SCROLLING = booleanPreferencesKey("use_natural_scrolling")
        private val SHOW_GESTURE_VISUAL = booleanPreferencesKey("show_gesture_visual")
        private val VISUAL_SIZE = intPreferencesKey("visual_size")
        private val CURSOR_SPEED = intPreferencesKey("cursor_speed")
        private val CURSOR_ACCELERATION = intPreferencesKey("cursor_acceleration")
        private val CURSOR_SIZE = intPreferencesKey("cursor_size")
        private val CURSOR_ACCELERATION_START = longPreferencesKey("cursor_acceleration_start")
        private val CURSOR_ACCELERATION_DURATION = longPreferencesKey("cursor_acceleration_duration")
        private val GRID_ACTIVATION_KEY = intPreferencesKey("grid_activation_key")
        private val CURSOR_ACTIVATION_KEY = intPreferencesKey("cursor_activation_key")
        private val CONTROL_SCHEME = stringPreferencesKey("control_scheme")
        private val CURSOR_EDGE_BEHAVIOR = stringPreferencesKey("cursor_edge_behavior")
        private val GESTURE_STYLE = stringPreferencesKey("gesture_style")
        private val TOGGLE_HOLD = booleanPreferencesKey("toggle_hold")
        private val SCROLL_DURATION = longPreferencesKey("scroll_duration")
        private val SCROLL_MULTIPLIER = floatPreferencesKey("scroll_multiplier")
        private val ZOOM_DURATION = longPreferencesKey("zoom_duration")
        private val ALLOW_PASSTHROUGH = booleanPreferencesKey("allow_passthrough")
        private val ENABLE_SHIZUKU_INTEGRATION = booleanPreferencesKey("enable_shizuku_integration")
        private val HIDE_ON_KEYBOARD_OPEN = booleanPreferencesKey("hide_on_keyboard_open")
        private val HIDE_ON_LAUNCHER_OPEN = booleanPreferencesKey("hide_on_launcher_open")
        private val HIDE_ON_LOCK_SCREEN = booleanPreferencesKey("hide_on_lock_screen")
        private val ROTATE_BUTTONS_WITH_ORIENTATION = booleanPreferencesKey("rotate_buttons_with_orientation")
        private val ROUNDED_CURSOR_CORNERS = booleanPreferencesKey("rounded_cursor_corners")
        private val USE_PHYSICAL_SIZE = booleanPreferencesKey("use_physical_size")
        private val STANDARD_CURSOR_HEX = stringPreferencesKey("standard_cursor_hex")
        private val STANDARD_CURSOR_MATCH_BORDER = booleanPreferencesKey("standard_cursor_match_border")
        private val ALLOW_OVERLAPPING_GESTURES = booleanPreferencesKey("allow_overlapping_gestures")
        private val FORCE_SMOOTHER_GESTURES = booleanPreferencesKey("force_smoother_gestures")
        private val CURSOR_IMAGE_PATH = stringPreferencesKey("cursor_image_path")
        private val SCROLL_TOGGLE_IMAGE_PATH = stringPreferencesKey("scroll_toggle_image_path")
        private val USE_CUSTOM_CURSOR_ICON = booleanPreferencesKey("use_custom_cursor_icon")
        private val CURSOR_IMAGE_ALIGNMENT = stringPreferencesKey("cursor_image_alignment")
        private val SCROLL_TOGGLE_IMAGE_ALIGNMENT = stringPreferencesKey("scroll_toggle_image_alignment")
        private val USE_ADVANCED_SCROLLING = booleanPreferencesKey("use_advanced_scrolling")
        private val CONTINUOUS_SCROLL_DURATION = longPreferencesKey("continuous_scroll_duration")
        private val CONTINUOUS_SCROLL_MULTIPLIER = floatPreferencesKey("continuous_scroll_multiplier")
        private val CONTINUOUS_SCROLL_ACCELERATION_START = longPreferencesKey("continuous_scroll_acceleration_start")
        private val CONTINUOUS_SCROLL_ACCELERATION_DURATION = longPreferencesKey("continuous_scroll_acceleration_duration")
        private val EDGE_SCROLL_DURATION = longPreferencesKey("edge_scroll_duration")
        private val EDGE_SCROLL_MULTIPLIER = floatPreferencesKey("edge_scroll_multiplier")
        private val EDGE_SCROLL_ACCELERATION_START = longPreferencesKey("edge_scroll_acceleration_start")
        private val EDGE_SCROLL_ACCELERATION_DURATION = longPreferencesKey("edge_scroll_acceleration_duration")
    }

    private inline fun <reified T : Enum<T>> getEnumPreference(
        preferences: Preferences,
        preferencesKey: Preferences.Key<String>,
        defaultValue: T,
        logTag: String
    ): T {
        val valueStr = preferences[preferencesKey]
        return if (valueStr != null) {
            try {
                enumValueOf<T>(valueStr)
            } catch (e: Exception) {
                Logger.w("Invalid $logTag value: $valueStr", e)
                defaultValue
            }
        } else {
            defaultValue
        }
    }

    override fun getSettings(): Flow<OverlaySettings> {
        return dataStore.data
            .catch { exception ->
                Logger.e("Error reading settings: ${exception.message}", exception)
                emit(emptyPreferences())
            }
            .map { preferences ->
                val controlScheme = getEnumPreference(
                    preferences,
                    CONTROL_SCHEME,
                    OverlaySettings.DEFAULT.controlScheme,
                    "control scheme"
                )

                val gestureStyle = getEnumPreference(
                    preferences,
                    GESTURE_STYLE,
                    OverlaySettings.DEFAULT.gestureStyle,
                    "gesture style"
                )

                val gridLineVisibility = getEnumPreference(
                    preferences,
                    GRID_LINE_VISIBILITY,
                    OverlaySettings.DEFAULT.gridLineVisibility,
                    "grid line visibility"
                )

                val cursorEdgeBehavior = getEnumPreference(
                    preferences,
                    CURSOR_EDGE_BEHAVIOR,
                    OverlaySettings.DEFAULT.cursorEdgeBehavior,
                    "cursor edge behavior"
                )

                val cursorImageAlignment = getEnumPreference(
                    preferences,
                    CURSOR_IMAGE_ALIGNMENT,
                    OverlaySettings.DEFAULT.cursorImageAlignment,
                    "cursor image alignment"
                )

                val scrollToggleImageAlignment = getEnumPreference(
                    preferences,
                    SCROLL_TOGGLE_IMAGE_ALIGNMENT,
                    OverlaySettings.DEFAULT.scrollToggleImageAlignment,
                    "scroll toggle image alignment"
                )

                val settings = OverlaySettings(
                    activationDuration = preferences[ACTIVATION_DURATION]
                        ?: OverlaySettings.DEFAULT.activationDuration,
                    gridLevels = preferences[GRID_LEVELS] ?: OverlaySettings.DEFAULT.gridLevels,
                    overlayOpacity = preferences[OVERLAY_OPACITY]
                        ?: OverlaySettings.DEFAULT.overlayOpacity,
                    persistOverlay = preferences[PERSIST_OVERLAY]
                        ?: OverlaySettings.DEFAULT.persistOverlay,
                    hideNumbers = preferences[HIDE_NUMBERS] ?: OverlaySettings.DEFAULT.hideNumbers,
                    gridLineVisibility = gridLineVisibility,
                    useNaturalScrolling = preferences[USE_NATURAL_SCROLLING]
                        ?: OverlaySettings.DEFAULT.useNaturalScrolling,
                    showGestureVisualization = preferences[SHOW_GESTURE_VISUAL]
                        ?: OverlaySettings.DEFAULT.showGestureVisualization,
                    visualSize = preferences[VISUAL_SIZE] ?: OverlaySettings.DEFAULT.visualSize,
                    cursorSpeed = preferences[CURSOR_SPEED] ?: OverlaySettings.DEFAULT.cursorSpeed,
                    cursorAcceleration = preferences[CURSOR_ACCELERATION]
                        ?: OverlaySettings.DEFAULT.cursorAcceleration,
                    cursorSize = preferences[CURSOR_SIZE] ?: OverlaySettings.DEFAULT.cursorSize,
                    cursorAccelerationStart = preferences[CURSOR_ACCELERATION_START]
                        ?: OverlaySettings.DEFAULT.cursorAccelerationStart,
                    cursorAccelerationDuration = preferences[CURSOR_ACCELERATION_DURATION]
                        ?: OverlaySettings.DEFAULT.cursorAccelerationDuration,
                    gridActivationKey = preferences[GRID_ACTIVATION_KEY]
                        ?: OverlaySettings.DEFAULT.gridActivationKey,
                    cursorActivationKey = preferences[CURSOR_ACTIVATION_KEY]
                        ?: OverlaySettings.DEFAULT.cursorActivationKey,
                    controlScheme = controlScheme,
                    cursorEdgeBehavior = cursorEdgeBehavior,
                    gestureStyle = gestureStyle,
                    toggleHold = preferences[TOGGLE_HOLD] ?: OverlaySettings.DEFAULT.toggleHold,
                    scrollDuration = preferences[SCROLL_DURATION]
                        ?: OverlaySettings.DEFAULT.scrollDuration,
                    scrollMultiplier = preferences[SCROLL_MULTIPLIER]
                        ?: OverlaySettings.DEFAULT.scrollMultiplier,
                    zoomDuration = preferences[ZOOM_DURATION]
                        ?: OverlaySettings.DEFAULT.zoomDuration,
                    allowPassthrough = preferences[ALLOW_PASSTHROUGH]
                        ?: OverlaySettings.DEFAULT.allowPassthrough,
                    enableShizukuIntegration = preferences[ENABLE_SHIZUKU_INTEGRATION]
                        ?: OverlaySettings.DEFAULT.enableShizukuIntegration,
                    hideOnKeyboardOpen = preferences[HIDE_ON_KEYBOARD_OPEN]
                        ?: OverlaySettings.DEFAULT.hideOnKeyboardOpen,
                    hideOnLauncherOpen = preferences[HIDE_ON_LAUNCHER_OPEN]
                        ?: OverlaySettings.DEFAULT.hideOnLauncherOpen,
                    hideOnLockScreen = preferences[HIDE_ON_LOCK_SCREEN]
                        ?: OverlaySettings.DEFAULT.hideOnLockScreen,
                    rotateButtonsWithOrientation = preferences[ROTATE_BUTTONS_WITH_ORIENTATION]
                        ?: OverlaySettings.DEFAULT.rotateButtonsWithOrientation,
                    roundedCursorCorners = preferences[ROUNDED_CURSOR_CORNERS]
                        ?: OverlaySettings.DEFAULT.roundedCursorCorners,
                    usePhysicalSize = preferences[USE_PHYSICAL_SIZE]
                        ?: OverlaySettings.DEFAULT.usePhysicalSize,
                    standardCursorHex = preferences[STANDARD_CURSOR_HEX]
                        ?: OverlaySettings.DEFAULT.standardCursorHex,
                    standardCursorMatchBorder = preferences[STANDARD_CURSOR_MATCH_BORDER]
                        ?: OverlaySettings.DEFAULT.standardCursorMatchBorder,
                    allowOverlappingGestures = preferences[ALLOW_OVERLAPPING_GESTURES]
                        ?: OverlaySettings.DEFAULT.allowOverlappingGestures,
                    forceSmootherGestures = preferences[FORCE_SMOOTHER_GESTURES]
                        ?: OverlaySettings.DEFAULT.forceSmootherGestures,
                    cursorImagePath = preferences[CURSOR_IMAGE_PATH]
                        ?: OverlaySettings.DEFAULT.cursorImagePath,
                    scrollToggleImagePath = preferences[SCROLL_TOGGLE_IMAGE_PATH]
                        ?: OverlaySettings.DEFAULT.scrollToggleImagePath,
                    useCustomCursorIcon = preferences[USE_CUSTOM_CURSOR_ICON]
                        ?: OverlaySettings.DEFAULT.useCustomCursorIcon,
                    cursorImageAlignment = cursorImageAlignment,
                    scrollToggleImageAlignment = scrollToggleImageAlignment,
                    useAdvancedScrolling = preferences[USE_ADVANCED_SCROLLING]
                        ?: OverlaySettings.DEFAULT.useAdvancedScrolling,
                    continuousScrollDuration = preferences[CONTINUOUS_SCROLL_DURATION]
                        ?: OverlaySettings.DEFAULT.continuousScrollDuration,
                    continuousScrollMultiplier = preferences[CONTINUOUS_SCROLL_MULTIPLIER]
                        ?: OverlaySettings.DEFAULT.continuousScrollMultiplier,
                    continuousScrollAccelerationStart = preferences[CONTINUOUS_SCROLL_ACCELERATION_START]
                        ?: OverlaySettings.DEFAULT.continuousScrollAccelerationStart,
                    continuousScrollAccelerationDuration = preferences[CONTINUOUS_SCROLL_ACCELERATION_DURATION]
                        ?: OverlaySettings.DEFAULT.continuousScrollAccelerationDuration,
                    edgeScrollDuration = preferences[EDGE_SCROLL_DURATION]
                        ?: OverlaySettings.DEFAULT.edgeScrollDuration,
                    edgeScrollMultiplier = preferences[EDGE_SCROLL_MULTIPLIER]
                        ?: OverlaySettings.DEFAULT.edgeScrollMultiplier,
                    edgeScrollAccelerationStart = preferences[EDGE_SCROLL_ACCELERATION_START]
                        ?: OverlaySettings.DEFAULT.edgeScrollAccelerationStart,
                    edgeScrollAccelerationDuration = preferences[EDGE_SCROLL_ACCELERATION_DURATION]
                        ?: OverlaySettings.DEFAULT.edgeScrollAccelerationDuration
                )

                if (VersionUtil.belowVersion(Build.VERSION_CODES.O)) settings.copy(enableShizukuIntegration = true) else settings
            }
    }

    override suspend fun updateSettings(settings: OverlaySettings) {
        try {
            dataStore.edit { preferences ->
                preferences[ACTIVATION_DURATION] = settings.activationDuration
                preferences[GRID_LEVELS] = settings.gridLevels
                preferences[OVERLAY_OPACITY] = settings.overlayOpacity
                preferences[PERSIST_OVERLAY] = settings.persistOverlay
                preferences[HIDE_NUMBERS] = settings.hideNumbers
                preferences[GRID_LINE_VISIBILITY] = settings.gridLineVisibility.name
                preferences[USE_NATURAL_SCROLLING] = settings.useNaturalScrolling
                preferences[SHOW_GESTURE_VISUAL] = settings.showGestureVisualization
                preferences[VISUAL_SIZE] = settings.visualSize
                preferences[CURSOR_SPEED] = settings.cursorSpeed
                preferences[CURSOR_ACCELERATION] = settings.cursorAcceleration
                preferences[CURSOR_SIZE] = settings.cursorSize
                preferences[CURSOR_ACCELERATION_START] = settings.cursorAccelerationStart
                preferences[CURSOR_ACCELERATION_DURATION] = settings.cursorAccelerationDuration
                preferences[GRID_ACTIVATION_KEY] = settings.gridActivationKey
                preferences[CURSOR_ACTIVATION_KEY] = settings.cursorActivationKey
                preferences[CONTROL_SCHEME] = settings.controlScheme.name
                preferences[CURSOR_EDGE_BEHAVIOR] = settings.cursorEdgeBehavior.name
                preferences[GESTURE_STYLE] = settings.gestureStyle.name
                preferences[TOGGLE_HOLD] = settings.toggleHold
                preferences[SCROLL_DURATION] = settings.scrollDuration
                preferences[SCROLL_MULTIPLIER] = settings.scrollMultiplier
                preferences[ZOOM_DURATION] = settings.zoomDuration
                preferences[ALLOW_PASSTHROUGH] = settings.allowPassthrough
                preferences[ENABLE_SHIZUKU_INTEGRATION] = settings.enableShizukuIntegration
                preferences[HIDE_ON_KEYBOARD_OPEN] = settings.hideOnKeyboardOpen
                preferences[HIDE_ON_LAUNCHER_OPEN] = settings.hideOnLauncherOpen
                preferences[HIDE_ON_LOCK_SCREEN] = settings.hideOnLockScreen
                preferences[ROTATE_BUTTONS_WITH_ORIENTATION] = settings.rotateButtonsWithOrientation
                preferences[ROUNDED_CURSOR_CORNERS] = settings.roundedCursorCorners
                preferences[USE_PHYSICAL_SIZE] = settings.usePhysicalSize
                preferences[STANDARD_CURSOR_HEX] = settings.standardCursorHex
                preferences[STANDARD_CURSOR_MATCH_BORDER] = settings.standardCursorMatchBorder
                preferences[ALLOW_OVERLAPPING_GESTURES] = settings.allowOverlappingGestures
                preferences[FORCE_SMOOTHER_GESTURES] = settings.forceSmootherGestures
                preferences[USE_CUSTOM_CURSOR_ICON] = settings.useCustomCursorIcon
                preferences[CURSOR_IMAGE_ALIGNMENT] = settings.cursorImageAlignment.name
                preferences[SCROLL_TOGGLE_IMAGE_ALIGNMENT] = settings.scrollToggleImageAlignment.name
                preferences[USE_ADVANCED_SCROLLING] = settings.useAdvancedScrolling
                preferences[CONTINUOUS_SCROLL_DURATION] = settings.continuousScrollDuration
                preferences[CONTINUOUS_SCROLL_MULTIPLIER] = settings.continuousScrollMultiplier
                preferences[CONTINUOUS_SCROLL_ACCELERATION_START] = settings.continuousScrollAccelerationStart
                preferences[CONTINUOUS_SCROLL_ACCELERATION_DURATION] = settings.continuousScrollAccelerationDuration
                preferences[EDGE_SCROLL_DURATION] = settings.edgeScrollDuration
                preferences[EDGE_SCROLL_MULTIPLIER] = settings.edgeScrollMultiplier
                preferences[EDGE_SCROLL_ACCELERATION_START] = settings.edgeScrollAccelerationStart
                preferences[EDGE_SCROLL_ACCELERATION_DURATION] = settings.edgeScrollAccelerationDuration

                if (settings.cursorImagePath != null) {
                    preferences[CURSOR_IMAGE_PATH] = settings.cursorImagePath
                } else {
                    preferences.remove(CURSOR_IMAGE_PATH)
                }

                if (settings.scrollToggleImagePath != null) {
                    preferences[SCROLL_TOGGLE_IMAGE_PATH] = settings.scrollToggleImagePath
                } else {
                    preferences.remove(SCROLL_TOGGLE_IMAGE_PATH)
                }
            }
        } catch (e: Exception) {
            Logger.e("Error updating settings", e)
        }
    }

    override suspend fun validateAndUpdateSettings(settings: OverlaySettings): OverlaySettings.ValidationResult {
        try {
            val validationResult = settings.validate()

            val settingsToSave =
                if (validationResult.isValid) {
                    settings
                } else {
                    Logger.w("Saving sanitized settings due to validation errors: ${validationResult.errors}")
                    settings.sanitized()
                }

            updateSettings(settingsToSave)

            return validationResult
        } catch (e: Exception) {
            Logger.e("Error updating settings", e)
            return OverlaySettings.ValidationResult(
                isValid = false,
                errors = listOf("Error updating settings: ${e.message}"),
            )
        }
    }

    fun exportSettings(): String {
        return try {
            runBlocking {
                val preferences = dataStore.data.first()
                val sb = StringBuilder()
                val sortedKeys = preferences.asMap().keys.sortedBy { it.name }
                for (key in sortedKeys) {
                    val value = preferences[key]
                    sb.appendLine("${key.name}=$value")
                }

                sb.toString()
            }
        } catch (e: Exception) {
            Logger.e("Failed to export DataStore contents", e)
            "Failed to export settings: ${e.message}"
        }
    }
}
