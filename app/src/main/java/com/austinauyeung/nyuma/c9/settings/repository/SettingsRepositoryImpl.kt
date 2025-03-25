package com.austinauyeung.nyuma.c9.settings.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.austinauyeung.nyuma.c9.common.domain.GestureStyle
import com.austinauyeung.nyuma.c9.core.util.Logger
import com.austinauyeung.nyuma.c9.grid.domain.GridLineVisibility
import com.austinauyeung.nyuma.c9.settings.domain.ControlScheme
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Persists user settings.
 */
class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    companion object {
        private val GRID_LEVELS = intPreferencesKey("grid_levels")
        private val OVERLAY_OPACITY = intPreferencesKey("overlay_opacity")
        private val PERSIST_OVERLAY = booleanPreferencesKey("persist_overlay")
        private val HIDE_NUMBERS = booleanPreferencesKey("hide_numbers")
        private val GRID_LINE_VISIBILITY = stringPreferencesKey("grid_line_visibility")
        private val USE_NATURAL_SCROLLING = booleanPreferencesKey("use_natural_scrolling")
        private val SHOW_GESTURE_VISUAL = booleanPreferencesKey("show_gesture_visual")
        private val CURSOR_SPEED = intPreferencesKey("cursor_speed")
        private val CURSOR_ACCELERATION = intPreferencesKey("cursor_acceleration")
        private val CURSOR_SIZE = intPreferencesKey("cursor_size")
        private val GRID_ACTIVATION_KEY = intPreferencesKey("grid_activation_key")
        private val CURSOR_ACTIVATION_KEY = intPreferencesKey("cursor_activation_key")
        private val CONTROL_SCHEME = stringPreferencesKey("control_scheme")
        private val CURSOR_WRAP_AROUND = booleanPreferencesKey("cursor_wrap_around")
        private val GESTURE_STYLE = stringPreferencesKey("gesture_style")
        private val TOGGLE_HOLD = booleanPreferencesKey("toggle_hold")
        private val GESTURE_DURATION = longPreferencesKey("gesture_duration")
        private val SCROLL_MULTIPLIER = floatPreferencesKey("scroll_multiplier")
        private val ALLOW_PASSTHROUGH = booleanPreferencesKey("allow_passthrough")
    }

    override fun getSettings(): Flow<OverlaySettings> {
        return dataStore.data
            .catch { exception ->
                Logger.e("Error reading settings: ${exception.message}", exception)
                emit(emptyPreferences())
            }
            .map { preferences ->
                val controlSchemeStr = preferences[CONTROL_SCHEME]
                val controlScheme =
                    if (controlSchemeStr != null) {
                        try {
                            ControlScheme.valueOf(controlSchemeStr)
                        } catch (e: Exception) {
                            Logger.w("Invalid control scheme value: $controlSchemeStr", e)
                            OverlaySettings.DEFAULT.controlScheme
                        }
                    } else {
                        OverlaySettings.DEFAULT.controlScheme
                    }

                val gestureStyleStr = preferences[GESTURE_STYLE]
                val gestureStyle =
                    if (gestureStyleStr != null) {
                        try {
                            GestureStyle.valueOf(gestureStyleStr)
                        } catch (e: Exception) {
                            Logger.w("Invalid scroll mode value: $gestureStyleStr", e)
                            OverlaySettings.DEFAULT.gestureStyle
                        }
                    } else {
                        OverlaySettings.DEFAULT.gestureStyle
                    }

                val gridLineVisibilityStr = preferences[GRID_LINE_VISIBILITY]
                val gridLineVisibility =
                    if (gridLineVisibilityStr != null) {
                        try {
                            GridLineVisibility.valueOf(gridLineVisibilityStr)
                        } catch (e: Exception) {
                            Logger.w("Invalid grid line visibility value: $gridLineVisibilityStr", e)
                            OverlaySettings.DEFAULT.gridLineVisibility
                        }
                    } else {
                        OverlaySettings.DEFAULT.gridLineVisibility
                    }

                OverlaySettings(
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
                    cursorSpeed = preferences[CURSOR_SPEED] ?: OverlaySettings.DEFAULT.cursorSpeed,
                    cursorAcceleration = preferences[CURSOR_ACCELERATION]
                        ?: OverlaySettings.DEFAULT.cursorAcceleration,
                    cursorSize = preferences[CURSOR_SIZE] ?: OverlaySettings.DEFAULT.cursorSize,
                    gridActivationKey = preferences[GRID_ACTIVATION_KEY]
                        ?: OverlaySettings.DEFAULT.gridActivationKey,
                    cursorActivationKey = preferences[CURSOR_ACTIVATION_KEY]
                        ?: OverlaySettings.DEFAULT.cursorActivationKey,
                    controlScheme = controlScheme,
                    cursorWrapAround = preferences[CURSOR_WRAP_AROUND]
                        ?: OverlaySettings.DEFAULT.cursorWrapAround,
                    gestureStyle = gestureStyle,
                    toggleHold = preferences[TOGGLE_HOLD] ?: OverlaySettings.DEFAULT.toggleHold,
                    gestureDuration = preferences[GESTURE_DURATION]
                        ?: OverlaySettings.DEFAULT.gestureDuration,
                    scrollMultiplier = preferences[SCROLL_MULTIPLIER]
                        ?: OverlaySettings.DEFAULT.scrollMultiplier,
                    allowPassthrough = preferences[ALLOW_PASSTHROUGH]
                        ?: OverlaySettings.DEFAULT.allowPassthrough
                )
            }
    }

    override suspend fun updateSettings(settings: OverlaySettings) {
        try {
            dataStore.edit { preferences ->
                preferences[GRID_LEVELS] = settings.gridLevels
                preferences[OVERLAY_OPACITY] = settings.overlayOpacity
                preferences[PERSIST_OVERLAY] = settings.persistOverlay
                preferences[HIDE_NUMBERS] = settings.hideNumbers
                preferences[GRID_LINE_VISIBILITY] = settings.gridLineVisibility.name
                preferences[USE_NATURAL_SCROLLING] = settings.useNaturalScrolling
                preferences[SHOW_GESTURE_VISUAL] = settings.showGestureVisualization
                preferences[CURSOR_SPEED] = settings.cursorSpeed
                preferences[CURSOR_ACCELERATION] = settings.cursorAcceleration
                preferences[CURSOR_SIZE] = settings.cursorSize
                preferences[GRID_ACTIVATION_KEY] = settings.gridActivationKey
                preferences[CURSOR_ACTIVATION_KEY] = settings.cursorActivationKey
                preferences[CONTROL_SCHEME] = settings.controlScheme.name
                preferences[CURSOR_WRAP_AROUND] = settings.cursorWrapAround
                preferences[GESTURE_STYLE] = settings.gestureStyle.name
                preferences[TOGGLE_HOLD] = settings.toggleHold
                preferences[GESTURE_DURATION] = settings.gestureDuration
                preferences[SCROLL_MULTIPLIER] = settings.scrollMultiplier
                preferences[ALLOW_PASSTHROUGH] = settings.allowPassthrough
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
}
