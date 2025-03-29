package com.austinauyeung.nyuma.c9.grid.domain

import com.austinauyeung.nyuma.c9.common.domain.ScreenDimensions
import com.austinauyeung.nyuma.c9.core.constants.GridConstants
import com.austinauyeung.nyuma.c9.core.logs.Logger
import kotlinx.coroutines.flow.StateFlow

/**
 * Navigates through grid hierarchy.
 */
class GridNavigator(private val dimensionsFlow: StateFlow<ScreenDimensions>) {

    private fun getCellIndexFromKey(key: Int): Int {
        if (key !in 1..9) {
            Logger.e("Invalid key: $key, must be 1-9")
            return -1
        }

        val row = (key - 1) / GridConstants.DIMENSION
        val col = (key - 1) % GridConstants.DIMENSION
        return row * GridConstants.DIMENSION + col
    }

    fun getSubgrid(grid: Grid, selectedCellIndex: Int): Grid {
        val selectedCell = grid.cells.getOrNull(selectedCellIndex)

        if (selectedCell == null) {
            Logger.e("Invalid cell index: $selectedCellIndex")
            return grid
        }

        return if (grid.level < grid.maxLevels) {
            Grid(
                cells = Grid.generateDefaultGrid(),
                level = grid.level + 1,
                maxLevels = grid.maxLevels,
                parentCell = selectedCell,
                parentGrid = grid,
                isVisible = true,
            )
        } else {
            grid.copy(
                parentCell = selectedCell,
                isVisible = true,
            )
        }
    }

    fun calculateClickCoordinates(
        grid: Grid,
        selectedCellIndex: Int
    ): Pair<Float, Float> {
        val selectedCell = grid.cells.getOrNull(selectedCellIndex)
        val dimensions = dimensionsFlow.value

        if (selectedCell == null) {
            Logger.e("Invalid cell index for click: $selectedCellIndex")
            return Pair(dimensions.width / 2f, dimensions.height / 2f)
        }

        try {
            val gridHierarchy = buildGridHierarchy(grid)

            var currentWidth = dimensions.width.toFloat()
            var currentHeight = dimensions.height.toFloat()
            var currentX = 0f
            var currentY = 0f

            for (i in 0 until gridHierarchy.size - 1) {
                currentWidth /= GridConstants.DIMENSION
                currentHeight /= GridConstants.DIMENSION

                val childGrid = gridHierarchy[i + 1]
                val parentCell = childGrid.parentCell

                if (parentCell == null) {
                    Logger.e("Missing parent cell in grid hierarchy at level ${i + 1}")
                    continue
                }

                currentX += parentCell.column * currentWidth
                currentY += parentCell.row * currentHeight
            }

            val finalCellWidth = currentWidth / GridConstants.DIMENSION
            val finalCellHeight = currentHeight / GridConstants.DIMENSION

            val x = currentX + (selectedCell.column * finalCellWidth) + (finalCellWidth / 2)
            val y = currentY + (selectedCell.row * finalCellHeight) + (finalCellHeight / 2)

            return Pair(x, y)
        } catch (e: Exception) {
            Logger.e("Error calculating click coordinates: ${e.message}", e)
            return Pair(dimensions.width / 2f, dimensions.height / 2f)
        }
    }

    private fun buildGridHierarchy(grid: Grid): List<Grid> {
        return buildList {
            var currentGrid: Grid? = grid
            while (currentGrid != null) {
                add(currentGrid)
                currentGrid = currentGrid.parentGrid
            }
        }.reversed()
    }

    // Returns (success, needsClick, cellIndex)
    fun processNumberKey(
        keyNumber: Int,
        currentGrid: Grid
    ): Triple<Boolean, Boolean, Int> {
        if (!currentGrid.isVisible) return Triple(false, false, -1)

        if (keyNumber !in 1..9) {
            Logger.w("Invalid key number: $keyNumber")
            return Triple(false, false, -1)
        }

        val cellIndex = getCellIndexFromKey(keyNumber)
        if (cellIndex < 0) {
            Logger.e("Invalid cell index calculated from key: $keyNumber")
            return Triple(false, false, -1)
        }

        return if (currentGrid.canCreateSubgrid()) {
            Triple(true, false, cellIndex)
        } else {
            Triple(true, true, cellIndex)
        }
    }

    fun isValidCellIndex(grid: Grid, cellIndex: Int?): Boolean {
        if (cellIndex == null) return false
        return cellIndex >= 0 && cellIndex < grid.cells.size
    }
}