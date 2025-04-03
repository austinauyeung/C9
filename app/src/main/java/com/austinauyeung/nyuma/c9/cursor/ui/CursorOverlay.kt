package com.austinauyeung.nyuma.c9.cursor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.austinauyeung.nyuma.c9.common.domain.ScreenDimensions
import com.austinauyeung.nyuma.c9.core.constants.CursorConstants
import com.austinauyeung.nyuma.c9.cursor.domain.CursorState
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Renders the standard cursor overlay.
 */
@Composable
fun CursorOverlay(
    cursorState: CursorState,
    settings: OverlaySettings? = null,
    modifier: Modifier = Modifier,
    dimensions: ScreenDimensions
) {
    var cursorSize = settings?.cursorSize?.toFloat() ?: CursorConstants.DEFAULT_SIZE.toFloat()
    cursorSize *= CursorConstants.SIZE_MULTIPLIER * dimensions.getScreenScaleFactor()
    val opacity = CursorConstants.OPACITY

    Canvas(modifier = modifier.fillMaxSize()) {
        val position = cursorState.position

        // Arrowhead shape
        val side1Y = cursorSize * 1.0f
        val side4Y = cursorSize * 0.7f
        val side2X = cursorSize * 0.3f
        val side2Y = side1Y - side4Y
        val side2 = sqrt(side2X.pow(2) + side2Y.pow(2))
        val side3X = side2
        val holdOffsetX = cursorSize * 0.10f
        val holdOffsetY = cursorSize * 0.25f
        val scrollOffsetX = cursorSize * 0.28f

        val cornerRadius = cursorSize * if (settings?.roundedCursorCorners == true) 0.15f else 0f
        val theta = atan2(side2X, side1Y - side4Y)

        val cursorPath = Path().apply {
            moveTo(position.x, position.y + cornerRadius)
            lineTo(position.x, position.y + side1Y - cornerRadius)
            quadraticTo(position.x, position.y + side1Y, position.x + cornerRadius * sin(theta), position.y + side1Y - cornerRadius * cos(theta))
            lineTo(position.x + side2X - cornerRadius * sin(theta), position.y + side4Y + cornerRadius * cos(theta))
            quadraticTo(position.x + side2X, position.y + side4Y, position.x + side2X + cornerRadius, position.y + side4Y)
            lineTo(position.x + side2X + side3X - cornerRadius, position.y + side4Y)
            quadraticTo(position.x + side2X + side3X, position.y + side4Y, position.x + side2X + side3X - cornerRadius * sin(theta), position.y + side4Y - cornerRadius * cos(theta))
            lineTo(position.x + cornerRadius * cos(theta), position.y + cornerRadius * sin(theta))
            quadraticTo(position.x, position.y, position.x, position.y + cornerRadius)
            close()
        }

        drawPath(
            path = cursorPath,
            color = Color.White.copy(alpha = opacity),
        )

        drawPath(
            path = cursorPath,
            color = Color.Black.copy(alpha = opacity),
            style = Stroke(width = cursorSize * 0.1f),
        )

        // Trace bottom of cursor
        if (cursorState.isHoldActive) {
            val holdPath =
                Path().apply {
                    moveTo(position.x + holdOffsetX, position.y + side1Y + holdOffsetY)
                    lineTo(position.x + side2X + holdOffsetX, position.y + side4Y + holdOffsetY)
                    lineTo(
                        position.x + side2X + side3X + holdOffsetX,
                        position.y + side4Y + holdOffsetY
                    )
                }

            drawPath(
                path = holdPath,
                color = Color.Black,
                style = Stroke(width = cursorSize * 0.1f)
            )
        }

        // Add vertical bar
        if (cursorState.inScrollMode) {
            val lineX = position.x - scrollOffsetX
            val lineLength = cursorSize * 1.0f
            val lineHeight = cursorSize * 0.1f

            drawLine(
                color = Color.Black,
                start = Offset(x = lineX, y = position.y),
                end = Offset(x = lineX, y = position.y + lineLength),
                strokeWidth = lineHeight,
            )
        }
    }
}
