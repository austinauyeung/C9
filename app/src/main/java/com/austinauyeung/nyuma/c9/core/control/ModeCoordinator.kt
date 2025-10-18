package com.austinauyeung.nyuma.c9.core.control

import com.austinauyeung.nyuma.c9.core.logs.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ensures only one overlay is active at a time.
 */
class ModeCoordinator {
    enum class OverlayMode {
        OFF,
        AUTOHIDDEN,
        GRID,
        CURSOR
    }

    private val _activeMode = MutableStateFlow(OverlayMode.OFF)
    val activeMode: StateFlow<OverlayMode> = _activeMode.asStateFlow()

    fun requestActivation(mode: OverlayMode): Boolean {
        val currentMode = _activeMode.value
        val mismatch1 = (currentMode == OverlayMode.GRID) && (mode == OverlayMode.CURSOR)
        val mismatch2 = (currentMode == OverlayMode.CURSOR) && (mode == OverlayMode.GRID)
        if (mismatch1 || mismatch2) {
            Logger.d("Cannot activate $mode, $currentMode is already active")
            return false
        }

        val newMode = if (currentMode == mode) OverlayMode.OFF else mode
        _activeMode.value = newMode
        Logger.d("Overlay mode changed: $currentMode -> $newMode")
        return true
    }

    fun deactivate(mode: OverlayMode, fromAutoHide: Boolean) {
        if (_activeMode.value == mode) {
            _activeMode.value = if (fromAutoHide) OverlayMode.AUTOHIDDEN else OverlayMode.OFF
            Logger.d("Overlay mode deactivated: $mode")
        }
    }
}