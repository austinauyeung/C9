package com.austinauyeung.nyuma.c9.grid.handler

import com.austinauyeung.nyuma.c9.common.domain.ScreenDimensions
import com.austinauyeung.nyuma.c9.gesture.api.GestureManager
import com.austinauyeung.nyuma.c9.grid.domain.Grid
import com.austinauyeung.nyuma.c9.grid.domain.GridNavigator
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Manages grid state including visibility, grid hierarchy, and state transitions.
 */
class GridStateManager(
    private val gridNavigator: GridNavigator,
    private val gestureManager: GestureManager,
    private val settingsFlow: StateFlow<OverlaySettings>,
    private val dimensionsFlow: StateFlow<ScreenDimensions>,
    private val backgroundScope: CoroutineScope,
    private val onGridStateChanged: (Grid?) -> Unit
) {
    private val _gridState = MutableStateFlow<Grid?>(null)
    val gridState: StateFlow<Grid?> = _gridState.asStateFlow()

    init {
        _gridState
            .onEach { grid ->
                onGridStateChanged(grid)
            }
            .launchIn(CoroutineScope(Dispatchers.Main + SupervisorJob()))
    }

    fun isGridVisible(): Boolean = _gridState.value?.isVisible == true

    fun handleNumberKey(number: Int): Boolean {
        val currentGrid = _gridState.value ?: return false
        val settings = settingsFlow.value

        val (success, needsClick, cellIndex) = gridNavigator.processNumberKey(number, currentGrid)
        if (!success) return false

        if (!needsClick) {
            // Navigate to subgrid
            val newGrid = gridNavigator.getSubgrid(currentGrid, cellIndex)
            updateGrid(newGrid)
            return true
        } else {
            // Perform click at the selected cell
            performFinalAction(cellIndex, settings.persistOverlay)
            return true
        }
    }

    fun toggleGridVisibility() {
        if (_gridState.value == null) {
            showGrid()
        } else {
            hideGrid()
        }
    }

    private fun showGrid() {
        val settings = settingsFlow.value
        val newGrid = Grid.createGrid(settings)
        updateGrid(newGrid)
    }

    fun hideGrid() {
        updateGrid(null)
    }

    private fun updateGrid(grid: Grid?) {
        _gridState.value = grid
    }

    // Performs the final action when a cell is selected in the deepest grid level
    private fun performFinalAction(
        cellIndex: Int,
        persistOverlay: Boolean
    ) {
        val grid = _gridState.value
        if (grid != null && gridNavigator.isValidCellIndex(grid, cellIndex)) {
            val coordinates = gridNavigator.calculateClickCoordinates(grid, cellIndex)
            val (x, y) = coordinates

            backgroundScope.launch {
                gestureManager.startTap(x, y)
                gestureManager.endTap(x, y)
            }
        }

        if (!persistOverlay) {
            hideGrid()
        } else {
            showGrid()
        }
    }

    fun resetToMainGrid(force: Boolean = false) {
        val settings = settingsFlow.value
        if ((_gridState.value?.level != null && _gridState.value?.level!! > 1) || force) {
            val newGrid = Grid.createGrid(settings)
            updateGrid(newGrid)
        }
    }

    fun getCellCoordinates(cellIndex: Int?): Pair<Float, Float> {
        val grid = _gridState.value
        val dimensions = dimensionsFlow.value
        if (grid == null || !gridNavigator.isValidCellIndex(grid, cellIndex)) {
            // Default to screen center if grid or cell index is null/invalid
            return Pair(dimensions.width / 2f, dimensions.height / 2f)
        }

        return gridNavigator.calculateClickCoordinates(grid, cellIndex!!)
    }
}