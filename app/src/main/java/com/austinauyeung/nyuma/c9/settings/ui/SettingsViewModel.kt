package com.austinauyeung.nyuma.c9.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.austinauyeung.nyuma.c9.accessibility.service.OverlayAccessibilityService
import com.austinauyeung.nyuma.c9.common.domain.GestureStyle
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
                            cursorSpeed = settings.cursorSpeed,
                            cursorAcceleration = settings.cursorAcceleration,
                            cursorSize = settings.cursorSize,
                            cursorAccelerationThreshold = settings.cursorAccelerationThreshold,
                            gridActivationKey = settings.gridActivationKey,
                            cursorActivationKey = settings.cursorActivationKey,
                            controlScheme = settings.controlScheme,
                            cursorWrapAround = settings.cursorWrapAround,
                            gestureStyle = settings.gestureStyle,
                            toggleHold = settings.toggleHold,
                            gestureDuration = settings.gestureDuration,
                            scrollMultiplier = settings.scrollMultiplier,
                            allowPassthrough = settings.allowPassthrough,
                            enableShizukuIntegration = settings.enableShizukuIntegration,
                            hideOnTextField = settings.hideOnTextField,
                            rotateButtonsWithOrientation = settings.rotateButtonsWithOrientation
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
            cursorSpeed = _uiState.value.cursorSpeed,
            cursorAcceleration = _uiState.value.cursorAcceleration,
            cursorSize = _uiState.value.cursorSize,
            cursorAccelerationThreshold = _uiState.value.cursorAccelerationThreshold,
            gridActivationKey = _uiState.value.gridActivationKey,
            cursorActivationKey = _uiState.value.cursorActivationKey,
            controlScheme = _uiState.value.controlScheme,
            cursorWrapAround = _uiState.value.cursorWrapAround,
            gestureStyle = _uiState.value.gestureStyle,
            toggleHold = _uiState.value.toggleHold,
            gestureDuration = _uiState.value.gestureDuration,
            scrollMultiplier = _uiState.value.scrollMultiplier,
            allowPassthrough = _uiState.value.allowPassthrough,
            enableShizukuIntegration = _uiState.value.enableShizukuIntegration,
            hideOnTextField = _uiState.value.hideOnTextField,
            rotateButtonsWithOrientation = _uiState.value.rotateButtonsWithOrientation
        )
    }

    fun updateGridLevels(levels: Int) {
        updateSettings { it.copy(gridLevels = levels) }
    }

    fun updateOverlayOpacity(opacity: Int) {
        updateSettings { it.copy(overlayOpacity = opacity) }
    }

    fun updatePersistOverlay(persist: Boolean) {
        updateSettings { it.copy(persistOverlay = persist) }
    }

    fun updateHideNumbers(hide: Boolean) {
        updateSettings { it.copy(hideNumbers = hide) }
    }

    fun updateAccessibilityServiceStatus(isEnabled: Boolean) {
        _uiState.update { it.copy(isAccessibilityServiceEnabled = isEnabled) }
    }

    fun updateNaturalScrolling(useNatural: Boolean) {
        updateSettings { it.copy(useNaturalScrolling = useNatural) }
    }

    fun updateGestureVisualization(useVisual: Boolean) {
        updateSettings { it.copy(showGestureVisualization = useVisual) }
    }

    fun updateCursorSpeed(speed: Int) {
        updateSettings { it.copy(cursorSpeed = speed) }
    }

    fun updateCursorAcceleration(acc: Int) {
        updateSettings { it.copy(cursorAcceleration = acc) }
    }

    fun updateCursorSize(size: Int) {
        updateSettings { it.copy(cursorSize = size) }
    }

    fun updateCursorAccelerationThreshold(threshold: Long) {
        updateSettings { it.copy(cursorAccelerationThreshold = threshold) }
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

    fun updateControlScheme(scheme: ControlScheme) {
        updateSettings { it.copy(controlScheme = scheme) }
    }

    fun updateCursorWrapAround(wrapAround: Boolean) {
        updateSettings { it.copy(cursorWrapAround = wrapAround) }
    }

    fun updateGestureStyle(type: GestureStyle) {
        updateSettings { it.copy(gestureStyle = type) }
    }

    fun updateToggleHold(hold: Boolean) {
        updateSettings { it.copy(toggleHold = hold) }
    }

    fun updateGestureDuration(duration: Long) {
        updateSettings { it.copy(gestureDuration = duration) }
    }

    fun updateScrollMultiplier(mult: Float) {
        updateSettings { it.copy(scrollMultiplier = mult) }
    }

    fun updateAllowPassthrough(allow: Boolean) {
        updateSettings { it.copy(allowPassthrough = allow) }
    }

    fun updateGridLineVisibility(visibility: GridLineVisibility) {
        updateSettings { it.copy(gridLineVisibility = visibility) }
    }

    fun updateEnableShizukuIntegration(integrate: Boolean) {
        updateSettings { it.copy(enableShizukuIntegration = integrate) }
    }

    fun updateHideOnTextField(hide: Boolean) {
        updateSettings { it.copy(hideOnTextField = hide) }
    }

    fun updateRotateButtons(rotate: Boolean) {
        updateSettings { it.copy(rotateButtonsWithOrientation = rotate) }
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
    val showError: Boolean = false,
    val errorMessage: String = "",
    val cursorSpeed: Int = Defaults.Settings.CURSOR_SPEED,
    val cursorAcceleration: Int = Defaults.Settings.CURSOR_ACCELERATION,
    val cursorSize: Int = Defaults.Settings.CURSOR_SIZE,
    val cursorAccelerationThreshold: Long = Defaults.Settings.CURSOR_ACCELERATION_THRESHOLD,
    val gridActivationKey: Int = Defaults.Settings.GRID_ACTIVATION_KEY,
    val cursorActivationKey: Int = Defaults.Settings.CURSOR_ACTIVATION_KEY,
    val controlScheme: ControlScheme = Defaults.Settings.CONTROL_SCHEME,
    val cursorWrapAround: Boolean = Defaults.Settings.CURSOR_WRAP_AROUND,
    val gestureStyle: GestureStyle = Defaults.Settings.GESTURE_STYLE,
    val toggleHold: Boolean = Defaults.Settings.TOGGLE_HOLD,
    val gestureDuration: Long = Defaults.Settings.GESTURE_DURATION,
    val scrollMultiplier: Float = Defaults.Settings.SCROLL_MULTIPLIER,
    val allowPassthrough: Boolean = Defaults.Settings.ALLOW_PASSTHROUGH,
    val enableShizukuIntegration: Boolean = Defaults.Settings.ENABLE_SHIZUKU_INTEGRATION,
    val hideOnTextField: Boolean = Defaults.Settings.HIDE_ON_TEXT_FIELD,
    val rotateButtonsWithOrientation: Boolean = Defaults.Settings.ROTATE_BUTTONS_WITH_ORIENTATION
)
