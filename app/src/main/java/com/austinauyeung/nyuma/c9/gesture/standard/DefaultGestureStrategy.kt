package com.austinauyeung.nyuma.c9.gesture.standard

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import com.austinauyeung.nyuma.c9.core.constants.GestureConstants
import com.austinauyeung.nyuma.c9.core.domain.GestureStyle
import com.austinauyeung.nyuma.c9.core.logs.Logger
import com.austinauyeung.nyuma.c9.gesture.api.GestureCompletionListener
import com.austinauyeung.nyuma.c9.gesture.api.GestureStrategy
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Implements gestures using the AccessibilityService API.
 */
class DefaultGestureStrategy(
    private val service: AccessibilityService,
    private val settingsFlow: StateFlow<OverlaySettings>
) : GestureStrategy {

    private val scrollPath = Path()
    private val tapPath = Path()
    private var activeStroke: GestureDescription.StrokeDescription? = null

    private suspend fun AccessibilityService.dispatchGestureAwait(
        gesture: GestureDescription,
        _onCompleted: (() -> Unit)? = null,
        _onCancelled: (() -> Unit)? = null
    ): Boolean =
        suspendCancellableCoroutine { cont ->
            val handler = Handler(Looper.getMainLooper())
            dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    _onCompleted?.invoke()
                    cont.resume(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    _onCancelled?.invoke()
                    cont.resume(false)
                }
            }, handler)

            cont.invokeOnCancellation {
                // Optional: cancel gesture if coroutine is cancelled
                // Not all versions support cancelling gestures, but can try:
                // dispatchGesture(gesture, null, handler) ?
            }
        }

    // Callbacks to pause for fixed gesture style
    private fun completeGestureCallback(completionListener: GestureCompletionListener?): AccessibilityService.GestureResultCallback {
        return object : AccessibilityService.GestureResultCallback () {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                completionListener?.onGestureCompleted(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                completionListener?.onGestureCompleted(true)
            }
        }
    }

    private fun pauseGestureCallback(
        stroke: GestureDescription.StrokeDescription,
        endX: Float,
        endY: Float,
        willContinue: Boolean,
        completionListener: GestureCompletionListener?
    ): AccessibilityService.GestureResultCallback {
        return object : AccessibilityService.GestureResultCallback() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onCompleted(gestureDescription: GestureDescription?) {
                val settings = settingsFlow.value
                if (willContinue) {
                    val pausePath = Path().apply {
                        when (settings.gestureStyle) {
                            GestureStyle.FIXED -> moveTo(endX, endY)
                            GestureStyle.FIXED_2 -> moveTo(endX / 2, endY / 2)
                            else -> moveTo(endX, endY)
                        }
                    }

                    val pauseStrokeDescription = stroke.continueStroke(
                        pausePath,
                        0,
                        GestureConstants.GESTURE_PAUSE,
                        false
                    )

                    val pauseGesture = GestureDescription.Builder()
                        .addStroke(pauseStrokeDescription)
                        .build()

                    service.dispatchGesture(pauseGesture, completeGestureCallback(completionListener), null)
                } else {
                    completionListener?.onGestureCompleted(true)
                }
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                completionListener?.onGestureCompleted(true)
            }
        }
    }

    private fun pauseGestureCallback(
        stroke1: GestureDescription.StrokeDescription,
        stroke2: GestureDescription.StrokeDescription,
        endX1: Float, endY1: Float,
        endX2: Float, endY2: Float,
        willContinue: Boolean,
        completionListener: GestureCompletionListener?
    ): AccessibilityService.GestureResultCallback {
        return object : AccessibilityService.GestureResultCallback() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onCompleted(gestureDescription: GestureDescription?) {
                if (willContinue) {
                    val finger1PausePath = Path().apply {
                        moveTo(endX1, endY1)
                    }

                    val finger2PausePath = Path().apply {
                        moveTo(endX2, endY2)
                    }

                    val stroke1Pause = stroke1.continueStroke(
                        finger1PausePath,
                        0,
                        GestureConstants.GESTURE_PAUSE,
                        false
                    )

                    val stroke2Pause = stroke2.continueStroke(
                        finger2PausePath,
                        0,
                        GestureConstants.GESTURE_PAUSE,
                        false
                    )

                    val pauseGesture = GestureDescription.Builder()
                        .addStroke(stroke1Pause)
                        .addStroke(stroke2Pause)
                        .build()

                    service.dispatchGesture(pauseGesture, completeGestureCallback(completionListener), null)
                } else {
                    completionListener?.onGestureCompleted(true)
                }
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                completionListener?.onGestureCompleted(true)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun performScroll(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        forceFixedGesture: Boolean,
        duration: Long,
        completionListener: GestureCompletionListener?
    ): Boolean {
        try {
            val settings = settingsFlow.value
            val willContinue = (settings.gestureStyle != GestureStyle.INERTIA) || forceFixedGesture

            Logger.d("DefaultGestureStrategy: performing scroll from ($startX, $startY) to ($endX, $endY)")

            if (!settings.forceSmootherGestures) {
                scrollPath.reset()
                scrollPath.moveTo(startX, startY)
                scrollPath.lineTo(endX, endY)

                val mainStrokeDescription =
                    GestureDescription.StrokeDescription(
                        scrollPath,
                        0,
                        duration,
                        willContinue
                    )

                val gesture =
                    GestureDescription.Builder()
                        .addStroke(mainStrokeDescription)
                        .build()

                service.dispatchGesture(
                    gesture,
                    pauseGestureCallback(
                        mainStrokeDescription,
                        endX,
                        endY,
                        willContinue,
                        completionListener
                    ),
                    null
                )
            } else {
                val steps = GestureConstants.calculateSteps(duration)
                val dx = (endX - startX) / steps
                val dy = (endY - startY) / steps
                fun dispatchStep(i: Int, prevStroke: GestureDescription.StrokeDescription? = null) {
                    if (i >= steps) return

                    val path = Path()
                    val x0 = startX + dx * i
                    val y0 = startY + dy * i
                    val x1 = startX + dx * (i + 1)
                    val y1 = startY + dy * (i + 1)

                    path.moveTo(x0, y0)
                    path.lineTo(x1, y1)

                    val stroke = if (prevStroke == null) {
                        GestureDescription.StrokeDescription(
                            path,
                            0,
                            duration / steps,
                            willContinue || (i < steps - 1)
                        )
                    } else {
                        prevStroke.continueStroke(
                            path,
                            0,
                            duration / steps,
                            willContinue || (i < steps - 1)
                        )
                    }

                    val gesture = GestureDescription.Builder()
                        .addStroke(stroke)
                        .build()

                    if (i < steps - 1) {
                        service.dispatchGesture(
                            gesture,
                            object : AccessibilityService.GestureResultCallback() {
                                override fun onCompleted(gestureDescription: GestureDescription?) {
                                    dispatchStep(i + 1, stroke)
                                }

                                override fun onCancelled(gestureDescription: GestureDescription?) {
                                    Logger.w("Gesture step $i was cancelled.")
                                    completionListener?.onGestureCompleted(true)
                                }
                            },
                            null
                        )
                    } else {
                        service.dispatchGesture(
                            gesture,
                            pauseGestureCallback(
                                stroke,
                                endX,
                                endY,
                                willContinue,
                                completionListener
                            ),
                            null
                        )
                    }
                }

                dispatchStep(0)
            }

            return true
        } catch (e: Exception) {
            Logger.e("Error performing gesture scroll", e)
            return false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun performZoom(
        isZoomIn: Boolean,
        startX1: Float, startY1: Float,
        startX2: Float, startY2: Float,
        endX1: Float, endY1: Float,
        endX2: Float, endY2: Float,
        forceFixedGesture: Boolean,
        completionListener: GestureCompletionListener?
    ): Boolean {
        try {
            val settings = settingsFlow.value
            val willContinue = settings.gestureStyle != GestureStyle.INERTIA || forceFixedGesture

            Logger.d("DefaultGestureStrategy: performing ${if (isZoomIn) "zoom in" else "zoom out"} gesture")

            if (!settings.forceSmootherGestures) {
                val path1 = Path()
                val path2 = Path()

                path1.moveTo(startX1, startY1)
                path1.lineTo(endX1, endY1)

                path2.moveTo(startX2, startY2)
                path2.lineTo(endX2, endY2)

                val stroke1 = GestureDescription.StrokeDescription(
                    path1,
                    0,
                    settings.zoomDuration,
                    willContinue
                )

                val stroke2 = GestureDescription.StrokeDescription(
                    path2,
                    0,
                    settings.zoomDuration,
                    willContinue
                )

                val gestureBuilder = GestureDescription.Builder()
                    .addStroke(stroke1)
                    .addStroke(stroke2)

                val gesture = gestureBuilder.build()

                service.dispatchGesture(
                    gesture,
                    pauseGestureCallback(
                        stroke1,
                        stroke2,
                        endX1,
                        endY1,
                        endX2,
                        endY2,
                        willContinue,
                        completionListener
                    ),
                    null
                )
            } else {
                val steps = GestureConstants.calculateSteps(settings.zoomDuration)
                val dx1 = (endX1 - startX1) / steps
                val dy1 = (endY1 - startY1) / steps
                val dx2 = (endX2 - startX2) / steps
                val dy2 = (endY2 - startY2) / steps

                @RequiresApi(Build.VERSION_CODES.O)
                fun dispatchStep(i: Int, prevStroke1: GestureDescription.StrokeDescription? = null, prevStroke2: GestureDescription.StrokeDescription? = null) {
                    if (i >= steps) return

                    val path1 = Path()
                    val path2 = Path()

                    path1.moveTo(startX1 + dx1 * i, startY1 + dy1 * i)
                    path1.lineTo(startX1 + dx1 * (i + 1), startY1 + dy1 * (i + 1))

                    path2.moveTo(startX2 + dx2 * i, startY2 + dy2 * i)
                    path2.lineTo(startX2 + dx2 * (i + 1), startY2 + dy2 * (i + 1))

                    val stroke1 = if (prevStroke1 == null) {
                        GestureDescription.StrokeDescription(
                            path1,
                            0,
                            settings.zoomDuration / steps,
                            willContinue || (i < steps - 1)
                        )
                    } else {
                        prevStroke1.continueStroke(
                            path1,
                            0,
                            settings.zoomDuration / steps,
                            willContinue || (i < steps - 1)
                        )
                    }

                    val stroke2 = if (prevStroke2 == null) {
                        GestureDescription.StrokeDescription(
                            path2,
                            0,
                            settings.zoomDuration / steps,
                            willContinue || (i < steps - 1)
                        )
                    } else {
                        prevStroke2.continueStroke(
                            path2,
                            0,
                            settings.zoomDuration / steps,
                            willContinue || (i < steps - 1)
                        )
                    }

                    val gesture = GestureDescription.Builder()
                        .addStroke(stroke1)
                        .addStroke(stroke2)
                        .build()

                    if (i < steps - 1) {
                        service.dispatchGesture(
                            gesture,
                            object : AccessibilityService.GestureResultCallback() {
                                override fun onCompleted(gestureDescription: GestureDescription?) {
                                    dispatchStep(i + 1, stroke1, stroke2)
                                }

                                override fun onCancelled(gestureDescription: GestureDescription?) {
                                    Logger.w("Gesture step $i was cancelled.")
                                    completionListener?.onGestureCompleted(true)
                                }
                            },
                            null
                        )
                    } else {
                        service.dispatchGesture(
                            gesture,
                            pauseGestureCallback(
                                stroke1,
                                stroke2,
                                endX1,
                                endY1,
                                endX2,
                                endY2,
                                willContinue,
                                completionListener
                            ),
                            null
                        )
                    }
                }

                dispatchStep(0)
            }

            return true

        } catch (e: Exception) {
            Logger.e("Error performing zoom gesture", e)
            return false
        }
    }

    override suspend fun immediateTap(x: Float, y: Float): Boolean {
        try {
            Logger.d("DefaultGestureStrategy: performing immediate tap at ($x, $y)")

            tapPath.reset()
            tapPath.moveTo(x, y)

            activeStroke = GestureDescription.StrokeDescription(
                tapPath,
                0,
                1,
            )

            val gesture = GestureDescription.Builder()
                .addStroke(activeStroke!!)
                .build()

            service.dispatchGesture(gesture, null, null)

            return true

        } catch (e: Exception) {
            Logger.e("Error performing tap", e)
            return false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun startTap(x: Float, y: Float, completionListener: GestureCompletionListener?): Boolean {
        try {
            Logger.d("DefaultGestureStrategy: starting tap at ($x, $y)")

            tapPath.reset()
            tapPath.moveTo(x, y)

            activeStroke = GestureDescription.StrokeDescription(
                tapPath,
                0,
                1,
                true // Hold gesture
            )

            val gesture = GestureDescription.Builder()
                .addStroke(activeStroke!!)
                .build()

            service.dispatchGestureAwait(
                gesture,
                {
                    Logger.d("DefaultGestureStrategy: start tap completed successfully")
                    completionListener?.onGestureCompleted(true)
                },
                {
                    Logger.d("DefaultGestureStrategy: start tap was cancelled")
                    completionListener?.onGestureCompleted(true)
                }
            )

            return true

        } catch (e: Exception) {
            cancelTap(completionListener)
            Logger.e("Error starting tap", e)
            return false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun dragTap(fromX: Float, fromY: Float, toX: Float, toY: Float, completionListener: GestureCompletionListener?): Boolean {
        try {
            Logger.d("DefaultGestureStrategy: dragging from ($fromX, $fromY) to ($toX, $toY)")

            if (activeStroke == null) {
                Logger.d("Cannot continue drag: no active long press")
                cancelTap(completionListener)
                return false
            }

            tapPath.reset()
            tapPath.moveTo(fromX, fromY)
            tapPath.lineTo(toX, toY)

            val continuedStroke = activeStroke!!.continueStroke(
                tapPath,
                0, // Continue immediately
                1,
                true
            )

            activeStroke = continuedStroke

            val gesture = GestureDescription.Builder()
                .addStroke(continuedStroke)
                .build()

            service.dispatchGestureAwait(
                gesture,
                {
                    Logger.d("DefaultGestureStrategy: drag completed successfully")
                    completionListener?.onGestureCompleted(true)
                },
                {
                    Logger.d("DefaultGestureStrategy: drag was cancelled")
                    completionListener?.onGestureCompleted(true)
                }
            )

            return true

        } catch (e: Exception) {
            cancelTap(completionListener)
            Logger.e("Error continuing drag", e)
            return false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun endTap(finalX: Float, finalY: Float, completionListener: GestureCompletionListener?): Boolean {
        try {
            Logger.d("DefaultGestureStrategy: ending tap at ($finalX, $finalY)")

            if (activeStroke == null) {
                Logger.d("Cannot end tap: no active tap operation")
                cancelTap(completionListener)
                return false
            }

            tapPath.reset()
            tapPath.moveTo(finalX, finalY)

            val finalStroke = activeStroke!!.continueStroke(
                tapPath,
                0,
                1,
                false
            )

            val gesture = GestureDescription.Builder()
                .addStroke(finalStroke)
                .build()

            service.dispatchGestureAwait(
                gesture,
                {
                    Logger.d("DefaultGestureStrategy: end tap completed successfully")
                    completionListener?.onGestureCompleted(true)
                },
                {
                    Logger.d("DefaultGestureStrategy: end tap was cancelled")
                    completionListener?.onGestureCompleted(true)
                }
            )

            return true

        } catch (e: Exception) {
            cancelTap(completionListener)
            Logger.e("Error ending tap", e)
            return false
        }
    }

    override fun cancelTap(completionListener: GestureCompletionListener?): Boolean {
        completionListener?.onGestureCompleted(true)

        if (activeStroke == null) {
            return false
        }

        activeStroke = null
        Logger.d("DefaultGestureStrategy: tap operation cancelled")
        return true
    }
}