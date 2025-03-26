package com.austinauyeung.nyuma.c9.gesture.standard

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import com.austinauyeung.nyuma.c9.common.domain.GestureStyle
import com.austinauyeung.nyuma.c9.common.domain.ScrollDirection
import com.austinauyeung.nyuma.c9.core.constants.GestureConstants
import com.austinauyeung.nyuma.c9.core.logs.Logger
import com.austinauyeung.nyuma.c9.gesture.api.GestureStrategy
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings
import kotlinx.coroutines.flow.StateFlow

/**
 * Implements gestures using the AccessibilityService API.
 */
class StandardGestureStrategy(
    private val service: AccessibilityService,
    private val settingsFlow: StateFlow<OverlaySettings>
) : GestureStrategy {

    private val scrollPath = Path()
    private val tapPath = Path()
    private var activeStroke: GestureDescription.StrokeDescription? = null

    // Callbacks to pause for fixed gesture style
    private fun createGestureCallback(
        stroke: GestureDescription.StrokeDescription,
        endX: Float,
        endY: Float
    ): AccessibilityService.GestureResultCallback? {
        val settings = settingsFlow.value

        if (settings.gestureStyle == GestureStyle.FIXED) {
            return object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    val pausePath = Path().apply {
                        moveTo(endX, endY)
                    }

                    val pauseStrokeDescription = stroke.continueStroke(
                        pausePath,
                        0,
                        GestureConstants.SCROLL_END_PAUSE,
                        false
                    )

                    val pauseGesture = GestureDescription.Builder()
                        .addStroke(pauseStrokeDescription)
                        .build()

                    service.dispatchGesture(pauseGesture, null, null)
                }
            }
        }
        return null
    }

    private fun createGestureCallback(
        stroke1: GestureDescription.StrokeDescription,
        stroke2: GestureDescription.StrokeDescription,
        endX1: Float, endY1: Float,
        endX2: Float, endY2: Float,
    ): AccessibilityService.GestureResultCallback? {
        val settings = settingsFlow.value

        if (settings.gestureStyle == GestureStyle.FIXED) {
            return object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Logger.d("StandardGestureStrategy: zoom completed")
                    val finger1PausePath = Path().apply {
                        moveTo(endX1, endY1)
                    }

                    val finger2PausePath = Path().apply {
                        moveTo(endX2, endY2)
                    }

                    val stroke1Pause = stroke1.continueStroke(
                        finger1PausePath,
                        0,
                        GestureConstants.SCROLL_END_PAUSE,
                        false
                    )

                    val stroke2Pause = stroke2.continueStroke(
                        finger2PausePath,
                        0,
                        GestureConstants.SCROLL_END_PAUSE,
                        false
                    )

                    val pauseGesture = GestureDescription.Builder()
                        .addStroke(stroke1Pause)
                        .addStroke(stroke2Pause)
                        .build()

                    service.dispatchGesture(pauseGesture, null, null)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Logger.d("StandardGestureStrategy: zoom cancelled")
                }
            }
        }
        return null
    }

    override fun performScroll(
        direction: ScrollDirection,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float
    ): Boolean {
        try {
            val settings = settingsFlow.value

            Logger.d("StandardGestureStrategy: performing scroll from ($startX, $startY) to ($endX, $endY)")

            scrollPath.reset()
            scrollPath.moveTo(startX, startY)
            scrollPath.lineTo(endX, endY)

            val mainStrokeDescription =
                GestureDescription.StrokeDescription(
                    scrollPath,
                    0,
                    settings.gestureDuration,
                    settings.gestureStyle == GestureStyle.FIXED
                )

            val gesture =
                GestureDescription.Builder()
                    .addStroke(mainStrokeDescription)
                    .build()

            service.dispatchGesture(
                gesture,
                createGestureCallback(mainStrokeDescription, endX, endY),
                null
            )

            return true
        } catch (e: Exception) {
            Logger.e("Error performing gesture scroll", e)
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
        try {
            val settings = settingsFlow.value

            Logger.d("StandardGestureStrategy: performing ${if (isZoomIn) "zoom in" else "zoom out"} gesture")

            val path1 = Path()
            val path2 = Path()

            path1.moveTo(startX1, startY1)
            path1.lineTo(endX1, endY1)

            path2.moveTo(startX2, startY2)
            path2.lineTo(endX2, endY2)

            val stroke1 = GestureDescription.StrokeDescription(
                path1,
                0,
                settings.gestureDuration,
                settings.gestureStyle == GestureStyle.FIXED
            )

            val stroke2 = GestureDescription.StrokeDescription(
                path2,
                0,
                settings.gestureDuration,
                settings.gestureStyle == GestureStyle.FIXED
            )

            val gestureBuilder = GestureDescription.Builder()
                .addStroke(stroke1)
                .addStroke(stroke2)

            val gesture = gestureBuilder.build()

            service.dispatchGesture(
                gesture,
                createGestureCallback(stroke1, stroke2, endX1, endY1, endX2, endY2),
                null
            )

            return true

        } catch (e: Exception) {
            Logger.e("Error performing zoom gesture", e)
            return false
        }
    }

    override fun startTap(x: Float, y: Float): Boolean {
        try {
            Logger.d("StandardGestureStrategy: starting tap at ($x, $y)")

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

            service.dispatchGesture(
                gesture,
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        Logger.d("StandardGestureStrategy: start tap completed successfully")
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        Logger.d("StandardGestureStrategy: start tap was cancelled")
                    }
                },
                null
            )

            return true

        } catch (e: Exception) {
            cancelTap()
            Logger.e("Error starting tap", e)
            return false
        }
    }

    override fun dragTap(fromX: Float, fromY: Float, toX: Float, toY: Float): Boolean {
        try {
            Logger.d("StandardGestureStrategy: dragging from ($fromX, $fromY) to ($toX, $toY)")

            if (activeStroke == null) {
                Logger.d("Cannot continue drag: no active long press")
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

            service.dispatchGesture(
                gesture,
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        Logger.d("StandardGestureStrategy: drag completed successfully")
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        Logger.d("StandardGestureStrategy: drag was cancelled")
                        cancelTap()
                    }
                },
                null
            )

            return true

        } catch (e: Exception) {
            cancelTap()
            Logger.e("Error continuing drag", e)
            return false
        }
    }

    override fun endTap(finalX: Float, finalY: Float): Boolean {
        try {
            Logger.d("StandardGestureStrategy: ending tap at ($finalX, $finalY)")

            if (activeStroke == null) {
                Logger.d("Cannot end tap: no active tap operation")
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

            service.dispatchGesture(
                gesture,
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        activeStroke = null
                        Logger.d("StandardGestureStrategy: end tap completed successfully")
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        Logger.d("StandardGestureStrategy: end tap was cancelled")
                        cancelTap()
                    }
                },
                null
            )

            return true

        } catch (e: Exception) {
            cancelTap()
            Logger.e("Error ending tap", e)
            return false
        }
    }

    override fun cancelTap(): Boolean {
        if (activeStroke == null) {
            return false
        }

        activeStroke = null
        Logger.d("StandardGestureStrategy: tap operation cancelled")
        return true
    }
}