package com.austinauyeung.nyuma.c9.core.constants

object GridConstants {
    const val DIMENSION = 3
    const val MIN_LEVELS = 2
    const val MAX_LEVELS = 4
    const val DEFAULT_LEVELS = 3
    const val PERSIST_OVERLAY = true
    const val HIDE_NUMBERS = false

    val INITIAL_NUMBERS = arrayOf(
        arrayOf(1, 2, 3),
        arrayOf(4, 5, 6),
        arrayOf(7, 8, 9),
    )

    const val MIN_OPACITY = 0
    const val MAX_OPACITY = 80
    const val DEFAULT_OPACITY = 20
    const val GRID_FONT_SIZE = 0.5f // Percentage of cell dimensions
}