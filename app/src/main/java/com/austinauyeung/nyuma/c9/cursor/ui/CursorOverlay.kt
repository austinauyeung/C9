package com.austinauyeung.nyuma.c9.cursor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.austinauyeung.nyuma.c9.core.constants.CursorConstants
import com.austinauyeung.nyuma.c9.core.domain.ScreenDimensions
import com.austinauyeung.nyuma.c9.cursor.domain.CursorState
import com.austinauyeung.nyuma.c9.cursor.domain.IconAlignment
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings
import java.io.File
import kotlin.math.PI
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
    val cursorColor = settings?.standardCursorHex?.let {
        try {
            Color("#$it".toColorInt())
        } catch (e: Exception) {
            Color.White
        }
    } ?: Color.White
    val matchBorder = settings?.standardCursorMatchBorder ?: false

    val iconImageUri = settings?.cursorImagePath?.takeIf {
        it.isNotEmpty() && File(it).exists()
    }
    val clickableImageUri = settings?.clickableImagePath?.takeIf {
        it.isNotEmpty() && File(it).exists()
    }
    val scrollToggleIconImageUri = settings?.scrollToggleImagePath?.takeIf {
        it.isNotEmpty() && File(it).exists()
    }

//    var customImageLoaded by remember { mutableStateOf(false) }
    val position = cursorState.position
    val density = LocalDensity.current
    val context = LocalContext.current

    val isClickable = cursorState.clickable && settings?.checkClickable == true

    Box(modifier = modifier
        .fillMaxSize()
        .border(
            width = if (cursorState.inScrollMode) 2.dp else 0.dp,
            color = cursorColor.copy(alpha = if (cursorState.inScrollMode && (settings?.useCustomCursorIcon != true || scrollToggleIconImageUri == null)) 1f else 0f),
        )
    ) {
        val showScrollToggleIcon = (settings?.useCustomCursorIcon == true) && (cursorState.inScrollMode) && (scrollToggleIconImageUri != null)
        val showClickableIcon = !showScrollToggleIcon && (settings?.useCustomCursorIcon == true) && (isClickable) && (clickableImageUri != null)
        val showBaseCustomIcon = !showScrollToggleIcon && !showClickableIcon && (settings?.useCustomCursorIcon == true)

        when {
            showScrollToggleIcon -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(scrollToggleIconImageUri)
                        .build(),
                    contentDescription = "Custom Scroll Toggle Cursor Icon",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(cursorSize.dp)
                        .offset(
                            x = with(density) { position.x.toDp() } - if (settings?.scrollToggleImageAlignment == IconAlignment.CENTER) (cursorSize/2).dp else 0.dp,
                            y = with(density) { position.y.toDp() } - if (settings?.scrollToggleImageAlignment == IconAlignment.CENTER) (cursorSize/2).dp else 0.dp
                        )
                )
            }
            showClickableIcon -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(clickableImageUri)
                        .build(),
                    contentDescription = "Custom Clickable Cursor Icon",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(cursorSize.dp)
                        .offset(
                            x = with(density) { position.x.toDp() } - if (settings?.clickableImageAlignment == IconAlignment.CENTER) (cursorSize/2).dp else 0.dp,
                            y = with(density) { position.y.toDp() } - if (settings?.clickableImageAlignment == IconAlignment.CENTER) (cursorSize/2).dp else 0.dp
                        )
                )
            }
            showBaseCustomIcon -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(iconImageUri)
                        .build(),
                    contentDescription = "Custom Cursor Icon",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(cursorSize.dp)
                        .offset(
                            x = with(density) { position.x.toDp() } - if (settings?.cursorImageAlignment == IconAlignment.CENTER) (cursorSize/2).dp else 0.dp,
                            y = with(density) { position.y.toDp() } - if (settings?.cursorImageAlignment == IconAlignment.CENTER) (cursorSize/2).dp else 0.dp
                        )
//                        .border(1.dp, Color.Black)
//                onSuccess = {
//                    Logger.d("Successfully loaded custom cursor image")
//                    customImageLoaded = true
//                },
//                onError = {
//                    Logger.e("Failed to load custom cursor image")
//                    customImageLoaded = false
//                }
                )
            }
            else -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawDefaultCursor(
                        position = position,
                        cursorSize = cursorSize,
                        opacity = opacity,
                        cursorColor = cursorColor,
                        matchBorder = matchBorder,
                        roundedCorners = settings?.roundedCursorCorners ?: false,
                        isHoldActive = cursorState.isHoldActive,
                        isClickable = isClickable
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawDefaultCursor(
    position: Offset,
    cursorSize: Float,
    opacity: Float,
    cursorColor: Color,
    matchBorder: Boolean,
    roundedCorners: Boolean,
    isHoldActive: Boolean,
    isClickable: Boolean
) {
    // Arrowhead shape
    val side1Y = cursorSize * 1.0f
    val side4Y = cursorSize * 0.7f
    val side2X = cursorSize * 0.3f
    val side2Y = side1Y - side4Y
    val side2 = sqrt(side2X.pow(2) + side2Y.pow(2))
    val side3X = side2
    val holdOffsetX = cursorSize * 0.10f
    val holdOffsetY = cursorSize * 0.25f

    val cornerRadius = cursorSize * if (roundedCorners) 0.15f else 0f
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
        color = cursorColor.copy(alpha = opacity),
    )

    drawPath(
        path = cursorPath,
        color = if (matchBorder) cursorColor else Color.Black,
        style = Stroke(width = cursorSize * 0.1f),
    )

    if (isClickable) {
        val radius = cursorSize * 0.25f
        val center = Offset(position.x, position.y)
        val rect = Rect(
            left = center.x - radius,
            top = center.y - radius,
            right = center.x + radius,
            bottom = center.y + radius
        )
        val padding = 1
        val angle = theta * 180 / PI
        val path = Path().apply {
            arcTo(
                rect = rect,
                startAngleDegrees = ((1 - padding) * angle).toFloat(),
                sweepAngleDegrees = -(360 - (90 - angle) - 2 * padding * angle).toFloat(),
                forceMoveTo = false
            )
        }
        drawPath(path, if (matchBorder) cursorColor else Color.Black, style = Stroke(width = cursorSize * 0.1f))
    }

    // Trace bottom of cursor
    if (isHoldActive) {
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
}
