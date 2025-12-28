package com.austinauyeung.nyuma.c9.grid.control

import android.view.KeyEvent
import com.austinauyeung.nyuma.c9.BuildConfig
import com.austinauyeung.nyuma.c9.core.control.CoreManager.ChannelMessage
import com.austinauyeung.nyuma.c9.core.control.ModeCoordinator
import com.austinauyeung.nyuma.c9.core.domain.ScrollDirection
import com.austinauyeung.nyuma.c9.core.logs.Logger
import com.austinauyeung.nyuma.c9.core.util.AccelerationUtil.cubicBezier
import com.austinauyeung.nyuma.c9.core.util.AccelerationUtil.normalizeValue
import com.austinauyeung.nyuma.c9.core.util.OrientationUtil
import com.austinauyeung.nyuma.c9.gesture.api.GestureManager
import com.austinauyeung.nyuma.c9.gesture.util.GestureUtility.launchContinuousGesture
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
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
    private val modeCoordinator: ModeCoordinator,
    private val orientationProvider: () -> OrientationUtil.Orientation = { OrientationUtil.Orientation.PORTRAIT }
) {
    // Separate from gesture ready, which is used for dispatch implementation (e.g. during a drag, gestureReady becomes true to initiate next delta)
    // Prevents overlapping gesture categories
    private enum class CurrentAction {
        ACTIVATE,
        NUMBER,
        SCROLL,
        ZOOM,
        ACTION
    }
    private var currentAction: CurrentAction? = null

    private var activationKeyPressStartTime: Long = -1
    private var isActivationKeyPressed: Boolean = false
    private var wasOverlayActivated: Boolean = false
    private var activationJob: Job? = null
    private var continuousGestureJob: Job? = null

    private var heldNumberKeyCode: Int? = null
    private var heldNumberKey: Int? = null
    private var gestureDispatchedDuringHold: Boolean = false

    private var currentScrollDirection: ScrollDirection? = null
    private var currentZoomDirection: Boolean? = null
    private var lastScrollTime: Long? = null

    private fun cancelActivationJob() {
        activationJob?.cancel()
        activationJob = null
    }

    private fun cancelContinuousGesture() {
        currentScrollDirection = null
        currentZoomDirection = null
        continuousGestureJob?.cancel()
        continuousGestureJob = null
        lastScrollTime = null
    }

    fun cleanup() {
        cancelActivationJob()
        cancelContinuousGesture()
    }

    private var numKeys = emptySet<Int>()
    private var scrollKeys = emptySet<Int>()
    private var zoomKeys = emptySet<Int>()
    private var actionKeys = emptySet<Int>()

    fun handleKeyEvent(event: KeyEvent?, channel: Channel<ChannelMessage>): Boolean {
        val settings = settingsFlow.value

        try {
            if (event == null) return false

            val activateKeys = buildSet {
                add(settings.gridActivationKey)
            }

            if (event.keyCode in activateKeys) {
                return handleActivationKey(event)
            }

            if (settings.ignoreNumpad) {
                if (event.keyCode in setOf(
                    KeyEvent.KEYCODE_1,
                    KeyEvent.KEYCODE_2,
                    KeyEvent.KEYCODE_3,
                    KeyEvent.KEYCODE_4,
                    KeyEvent.KEYCODE_5,
                    KeyEvent.KEYCODE_6,
                    KeyEvent.KEYCODE_7,
                    KeyEvent.KEYCODE_8,
                    KeyEvent.KEYCODE_9,
                    KeyEvent.KEYCODE_0,
                    KeyEvent.KEYCODE_STAR,
                    KeyEvent.KEYCODE_POUND
                )) {
                    return false
                }
            }

            // Can assume grid is not null if not returning
            if (!gridStateManager.isGridVisible()) return false

            numKeys = setOf(
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

            scrollKeys = setOf(
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT
            )

            zoomKeys = buildSet {
                add(KeyEvent.KEYCODE_STAR)
                add(KeyEvent.KEYCODE_0)

                if (BuildConfig.DEBUG) {
                    add(KeyEvent.KEYCODE_LEFT_BRACKET)
                    add(KeyEvent.KEYCODE_RIGHT_BRACKET)
                }
            }

            actionKeys = setOf(
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER
            )

            val reservedKeys = numKeys + scrollKeys + zoomKeys + actionKeys
            if (event.keyCode !in reservedKeys) {
                return false
            }

            // Buttons should be consumed at this point
            backgroundScope.launch {
                channel.send(
                    ChannelMessage(
                        event = event,
                        handler = ::handleKeyEventInternal
                    )
                )
            }

            return true
        } catch (e: Exception) {
            Logger.e("Error processing grid key event", e)
            heldNumberKeyCode = null
            cancelContinuousGesture()
            return false
        }
    }

    private suspend fun handleKeyEventInternal(event: KeyEvent) = coroutineScope {
        val settings = settingsFlow.value

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

        when (event.keyCode) {
            in numKeys -> {
                launch { handleNumberKey(event, effectiveKeyCode) }
            }

            in scrollKeys -> {
                launch { handleScrollKey(event, effectiveKeyCode) }
            }

            in zoomKeys -> {
                launch { handleZoomKey(event) }
            }

            in actionKeys -> {
                launch { handleActionKey(event) }
            }

            else -> {}
        }
    }

    private fun handleActivationKey(event: KeyEvent): Boolean {
        cancelContinuousGesture()
        val settings = settingsFlow.value

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                currentAction = CurrentAction.ACTIVATE
                cancelActivationJob()

                activationKeyPressStartTime = System.currentTimeMillis()
                isActivationKeyPressed = true
                wasOverlayActivated = false

                activationJob = backgroundScope.launch {
                    delay(settings.activationDuration)
                    if (isActivationKeyPressed) {
                        if (modeCoordinator.requestActivation(ModeCoordinator.OverlayMode.GRID)) {
                            gridStateManager.toggleGridVisibility()
                            wasOverlayActivated = gridStateManager.isGridVisible()

                            if (!wasOverlayActivated) {
                                modeCoordinator.deactivate(ModeCoordinator.OverlayMode.GRID, false)
                                gestureManager.setGestureReady(true)
                            }
                        }
                    }
                }

                // Do not intercept if grid not visible yet
                return gridStateManager.isGridVisible()
            }

            KeyEvent.ACTION_UP -> {
                currentAction = null
                isActivationKeyPressed = false
                cancelActivationJob()

                // Do not intercept if grid just activated
                if (wasOverlayActivated) {
                    wasOverlayActivated = false
                    return false
                }

                if (gridStateManager.isGridVisible()) {
                    val pressDuration = System.currentTimeMillis() - activationKeyPressStartTime
                    if (pressDuration < settings.activationDuration) {
                        gridStateManager.resetToMainGrid()
                    }
                    return true
                }
                return false
            }

            else -> return false
        }
    }

    private suspend fun handleNumberKey(event: KeyEvent, keyCode: Int): Boolean {
        // currentAction not set to allow gestures
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

    private suspend fun handleScrollKey(event: KeyEvent, keyCode: Int): Boolean {
        val settings = settingsFlow.value
        if (settings.overrideAndroid7) return false

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                currentAction = CurrentAction.SCROLL
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
                    performScroll(direction, startX = x, startY = y)

                    continuousGestureJob = launchContinuousGesture(
                        backgroundScope = backgroundScope,
                        gestureManager = gestureManager,
                        initialDelay = settings.scrollDuration,
                        condition = { currentScrollDirection == direction },
                        action = { performScroll(direction, startX = x, startY = y, forceFixedGesture = true) }
                    )
                }
            }

            KeyEvent.ACTION_UP -> {
                currentAction = null
                cancelContinuousGesture()
            }
        }

        return true
    }

    private suspend fun performScroll(direction: ScrollDirection, startX: Float, startY: Float, forceFixedGesture: Boolean = false): Boolean {
        val settings = settingsFlow.value

        if (!settings.useAdvancedScrolling) {
            gestureManager.performScroll(direction, startX = startX, startY = startY)
        } else {
            // Currently, forceFixedGesture = true indicates continuous scrolling
            if (lastScrollTime == null && forceFixedGesture) {
                lastScrollTime = System.currentTimeMillis()
            }

            val currentTime = System.currentTimeMillis()
            val timeHeld = currentTime - (lastScrollTime ?: currentTime)
            val startTime = settings.continuousScrollAccelerationStart
            val endTime = settings.continuousScrollAccelerationStart + settings.continuousScrollAccelerationDuration
            val normalizedTime = normalizeValue(timeHeld, startTime, endTime)
            val accelerationFactor = cubicBezier(normalizedTime)

            gestureManager.performScroll(
                direction,
                startX = startX,
                startY = startY,
                duration = (settings.scrollDuration + accelerationFactor * (settings.continuousScrollDuration - settings.scrollDuration)).toLong(),
                forceFixedGesture = forceFixedGesture,
                distanceFactor = settings.scrollMultiplier + accelerationFactor * (settings.continuousScrollMultiplier - settings.scrollMultiplier)
            )
        }
        return true
    }

    private suspend fun handleZoomKey(event: KeyEvent): Boolean {
        val settings = settingsFlow.value
        if (settings.overrideAndroid7) return false

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                currentAction = CurrentAction.ZOOM
                cancelContinuousGesture()
                val (x, y) = gridStateManager.getCellCoordinates(heldNumberKey)

                if (heldNumberKey != null) {
                    gestureDispatchedDuringHold = true
                }

                val isZoomIn = when (event.keyCode) {
                    KeyEvent.KEYCODE_STAR, KeyEvent.KEYCODE_LEFT_BRACKET -> false
                    KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_RIGHT_BRACKET -> true
                    else -> return false
                }

                currentZoomDirection = isZoomIn
                performZoom(isZoomIn, x, y)

                continuousGestureJob = launchContinuousGesture(
                    backgroundScope = backgroundScope,
                    gestureManager = gestureManager,
                    initialDelay = settings.zoomDuration,
                    condition = { currentZoomDirection == isZoomIn },
                    action = { performZoom(isZoomIn, x, y, forceFixedGesture = true) }
                )
            }

            KeyEvent.ACTION_UP -> {
                currentAction = null
                cancelContinuousGesture()
            }
        }
        return true
    }

    private suspend fun performZoom(isZoomIn: Boolean, x: Float, y: Float, forceFixedGesture: Boolean = false): Boolean {
        val orientation = orientationProvider()
        gestureManager.performZoom(isZoomIn, x, y, orientation, forceFixedGesture = forceFixedGesture)

        return true
    }

    private suspend fun handleActionKey(event: KeyEvent): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                currentAction = CurrentAction.ACTION
                val (x, y) = gridStateManager.getCellCoordinates(heldNumberKey)

                if (heldNumberKey != null) {
                    gestureDispatchedDuringHold = true
                }

                gestureManager.startTap(x, y)
            }

            KeyEvent.ACTION_UP -> {
                currentAction = null
                val (x, y) = gridStateManager.getCellCoordinates(heldNumberKey)

                if (heldNumberKey != null) {
                    gestureDispatchedDuringHold = true
                }

                gestureManager.endTap(x, y)
            }
        }

        return true
    }
}