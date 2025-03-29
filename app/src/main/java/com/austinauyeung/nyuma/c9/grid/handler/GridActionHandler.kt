package com.austinauyeung.nyuma.c9.grid.handler

import android.view.KeyEvent
import com.austinauyeung.nyuma.c9.BuildConfig
import com.austinauyeung.nyuma.c9.accessibility.coordinator.OverlayModeCoordinator
import com.austinauyeung.nyuma.c9.common.domain.GestureStyle
import com.austinauyeung.nyuma.c9.common.domain.ScrollDirection
import com.austinauyeung.nyuma.c9.core.constants.ApplicationConstants
import com.austinauyeung.nyuma.c9.core.constants.GestureConstants
import com.austinauyeung.nyuma.c9.core.logs.Logger
import com.austinauyeung.nyuma.c9.gesture.api.GestureManager
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Handles key events for the grid cursor.
 */
class GridActionHandler(
    private val gridStateManager: GridStateManager,
    private val gestureManager: GestureManager,
    private val settingsFlow: StateFlow<OverlaySettings>,
    private val backgroundScope: CoroutineScope,
    private val modeCoordinator: OverlayModeCoordinator
) {
    private var activationKeyPressStartTime: Long = -1
    private var isActivationKeyPressed: Boolean = false
    private var wasOverlayActivated: Boolean = false
    private var activationJob: Job? = null
    private var continuousScrollJob: Job? = null

    private var heldNumberKey: Int? = null
    private var heldCellIndex: Int? = null
    private var gestureDispatchedDuringHold: Boolean = false

    private var currentScrollDirection: ScrollDirection? = null

    private fun cancelActivationJob() {
        activationJob?.cancel()
        activationJob = null
    }

    private fun cancelContinuousScrolling() {
        currentScrollDirection = null
        continuousScrollJob?.cancel()
        continuousScrollJob = null
    }

    fun cleanup() {
        cancelActivationJob()
        cancelContinuousScrolling()
    }

    fun handleKeyEvent(event: KeyEvent?): Boolean {
        val settings = settingsFlow.value

        try {
            if (event == null || settings.gridActivationKey == OverlaySettings.KEY_NONE) return false

            val activateKeys = buildSet {
                add(settings.gridActivationKey)
            }

            if (event.keyCode in activateKeys) {
                return handleActivationKey(event)
            }

            if (!gridStateManager.isGridVisible()) return false

            val numKeys = setOf(
                KeyEvent.KEYCODE_1,
                KeyEvent.KEYCODE_2,
                KeyEvent.KEYCODE_3,
                KeyEvent.KEYCODE_4,
                KeyEvent.KEYCODE_5,
                KeyEvent.KEYCODE_6,
                KeyEvent.KEYCODE_7,
                KeyEvent.KEYCODE_8,
                KeyEvent.KEYCODE_9
            )

            val scrollKeys = setOf(
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT
            )

            val zoomKeys = buildSet {
                add(KeyEvent.KEYCODE_STAR)
                add(KeyEvent.KEYCODE_0)

                if (BuildConfig.DEBUG) {
                    add(KeyEvent.KEYCODE_LEFT_BRACKET)
                    add(KeyEvent.KEYCODE_RIGHT_BRACKET)
                }
            }

            val actionKeys = setOf(
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER
            )

            return when (event.keyCode) {
                in numKeys -> {
                    handleNumberKey(event)
                }

                in scrollKeys -> {
                    handleScrollKey(event)
                }

                in zoomKeys -> {
                    handleZoomKey(event)
                }

                in actionKeys -> {
                    handleActionKey(event)
                }

                else -> false
            }
        } catch (e: Exception) {
            Logger.e("Error processing grid key event", e)
            heldNumberKey = null
            cancelContinuousScrolling()
            return false
        }
    }

    private fun handleActivationKey(event: KeyEvent): Boolean {
        cancelContinuousScrolling()

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                cancelActivationJob()

                activationKeyPressStartTime = System.currentTimeMillis()
                isActivationKeyPressed = true
                wasOverlayActivated = false

                activationJob = backgroundScope.launch {
                    delay(ApplicationConstants.ACTIVATION_HOLD_DURATION)
                    if (isActivationKeyPressed) {
                        if (modeCoordinator.requestActivation(OverlayModeCoordinator.OverlayMode.GRID)) {
                            gridStateManager.toggleGridVisibility()
                            wasOverlayActivated = gridStateManager.isGridVisible()

                            if (!wasOverlayActivated) {
                                modeCoordinator.deactivate(OverlayModeCoordinator.OverlayMode.GRID)
                            }
                        }
                    }
                }

                // Do not intercept if grid not visible yet
                return gridStateManager.isGridVisible()
            }

            KeyEvent.ACTION_UP -> {
                isActivationKeyPressed = false
                cancelActivationJob()

                // Do not intercept if grid just activated
                if (wasOverlayActivated) {
                    wasOverlayActivated = false
                    return false
                }

                if (gridStateManager.isGridVisible()) {
                    val pressDuration = System.currentTimeMillis() - activationKeyPressStartTime
                    if (pressDuration < ApplicationConstants.ACTIVATION_HOLD_DURATION) {
                        gridStateManager.resetToMainGrid()
                    }
                    return true
                }
                return false
            }

            else -> return false
        }
    }

    private fun handleNumberKey(event: KeyEvent): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (heldNumberKey != null) {
                    return true
                }

                heldNumberKey = event.keyCode
                heldCellIndex = event.keyCode - KeyEvent.KEYCODE_1
                gestureDispatchedDuringHold = false
                return true
            }

            KeyEvent.ACTION_UP -> {
                var result: Boolean? = null

                if (heldNumberKey == event.keyCode && !gestureDispatchedDuringHold) {
                    result = gridStateManager.handleNumberKey(heldCellIndex!! + 1)
                }

                heldNumberKey = null
                heldCellIndex = null
                gestureDispatchedDuringHold = false
                return result ?: true
            }

            else -> return true
        }
    }

    private fun handleScrollKey(event: KeyEvent): Boolean {
        val settings = settingsFlow.value
        val offset = if (settings.gestureStyle == GestureStyle.FIXED) GestureConstants.SCROLL_END_PAUSE else 0
        val gestureInterval = ((settings.gestureDuration + offset) * GestureConstants.CONTINUOUS_REPEAT_INTERVAL_FACTOR).toLong()
        val initialDelay = ((GestureConstants.MAX_GESTURE_DURATION + offset) * GestureConstants.CONTINUOUS_INITIAL_DELAY_FACTOR).toLong()

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                cancelContinuousScrolling()
                val (x, y) = gridStateManager.getCellCoordinates(heldCellIndex)

                if (heldCellIndex != null) {
                    gestureDispatchedDuringHold = true
                }

                val direction = when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> ScrollDirection.UP
                    KeyEvent.KEYCODE_DPAD_DOWN -> ScrollDirection.DOWN
                    KeyEvent.KEYCODE_DPAD_LEFT -> ScrollDirection.LEFT
                    KeyEvent.KEYCODE_DPAD_RIGHT -> ScrollDirection.RIGHT
                    else -> null
                }

                if (direction != null) {
                    currentScrollDirection = direction
                    backgroundScope.launch {
                        gestureManager.performScroll(direction, x, y)
                    }

                    continuousScrollJob = backgroundScope.launch {
                        delay(initialDelay)
                        while (currentScrollDirection == direction) {
                            gestureManager.performScroll(direction, x, y)
                            delay(gestureInterval)
                        }
                    }
                }
            }

            KeyEvent.ACTION_UP -> {
                cancelContinuousScrolling()
            }
        }

        return true
    }

    private fun handleZoomKey(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP) {
            backgroundScope.launch {
                val (x, y) = gridStateManager.getCellCoordinates(heldCellIndex)

                if (heldCellIndex != null) {
                    gestureDispatchedDuringHold = true
                }

                when (event.keyCode) {
                    KeyEvent.KEYCODE_STAR, KeyEvent.KEYCODE_LEFT_BRACKET -> gestureManager.performZoom(
                        false,
                        x,
                        y
                    )

                    KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_RIGHT_BRACKET -> gestureManager.performZoom(
                        true,
                        x,
                        y
                    )
                }
            }
        }
        return true
    }

    private fun handleActionKey(event: KeyEvent): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                backgroundScope.launch {
                    val (x, y) = gridStateManager.getCellCoordinates(heldCellIndex)

                    if (heldCellIndex != null) {
                        gestureDispatchedDuringHold = true
                    }

                    gestureManager.startTap(x, y)
                }
            }

            KeyEvent.ACTION_UP -> {
                backgroundScope.launch {
                    val (x, y) = gridStateManager.getCellCoordinates(heldCellIndex)

                    if (heldCellIndex != null) {
                        gestureDispatchedDuringHold = true
                    }

                    gestureManager.endTap(x, y)
                }
            }
        }

        return true
    }
}