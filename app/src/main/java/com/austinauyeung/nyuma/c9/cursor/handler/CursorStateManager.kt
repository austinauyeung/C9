package com.austinauyeung.nyuma.c9.cursor.handler

import androidx.compose.ui.geometry.Offset
import com.austinauyeung.nyuma.c9.common.domain.ScreenDimensions
import com.austinauyeung.nyuma.c9.core.constants.CursorConstants
import com.austinauyeung.nyuma.c9.core.logs.Logger
import com.austinauyeung.nyuma.c9.cursor.domain.CursorDirection
import com.austinauyeung.nyuma.c9.cursor.domain.CursorState
import com.austinauyeung.nyuma.c9.settings.domain.ControlScheme
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Manages the cursor state, including position, visibility and mode.
 */
class CursorStateManager(
    private val settingsFlow: StateFlow<OverlaySettings>,
    private val dimensionsFlow: StateFlow<ScreenDimensions>,
    private val onCursorStateChanged: (CursorState?) -> Unit
) {
    private val _cursorState = MutableStateFlow<CursorState?>(null)
    val cursorState: StateFlow<CursorState?> = _cursorState.asStateFlow()

    init {
        _cursorState
            .onEach { newCursorState ->
                onCursorStateChanged(newCursorState)
            }
            .launchIn(CoroutineScope(Dispatchers.Main + SupervisorJob()))
    }

    fun isCursorVisible(): Boolean = _cursorState.value != null

    fun toggleCursorVisibility() {
        if (_cursorState.value == null) {
            showCursor()
        } else {
            hideCursor()
        }
    }

    // Only applies to toggle schema
    fun toggleScrollMode(): Boolean {
        val settings = settingsFlow.value
        if (settings.controlScheme != ControlScheme.TOGGLE_MODE || !isCursorVisible()) {
            return false
        }

        _cursorState.update { currentState ->
            currentState?.copy(
                inScrollMode = !currentState.inScrollMode,
                isHoldActive = if (!currentState.inScrollMode) false else currentState.isHoldActive
            )
        }

        Logger.d("Toggled cursor mode to: ${if (_cursorState.value?.inScrollMode == true) "scroll" else "move"}")

        return true
    }

    fun isInScrollMode(): Boolean = _cursorState.value?.inScrollMode == true

    private fun showCursor() {
        val dimensions = dimensionsFlow.value
        val (centerX, centerY) = dimensions.center()
        val initialPosition = Offset(centerX, centerY)

        val newCursor = CursorState(
            position = initialPosition,
            isVisible = true,
            inScrollMode = false
        )

        updateCursor(newCursor)
    }

    fun hideCursor() {
        updateCursor(null)
    }

    fun updatePosition(position: Offset): CursorState? {
        _cursorState.update { currentState ->
            currentState?.copy(position = position)
        }

        return _cursorState.value
    }

    fun updateHoldState(isHoldActive: Boolean): CursorState? {
        _cursorState.update { currentState ->
            currentState?.copy(
                isHoldActive = isHoldActive,
                inScrollMode = if (isHoldActive) false else currentState.inScrollMode
            )
        }

        return _cursorState.value
    }

    private fun updateCursor(cursor: CursorState?) {
        _cursorState.value = cursor
    }

    fun calculateFrameSpeed(timeHeld: Long): Float {
        val dimensions = dimensionsFlow.value
        val settings = settingsFlow.value
        val currentExponent = settings.cursorSpeed.toFloat().pow(CursorConstants.DEFAULT_EXPONENT)
        val maxExponent = CursorConstants.MAX_SPEED.toFloat().pow(CursorConstants.DEFAULT_EXPONENT)
        val baseSpeed = CursorConstants.DEFAULT_SPEED_MULTIPLIER * currentExponent

        val totalPixels = dimensions.width * dimensions.height
        val screenScaleFactor = sqrt(totalPixels.toFloat()) / 1000f

        val speed = if (timeHeld > settings.cursorAccelerationThreshold) {
            baseSpeed + (maxExponent * CursorConstants.DEFAULT_ACCELERATION_MULTIPLIER - currentExponent * CursorConstants.DEFAULT_SPEED_MULTIPLIER) * (settings.cursorAcceleration - 1) / (CursorConstants.MAX_ACCELERATION - CursorConstants.MIN_ACCELERATION)
        } else {
            baseSpeed
        }

        // Pixels per second * seconds per frame = pixels per frame
        val frameSpeed = speed * (CursorConstants.FRAME_DURATION_MS / 1000f) * screenScaleFactor

        return frameSpeed
    }

    fun calculateMovement(direction: CursorDirection, timeHeld: Long): Offset {
        val frameSpeed = calculateFrameSpeed(timeHeld)

        return when (direction) {
            CursorDirection.UP -> Offset(0f, -frameSpeed)
            CursorDirection.DOWN -> Offset(0f, frameSpeed)
            CursorDirection.LEFT -> Offset(-frameSpeed, 0f)
            CursorDirection.RIGHT -> Offset(frameSpeed, 0f)
        }
    }

    fun applyMovement(delta: Offset): Offset {
        val dimensions = dimensionsFlow.value
        val currentState = _cursorState.value ?: return Offset.Zero
        val settings = settingsFlow.value

        val currentPosition = currentState.position
        val newX = currentPosition.x + delta.x
        val newY = currentPosition.y + delta.y

        return if (settings.cursorWrapAround) {
            Offset(
                x = when {
                    newX < 0 -> dimensions.width.toFloat()
                    newX > dimensions.width -> 0f
                    else -> newX
                },
                y = when {
                    newY < 0 -> dimensions.height.toFloat()
                    newY > dimensions.height -> 0f
                    else -> newY
                }
            )
        } else {
            dimensions.constrainToBounds(newX, newY).let { (x, y) -> Offset(x, y) }
        }
    }
}