package com.austinauyeung.nyuma.c9.cursor.handler

import android.view.KeyEvent
import androidx.compose.ui.geometry.Offset
import com.austinauyeung.nyuma.c9.BuildConfig
import com.austinauyeung.nyuma.c9.accessibility.coordinator.OverlayModeCoordinator
import com.austinauyeung.nyuma.c9.accessibility.service.OverlayAccessibilityService
import com.austinauyeung.nyuma.c9.common.domain.ScreenDimensions
import com.austinauyeung.nyuma.c9.common.domain.ScreenEdge
import com.austinauyeung.nyuma.c9.common.domain.ScreenEdgeBehavior
import com.austinauyeung.nyuma.c9.common.domain.ScrollDirection
import com.austinauyeung.nyuma.c9.core.constants.ApplicationConstants
import com.austinauyeung.nyuma.c9.core.constants.CursorConstants
import com.austinauyeung.nyuma.c9.core.constants.GestureConstants
import com.austinauyeung.nyuma.c9.core.logs.Logger
import com.austinauyeung.nyuma.c9.core.util.OrientationUtil
import com.austinauyeung.nyuma.c9.cursor.domain.CursorDirection
import com.austinauyeung.nyuma.c9.gesture.api.GestureManager
import com.austinauyeung.nyuma.c9.gesture.util.GestureUtility.launchContinuousGesture
import com.austinauyeung.nyuma.c9.settings.domain.ControlScheme
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Handles key events for the standard cursor mode.
 */
class CursorActionHandler(
    private val cursorStateManager: CursorStateManager,
    private val gestureManager: GestureManager,
    private val settingsFlow: StateFlow<OverlaySettings>,
    private val backgroundScope: CoroutineScope,
    private val modeCoordinator: OverlayModeCoordinator,
    private val orientationProvider: () -> OrientationUtil.Orientation = { OrientationUtil.Orientation.PORTRAIT },
    private val dimensionsFlow: StateFlow<ScreenDimensions>,
) {
    private var activationKeyPressStartTime: Long = -1
    private var isActivationKeyPressed: Boolean = false
    private var wasActivated: Boolean = false
    private var currentScrollDirection: ScrollDirection? = null
    private var activationJob: Job? = null
    private var continuousGestureJob: Job? = null
    private var movementJob: Job? = null
    private var currentScreenEdge: ScreenEdge? = null
    private var slowScrollJob: Job? = null

    private val activeDirections = mutableSetOf<CursorDirection>()
    private var lastMovementTime = 0L

    private var isGestureActive = false
    private var lastDragPosition: Offset? = null

    private var actionKeysPressed = 0

    private fun cancelActivationJob() {
        activationJob?.cancel()
        activationJob = null
    }

    private fun cancelContinuousGesture() {
        currentScrollDirection = null
        continuousGestureJob?.cancel()
        continuousGestureJob = null
    }

    private fun cancelMovementJob() {
        movementJob?.cancel()
        movementJob = null
        activeDirections.clear()
    }

    private fun cancelSlowScrollJob() {
        slowScrollJob?.cancel()
        slowScrollJob = null
        currentScreenEdge = null
    }

    fun cleanup() {
        cancelActivationJob()
        cancelContinuousGesture()
        cancelMovementJob()
        slowScrollJob?.cancel()
    }

    fun handleKeyEvent(event: KeyEvent?): Boolean {
        val settings = settingsFlow.value

        try {
            if (event == null) return false

            val activateKeys = buildSet {
                add(settings.cursorActivationKey)
            }

            if (event.keyCode in activateKeys) {
                return handleActivationKey(event)
            }

            if (!cursorStateManager.isCursorVisible()) return false

            // Map keys based on control scheme
            val (movementKeys, scrollKeys) =
                when (settings.controlScheme) {
                    ControlScheme.STANDARD -> {
                        val movementKeys = setOf(
                            KeyEvent.KEYCODE_DPAD_UP,
                            KeyEvent.KEYCODE_DPAD_DOWN,
                            KeyEvent.KEYCODE_DPAD_LEFT,
                            KeyEvent.KEYCODE_DPAD_RIGHT
                        )

                        val scrollKeys = setOf(
                            KeyEvent.KEYCODE_2,
                            KeyEvent.KEYCODE_4,
                            KeyEvent.KEYCODE_6,
                            KeyEvent.KEYCODE_8
                        )

                        Pair(movementKeys, scrollKeys)
                    }

                    ControlScheme.SWAPPED -> {
                        val movementKeys = setOf(
                            KeyEvent.KEYCODE_2,
                            KeyEvent.KEYCODE_4,
                            KeyEvent.KEYCODE_6,
                            KeyEvent.KEYCODE_8
                        )

                        val scrollKeys = setOf(
                            KeyEvent.KEYCODE_DPAD_UP,
                            KeyEvent.KEYCODE_DPAD_DOWN,
                            KeyEvent.KEYCODE_DPAD_LEFT,
                            KeyEvent.KEYCODE_DPAD_RIGHT
                        )

                        Pair(movementKeys, scrollKeys)
                    }

                    ControlScheme.DPAD_TOGGLE -> {
                        if (cursorStateManager.isInScrollMode()) {
                            Pair(
                                emptySet(),
                                setOf(
                                    KeyEvent.KEYCODE_DPAD_UP,
                                    KeyEvent.KEYCODE_DPAD_DOWN,
                                    KeyEvent.KEYCODE_DPAD_LEFT,
                                    KeyEvent.KEYCODE_DPAD_RIGHT
                                )
                            )
                        } else {
                            Pair(
                                setOf(
                                    KeyEvent.KEYCODE_DPAD_UP,
                                    KeyEvent.KEYCODE_DPAD_DOWN,
                                    KeyEvent.KEYCODE_DPAD_LEFT,
                                    KeyEvent.KEYCODE_DPAD_RIGHT
                                ),
                                emptySet()
                            )
                        }
                    }

                    ControlScheme.NUMPAD_TOGGLE -> {
                        if (cursorStateManager.isInScrollMode()) {
                            Pair(
                                emptySet(),
                                setOf(
                                    KeyEvent.KEYCODE_2,
                                    KeyEvent.KEYCODE_8,
                                    KeyEvent.KEYCODE_4,
                                    KeyEvent.KEYCODE_6
                                )
                            )
                        } else {
                            Pair(
                                setOf(
                                    KeyEvent.KEYCODE_2,
                                    KeyEvent.KEYCODE_8,
                                    KeyEvent.KEYCODE_4,
                                    KeyEvent.KEYCODE_6
                                ),
                                emptySet()
                            )
                        }
                    }
                }

            val actionKeys = setOf(
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_5
            )

            val zoomKeys = buildSet {
                add(KeyEvent.KEYCODE_1)
                add(KeyEvent.KEYCODE_3)

                if (BuildConfig.DEBUG) {
                    add(KeyEvent.KEYCODE_LEFT_BRACKET)
                    add(KeyEvent.KEYCODE_RIGHT_BRACKET)
                }
            }

            val disableKeys = setOf(
                KeyEvent.KEYCODE_7,
                KeyEvent.KEYCODE_9,
                KeyEvent.KEYCODE_0,
                KeyEvent.KEYCODE_POUND,
                KeyEvent.KEYCODE_STAR
            )

            val originalKeyCode = event.keyCode
            val effectiveKeyCode = if (settings.rotateButtonsWithOrientation) {
                val orientation = orientationProvider()
                when {
                    OrientationUtil.isDpadDirection(originalKeyCode) ->
                        OrientationUtil.mapDPadKey(originalKeyCode, orientation)
                    OrientationUtil.isNumberKey(originalKeyCode) ->
                        OrientationUtil.mapNumberKey(originalKeyCode, orientation)
                    else -> originalKeyCode
                }
            } else {
                originalKeyCode
            }


            return when (event.keyCode) {
                in movementKeys -> {
                    handleMovementKey(event, effectiveKeyCode)
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

                in disableKeys -> {
                    true
                }

                else -> false
            }
        } catch (e: Exception) {
            Logger.e("Error processing cursor key event", e)
            cancelContinuousGesture()
            return false
        }
    }

    private fun handleActivationKey(event: KeyEvent): Boolean {
        cancelContinuousGesture()
        val settings = settingsFlow.value

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                cancelActivationJob()

                activationKeyPressStartTime = System.currentTimeMillis()
                isActivationKeyPressed = true
                wasActivated = false

                activationJob = backgroundScope.launch {
                    delay(ApplicationConstants.ACTIVATION_HOLD_DURATION)
                    if (isActivationKeyPressed) {
                        if (modeCoordinator.requestActivation(OverlayModeCoordinator.OverlayMode.CURSOR)) {
                            cursorStateManager.toggleCursorVisibility()
                            wasActivated = cursorStateManager.isCursorVisible()

                            if (wasActivated) {
                                OverlayAccessibilityService.getInstance()?.setHidingCursor(false)
                            } else {
                                modeCoordinator.deactivate(OverlayModeCoordinator.OverlayMode.CURSOR)
                                gestureManager.setGestureReady(true)
                            }
                        }
                    }
                }

                // Do not intercept if cursor not visible yet
                return cursorStateManager.isCursorVisible()
            }

            KeyEvent.ACTION_UP -> {
                isActivationKeyPressed = false
                cancelActivationJob()

                // Do not intercept if cursor just activated
                if (wasActivated) {
                    wasActivated = false
                    return false
                }

                if (cursorStateManager.isCursorVisible()) {
                    val pressDuration = System.currentTimeMillis() - activationKeyPressStartTime
                    if (pressDuration < ApplicationConstants.ACTIVATION_HOLD_DURATION) {
                        if (settings.controlScheme == ControlScheme.DPAD_TOGGLE || settings.controlScheme == ControlScheme.NUMPAD_TOGGLE) {
                            cursorStateManager.toggleScrollMode()

                            if (isGestureActive) {
                                endTap()
                            }
                        }
                    } else {
                        cursorStateManager.hideCursor()
                    }
                    return true
                }
                return false
            }

            else -> return false
        }
    }

    private fun handleMovementKey(event: KeyEvent, keyCode: Int): Boolean {
        val direction = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_2 -> CursorDirection.UP
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_8 -> CursorDirection.DOWN
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_4 -> CursorDirection.LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_6 -> CursorDirection.RIGHT
            else -> return false
        }

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                startMovingCursor(direction)
                return true
            }

            KeyEvent.ACTION_UP -> {
                stopMovingCursor(direction)
                return true
            }

            else -> return false
        }
    }

    private fun handleScrollKey(event: KeyEvent, keyCode: Int): Boolean {
        val settings = settingsFlow.value
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                cancelContinuousGesture()

                val direction = when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_2 -> ScrollDirection.UP
                    KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_8 -> ScrollDirection.DOWN
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_4 -> ScrollDirection.LEFT
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_6 -> ScrollDirection.RIGHT
                    else -> null
                }

                if (direction != null) {
                    currentScrollDirection = direction
                    backgroundScope.launch {
                        performScroll(direction)
                    }

                    continuousGestureJob = launchContinuousGesture(
                        backgroundScope = backgroundScope,
                        gestureManager = gestureManager,
                        initialDelay = settings.gestureDuration,
                        condition = { currentScrollDirection == direction },
                        action = { performScroll(direction, true) }
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
            val isZoomIn = when (event.keyCode) {
                KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_LEFT_BRACKET -> false
                KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_RIGHT_BRACKET -> true
                else -> return false
            }

            performZoom(isZoomIn)
            return true
        }
        return true
    }

    private fun handleActionKey(event: KeyEvent): Boolean {
        return when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                handleActionKeyDown()
            }

            KeyEvent.ACTION_UP -> {
                handleActionKeyUp()
            }

            else -> true
        }
    }

    private fun startMovingCursor(direction: CursorDirection) {
        activeDirections.add(direction)
        lastMovementTime = System.currentTimeMillis()

        if (movementJob == null) {
            moveCursor(direction)

            movementJob = backgroundScope.launch {
                while (activeDirections.isNotEmpty()) {
                    moveCursor(direction)
                    delay(CursorConstants.FRAME_DURATION_MS.toLong())
                }
            }
        }
    }

    private fun stopMovingCursor(direction: CursorDirection) {
        activeDirections.remove(direction)

        if (activeDirections.isEmpty()) {
            cancelSlowScrollJob()
            movementJob = null
        }
    }

    private fun moveCursor(direction: CursorDirection) {
        if (activeDirections.isEmpty()) return

        val settings = settingsFlow.value
        val currentTime = System.currentTimeMillis()
        val timeHeld = currentTime - lastMovementTime

        var deltaX = 0f
        var deltaY = 0f

        for (dir in activeDirections) {
            val delta = cursorStateManager.calculateMovement(dir, timeHeld)
            deltaX += delta.x
            deltaY += delta.y
        }

        // Normalize diagonal speed
        if (deltaX != 0f && deltaY != 0f) {
            val length = sqrt(deltaX * deltaX + deltaY * deltaY)
            val frameSpeed = cursorStateManager.calculateFrameSpeed(timeHeld)
            val normalizer = frameSpeed / length
            deltaX *= normalizer
            deltaY *= normalizer
        }

        val newPosition = cursorStateManager.applyMovement(Offset(deltaX, deltaY))
        cursorStateManager.updatePosition(newPosition)

        // Handle drag if active
        if (isGestureActive && lastDragPosition != null) {
            dragToNewPosition(lastDragPosition!!, newPosition)
            lastDragPosition = newPosition
            return
        }

        if (settings.cursorEdgeBehavior == ScreenEdgeBehavior.AUTO_SCROLL) {
            currentScreenEdge = cursorStateManager.checkEdge(direction, newPosition)
            if (currentScreenEdge != ScreenEdge.NONE && slowScrollJob == null) {
                slowScrollJob = launchContinuousGesture(
                    backgroundScope = backgroundScope,
                    gestureManager = gestureManager,
                    initialDelay = 0L,
                    condition = { currentScreenEdge != ScreenEdge.NONE },
                    action = { performSlowScroll(currentScreenEdge!!, GestureConstants.SLOW_SCROLL_DURATION) }
                )
            }
        }
    }

    private suspend fun performSlowScroll(edge: ScreenEdge, duration: Long): Boolean {
        var direction: ScrollDirection? = null
        val dimensions = dimensionsFlow.value
        var x = dimensions.width / 2f
        var y = dimensions.height / 2f
        val cursorState = cursorStateManager.cursorState.value

        when (edge) {
            ScreenEdge.TOP -> {
                direction = ScrollDirection.UP
                if (cursorState != null) x = cursorState.position.x
            }
            ScreenEdge.BOTTOM -> {
                direction = ScrollDirection.DOWN
                if (cursorState != null) x = cursorState.position.x
            }
            ScreenEdge.LEFT -> {
                direction = ScrollDirection.LEFT
                if (cursorState != null) y = cursorState.position.y
            }
            ScreenEdge.RIGHT -> {
                direction = ScrollDirection.RIGHT
                if (cursorState != null) y = cursorState.position.y
            }
            ScreenEdge.NONE -> null
        }

        if (direction != null) gestureManager.performScroll(direction, startX = x, startY = y, duration = duration, useNaturalScrolling = false, forceFixedScroll = true, distanceFactor = GestureConstants.SLOW_SCROLL_MULTIPLIER)
        return true
    }

    private suspend fun performScroll(direction: ScrollDirection, forceFixedScroll: Boolean = false): Boolean {
        val cursorState = cursorStateManager.cursorState.value ?: return false
        gestureManager.performScroll(direction, startX = cursorState.position.x, startY = cursorState.position.y, forceFixedScroll = forceFixedScroll)

        return true
    }

    private fun performZoom(isZoomIn: Boolean): Boolean {
        val cursorState = cursorStateManager.cursorState.value ?: return false
        backgroundScope.launch {
            gestureManager.performZoom(isZoomIn, cursorState.position.x, cursorState.position.y)
        }

        return true
    }

    private fun handleActionKeyDown(): Boolean {
        val settings = settingsFlow.value

        actionKeysPressed++
        val cursorState = cursorStateManager.cursorState.value

        if (cursorState != null) {
            var allowTap = false

            // Disable long press hold setting for now
            if (actionKeysPressed == 2 && settings.toggleHold && false) {
                val updatedState = cursorStateManager.updateHoldState(!cursorState.isHoldActive)
                if (updatedState != null) {
                    allowTap = !updatedState.inScrollMode && updatedState.isHoldActive
                }
            } else {
                allowTap = !cursorState.inScrollMode && !cursorState.isHoldActive
            }

            if (!isGestureActive && allowTap) {
                isGestureActive = true
                lastDragPosition = cursorState.position

                backgroundScope.launch {
                    gestureManager.startTap(cursorState.position.x, cursorState.position.y)
                }
            }
        }

        return true
    }

    private fun handleActionKeyUp(): Boolean {
        val settings = settingsFlow.value
        val cursorState = cursorStateManager.cursorState.value

        actionKeysPressed--

        if (cursorState != null) {
            if (!isGestureActive) {
                return false
            }

            if (!settings.toggleHold || !cursorState.isHoldActive) {
                endTap()
            }
        }

        return true
    }

    private fun dragToNewPosition(fromPosition: Offset, toPosition: Offset) {
        backgroundScope.launch {
            val result = gestureManager.dragTap(
                fromPosition.x,
                fromPosition.y,
                toPosition.x,
                toPosition.y
            )

            if (!result) {
                endTap()
                cursorStateManager.updateHoldState(false)
            }
        }
    }

    private fun endTap(): Boolean {
        val cursorState = cursorStateManager.cursorState.value ?: return false
        isGestureActive = false
        lastDragPosition = null

        backgroundScope.launch {
            gestureManager.endTap(cursorState.position.x, cursorState.position.y)
        }
        return true
    }
}