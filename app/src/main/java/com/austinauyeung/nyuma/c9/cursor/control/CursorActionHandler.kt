package com.austinauyeung.nyuma.c9.cursor.control

import android.view.KeyEvent
import androidx.compose.ui.geometry.Offset
import com.austinauyeung.nyuma.c9.BuildConfig
import com.austinauyeung.nyuma.c9.accessibility.AppAccessibilityService
import com.austinauyeung.nyuma.c9.core.constants.CursorConstants
import com.austinauyeung.nyuma.c9.core.constants.GestureConstants
import com.austinauyeung.nyuma.c9.core.control.CoreManager.ChannelMessage
import com.austinauyeung.nyuma.c9.core.control.ModeCoordinator
import com.austinauyeung.nyuma.c9.core.domain.ScreenDimensions
import com.austinauyeung.nyuma.c9.core.domain.ScreenEdge
import com.austinauyeung.nyuma.c9.core.domain.ScreenEdgeBehavior
import com.austinauyeung.nyuma.c9.core.domain.ScrollDirection
import com.austinauyeung.nyuma.c9.core.logs.Logger
import com.austinauyeung.nyuma.c9.core.util.AccelerationUtil.cubicBezier
import com.austinauyeung.nyuma.c9.core.util.AccelerationUtil.normalizeValue
import com.austinauyeung.nyuma.c9.core.util.OrientationUtil
import com.austinauyeung.nyuma.c9.cursor.domain.ControlScheme
import com.austinauyeung.nyuma.c9.cursor.domain.CursorDirection
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
import kotlin.math.sqrt

/**
 * Handles key events for the standard cursor mode.
 */
class CursorActionHandler(
    private val cursorStateManager: CursorStateManager,
    private val gestureManager: GestureManager,
    private val settingsFlow: StateFlow<OverlaySettings>,
    private val backgroundScope: CoroutineScope,
    private val modeCoordinator: ModeCoordinator,
    private val orientationProvider: () -> OrientationUtil.Orientation = { OrientationUtil.Orientation.PORTRAIT },
    private val dimensionsFlow: StateFlow<ScreenDimensions>,
) {
    // Separate from gesture ready, which is used for dispatch implementation (e.g. during a drag, gestureReady becomes true to initiate next delta)
    // Prevents overlapping gesture categories
    private enum class CurrentAction {
        ACTIVATE,
        MOVEMENT,
        SCROLL,
        ZOOM,
        ACTION
    }
    private var currentAction: CurrentAction? = null

    private var activationKeyPressStartTime: Long = -1
    private var isActivationKeyPressed: Boolean = false
    private var wasActivated: Boolean = false
    private var currentScrollDirection: ScrollDirection? = null
    private var currentZoomDirection: Boolean? = null
    private var activationJob: Job? = null
    private var continuousGestureJob: Job? = null
    private var movementJob: Job? = null
    private var currentScreenEdge: ScreenEdge? = null
    private var slowScrollJob: Job? = null

    private val activeDirections = mutableSetOf<CursorDirection>()
    private var lastMovementTime = 0L
    private var lastScrollTime: Long? = null

    private var isLongPressing = false
    private var lastDragPosition: Offset? = null
    private var isDragging = false

    private var actionKeysPressed = 0

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

    private fun cancelMovementJob() {
        movementJob?.cancel()
        movementJob = null
        activeDirections.clear()
    }

    private fun cancelSlowScrollJob() {
        slowScrollJob?.cancel()
        slowScrollJob = null
        currentScreenEdge = null
        lastScrollTime = null
    }

    fun cleanup() {
        cancelActivationJob()
        cancelContinuousGesture()
        cancelMovementJob()
        slowScrollJob?.cancel()
    }

    private var movementKeys = emptySet<Int>()
    private var scrollKeys = emptySet<Int>()
    private var zoomKeys = emptySet<Int>()
    private var actionKeys = emptySet<Int>()
    private var disableKeys = emptySet<Int>()

    fun handleKeyEvent(event: KeyEvent?, channel: Channel<ChannelMessage>): Boolean {
        cursorStateManager.updateClickable(false)
        val settings = settingsFlow.value

        try {
            if (event == null) return false

            val activateKeys = buildSet {
                add(settings.cursorActivationKey)
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

            if (!cursorStateManager.isCursorVisible()) return false

            // Map keys based on control scheme
            movementKeys =
                when (settings.controlScheme) {
                    ControlScheme.STANDARD, ControlScheme.TV -> {
                        setOf(
                            KeyEvent.KEYCODE_DPAD_UP,
                            KeyEvent.KEYCODE_DPAD_DOWN,
                            KeyEvent.KEYCODE_DPAD_LEFT,
                            KeyEvent.KEYCODE_DPAD_RIGHT
                        )
                    }

                    ControlScheme.SWAPPED -> {
                        setOf(
                            KeyEvent.KEYCODE_2,
                            KeyEvent.KEYCODE_4,
                            KeyEvent.KEYCODE_6,
                            KeyEvent.KEYCODE_8
                        )
                    }

                    ControlScheme.DPAD_TOGGLE -> {
                        if (cursorStateManager.isInScrollMode()) {
                            emptySet()
                        } else {
                            setOf(
                                KeyEvent.KEYCODE_DPAD_UP,
                                KeyEvent.KEYCODE_DPAD_DOWN,
                                KeyEvent.KEYCODE_DPAD_LEFT,
                                KeyEvent.KEYCODE_DPAD_RIGHT
                            )
                        }
                    }

                    ControlScheme.NUMPAD_TOGGLE -> {
                        if (cursorStateManager.isInScrollMode()) {
                            emptySet()
                        } else {
                            setOf(
                                KeyEvent.KEYCODE_2,
                                KeyEvent.KEYCODE_8,
                                KeyEvent.KEYCODE_4,
                                KeyEvent.KEYCODE_6
                            )
                        }
                    }
                }

            scrollKeys =
                when (settings.controlScheme) {
                    ControlScheme.STANDARD -> {
                        setOf(
                            KeyEvent.KEYCODE_2,
                            KeyEvent.KEYCODE_4,
                            KeyEvent.KEYCODE_6,
                            KeyEvent.KEYCODE_8
                        )
                    }

                    ControlScheme.SWAPPED -> {
                        setOf(
                            KeyEvent.KEYCODE_DPAD_UP,
                            KeyEvent.KEYCODE_DPAD_DOWN,
                            KeyEvent.KEYCODE_DPAD_LEFT,
                            KeyEvent.KEYCODE_DPAD_RIGHT
                        )
                    }

                    ControlScheme.DPAD_TOGGLE -> {
                        if (cursorStateManager.isInScrollMode()) {
                            setOf(
                                    KeyEvent.KEYCODE_DPAD_UP,
                                    KeyEvent.KEYCODE_DPAD_DOWN,
                                    KeyEvent.KEYCODE_DPAD_LEFT,
                                    KeyEvent.KEYCODE_DPAD_RIGHT
                            )
                        } else {
                            emptySet()
                        }
                    }

                    ControlScheme.NUMPAD_TOGGLE -> {
                        if (cursorStateManager.isInScrollMode()) {
                            setOf(
                                    KeyEvent.KEYCODE_2,
                                    KeyEvent.KEYCODE_8,
                                    KeyEvent.KEYCODE_4,
                                    KeyEvent.KEYCODE_6
                            )
                        } else {
                            emptySet()
                        }
                    }

                    ControlScheme.TV -> {
                        setOf(
                            settings.scrollUpKey,
                            settings.scrollDownKey,
                            settings.scrollLeftKey,
                            settings.scrollRightKey
                        )
                    }
                }

            actionKeys =
                when (settings.controlScheme) {
                    ControlScheme.STANDARD, ControlScheme.DPAD_TOGGLE, ControlScheme.TV -> {
                        setOf(
                            KeyEvent.KEYCODE_DPAD_CENTER,
                            KeyEvent.KEYCODE_ENTER,
                        )
                    }

                    ControlScheme.SWAPPED, ControlScheme.NUMPAD_TOGGLE -> {
                        setOf(
                            KeyEvent.KEYCODE_5,
                        )
                    }
                }

            zoomKeys = buildSet {
                add(KeyEvent.KEYCODE_1)
                add(KeyEvent.KEYCODE_3)

                if (BuildConfig.DEBUG) {
                    add(KeyEvent.KEYCODE_LEFT_BRACKET)
                    add(KeyEvent.KEYCODE_RIGHT_BRACKET)
                }
            }

            disableKeys = setOf(
                KeyEvent.KEYCODE_7,
                KeyEvent.KEYCODE_9,
                KeyEvent.KEYCODE_0,
                KeyEvent.KEYCODE_POUND,
                KeyEvent.KEYCODE_STAR
            )

            val reservedKeys = movementKeys + scrollKeys + zoomKeys + actionKeys + disableKeys
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
            Logger.e("Error processing cursor key event", e)
            cancelContinuousGesture()
            return false
        }
    }

    private suspend fun handleKeyEventInternal(event: KeyEvent) = coroutineScope {
        val settings = settingsFlow.value

        val originalKeyCode = event.keyCode
        val effectiveKeyCode = if (settings.rotateButtonsWithOrientation && (settings.controlScheme != ControlScheme.TV)) {
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
            in movementKeys -> launch { handleMovementKey(event, effectiveKeyCode) }

            in scrollKeys -> {
                launch { handleScrollKey(event, effectiveKeyCode) }
            }

            in zoomKeys -> {
                launch { handleZoomKey(event) }
            }

            in actionKeys -> {
                launch { handleActionKey(event) }
            }

            in disableKeys -> {}

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
                wasActivated = false

                activationJob = backgroundScope.launch {
                    delay(settings.activationDuration)
                    if (isActivationKeyPressed) {
                        if (modeCoordinator.requestActivation(ModeCoordinator.OverlayMode.CURSOR)) {
                            cursorStateManager.toggleCursorVisibility()
                            wasActivated = cursorStateManager.isCursorVisible()

                            if (!wasActivated) {
                                modeCoordinator.deactivate(ModeCoordinator.OverlayMode.CURSOR, false)
                                gestureManager.setGestureReady(true)
                            }
                        }
                    }
                }

                // Do not intercept if cursor not visible yet
                return cursorStateManager.isCursorVisible()
            }

            KeyEvent.ACTION_UP -> {
                currentAction = null
                isActivationKeyPressed = false
                cancelActivationJob()

                // Do not intercept if cursor just activated
                if (wasActivated) {
                    wasActivated = false
                    return false
                }

                if (cursorStateManager.isCursorVisible()) {
                    val pressDuration = System.currentTimeMillis() - activationKeyPressStartTime
                    if (pressDuration < settings.activationDuration) {
                        if (settings.controlScheme == ControlScheme.DPAD_TOGGLE || settings.controlScheme == ControlScheme.NUMPAD_TOGGLE) {
                            cursorStateManager.toggleScrollMode()

//                            if (isLongPressing) {
//                                endTap()
//                            }
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

    private suspend fun handleMovementKey(event: KeyEvent, keyCode: Int): Boolean {
        val direction = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_2 -> CursorDirection.UP
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_8 -> CursorDirection.DOWN
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_4 -> CursorDirection.LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_6 -> CursorDirection.RIGHT
            else -> return false
        }

        // currentAction not set to allow gestures
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

    private suspend fun handleScrollKey(event: KeyEvent, keyCode: Int): Boolean {
        val settings = settingsFlow.value
        if (settings.overrideAndroid7) return false

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                currentAction = CurrentAction.SCROLL
                cancelContinuousGesture()

                val direction = when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_2, settings.scrollUpKey -> ScrollDirection.UP
                    KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_8, settings.scrollDownKey -> ScrollDirection.DOWN
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_4, settings.scrollLeftKey -> ScrollDirection.LEFT
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_6, settings.scrollRightKey -> ScrollDirection.RIGHT
                    else -> null
                }

                if (direction != null) {
                    currentScrollDirection = direction
                    performScroll(direction)

                    continuousGestureJob = launchContinuousGesture(
                        backgroundScope = backgroundScope,
                        gestureManager = gestureManager,
                        initialDelay = settings.scrollDuration,
                        condition = { currentScrollDirection == direction },
                        action = { performScroll(direction, true) }
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

    private suspend fun handleZoomKey(event: KeyEvent): Boolean {
        val settings = settingsFlow.value
        if (settings.overrideAndroid7) return false

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                currentAction = CurrentAction.ZOOM
                cancelContinuousGesture()

                val isZoomIn = when (event.keyCode) {
                    KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_LEFT_BRACKET -> false
                    KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_RIGHT_BRACKET -> true
                    else -> return false
                }

                currentZoomDirection = isZoomIn

                performZoom(isZoomIn)

                continuousGestureJob = launchContinuousGesture(
                    backgroundScope = backgroundScope,
                    gestureManager = gestureManager,
                    initialDelay = settings.zoomDuration,
                    condition = { currentZoomDirection == isZoomIn },
                    action = { performZoom(isZoomIn, true) }
                )
            }

            KeyEvent.ACTION_UP -> {
                currentAction = null
                cancelContinuousGesture()
            }
        }
        return true
    }

    private suspend fun handleActionKey(event: KeyEvent): Boolean {
        return when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                currentAction = CurrentAction.ACTION
                handleActionKeyDown()
            }

            KeyEvent.ACTION_UP -> {
                currentAction = null
                handleActionKeyUp()
            }

            else -> true
        }
    }

    private suspend fun startMovingCursor(direction: CursorDirection) {
        activeDirections.add(direction)
        lastMovementTime = System.currentTimeMillis()

        if (movementJob == null) {
            moveCursor(direction)

            movementJob = backgroundScope.launch {
                while (activeDirections.isNotEmpty()) {
                    moveCursor(direction)
                    delay(CursorConstants.POLLING_DURATION_MS.toLong())
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

        val service = AppAccessibilityService.getInstance()
        val clickable = service?.isNodeClickable(cursorStateManager.cursorState.value?.position) == true && service.showClickableInCurrentApp()
        cursorStateManager.updateClickable(clickable)
    }

    private suspend fun moveCursor(direction: CursorDirection) {
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
        if (!isDragging) {
            if (isLongPressing && lastDragPosition != null) {
                isDragging = true
                dragToNewPosition(lastDragPosition!!, newPosition)
                return
            }
        } else {
            if (gestureManager.getGestureReady()) isDragging = false
        }

        if (settings.cursorEdgeBehavior == ScreenEdgeBehavior.AUTO_SCROLL) {
            currentScreenEdge = cursorStateManager.checkEdge(direction, newPosition)
            if (currentScreenEdge != ScreenEdge.NONE && slowScrollJob == null) {
                slowScrollJob = launchContinuousGesture(
                    backgroundScope = backgroundScope,
                    gestureManager = gestureManager,
                    initialDelay = settings.scrollDuration,
                    condition = { currentScreenEdge != ScreenEdge.NONE },
                    action = {
                        performSlowScroll(currentScreenEdge!!)
                    }
                )
            }
        }
    }

    private suspend fun performSlowScroll(edge: ScreenEdge): Boolean {
        var direction: ScrollDirection? = null
        val dimensions = dimensionsFlow.value
        var x = dimensions.width / 2f
        var y = dimensions.height / 2f
        val cursorState = cursorStateManager.cursorState.value
        val settings = settingsFlow.value

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

        if (direction != null) {
            gestureManager.performScroll(
                direction,
                startX = x,
                startY = y,
                duration = if (settings.useAdvancedScrolling) settings.edgeScrollDuration else GestureConstants.DEFAULT_EDGE_SCROLL_DURATION,
                useNaturalScrolling = false,
                forceFixedGesture = true,
                distanceFactor = if (settings.useAdvancedScrolling) settings.edgeScrollMultiplier else GestureConstants.DEFAULT_EDGE_SCROLL_MULTIPLIER
            )
        }
        return true
    }

    private suspend fun performScroll(direction: ScrollDirection, forceFixedGesture: Boolean = false): Boolean {
        val cursorState = cursorStateManager.cursorState.value ?: return false
        val settings = settingsFlow.value

        if (!settings.useAdvancedScrolling) {
            gestureManager.performScroll(direction, startX = cursorState.position.x, startY = cursorState.position.y, forceFixedGesture = forceFixedGesture)
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
                startX = cursorState.position.x,
                startY = cursorState.position.y,
                duration = (settings.scrollDuration + accelerationFactor * (settings.continuousScrollDuration - settings.scrollDuration)).toLong(),
                forceFixedGesture = forceFixedGesture,
                distanceFactor = settings.scrollMultiplier + accelerationFactor * (settings.continuousScrollMultiplier - settings.scrollMultiplier)
            )
        }
        return true
    }

    private suspend fun performZoom(isZoomIn: Boolean, forceFixedGesture: Boolean = false): Boolean {
        val cursorState = cursorStateManager.cursorState.value ?: return false
        val orientation = orientationProvider()

        gestureManager.performZoom(isZoomIn, cursorState.position.x, cursorState.position.y, orientation, forceFixedGesture = forceFixedGesture)

        return true
    }

    private suspend fun handleActionKeyDown(): Boolean {
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
                allowTap = !cursorState.isHoldActive
            }

            if (!isLongPressing && allowTap) {
                isLongPressing = true
                lastDragPosition = cursorState.position

                gestureManager.startTap(cursorState.position.x, cursorState.position.y)
            }
        }

        return true
    }

    private suspend fun handleActionKeyUp(): Boolean {
        val settings = settingsFlow.value
        val cursorState = cursorStateManager.cursorState.value

        actionKeysPressed--

        if (cursorState != null) {
            if (!isLongPressing) {
                return false
            }

            if (!settings.toggleHold || !cursorState.isHoldActive) {
                endTap()
            }
        }

        return true
    }

    private suspend fun dragToNewPosition(fromPosition: Offset, toPosition: Offset) {
        lastDragPosition = toPosition
        gestureManager.dragTap(
            fromPosition.x,
            fromPosition.y,
            toPosition.x,
            toPosition.y
        )
    }

    private suspend fun endTap(): Boolean {
        cursorStateManager.updateHoldState(false)
        isLongPressing = false
        lastDragPosition = null

        val cursorState = cursorStateManager.cursorState.value ?: return false
        gestureManager.endTap(cursorState.position.x, cursorState.position.y)
        return true
    }
}