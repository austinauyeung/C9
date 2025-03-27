package com.austinauyeung.nyuma.c9.gesture.shizuku

import android.annotation.SuppressLint
import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.MotionEvent
import com.austinauyeung.nyuma.c9.common.domain.GestureStyle
import com.austinauyeung.nyuma.c9.common.domain.ScrollDirection
import com.austinauyeung.nyuma.c9.core.constants.GestureConstants
import com.austinauyeung.nyuma.c9.core.logs.Logger
import com.austinauyeung.nyuma.c9.core.service.ShizukuServiceConnection
import com.austinauyeung.nyuma.c9.core.util.VersionUtil
import com.austinauyeung.nyuma.c9.gesture.api.GestureStrategy
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.Method

/**
 * Implements gestures using lower-level input injection via Shizuku.
 */
class ShizukuGestureStrategy(
    private val mainScope: CoroutineScope,
    private val settingsFlow: StateFlow<OverlaySettings>
) : GestureStrategy {

    private var _cachedIInputManagerInstance: Any? = null
    private val cachedIInputManagerInstance: Any?
        get() {
            return _cachedIInputManagerInstance ?: synchronized(this) {
                _cachedIInputManagerInstance = fetchIInputManagerInstance()
                _cachedIInputManagerInstance
            }
        }

    private var _cachedInjectInputEventMethod: Method? = null
    private val cachedInjectInputEventMethod: Method?
        @SuppressLint("PrivateApi")
        get() {
            if (_cachedInjectInputEventMethod == null) {
                synchronized(this) {
                    if (_cachedInjectInputEventMethod == null) {
                        try {
                            val iimClass = Class.forName("android.hardware.input.IInputManager")
                            _cachedInjectInputEventMethod = iimClass.getMethod(
                                "injectInputEvent",
                                InputEvent::class.java,
                                Int::class.java,
                            )
                        } catch (e: Exception) {
                            Logger.e("Failed to retrieve injectInputEvent method", e)
                        }
                    }
                }
            }
            return _cachedInjectInputEventMethod
        }

    private var _gestureActive = false
    private var _gestureDownTime = 0L
    private var _currentGestureX = 0f
    private var _currentGestureY = 0f

    private fun isAvailable(): Boolean {
        return VersionUtil.isAndroid11() && ShizukuServiceConnection.isReady()
    }

    @SuppressLint("PrivateApi")
    private fun fetchIInputManagerInstance(): Any? {
        if (!isAvailable()) return null

        try {
            val inputBinder = SystemServiceHelper.getSystemService("input")
            if (inputBinder == null) {
                Logger.e("Failed to get input service via ShizukuServiceConnection")
                return null
            }

            val stubClass = Class.forName("android.hardware.input.IInputManager\$Stub")
            val asInterfaceMethod =
                stubClass.getMethod("asInterface", android.os.IBinder::class.java)
            val iimInstance = asInterfaceMethod.invoke(null, ShizukuBinderWrapper(inputBinder))

            return iimInstance
        } catch (e: Exception) {
            Logger.e("Failed to get IInputManager instance via Shizuku", e)
            return null
        }
    }

    private fun injectEvent(event: MotionEvent): Boolean {
        val iimInstance =
            cachedIInputManagerInstance ?: fetchIInputManagerInstance() ?: return false

        return try {
            cachedInjectInputEventMethod?.invoke(iimInstance, event, 0) as? Boolean ?: false
        } catch (e: Exception) {
            Logger.e("Failed to inject event via Shizuku", e)
            false
        }
    }

    override fun performScroll(
        direction: ScrollDirection,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float
    ): Boolean {
        if (!isAvailable()) return false

        Logger.d("Using Shizuku to scroll $direction from ($startX, $startY) to ($endX, $endY)")

        try {
            mainScope.launch {
                val downTime = SystemClock.uptimeMillis()
                val duration = settingsFlow.value.gestureDuration
                val steps = GestureConstants.calculateSteps(duration)

                // Initial touch down event
                val downEvent =
                    createMotionEvent(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY)
                injectEvent(downEvent)
                downEvent.recycle()

                // Linear movement
                for (i in 1 until steps) {
                    val fraction = i.toFloat() / steps
                    val moveEvent = createMotionEvent(
                        downTime,
                        downTime + (duration * fraction).toLong(),
                        MotionEvent.ACTION_MOVE,
                        startX + (endX - startX) * fraction,
                        startY + (endY - startY) * fraction
                    )
                    injectEvent(moveEvent)
                    moveEvent.recycle()
                    delay(duration / steps)
                }

                val finalMoveEvent = createMotionEvent(
                    downTime,
                    downTime + duration,
                    MotionEvent.ACTION_MOVE,
                    endX,
                    endY
                )
                injectEvent(finalMoveEvent)
                finalMoveEvent.recycle()

                // Pause at the end for fixed scrolling
                if (settingsFlow.value.gestureStyle == GestureStyle.FIXED) {
                    delay(GestureConstants.SCROLL_END_PAUSE)
                }

                val upEvent = createMotionEvent(
                    downTime,
                    downTime + duration + GestureConstants.SCROLL_END_PAUSE,
                    MotionEvent.ACTION_UP,
                    endX,
                    endY
                )
                injectEvent(upEvent)
                upEvent.recycle()
            }

            return true
        } catch (e: Exception) {
            Logger.e("Error performing scroll via Shizuku", e)
            return false
        }
    }

    override fun performZoom(
        isZoomIn: Boolean,
        startX1: Float, startY1: Float,
        startX2: Float, startY2: Float,
        endX1: Float, endY1: Float,
        endX2: Float, endY2: Float
    ): Boolean {
        if (!isAvailable()) return false

        Logger.d("Using Shizuku to perform zoom gesture, isZoomIn: $isZoomIn")

        try {
            mainScope.launch {
                val downTime = SystemClock.uptimeMillis()
                val duration = settingsFlow.value.gestureDuration
                val steps = GestureConstants.calculateSteps(duration)
                val stepDuration = duration / steps
                val interEventDelayMs = 20L

                val firstFingerEvent = createMotionEvent(
                    downTime, downTime,
                    MotionEvent.ACTION_DOWN,
                    1,
                    intArrayOf(0),
                    floatArrayOf(startX1),
                    floatArrayOf(startY1)
                )

                if (!injectEvent(firstFingerEvent)) {
                    firstFingerEvent.recycle()
                    Logger.e("Failed to inject first finger down event")
                }
                firstFingerEvent.recycle()

                delay(interEventDelayMs)

                // Pointer of both fingers
                val pointerDownEvent = createMotionEvent(
                    downTime, downTime + interEventDelayMs,
                    MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    2,
                    intArrayOf(0, 1),
                    floatArrayOf(startX1, startX2),
                    floatArrayOf(startY1, startY2)
                )

                if (!injectEvent(pointerDownEvent)) {
                    pointerDownEvent.recycle()
                    Logger.e("Failed to inject second finger down event")
                }
                pointerDownEvent.recycle()

                var allMoveEventsSucceeded = true

                // Linear movement
                for (i in 1..steps) {
                    val fraction = i.toFloat() / steps
                    val currentTime = downTime + interEventDelayMs + (i * stepDuration)

                    val currentX1 = (startX1 + fraction * (endX1 - startX1))
                    val currentY1 = (startY1 + fraction * (endY1 - startY1))
                    val currentX2 = (startX2 + fraction * (endX2 - startX2))
                    val currentY2 = (startY2 + fraction * (endY2 - startY2))

                    val moveEvent = createMotionEvent(
                        downTime, currentTime,
                        MotionEvent.ACTION_MOVE,
                        2,
                        intArrayOf(0, 1),
                        floatArrayOf(currentX1, currentX2),
                        floatArrayOf(currentY1, currentY2)
                    )

                    val moveResult = injectEvent(moveEvent)
                    moveEvent.recycle()

                    if (!moveResult) {
                        allMoveEventsSucceeded = false
                        Logger.w("Failed to inject move event at step $i")
                    }

                    delay(stepDuration)
                }

                if (!allMoveEventsSucceeded) {
                    Logger.w("Some move events failed during zoom gesture")
                }

                val finalTime = downTime + duration + interEventDelayMs

                val pointerUpEvent = createMotionEvent(
                    downTime, finalTime,
                    MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    2,
                    intArrayOf(0, 1),
                    floatArrayOf(endX1, endX2),
                    floatArrayOf(endY1, endY2)
                )

                val pointerUpResult = injectEvent(pointerUpEvent)
                pointerUpEvent.recycle()

                if (!pointerUpResult) {
                    Logger.e("Failed to inject pointer up event")
                }

                delay(interEventDelayMs)

                val upEvent = createMotionEvent(
                    downTime, finalTime + interEventDelayMs,
                    MotionEvent.ACTION_UP,
                    1,
                    intArrayOf(0),
                    floatArrayOf(endX1),
                    floatArrayOf(endY1)
                )

                val upResult = injectEvent(upEvent)
                upEvent.recycle()

                if (!upResult) {
                    Logger.e("Failed to inject up event")
                }

                if (settingsFlow.value.gestureStyle == GestureStyle.FIXED) {
                    delay(GestureConstants.SCROLL_END_PAUSE)
                }
            }
            return true
        } catch (e: Exception) {
            Logger.e("Error performing scroll via Shizuku", e)
            return false
        }
    }

    override fun startTap(x: Float, y: Float): Boolean {
        if (!isAvailable()) return false

        Logger.d("Using Shizuku to start gesture at ($x, $y)")

        try {
            val downTime = SystemClock.uptimeMillis()
            _gestureDownTime = downTime
            _gestureActive = true

            val downEvent = createMotionEvent(downTime, downTime, MotionEvent.ACTION_DOWN, x, y)
            val result = injectEvent(downEvent)
            downEvent.recycle()

            _currentGestureX = x
            _currentGestureY = y

            return result
        } catch (e: Exception) {
            Logger.e("Error starting gesture via Shizuku", e)
            _gestureActive = false
            _gestureDownTime = 0
            return false
        }
    }

    override fun dragTap(fromX: Float, fromY: Float, toX: Float, toY: Float): Boolean {
        if (!isAvailable() || !_gestureActive || _gestureDownTime == 0L) return false

        Logger.d("Using Shizuku to continue gesture from ($fromX, $fromY) to ($toX, $toY)")

        try {
            mainScope.launch {
                val steps = GestureConstants.calculateSteps(GestureConstants.DRAG_SEGMENT_DURATION)
                val downTime = _gestureDownTime
                val startTime = SystemClock.uptimeMillis()

                for (i in 1..steps) {
                    val fraction = i.toFloat() / steps
                    val currentX = fromX + (toX - fromX) * fraction
                    val currentY = fromY + (toY - fromY) * fraction
                    val currentTime =
                        startTime + (GestureConstants.DRAG_SEGMENT_DURATION * fraction).toLong()

                    val moveEvent = createMotionEvent(
                        downTime,
                        currentTime,
                        MotionEvent.ACTION_MOVE,
                        currentX,
                        currentY
                    )
                    injectEvent(moveEvent)
                    moveEvent.recycle()

                    delay(GestureConstants.DRAG_SEGMENT_DURATION / steps)
                }

                _currentGestureX = toX
                _currentGestureY = toY
            }

            return true
        } catch (e: Exception) {
            Logger.e("Error continuing gesture via Shizuku", e)
            return false
        }
    }

    override fun endTap(finalX: Float, finalY: Float): Boolean {
        if (!isAvailable() || !_gestureActive || _gestureDownTime == 0L) return false

        Logger.d("Using Shizuku to end gesture at ($finalX, $finalY)")

        try {
            val downTime = _gestureDownTime
            val upTime = SystemClock.uptimeMillis()

            // Inject one more event if cursor moved from its last position
            if (_currentGestureX != finalX || _currentGestureY != finalY) {
                val finalMoveEvent = createMotionEvent(
                    downTime,
                    upTime - 5,
                    MotionEvent.ACTION_MOVE,
                    finalX,
                    finalY
                )
                injectEvent(finalMoveEvent)
                finalMoveEvent.recycle()
            }

            val upEvent = createMotionEvent(downTime, upTime, MotionEvent.ACTION_UP, finalX, finalY)
            val result = injectEvent(upEvent)
            upEvent.recycle()

            _gestureActive = false
            _gestureDownTime = 0
            _currentGestureX = 0f
            _currentGestureY = 0f

            return result
        } catch (e: Exception) {
            Logger.e("Error ending gesture via Shizuku", e)
            _gestureActive = false
            _gestureDownTime = 0
            return false
        }
    }

    override fun cancelTap(): Boolean {
        if (!isAvailable() || !_gestureActive) return false

        Logger.d("Using Shizuku to cancel gesture")

        try {
            if (_gestureDownTime != 0L) {
                val downTime = _gestureDownTime
                val cancelTime = SystemClock.uptimeMillis()

                val cancelEvent = createMotionEvent(
                    downTime,
                    cancelTime,
                    MotionEvent.ACTION_CANCEL,
                    _currentGestureX,
                    _currentGestureY
                )
                val result = injectEvent(cancelEvent)
                cancelEvent.recycle()

                _gestureActive = false
                _gestureDownTime = 0
                _currentGestureX = 0f
                _currentGestureY = 0f

                return result
            }

            _gestureActive = false
            _gestureDownTime = 0
            return true
        } catch (e: Exception) {
            Logger.e("Error canceling gesture via Shizuku", e)
            _gestureActive = false
            _gestureDownTime = 0
            return false
        }
    }

    private fun createMotionEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        x: Float,
        y: Float
    ): MotionEvent {
        return createMotionEvent(
            downTime,
            eventTime,
            action,
            1,
            intArrayOf(0),
            floatArrayOf(x),
            floatArrayOf(y)
        )
    }

    private fun createMotionEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        pointerCount: Int,
        pointerIds: IntArray,
        xs: FloatArray,
        ys: FloatArray
    ): MotionEvent {
        val properties = Array(pointerCount) { i ->
            MotionEvent.PointerProperties().apply {
                id = pointerIds[i]
                toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        }

        val coords = Array(pointerCount) { i ->
            MotionEvent.PointerCoords().apply {
                x = xs[i]
                y = ys[i]
                pressure = 1.0f
                size = 0.5f
            }
        }

        return MotionEvent.obtain(
            downTime, eventTime, action,
            pointerCount, properties, coords,
            0, 0, 1.0f, 1.0f, 0, 0,
            InputDevice.SOURCE_TOUCHSCREEN, 0
        )
    }

    fun reset() {
        _cachedIInputManagerInstance = null
        _cachedInjectInputEventMethod = null
        _gestureActive = false
        _gestureDownTime = 0L
    }

    fun shutdown() {
        Logger.d("Shutting down ShizukuInputHandler")
        _gestureActive = false
        _gestureDownTime = 0L
        reset()
    }
}