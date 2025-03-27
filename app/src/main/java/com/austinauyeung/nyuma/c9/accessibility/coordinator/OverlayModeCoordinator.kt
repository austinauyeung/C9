package com.austinauyeung.nyuma.c9.accessibility.coordinator

import com.austinauyeung.nyuma.c9.core.logs.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ensures only one overlay is active at a time.
 */
class OverlayModeCoordinator {
    enum class OverlayMode {
        NONE,
        GRID,
        CURSOR,
    }

    private val _activeMode = MutableStateFlow(OverlayMode.NONE)
    val activeMode: StateFlow<OverlayMode> = _activeMode.asStateFlow()

    fun requestActivation(mode: OverlayMode): Boolean {
        val currentMode = _activeMode.value

        if (currentMode == OverlayMode.NONE || currentMode == mode) {
            val newMode = if (currentMode == mode) OverlayMode.NONE else mode
            _activeMode.value = newMode
            Logger.d("Overlay mode changed: $currentMode -> $newMode")
            return true
        }

        Logger.d("Cannot activate $mode, $currentMode is already active")
        return false
    }

    fun deactivate(mode: OverlayMode) {
        if (_activeMode.value == mode) {
            _activeMode.value =
                OverlayMode.NONE
            Logger.d("Overlay mode deactivated: $mode")
        }
    }
}
