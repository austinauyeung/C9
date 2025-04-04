package com.austinauyeung.nyuma.c9.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.austinauyeung.nyuma.c9.accessibility.service.OverlayAccessibilityService
import com.austinauyeung.nyuma.c9.common.domain.AutoHideDetection
import com.austinauyeung.nyuma.c9.common.domain.GestureStyle
import com.austinauyeung.nyuma.c9.common.domain.ScreenEdgeBehavior
import com.austinauyeung.nyuma.c9.core.logs.Logger
import com.austinauyeung.nyuma.c9.grid.domain.GridLineVisibility
import com.austinauyeung.nyuma.c9.settings.domain.ControlScheme
import com.austinauyeung.nyuma.c9.settings.domain.Defaults
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings
import com.austinauyeung.nyuma.c9.settings.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Bridges settings with UI.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _validationErrors = MutableStateFlow<List<String>>(emptyList())
    val validationErrors: StateFlow<List<String>> = _validationErrors.asStateFlow()

    private var toastFunction: ((String) -> Unit)? = null

    fun setToastFunction(toastFn: (String) -> Unit) {
        toastFunction = toastFn
    }

    fun showToast(message: String) {
        toastFunction?.invoke(message)
    }

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                settingsRepository.getSettings().collect { settings ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            gridLevels = settings.gridLevels,
                            overlayOpacity = settings.overlayOpacity,
                            persistOverlay = settings.persistOverlay,
                            hideNumbers = settings.hideNumbers,
                            gridLineVisibility = settings.gridLineVisibility,
                            useNaturalScrolling = settings.useNaturalScrolling,
                            showGestureVisualization = settings.showGestureVisualization,
                            visualSize = settings.visualSize,
                            cursorSpeed = settings.cursorSpeed,
                            cursorAcceleration = settings.cursorAcceleration,
                            cursorSize = settings.cursorSize,
                            cursorAccelerationThreshold = settings.cursorAccelerationThreshold,
                            gridActivationKey = settings.gridActivationKey,
                            cursorActivationKey = settings.cursorActivationKey,
                            controlScheme = settings.controlScheme,
                            cursorEdgeBehavior = settings.cursorEdgeBehavior,
                            gestureStyle = settings.gestureStyle,
                            toggleHold = settings.toggleHold,
                            gestureDuration = settings.gestureDuration,
                            scrollMultiplier = settings.scrollMultiplier,
                            allowPassthrough = settings.allowPassthrough,
                            enableShizukuIntegration = settings.enableShizukuIntegration,
                            hideOnTextField = settings.hideOnTextField,
                            rotateButtonsWithOrientation = settings.rotateButtonsWithOrientation,
                            roundedCursorCorners = settings.roundedCursorCorners,
                            usePhysicalSize = settings.usePhysicalSize
                        )
                    }
                }
            } catch (error: Exception) {
                Logger.e("Failed to load settings", error)
                _uiState.update {
                    it.copy(
                        showError = true,
                        errorMessage = "Failed to load settings"
                    )
                }
            }
        }
    }

    private fun updateSettings(settingsUpdater: (OverlaySettings) -> OverlaySettings) {
        viewModelScope.launch {
            val currentSettings = createSettingsFromUiState()
            val updatedSettings = settingsUpdater(currentSettings)
            val result = settingsRepository.validateAndUpdateSettings(updatedSettings)

            if (result.isValid) {
                _validationErrors.value = emptyList()
                _uiState.update { it.copy(showInvalidSettingError = false) }
            } else {
                _validationErrors.value = result.errors
                _uiState.update { it.copy(showInvalidSettingError = true) }
            }
        }
    }

    private fun createSettingsFromUiState(): OverlaySettings {
        return OverlaySettings(
            gridLevels = _uiState.value.gridLevels,
            overlayOpacity = _uiState.value.overlayOpacity,
            persistOverlay = _uiState.value.persistOverlay,
            hideNumbers = _uiState.value.hideNumbers,
            gridLineVisibility = _uiState.value.gridLineVisibility,
            useNaturalScrolling = _uiState.value.useNaturalScrolling,
            showGestureVisualization = _uiState.value.showGestureVisualization,
            visualSize = _uiState.value.visualSize,
            cursorSpeed = _uiState.value.cursorSpeed,
            cursorAcceleration = _uiState.value.cursorAcceleration,
            cursorSize = _uiState.value.cursorSize,
            cursorAccelerationThreshold = _uiState.value.cursorAccelerationThreshold,
            gridActivationKey = _uiState.value.gridActivationKey,
            cursorActivationKey = _uiState.value.cursorActivationKey,
            controlScheme = _uiState.value.controlScheme,
            cursorEdgeBehavior = _uiState.value.cursorEdgeBehavior,
            gestureStyle = _uiState.value.gestureStyle,
            toggleHold = _uiState.value.toggleHold,
            gestureDuration = _uiState.value.gestureDuration,
            scrollMultiplier = _uiState.value.scrollMultiplier,
            allowPassthrough = _uiState.value.allowPassthrough,
            enableShizukuIntegration = _uiState.value.enableShizukuIntegration,
            hideOnTextField = _uiState.value.hideOnTextField,
            rotateButtonsWithOrientation = _uiState.value.rotateButtonsWithOrientation,
            roundedCursorCorners = _uiState.value.roundedCursorCorners,
            usePhysicalSize = _uiState.value.usePhysicalSize
        )
    }

    fun <T> updatePreference(value: T, updater: (OverlaySettings, T) -> OverlaySettings) {
        updateSettings { settings -> updater(settings, value) }
    }

    fun updateAccessibilityServiceStatus(isEnabled: Boolean) {
        _uiState.update { it.copy(isAccessibilityServiceEnabled = isEnabled) }
    }

    fun updateGridActivationKey(keyCode: Int) {
        updateSettings { it.copy(gridActivationKey = keyCode) }
    }

    fun updateCursorActivationKey(keyCode: Int) {
        updateSettings { it.copy(cursorActivationKey = keyCode) }
    }

    fun requestHideAllOverlays() {
        val serviceInstance = OverlayAccessibilityService.getInstance()
        serviceInstance?.forceHideAllOverlays()
    }

    fun updateAllowPassthrough(allow: Boolean) {
        updateSettings { it.copy(allowPassthrough = allow) }
    }

    fun updateEnableShizukuIntegration(integrate: Boolean) {
        updateSettings { it.copy(enableShizukuIntegration = integrate) }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

data class SettingsUiState(
    val gridLevels: Int = Defaults.Settings.GRID_LEVELS,
    val overlayOpacity: Int = Defaults.Settings.OVERLAY_OPACITY,
    val persistOverlay: Boolean = Defaults.Settings.PERSIST_OVERLAY,
    val isAccessibilityServiceEnabled: Boolean = false,
    val showInvalidSettingError: Boolean = false,
    val isServiceRunning: Boolean = false,
    val hideNumbers: Boolean = Defaults.Settings.HIDE_NUMBERS,
    val gridLineVisibility: GridLineVisibility = Defaults.Settings.GRID_LINE_VISIBILITY,
    val useNaturalScrolling: Boolean = Defaults.Settings.USE_NATURAL_SCROLLING,
    val showGestureVisualization: Boolean = Defaults.Settings.SHOW_GESTURE_VISUAL,
    val visualSize: Int = Defaults.Settings.VISUAL_SIZE,
    val showError: Boolean = false,
    val errorMessage: String = "",
    val cursorSpeed: Int = Defaults.Settings.CURSOR_SPEED,
    val cursorAcceleration: Int = Defaults.Settings.CURSOR_ACCELERATION,
    val cursorSize: Int = Defaults.Settings.CURSOR_SIZE,
    val cursorAccelerationThreshold: Long = Defaults.Settings.CURSOR_ACCELERATION_THRESHOLD,
    val gridActivationKey: Int = Defaults.Settings.GRID_ACTIVATION_KEY,
    val cursorActivationKey: Int = Defaults.Settings.CURSOR_ACTIVATION_KEY,
    val controlScheme: ControlScheme = Defaults.Settings.CONTROL_SCHEME,
    val cursorEdgeBehavior: ScreenEdgeBehavior = Defaults.Settings.CURSOR_EDGE_BEHAVIOR,
    val gestureStyle: GestureStyle = Defaults.Settings.GESTURE_STYLE,
    val toggleHold: Boolean = Defaults.Settings.TOGGLE_HOLD,
    val gestureDuration: Long = Defaults.Settings.GESTURE_DURATION,
    val scrollMultiplier: Float = Defaults.Settings.SCROLL_MULTIPLIER,
    val allowPassthrough: Boolean = Defaults.Settings.ALLOW_PASSTHROUGH,
    val enableShizukuIntegration: Boolean = Defaults.Settings.ENABLE_SHIZUKU_INTEGRATION,
    val hideOnTextField: AutoHideDetection = Defaults.Settings.HIDE_ON_TEXT_FIELD,
    val rotateButtonsWithOrientation: Boolean = Defaults.Settings.ROTATE_BUTTONS_WITH_ORIENTATION,
    val roundedCursorCorners: Boolean = Defaults.Settings.ROUNDED_CURSOR_CORNERS,
    val usePhysicalSize: Boolean = Defaults.Settings.USE_PHYSICAL_SIZE
)
