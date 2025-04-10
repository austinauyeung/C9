package com.austinauyeung.nyuma.c9.core.constants

object CursorConstants {
    // Speed
    const val MIN_SPEED = 1
    const val MAX_SPEED = 10
    const val DEFAULT_SPEED = 5
    const val DEFAULT_SPEED_MULTIPLIER = 20

    // Accelerated speed
    const val MIN_ACCELERATION = 1
    const val MAX_ACCELERATION = 10
    const val DEFAULT_ACCELERATION = 5
    const val DEFAULT_ACCELERATION_MULTIPLIER = 500
    const val MIN_ACCELERATION_THRESHOLD = 100L
    const val MAX_ACCELERATION_THRESHOLD = 500L
    const val DEFAULT_ACCELERATION_THRESHOLD = 300L

    // Size
    const val MIN_SIZE = 1
    const val MAX_SIZE = 10
    const val DEFAULT_SIZE = 5
    const val SIZE_MULTIPLIER = 8f

    // Appearance
    const val STANDARD_CURSOR_HEX = "FFFFFF"

    private const val POLLING_RATE = 240
    const val POLLING_DURATION_MS = 1000f / POLLING_RATE
    const val OPACITY = 0.7f

    const val TOGGLE_HOLD = false
}