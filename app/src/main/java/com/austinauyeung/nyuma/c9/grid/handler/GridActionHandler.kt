package com.austinauyeung.nyuma.c9.grid.handler

import android.view.KeyEvent
import com.austinauyeung.nyuma.c9.BuildConfig
import com.austinauyeung.nyuma.c9.accessibility.coordinator.OverlayModeCoordinator
import com.austinauyeung.nyuma.c9.accessibility.service.OverlayAccessibilityService
import com.austinauyeung.nyuma.c9.common.domain.ScrollDirection
import com.austinauyeung.nyuma.c9.core.constants.ApplicationConstants
import com.austinauyeung.nyuma.c9.core.logs.Logger
import com.austinauyeung.nyuma.c9.core.util.OrientationUtil
import com.austinauyeung.nyuma.c9.gesture.api.GestureManager
import com.austinauyeung.nyuma.c9.gesture.util.GestureUtility.launchContinuousGesture
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
    private val modeCoordinator: OverlayModeCoordinator,
    private val orientationProvider: () -> OrientationUtil.Orientation = { OrientationUtil.Orientation.PORTRAIT }
) {
    private var activationKeyPressStartTime: Long = -1
    private var isActivationKeyPressed: Boolean = false
    private var wasOverlayActivated: Boolean = false
    private var activationJob: Job? = null
    private var continuousGestureJob: Job? = null

    private var heldNumberKeyCode: Int? = null
    private var heldNumberKey: Int? = null
    private var gestureDispatchedDuringHold: Boolean = false

    private var currentScrollDirection: ScrollDirection? = null

    private fun cancelActivationJob() {
        activationJob?.cancel()
        activationJob = null
    }

    private fun cancelContinuousGesture() {
        currentScrollDirection = null
        continuousGestureJob?.cancel()
        continuousGestureJob = null
    }

    fun cleanup() {
        cancelActivationJob()
        cancelContinuousGesture()
    }

    fun handleKeyEvent(event: KeyEvent?): Boolean {
        val settings = settingsFlow.value

        try {
            if (event == null) return false

            val activateKeys = buildSet {
                add(settings.gridActivationKey)
            }

            if (event.keyCode in activateKeys) {
                return handleActivationKey(event)
            }

            // Can assume grid is not null if not returning
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

            val originalKeyCode = event.keyCode
            val effectiveKeyCode = if (settings.rotateButtonsWithOrientation) {
                val orientation = orientationProvider()
                when (originalKeyCode) {
                    in numKeys -> OrientationUtil.mapNumberKey(originalKeyCode, orientation)
                    in scrollKeys -> OrientationUtil.mapDPadKey(originalKeyCode, orientation)
                    else -> originalKeyCode
                }
            } else {
                originalKeyCode
            }

            return when (effectiveKeyCode) {
                in numKeys -> {
                    handleNumberKey(event, effectiveKeyCode)
                }

                in scrollKeys -> {
                    handleScrollKey(event, effectiveKeyCode)
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
            heldNumberKeyCode = null
            cancelContinuousGesture()
            return false
        }
    }

    private fun handleActivationKey(event: KeyEvent): Boolean {
        cancelContinuousGesture()

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

                            if (wasOverlayActivated) {
                                OverlayAccessibilityService.getInstance()?.setHidingCursor(false)
                            } else {
                                modeCoordinator.deactivate(OverlayModeCoordinator.OverlayMode.GRID)
                                gestureManager.setGestureReady(true)
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

    private fun handleNumberKey(event: KeyEvent, keyCode: Int): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (heldNumberKeyCode != null) {
                    return true
                }

                heldNumberKeyCode = keyCode
                heldNumberKey = keyCode - KeyEvent.KEYCODE_1 + 1
                gestureDispatchedDuringHold = false
                return true
            }

            KeyEvent.ACTION_UP -> {
                var result: Boolean? = null

                if (heldNumberKeyCode == keyCode && !gestureDispatchedDuringHold) {
                    result = gridStateManager.handleNumberKey(heldNumberKey!!)
                }

                heldNumberKeyCode = null
                heldNumberKey = null
                gestureDispatchedDuringHold = false
                return result ?: true
            }

            else -> return true
        }
    }

    private fun handleScrollKey(event: KeyEvent, keyCode: Int): Boolean {
        val settings = settingsFlow.value
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                cancelContinuousGesture()
                val (x, y) = gridStateManager.getCellCoordinates(heldNumberKey)

                if (heldNumberKey != null) {
                    gestureDispatchedDuringHold = true
                }

                val direction = when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> ScrollDirection.UP
                    KeyEvent.KEYCODE_DPAD_DOWN -> ScrollDirection.DOWN
                    KeyEvent.KEYCODE_DPAD_LEFT -> ScrollDirection.LEFT
                    KeyEvent.KEYCODE_DPAD_RIGHT -> ScrollDirection.RIGHT
                    else -> null
                }

                if (direction != null) {
                    currentScrollDirection = direction
                    backgroundScope.launch {
                        gestureManager.performScroll(direction, startX = x, startY = y)
                    }

                    continuousGestureJob = launchContinuousGesture(
                        backgroundScope = backgroundScope,
                        gestureManager = gestureManager,
                        initialDelay = settings.gestureDuration,
                        condition = { currentScrollDirection == direction },
                        action = { gestureManager.performScroll(direction, startX = x, startY = y, forceFixedScroll = true) }
                    )
                }
            }

            KeyEvent.ACTION_UP -> {
                cancelContinuousGesture()
            }
        }

        return true
    }

    private fun handleZoomKey(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP) {
            backgroundScope.launch {
                val (x, y) = gridStateManager.getCellCoordinates(heldNumberKey)

                if (heldNumberKey != null) {
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
                    val (x, y) = gridStateManager.getCellCoordinates(heldNumberKey)

                    if (heldNumberKey != null) {
                        gestureDispatchedDuringHold = true
                    }

                    gestureManager.startTap(x, y)
                }
            }

            KeyEvent.ACTION_UP -> {
                backgroundScope.launch {
                    val (x, y) = gridStateManager.getCellCoordinates(heldNumberKey)

                    if (heldNumberKey != null) {
                        gestureDispatchedDuringHold = true
                    }

                    gestureManager.endTap(x, y)
                }
            }
        }

        return true
    }
}