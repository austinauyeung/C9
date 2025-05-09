package com.austinauyeung.nyuma.c9.core.constants

object GestureConstants {
    // Timing
    const val TAP_DURATION = 50L
    const val DRAG_SEGMENT_DURATION = 10L
    private const val FRAMES_PER_SECOND = 60
    private const val FRAME_DURATION_MS = 1000f / FRAMES_PER_SECOND
    const val GESTURE_PAUSE = 50L

    const val MIN_ACCELERATION_START = 0L
    const val MAX_ACCELERATION_START = 1000L
    const val DEFAULT_ACCELERATION_START = 300L
    const val MIN_ACCELERATION_DURATION = 0L
    const val MAX_ACCELERATION_DURATION = 1000L
    const val DEFAULT_ACCELERATION_DURATION = 0L

    const val MAX_MAX_ACCELERATION_START = 3000L
    const val MAX_MAX_ACCELERATION_DURATION = 3000L

    // Scroll
    const val MIN_SCROLL_MULTIPLIER = 0.1f
    const val MAX_SCROLL_MULTIPLIER = 1.0f
    const val DEFAULT_SCROLL_MULTIPLIER = 0.3f
    const val USE_NATURAL_SCROLLING = false
    const val MIN_SCROLL_DURATION = 100L
    const val MAX_SCROLL_DURATION = 500L
    const val DEFAULT_SCROLL_DURATION = 300L

//    const val DEFAULT_EDGE_SCROLL_DURATION = 150L
//    const val DEFAULT_EDGE_SCROLL_MULTIPLIER = 0.1f
    const val DEFAULT_EDGE_SCROLL_DURATION = MAX_SCROLL_DURATION
    const val DEFAULT_EDGE_SCROLL_MULTIPLIER = MIN_SCROLL_MULTIPLIER

    // Zoom
    const val ZOOM_DISTANCE_FACTOR = 0.16f
    const val ZOOM_DISTANCE_OFFSET = ZOOM_DISTANCE_FACTOR / 4
    const val MIN_ZOOM_DURATION = 100L
    const val MAX_ZOOM_DURATION = 500L
    const val DEFAULT_ZOOM_DURATION = 300L

    // Visualization
    const val SHOW_GESTURE_VISUAL = true
    const val MIN_SIZE = 1
    const val MAX_SIZE = 10
    const val DEFAULT_SIZE = 5
    const val SIZE_MULTIPLIER = 3f

    // Intercept
    const val ALLOW_PASSTHROUGH = false

    // Screen Edge
    const val SCREEN_EDGE_THRESHOLD = 0.02f

    // Steps needed to maintain frame rate
    fun calculateSteps(duration: Long): Int {
        return ((duration / FRAME_DURATION_MS).toInt()).coerceAtLeast(1)
    }
}