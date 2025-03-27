package com.austinauyeung.nyuma.c9.gesture.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.austinauyeung.nyuma.c9.core.logs.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GesturePath(
    val id: String,
    val currentPosition: Offset,
    val type: GestureType,
    val startTime: Long,
)

enum class GestureType {
    TAP,
    SCROLL,
    ZOOM_FINGER1,
    ZOOM_FINGER2,
    LONG_PRESS,
}

/**
 * Defines gesture visualization paths rendered in the overlay UI manager.
 */
@Composable
fun GestureVisualization(
    gesturePaths: List<GesturePath>,
    modifier: Modifier = Modifier,
) {
    val circleColor = Color.White.copy(alpha = 0.8f)

    Canvas(modifier = modifier.fillMaxSize()) {
        gesturePaths.forEach { gesturePath ->
            drawCircle(
                color = Color.Black,
                radius = 15f,
                center = gesturePath.currentPosition,
                style = Stroke(width = 15f * 0.3f),
            )
            drawCircle(
                color = circleColor,
                radius = 15f,
                center = gesturePath.currentPosition,
            )
        }
    }
}

fun calculateInterpolatedPosition(
    startPosition: Offset,
    endPosition: Offset,
    fraction: Float,
): Offset {
    val x = startPosition.x + (endPosition.x - startPosition.x) * fraction
    val y = startPosition.y + (endPosition.y - startPosition.y) * fraction
    return Offset(x, y)
}

fun animateGesturePath(
    gestureId: String,
    startPosition: Offset,
    endPosition: Offset,
    duration: Long,
    type: GestureType,
    pathsFlow: MutableStateFlow<List<GesturePath>>,
    coroutineScope: CoroutineScope,
) {
    val path =
        GesturePath(
            id = gestureId,
            currentPosition = startPosition,
            type = type,
            startTime = System.currentTimeMillis(),
        )
    pathsFlow.update { it + path }

    coroutineScope.launch {
        val startTimeMs = System.currentTimeMillis()

        try {
            while (System.currentTimeMillis() - startTimeMs < duration) {
                val elapsedFraction =
                    (System.currentTimeMillis() - startTimeMs).toFloat() / duration
                val currentPosition =
                    calculateInterpolatedPosition(
                        startPosition,
                        endPosition,
                        elapsedFraction,
                    )

                pathsFlow.update { currentPaths ->
                    currentPaths.map {
                        if (it.id == gestureId) it.copy(currentPosition = currentPosition) else it
                    }
                }

                delay(16)
            }

            pathsFlow.update { currentPaths ->
                currentPaths.map {
                    if (it.id == gestureId) it.copy(currentPosition = endPosition) else it
                }
            }

            delay(50)
            pathsFlow.update { currentPaths ->
                currentPaths.filter { it.id != gestureId }
            }
        } catch (e: Exception) {
            Logger.e("Error updating gesture position", e)
            pathsFlow.update { currentPaths ->
                currentPaths.filter { it.id != gestureId }
            }
        }
    }
}

fun showStationaryGesture(
    gestureId: String,
    position: Offset,
    duration: Long,
    type: GestureType,
    pathsFlow: MutableStateFlow<List<GesturePath>>,
    coroutineScope: CoroutineScope,
) {
    val path =
        GesturePath(
            id = gestureId,
            currentPosition = position,
            type = type,
            startTime = System.currentTimeMillis(),
        )
    pathsFlow.update { it + path }

    coroutineScope.launch {
        delay(duration)
        pathsFlow.update { currentPaths ->
            currentPaths.filter { it.id != gestureId }
        }
    }
}
