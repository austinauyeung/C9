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

    const val GRID_FONT_SIZE = 5 // (/10 = percentage of cell dimensions)
    const val GRID_MIN_FONT_SIZE = 2
    const val GRID_MAX_FONT_SIZE = 8

    // Appearance
    const val GRID_BACKGROUND_HEX = "22000000"
    const val GRID_LINES_HEX = "22000000"
    const val GRID_NUMBERS_HEX = "22000000"
    const val GRID_LINE_WIDTH = 1
    const val GRID_LINE_MIN_WIDTH = 1
    const val GRID_LINE_MAX_WIDTH = 5
}