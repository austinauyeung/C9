package com.austinauyeung.nyuma.c9.gesture.api

import androidx.compose.ui.geometry.Offset
import com.austinauyeung.nyuma.c9.common.domain.ScreenDimensions
import com.austinauyeung.nyuma.c9.common.domain.ScrollDirection
import com.austinauyeung.nyuma.c9.core.constants.GestureConstants
import com.austinauyeung.nyuma.c9.core.logs.Logger
import com.austinauyeung.nyuma.c9.core.service.ShizukuServiceConnection
import com.austinauyeung.nyuma.c9.gesture.ui.GesturePath
import com.austinauyeung.nyuma.c9.gesture.ui.GestureType
import com.austinauyeung.nyuma.c9.gesture.ui.animateGesturePath
import com.austinauyeung.nyuma.c9.gesture.ui.showStationaryGesture
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Translates inputs from either cursor mode into gestures, using Shizuku if necessary.
 */
class GestureManager(
    private val defaultStrategy: GestureStrategy,
    private val shizukuStrategy: GestureStrategy,
    private val settingsFlow: StateFlow<OverlaySettings>,
    private val dimensionsFlow: StateFlow<ScreenDimensions>,
    private val serviceScope: CoroutineScope
) {
    private val _gesturePaths = MutableStateFlow<List<GesturePath>>(emptyList())
    val gesturePaths: StateFlow<List<GesturePath>> = _gesturePaths.asStateFlow()

    private var currentStrategy: GestureStrategy = defaultStrategy
    private var shizukuObserverJob: Job? = null

    private val _isReady = MutableStateFlow(true)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()
    private var gestureTimeoutJob: Job? = null

    fun setGestureReady(ready: Boolean) {
        _isReady.value = ready
    }

    init {
        evaluateStrategy()

        shizukuObserverJob = ShizukuServiceConnection.observeStatus { status ->
            Logger.d("Shizuku status changed to: $status, re-evaluating gesture strategy")
            serviceScope.launch {
                delay(1000)
                evaluateStrategy()
            }
        }

        serviceScope.launch {
            settingsFlow.collect {
                Logger.d("Settings changed, re-evaluating gesture strategy")
                evaluateStrategy()
            }
        }
    }

    private fun evaluateStrategy() {
        currentStrategy = if (shouldUseShizuku()) {
            Logger.d("Using Shizuku gesture strategy")
            shizukuStrategy
        } else {
            Logger.d("Using standard gesture strategy")
            defaultStrategy
        }
    }

    private var shouldShowGestures = false

    private fun shouldUseShizuku(): Boolean {
        val settings = settingsFlow.value
        if (!settings.enableShizukuIntegration) {
            return false
        }

        val isShizukuReady = ShizukuServiceConnection.isReady(forceRefresh = true)
        Logger.d("Shizuku ready status: $isShizukuReady")
        return isShizukuReady
    }

    private val completionListener = object : GestureCompletionListener {
        override fun onGestureCompleted(success: Boolean) {
            setGestureReady(true)
        }
    }

    suspend fun performScroll(
        direction: ScrollDirection,
        startX: Float = dimensionsFlow.value.width / 2f,
        startY: Float = dimensionsFlow.value.height / 2f,
        duration: Long = settingsFlow.value.gestureDuration,
        useNaturalScrolling: Boolean = settingsFlow.value.useNaturalScrolling,
        forceFixedScroll: Boolean = false,
        distanceFactor: Float = settingsFlow.value.scrollMultiplier
    ): Boolean {
        if (!_isReady.value) return false
        setGestureReady(false)

        gestureTimeoutJob?.cancel()
        gestureTimeoutJob = serviceScope.launch {
            val timeoutDuration = settingsFlow.value.gestureDuration * 3
            delay(timeoutDuration)
            if (!_isReady.value) {
                Logger.w("Gesture timed out after ${timeoutDuration}ms, resetting ready state")
                setGestureReady(true)
            }
        }

        val dimensions = dimensionsFlow.value
        try {
            Logger.d("Performing scroll gesture in direction $direction at position ($startX, $startY)")

            // Calculate motion direction based on natural scrolling setting
            val motionDirection = if (!useNaturalScrolling) {
                when (direction) {
                    ScrollDirection.UP -> ScrollDirection.DOWN
                    ScrollDirection.DOWN -> ScrollDirection.UP
                    ScrollDirection.LEFT -> ScrollDirection.RIGHT
                    ScrollDirection.RIGHT -> ScrollDirection.LEFT
                }
            } else {
                direction
            }

            val distance = when (motionDirection) {
                ScrollDirection.UP, ScrollDirection.DOWN -> dimensions.percentOfDimension(true, distanceFactor)
                ScrollDirection.LEFT, ScrollDirection.RIGHT -> dimensions.percentOfDimension(false, distanceFactor)
            }

            var (endX, endY) = when (motionDirection) {
                ScrollDirection.UP -> Pair(startX, startY - distance)
                ScrollDirection.DOWN -> Pair(startX, startY + distance)
                ScrollDirection.LEFT -> Pair(startX - distance, startY)
                ScrollDirection.RIGHT -> Pair(startX + distance, startY)
            }

            endX = endX.coerceIn(0f, dimensions.width.toFloat())
            endY = endY.coerceIn(0f, dimensions.height.toFloat())

            if (shouldShowGestures) {
                visualizeScroll(direction, startX, startY, endX, endY, duration)
            }

            val result = currentStrategy.performScroll(startX, startY, endX, endY, forceFixedScroll, duration, completionListener)
            delay(duration)

            return result
        } catch (e: Exception) {
            Logger.e("Error performing scroll gesture", e)
            setGestureReady(true)
            return false
        }
    }

    suspend fun performZoom(isZoomIn: Boolean, startX: Float, startY: Float): Boolean {
        if (!_isReady.value) return false
        setGestureReady(false)

        gestureTimeoutJob?.cancel()
        gestureTimeoutJob = serviceScope.launch {
            val timeoutDuration = settingsFlow.value.gestureDuration * 3
            delay(timeoutDuration)
            if (!_isReady.value) {
                Logger.w("Gesture timed out after ${timeoutDuration}ms, resetting ready state")
                setGestureReady(true)
            }
        }

        val dimensions = dimensionsFlow.value
        try {
            Logger.d("Performing ${if (isZoomIn) "zoom in" else "zoom out"} gesture at ($startX, $startY)")
            val randomness = 1
            val zoomDistance =
                dimensions.percentOfSmallerDimension(GestureConstants.ZOOM_DISTANCE_FACTOR) * randomness
            val zoomOffset =
                dimensions.percentOfSmallerDimension(GestureConstants.ZOOM_DISTANCE_OFFSET) * randomness

            var startX1 = startX * randomness - if (isZoomIn) zoomOffset else zoomDistance
            var startY1 = startY * randomness + if (isZoomIn) zoomOffset else zoomDistance
            var startX2 = startX * randomness + if (isZoomIn) zoomOffset else zoomDistance
            var startY2 = startY * randomness - if (isZoomIn) zoomOffset else zoomDistance

            var endX1 = startX * randomness - if (isZoomIn) zoomDistance else zoomOffset
            var endY1 = startY * randomness + if (isZoomIn) zoomDistance else zoomOffset
            var endX2 = startX * randomness + if (isZoomIn) zoomDistance else zoomOffset
            var endY2 = startY * randomness - if (isZoomIn) zoomDistance else zoomOffset

            startX1 = startX1.coerceIn(0f, dimensions.width.toFloat())
            startY1 = startY1.coerceIn(0f, dimensions.height.toFloat())
            startX2 = startX2.coerceIn(0f, dimensions.width.toFloat())
            startY2 = startY2.coerceIn(0f, dimensions.height.toFloat())
            endX1 = endX1.coerceIn(0f, dimensions.width.toFloat())
            endY1 = endY1.coerceIn(0f, dimensions.height.toFloat())
            endX2 = endX2.coerceIn(0f, dimensions.width.toFloat())
            endY2 = endY2.coerceIn(0f, dimensions.height.toFloat())

            if (shouldShowGestures) {
                visualizeZoomGesture(
                    startX1, startY1,
                    startX2, startY2,
                    endX1, endY1,
                    endX2, endY2
                )
            }

            return currentStrategy.performZoom(
                isZoomIn,
                startX1, startY1,
                startX2, startY2,
                endX1, endY1,
                endX2, endY2,
                completionListener
            )
        } catch (e: Exception) {
            Logger.e("Error performing zoom gesture", e)
            setGestureReady(true)
            return false
        }
    }

    suspend fun startTap(x: Float, y: Float): Boolean {
        try {
            Logger.d("Starting tap gesture at ($x, $y)")
            if (shouldShowGestures) {
                visualizeTap(x, y)
            }

            return currentStrategy.startTap(x, y)
        } catch (e: Exception) {
            Logger.e("Error starting tap gesture", e)
            cancelTap()
            return false
        }
    }

    suspend fun dragTap(fromX: Float, fromY: Float, toX: Float, toY: Float): Boolean {
        try {
            Logger.d("Dragging from ($fromX, $fromY) to ($toX, $toY)")
            return currentStrategy.dragTap(fromX, fromY, toX, toY)
        } catch (e: Exception) {
            Logger.e("Error during drag tap", e)
            cancelTap()
            return false
        }
    }

    suspend fun endTap(x: Float, y: Float): Boolean {
        try {
            Logger.d("Ending tap at ($x, $y)")
            return currentStrategy.endTap(x, y)
        } catch (e: Exception) {
            Logger.e("Error ending tap gesture", e)
            cancelTap()
            return false
        }
    }

    private fun cancelTap(): Boolean {
        try {
            Logger.d("Cancelling tap gesture")
            return currentStrategy.cancelTap()
        } catch (e: Exception) {
            Logger.e("Error cancelling tap gesture", e)
            return false
        }
    }

    private fun visualizeScroll(
        direction: ScrollDirection,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        duration: Long
    ) {
        val gestureId = "scroll_${System.currentTimeMillis()}_$direction"

        animateGesturePath(
            gestureId = gestureId,
            startPosition = Offset(startX, startY),
            endPosition = Offset(endX, endY),
            duration = duration,
            type = GestureType.SCROLL,
            pathsFlow = _gesturePaths,
            coroutineScope = serviceScope
        )
    }

    private fun visualizeTap(x: Float, y: Float) {
        val gestureId = "tap_${System.currentTimeMillis()}"
        showStationaryGesture(
            gestureId = gestureId,
            position = Offset(x, y),
            duration = GestureConstants.TAP_DURATION,
            type = GestureType.TAP,
            pathsFlow = _gesturePaths,
            coroutineScope = serviceScope
        )
    }

    private fun visualizeZoomGesture(
        finger1StartX: Float, finger1StartY: Float,
        finger2StartX: Float, finger2StartY: Float,
        finger1EndX: Float, finger1EndY: Float,
        finger2EndX: Float, finger2EndY: Float
    ) {
        val gestureId1 = "zoom_finger1_${System.currentTimeMillis()}"
        val gestureId2 = "zoom_finger2_${System.currentTimeMillis()}"
        val settings = settingsFlow.value

        animateGesturePath(
            gestureId = gestureId1,
            startPosition = Offset(finger1StartX, finger1StartY),
            endPosition = Offset(finger1EndX, finger1EndY),
            duration = settings.gestureDuration,
            type = GestureType.ZOOM_FINGER1,
            pathsFlow = _gesturePaths,
            coroutineScope = serviceScope
        )

        animateGesturePath(
            gestureId = gestureId2,
            startPosition = Offset(finger2StartX, finger2StartY),
            endPosition = Offset(finger2EndX, finger2EndY),
            duration = settings.gestureDuration,
            type = GestureType.ZOOM_FINGER2,
            pathsFlow = _gesturePaths,
            coroutineScope = serviceScope
        )
    }

    fun updateGestureVisibility(showGestures: Boolean) {
        shouldShowGestures = showGestures
    }

    fun cleanup() {
        shizukuObserverJob?.cancel()
        shizukuObserverJob = null
    }
}