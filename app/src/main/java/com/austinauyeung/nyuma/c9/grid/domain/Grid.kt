package com.austinauyeung.nyuma.c9.grid.domain

import androidx.compose.ui.geometry.Rect

/**
 * Represents the entire grid and supports recursive subgrids.
 */
data class Grid(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val level: Int
) {
    fun getBounds(): Rect = Rect(x, y, x + width, y + height)
    private fun getScreenCenter(): Pair<Float, Float> = Pair(x + width / 2, y + height / 2)
    fun getCellSize(): Pair<Float, Float> = Pair(width / 3f, height / 3f)
    fun getCellCenter(cellNumber: Int): Pair<Float, Float> {
        if (cellNumber < 1 || cellNumber > 9) return getScreenCenter()

        val (cellWidth, cellHeight) = getCellSize()
        val row = (cellNumber - 1) / 3
        val col = (cellNumber - 1) % 3
        val cellX = x + (col * cellWidth) + (cellWidth / 2)
        val cellY = y + (row * cellHeight) + (cellHeight / 2)

        return Pair(cellX, cellY)
    }
}
