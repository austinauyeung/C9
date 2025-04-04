package com.austinauyeung.nyuma.c9.grid.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.austinauyeung.nyuma.c9.R
import com.austinauyeung.nyuma.c9.core.constants.GridConstants
import com.austinauyeung.nyuma.c9.core.util.OrientationUtil
import com.austinauyeung.nyuma.c9.grid.domain.Grid
import com.austinauyeung.nyuma.c9.grid.domain.GridLineVisibility

/**
 * Renders the grid cursor overlay.
 */
@Composable
fun GridOverlay(
    grid: Grid,
    opacity: Int = 0,
    hideNumbers: Boolean = false,
    orientation: OrientationUtil.Orientation = OrientationUtil.Orientation.PORTRAIT,
    useRotatedNumbers: Boolean = false,
    gridLineVisibility: GridLineVisibility = GridLineVisibility.SHOW_ALL
) {
    val textMeasurer = rememberTextMeasurer()
    val gridBackground = colorResource(id = R.color.grid_background)
    val gridBorderColor = colorResource(id = R.color.grid_border)

    val (cellWidth, cellHeight) = grid.getCellSize()

    val density = LocalDensity.current
    val fontSize = (minOf(
        cellWidth,
        cellHeight
    ) * GridConstants.GRID_FONT_SIZE / density.density).sp

    val textStyle =
        remember(fontSize) {
            TextStyle(
                color = Color.White,
                fontWeight = FontWeight.W300,
                fontSize = fontSize,
            )
        }

    val backgroundAlpha = remember(opacity) { opacity * 0.01f }

    val gridStrokeWidth = dimensionResource(R.dimen.grid_stroke_width).value
    val borderStrokeWidth = dimensionResource(R.dimen.grid_border_width).value

    val shouldShowGridLines = when (gridLineVisibility) {
        GridLineVisibility.SHOW_ALL -> true
        GridLineVisibility.FINAL_LEVEL_ONLY -> grid.level == GridConstants.MAX_LEVELS
        GridLineVisibility.HIDE_ALL -> false
    }

    val gridNumbers = remember(orientation, useRotatedNumbers) {
        if (useRotatedNumbers) {
            OrientationUtil.getRotatedGridNumbers(orientation)
        } else {
            GridConstants.INITIAL_NUMBERS
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        drawRect(
            color = gridBackground.copy(alpha = backgroundAlpha),
            size = size,
        )

        if (!hideNumbers) {
            for (row in 0 until GridConstants.DIMENSION) {
                for (col in 0 until GridConstants.DIMENSION) {
                    drawCell(
                        grid = grid,
                        row = row,
                        col = col,
                        textMeasurer = textMeasurer,
                        textStyle = textStyle,
                        gridNumbers = gridNumbers,
                    )
                }
            }
        }

        drawRect(
            color = Color.White,
            topLeft = Offset(grid.x, grid.y),
            size = Size(grid.width, grid.height),
            style = Stroke(width = borderStrokeWidth),
        )

        if (shouldShowGridLines) {
            drawGridLines(
                grid = grid,
                gridBorderColor = gridBorderColor,
                gridStrokeWidth = gridStrokeWidth,
            )
        }
    }
}

private fun DrawScope.drawCell(
    grid: Grid,
    row: Int, col: Int,
    textMeasurer: TextMeasurer,
    textStyle: TextStyle,
    gridNumbers: Array<Array<Int>>,
) {
    val (cellWidth, cellHeight) = grid.getCellSize()

    val left = grid.x + (col * cellWidth)
    val top = grid.y + (row * cellHeight)

    val cellNumber = gridNumbers[row][col]

    val textLayoutResult =
        textMeasurer.measure(
            text = cellNumber.toString(),
            style = textStyle,
        )

    val textOffset =
        Offset(
            x = left + (cellWidth - textLayoutResult.size.width) / 2,
            y = top + (cellHeight - textLayoutResult.size.height) / 2,
        )

    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = textOffset,
    )
}

private fun DrawScope.drawGridLines(
    grid: Grid,
    gridBorderColor: Color,
    gridStrokeWidth: Float
) {
    for (i in 1 until GridConstants.DIMENSION) {
        drawLine(
            color = gridBorderColor,
            start = Offset(
                x = grid.x + (i * grid.width / GridConstants.DIMENSION),
                y = grid.y
            ),
            end = Offset(
                x = grid.x + (i * grid.width / GridConstants.DIMENSION),
                y = grid.y + grid.height
            ),
            strokeWidth = gridStrokeWidth,
        )

        drawLine(
            color = gridBorderColor,
            start = Offset(
                x = grid.x,
                y = grid.y + (i * grid.height / GridConstants.DIMENSION)
            ),
            end = Offset(
                x = grid.x + grid.width,
                y = grid.y + (i * grid.height / GridConstants.DIMENSION)
            ),
            strokeWidth = gridStrokeWidth,
        )
    }
}
