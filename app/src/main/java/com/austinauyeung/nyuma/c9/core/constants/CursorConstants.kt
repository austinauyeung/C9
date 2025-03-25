package com.austinauyeung.nyuma.c9.core.constants

object CursorConstants {
    // Speed
    const val MIN_SPEED = 1
    const val MAX_SPEED = 5
    const val DEFAULT_SPEED = 3
    const val DEFAULT_SPEED_MULTIPLIER = 60
    const val DEFAULT_EXPONENT = 2

    // Accelerated speed
    const val MIN_ACCELERATION = 1
    const val MAX_ACCELERATION = 5
    const val DEFAULT_ACCELERATION = 3
    const val DEFAULT_ACCELERATION_MULTIPLIER = 120
    const val MIN_ACCELERATION_THRESHOLD = 100L
    const val MAX_ACCELERATION_THRESHOLD = 500L
    const val DEFAULT_ACCELERATION_THRESHOLD = 300L

    // Size
    const val MIN_SIZE = 1
    const val MAX_SIZE = 5
    const val DEFAULT_SIZE = 3
    const val SIZE_MULTIPLIER = 10f

    private const val TARGET_FPS = 60
    const val FRAME_DURATION_MS = 1000f / TARGET_FPS
    const val OPACITY = 0.8f
    const val CURSOR_WRAP_AROUND = false

    const val TOGGLE_HOLD = false
}