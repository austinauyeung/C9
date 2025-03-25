package com.austinauyeung.nyuma.c9.grid.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.austinauyeung.nyuma.c9.R
import com.austinauyeung.nyuma.c9.core.constants.GridConstants
import com.austinauyeung.nyuma.c9.grid.domain.Grid
import com.austinauyeung.nyuma.c9.grid.domain.GridCell
import com.austinauyeung.nyuma.c9.grid.domain.GridLineVisibility

/**
 * Renders the grid cursor overlay.
 */
@Composable
fun GridOverlay(
    grid: Grid,
    opacity: Int = 0,
    hideNumbers: Boolean = false,
    gridLineVisibility: GridLineVisibility = GridLineVisibility.SHOW_ALL
) {
    val textMeasurer = rememberTextMeasurer()
    val gridBackground = colorResource(id = R.color.grid_background)
    val gridBorderColor = colorResource(id = R.color.grid_border)
    val layoutState = remember { mutableStateOf(IntSize.Zero) }

    val dimensions =
        remember(grid, layoutState.value.width, layoutState.value.height) {
            calculateGridDimensions(
                grid,
                layoutState.value.width.toFloat(),
                layoutState.value.height.toFloat(),
            )
        }

    val density = LocalDensity.current
    val fontSize = (minOf(
        dimensions.cellWidth,
        dimensions.cellHeight
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
        GridLineVisibility.FINAL_LEVEL_ONLY -> grid.level == grid.maxLevels
        GridLineVisibility.HIDE_ALL -> false
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { newSize ->
                layoutState.value = newSize
            },
    ) {
        drawRect(
            color = gridBackground.copy(alpha = backgroundAlpha),
            size = size,
        )

        if (!hideNumbers) {
            grid.cells.forEach { cell ->
                drawCell(
                    cell = cell,
                    dimensions = dimensions,
                    textMeasurer = textMeasurer,
                    textStyle = textStyle,
                )
            }
        }

        drawRect(
            color = Color.White,
            topLeft = Offset(dimensions.x, dimensions.y),
            size = Size(dimensions.width, dimensions.height),
            style = Stroke(width = borderStrokeWidth),
        )

        if (shouldShowGridLines) {
            drawGridLines(
                dimensions = dimensions,
                gridBorderColor = gridBorderColor,
                gridStrokeWidth = gridStrokeWidth,
            )
        }
    }
}

private data class GridDimensions(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val cellWidth: Float,
    val cellHeight: Float,
)

private fun calculateGridDimensions(
    grid: Grid,
    totalWidth: Float,
    totalHeight: Float,
): GridDimensions {
    var currentWidth = totalWidth
    var currentHeight = totalHeight
    var currentX = 0f
    var currentY = 0f

    val gridPath = mutableListOf<Grid>()
    var currentGrid: Grid? = grid
    while (currentGrid != null) {
        gridPath.add(currentGrid)
        currentGrid = currentGrid.parentGrid
    }
    gridPath.reverse()

    for (i in 0 until gridPath.size - 1) {
        currentWidth /= GridConstants.DIMENSION
        currentHeight /= GridConstants.DIMENSION

        val nextGrid = gridPath[i + 1]
        val parentCell = nextGrid.parentCell ?: continue

        currentX += parentCell.column * currentWidth
        currentY += parentCell.row * currentHeight
    }

    val cellWidth = currentWidth / GridConstants.DIMENSION
    val cellHeight = currentHeight / GridConstants.DIMENSION

    return GridDimensions(
        x = currentX,
        y = currentY,
        width = currentWidth,
        height = currentHeight,
        cellWidth = cellWidth,
        cellHeight = cellHeight,
    )
}

private fun DrawScope.drawCell(
    cell: GridCell,
    dimensions: GridDimensions,
    textMeasurer: TextMeasurer,
    textStyle: TextStyle,
) {
    val left = dimensions.x + (cell.column * dimensions.cellWidth)
    val top = dimensions.y + (cell.row * dimensions.cellHeight)

    val textLayoutResult =
        textMeasurer.measure(
            text = cell.number.toString(),
            style = textStyle,
        )

    val textOffset =
        Offset(
            x = left + (dimensions.cellWidth - textLayoutResult.size.width) / 2,
            y = top + (dimensions.cellHeight - textLayoutResult.size.height) / 2,
        )

    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = textOffset,
    )
}

private fun DrawScope.drawGridLines(
    dimensions: GridDimensions,
    gridBorderColor: Color,
    gridStrokeWidth: Float
) {
    for (i in 1 until GridConstants.DIMENSION) {
        drawLine(
            color = gridBorderColor,
            start = Offset(
                x = dimensions.x + (i * dimensions.width / GridConstants.DIMENSION),
                y = dimensions.y
            ),
            end = Offset(
                x = dimensions.x + (i * dimensions.width / GridConstants.DIMENSION),
                y = dimensions.y + dimensions.height
            ),
            strokeWidth = gridStrokeWidth,
        )

        drawLine(
            color = gridBorderColor,
            start = Offset(
                x = dimensions.x,
                y = dimensions.y + (i * dimensions.height / GridConstants.DIMENSION)
            ),
            end = Offset(
                x = dimensions.x + dimensions.width,
                y = dimensions.y + (i * dimensions.height / GridConstants.DIMENSION)
            ),
            strokeWidth = gridStrokeWidth,
        )
    }
}
