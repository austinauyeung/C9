package com.austinauyeung.nyuma.c9.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.austinauyeung.nyuma.c9.accessibility.AppAccessibilityService
import com.austinauyeung.nyuma.c9.core.domain.GestureStyle
import com.austinauyeung.nyuma.c9.core.domain.ScreenEdgeBehavior
import com.austinauyeung.nyuma.c9.core.logs.Logger
import com.austinauyeung.nyuma.c9.cursor.domain.ControlScheme
import com.austinauyeung.nyuma.c9.cursor.domain.IconAlignment
import com.austinauyeung.nyuma.c9.grid.domain.GridLineVisibility
import com.austinauyeung.nyuma.c9.settings.domain.AppListType
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
class SettingsState(
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
                            activationDuration = settings.activationDuration,
                            gridLevels = settings.gridLevels,
                            persistOverlay = settings.persistOverlay,
                            hideNumbers = settings.hideNumbers,
                            gridLineVisibility = settings.gridLineVisibility,
                            useNaturalScrolling = settings.useNaturalScrolling,
                            showGestureVisualization = settings.showGestureVisualization,
                            visualSize = settings.visualSize,
                            cursorSpeed = settings.cursorSpeed,
                            cursorAcceleration = settings.cursorAcceleration,
                            cursorSize = settings.cursorSize,
                            cursorAccelerationStart = settings.cursorAccelerationStart,
                            cursorAccelerationDuration = settings.cursorAccelerationDuration,
                            gridActivationKey = settings.gridActivationKey,
                            cursorActivationKey = settings.cursorActivationKey,
                            scrollUpKey = settings.scrollUpKey,
                            scrollDownKey = settings.scrollDownKey,
                            scrollLeftKey = settings.scrollLeftKey,
                            scrollRightKey = settings.scrollRightKey,
                            controlScheme = settings.controlScheme,
                            cursorEdgeBehavior = settings.cursorEdgeBehavior,
                            gestureStyle = settings.gestureStyle,
                            toggleHold = settings.toggleHold,
                            scrollDuration = settings.scrollDuration,
                            scrollMultiplier = settings.scrollMultiplier,
                            zoomDuration = settings.zoomDuration,
                            zoomFactor = settings.zoomFactor,
                            allowPassthrough = settings.allowPassthrough,
                            enableShizukuIntegration = settings.enableShizukuIntegration,
                            overrideAndroid7 = settings.overrideAndroid7,
                            hideOnKeyboardOpen = settings.hideOnKeyboardOpen,
                            hideOnLauncherOpen = settings.hideOnLauncherOpen,
                            hideOnLockScreen = settings.hideOnLockScreen,
                            rotateButtonsWithOrientation = settings.rotateButtonsWithOrientation,
                            roundedCursorCorners = settings.roundedCursorCorners,
                            usePhysicalSize = settings.usePhysicalSize,
                            standardCursorHex = settings.standardCursorHex,
                            standardCursorMatchBorder = settings.standardCursorMatchBorder,
                            allowOverlappingGestures = settings.allowOverlappingGestures,
                            forceSmootherGestures = settings.forceSmootherGestures,
                            cursorImagePath = settings.cursorImagePath,
                            clickableImagePath = settings.clickableImagePath,
                            scrollToggleImagePath = settings.scrollToggleImagePath,
                            useCustomCursorIcon = settings.useCustomCursorIcon,
                            cursorImageAlignment = settings.cursorImageAlignment,
                            clickableImageAlignment = settings.clickableImageAlignment,
                            scrollToggleImageAlignment = settings.scrollToggleImageAlignment,
                            useAdvancedScrolling = settings.useAdvancedScrolling,
                            continuousScrollDuration = settings.continuousScrollDuration,
                            continuousScrollMultiplier = settings.continuousScrollMultiplier,
                            continuousScrollAccelerationStart = settings.continuousScrollAccelerationStart,
                            continuousScrollAccelerationDuration = settings.continuousScrollAccelerationDuration,
                            edgeScrollDuration = settings.edgeScrollDuration,
                            edgeScrollMultiplier = settings.edgeScrollMultiplier,
                            edgeScrollAccelerationStart = settings.edgeScrollAccelerationStart,
                            edgeScrollAccelerationDuration = settings.edgeScrollAccelerationDuration,
                            collectLogs = settings.collectLogs,
                            autoHideApps = settings.autoHideApps,
                            clickableApps = settings.clickableApps,
                            showNotification = settings.showNotification,
                            applicationListType = settings.applicationListType,
                            clickableListType = settings.clickableListType,
                            ignoreNumpad = settings.ignoreNumpad,
                            checkClickable = settings.checkClickable,
                            keepCurrentGridTransparent = settings.keepCurrentGridTransparent,
                            gridCursorBackgroundHex = settings.gridCursorBackgroundHex,
                            gridCursorLinesHex = settings.gridCursorLinesHex,
                            gridCursorNumbersHex = settings.gridCursorNumbersHex,
                            gridCursorLineWidth = settings.gridCursorLineWidth,
                            gridCursorFontSize = settings.gridCursorFontSize,
                            disableTouchscreen = settings.disableTouchscreen
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
            activationDuration = _uiState.value.activationDuration,
            gridLevels = _uiState.value.gridLevels,
            persistOverlay = _uiState.value.persistOverlay,
            hideNumbers = _uiState.value.hideNumbers,
            gridLineVisibility = _uiState.value.gridLineVisibility,
            useNaturalScrolling = _uiState.value.useNaturalScrolling,
            showGestureVisualization = _uiState.value.showGestureVisualization,
            visualSize = _uiState.value.visualSize,
            cursorSpeed = _uiState.value.cursorSpeed,
            cursorAcceleration = _uiState.value.cursorAcceleration,
            cursorSize = _uiState.value.cursorSize,
            cursorAccelerationStart = _uiState.value.cursorAccelerationStart,
            cursorAccelerationDuration = _uiState.value.cursorAccelerationDuration,
            gridActivationKey = _uiState.value.gridActivationKey,
            cursorActivationKey = _uiState.value.cursorActivationKey,
            scrollUpKey = _uiState.value.scrollUpKey,
            scrollDownKey = _uiState.value.scrollDownKey,
            scrollLeftKey = _uiState.value.scrollLeftKey,
            scrollRightKey = _uiState.value.scrollRightKey,
            controlScheme = _uiState.value.controlScheme,
            cursorEdgeBehavior = _uiState.value.cursorEdgeBehavior,
            gestureStyle = _uiState.value.gestureStyle,
            toggleHold = _uiState.value.toggleHold,
            scrollDuration = _uiState.value.scrollDuration,
            scrollMultiplier = _uiState.value.scrollMultiplier,
            zoomDuration = _uiState.value.zoomDuration,
            zoomFactor = _uiState.value.zoomFactor,
            allowPassthrough = _uiState.value.allowPassthrough,
            enableShizukuIntegration = _uiState.value.enableShizukuIntegration,
            overrideAndroid7 = _uiState.value.overrideAndroid7,
            hideOnKeyboardOpen = _uiState.value.hideOnKeyboardOpen,
            hideOnLauncherOpen = _uiState.value.hideOnLauncherOpen,
            hideOnLockScreen = _uiState.value.hideOnLockScreen,
            rotateButtonsWithOrientation = _uiState.value.rotateButtonsWithOrientation,
            roundedCursorCorners = _uiState.value.roundedCursorCorners,
            usePhysicalSize = _uiState.value.usePhysicalSize,
            standardCursorHex = _uiState.value.standardCursorHex,
            standardCursorMatchBorder = _uiState.value.standardCursorMatchBorder,
            allowOverlappingGestures = _uiState.value.allowOverlappingGestures,
            forceSmootherGestures = _uiState.value.forceSmootherGestures,
            cursorImagePath = _uiState.value.cursorImagePath,
            clickableImagePath = _uiState.value.clickableImagePath,
            scrollToggleImagePath = _uiState.value.scrollToggleImagePath,
            useCustomCursorIcon = _uiState.value.useCustomCursorIcon,
            cursorImageAlignment = _uiState.value.cursorImageAlignment,
            clickableImageAlignment = _uiState.value.clickableImageAlignment,
            scrollToggleImageAlignment = _uiState.value.scrollToggleImageAlignment,
            useAdvancedScrolling = _uiState.value.useAdvancedScrolling,
            continuousScrollDuration = _uiState.value.continuousScrollDuration,
            continuousScrollMultiplier = _uiState.value.continuousScrollMultiplier,
            continuousScrollAccelerationStart = _uiState.value.continuousScrollAccelerationStart,
            continuousScrollAccelerationDuration = _uiState.value.continuousScrollAccelerationDuration,
            edgeScrollDuration = _uiState.value.edgeScrollDuration,
            edgeScrollMultiplier = _uiState.value.edgeScrollMultiplier,
            edgeScrollAccelerationStart = _uiState.value.edgeScrollAccelerationStart,
            edgeScrollAccelerationDuration = _uiState.value.edgeScrollAccelerationDuration,
            collectLogs = _uiState.value.collectLogs,
            autoHideApps = _uiState.value.autoHideApps,
            clickableApps = _uiState.value.clickableApps,
            showNotification = _uiState.value.showNotification,
            applicationListType = _uiState.value.applicationListType,
            clickableListType = _uiState.value.clickableListType,
            ignoreNumpad = _uiState.value.ignoreNumpad,
            checkClickable = _uiState.value.checkClickable,
            keepCurrentGridTransparent = _uiState.value.keepCurrentGridTransparent,
            gridCursorBackgroundHex = _uiState.value.gridCursorBackgroundHex,
            gridCursorLinesHex = _uiState.value.gridCursorLinesHex,
            gridCursorNumbersHex = _uiState.value.gridCursorNumbersHex,
            gridCursorLineWidth = _uiState.value.gridCursorLineWidth,
            gridCursorFontSize = _uiState.value.gridCursorFontSize,
            disableTouchscreen = _uiState.value.disableTouchscreen
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

    fun updateScrollUpKey(keyCode: Int) {
        updateSettings { it.copy(scrollUpKey = keyCode) }
    }

    fun updateScrollDownKey(keyCode: Int) {
        updateSettings { it.copy(scrollDownKey = keyCode) }
    }

    fun updateScrollLeftKey(keyCode: Int) {
        updateSettings { it.copy(scrollLeftKey = keyCode) }
    }

    fun updateScrollRightKey(keyCode: Int) {
        updateSettings { it.copy(scrollRightKey = keyCode) }
    }

    fun requestHideAllOverlays() {
        val serviceInstance = AppAccessibilityService.getInstance()
        serviceInstance?.forceHideAllOverlays(false)
    }

    fun updateAllowPassthrough(allow: Boolean) {
        updateSettings { it.copy(allowPassthrough = allow) }
    }

    fun updateDisableTouchscreen(disable: Boolean) {
        updateSettings { it.copy(disableTouchscreen = disable) }
    }

    fun updateEnableShizukuIntegration(integrate: Boolean) {
        updateSettings { it.copy(enableShizukuIntegration = integrate) }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsState::class.java)) {
                return SettingsState(settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

data class SettingsUiState(
    val activationDuration: Long = Defaults.Settings.ACTIVATION_DURATION,
    val gridLevels: Int = Defaults.Settings.GRID_LEVELS,
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
    val cursorAccelerationStart: Long = Defaults.Settings.CURSOR_ACCELERATION_START,
    val cursorAccelerationDuration: Long = Defaults.Settings.CURSOR_ACCELERATION_DURATION,
    val gridActivationKey: Int = Defaults.Settings.GRID_ACTIVATION_KEY,
    val cursorActivationKey: Int = Defaults.Settings.CURSOR_ACTIVATION_KEY,
    val scrollUpKey: Int = Defaults.Settings.SCROLL_UP_KEY,
    val scrollDownKey: Int = Defaults.Settings.SCROLL_DOWN_KEY,
    val scrollLeftKey: Int = Defaults.Settings.SCROLL_LEFT_KEY,
    val scrollRightKey: Int = Defaults.Settings.SCROLL_RIGHT_KEY,
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
    val overrideAndroid7: Boolean = Defaults.Settings.OVERRIDE_ANDROID_7,
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
    val clickableImagePath: String? = Defaults.Settings.CLICKABLE_IMAGE_PATH,
    val scrollToggleImagePath: String? = Defaults.Settings.SCROLL_TOGGLE_IMAGE_PATH,
    val useCustomCursorIcon: Boolean = Defaults.Settings.USE_CUSTOM_CURSOR_ICON,
    val cursorImageAlignment: IconAlignment = Defaults.Settings.CURSOR_IMAGE_ALIGNMENT,
    val clickableImageAlignment: IconAlignment = Defaults.Settings.CLICKABLE_IMAGE_ALIGNMENT,
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
    val clickableApps: Set<String> = Defaults.Settings.CLICKABLE_APPS,
    val showNotification: Boolean = Defaults.Settings.SHOW_NOTIFICATION,
    val applicationListType: AppListType = Defaults.Settings.APPLICATION_LIST_TYPE,
    val clickableListType: AppListType = Defaults.Settings.CLICKABLE_LIST_TYPE,
    val ignoreNumpad: Boolean = Defaults.Settings.IGNORE_NUMPAD,
    val checkClickable: Boolean = Defaults.Settings.CHECK_CLICKABLE,
    val keepCurrentGridTransparent: Boolean = Defaults.Settings.KEEP_CURRENT_GRID_TRANSPARENT,
    val gridCursorBackgroundHex: String = Defaults.Settings.GRID_CURSOR_BACKGROUND_HEX,
    val gridCursorLinesHex: String = Defaults.Settings.GRID_CURSOR_LINES_HEX,
    val gridCursorNumbersHex: String = Defaults.Settings.GRID_CURSOR_NUMBERS_HEX,
    val gridCursorLineWidth: Int = Defaults.Settings.GRID_CURSOR_LINE_WIDTH,
    val gridCursorFontSize: Int = Defaults.Settings.GRID_CURSOR_FONT_SIZE,
    val disableTouchscreen: Boolean = Defaults.Settings.DISABLE_TOUCHSCREEN
)
