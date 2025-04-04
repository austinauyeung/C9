package com.austinauyeung.nyuma.c9.grid.handler

import com.austinauyeung.nyuma.c9.common.domain.ScreenDimensions
import com.austinauyeung.nyuma.c9.core.logs.Logger
import com.austinauyeung.nyuma.c9.gesture.api.GestureManager
import com.austinauyeung.nyuma.c9.grid.domain.Grid
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
    private val gestureManager: GestureManager,
    private val settingsFlow: StateFlow<OverlaySettings>,
    private val dimensionsFlow: StateFlow<ScreenDimensions>,
    private val backgroundScope: CoroutineScope,
    private val onGridStateChanged: (Grid?) -> Unit
) {
    private val _gridState = MutableStateFlow<Grid?>(null)
    val gridState: StateFlow<Grid?> = _gridState.asStateFlow()
    private val keySequence = mutableListOf<Int>()

    init {
        _gridState
            .onEach { grid ->
                onGridStateChanged(grid)
            }
            .launchIn(CoroutineScope(Dispatchers.Main + SupervisorJob()))
    }

    fun isGridVisible(): Boolean = _gridState.value != null

    fun handleNumberKey(number: Int): Boolean {
        val settings = settingsFlow.value
        keySequence.add(number)
        Logger.d("Current grid sequence: $keySequence")
        if (keySequence.size >= settings.gridLevels) {
            performClick(number)
        } else {
            updateGrid(calculateGridFromSequence(keySequence))
        }

        return true
    }

    fun toggleGridVisibility() {
        if (_gridState.value == null) {
            showGrid()
        } else {
            hideGrid()
        }
    }

    private fun showGrid() {
        keySequence.clear()
        updateGrid(calculateGridFromSequence(keySequence))
    }

    fun hideGrid() {
        keySequence.clear()
        updateGrid(null)
    }

    private fun updateGrid(grid: Grid?) {
        _gridState.value = grid
    }

    // Performs the final action when a cell is selected in the deepest grid level
    private fun performClick(number: Int) {
        val settings = settingsFlow.value
        val grid = _gridState.value
        if (grid != null) {
            val coordinates = grid.getCellCenter(number)
            val (x, y) = coordinates

            backgroundScope.launch {
                gestureManager.startTap(x, y)
                gestureManager.endTap(x, y)
            }
        }

        if (!settings.persistOverlay) {
            hideGrid()
        } else {
            showGrid()
        }
    }

    fun resetToMainGrid(force: Boolean = false) {
        if ((_gridState.value?.level != null && _gridState.value?.level!! > 1) || force) {
            keySequence.clear()
            updateGrid(calculateGridFromSequence(keySequence))
        }
    }

    fun getCellCoordinates(number: Int?): Pair<Float, Float> {
        val grid = _gridState.value
        val dimensions = dimensionsFlow.value
        if (grid != null && number != null) return grid.getCellCenter(number)

        // Default to screen center if grid is null/invalid
        return Pair(dimensions.width / 2f, dimensions.height / 2f)
    }

    fun calculateGridFromSequence(sequence: List<Int>): Grid {
        var x = 0f
        var y = 0f
        val dimensions = dimensionsFlow.value
        var width = dimensions.width.toFloat()
        var height = dimensions.height.toFloat()

        sequence.forEach { number ->
            val row = (number - 1) / 3
            val col = (number - 1) % 3
            val cellWidth = width / 3
            val cellHeight = height / 3

            x += col * cellWidth
            y += row * cellHeight
            width = cellWidth
            height = cellHeight
        }

        return Grid(
            x = x,
            y = y,
            width = width,
            height = height,
            level = sequence.size
        )
    }
}