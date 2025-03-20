package com.austinauyeung.nyuma.c9.common.domain

/**
 * Handles screen size calculations.
 */
data class ScreenDimensions(
    val width: Int,
    val height: Int
) {

    fun center(): Pair<Float, Float> {
        return Pair(width / 2f, height / 2f)
    }

    fun isWithinBounds(x: Float, y: Float): Boolean {
        return x >= 0 && x <= width && y >= 0 && y <= height
    }

    fun constrainToBounds(x: Float, y: Float): Pair<Float, Float> {
        return Pair(
            x.coerceIn(0f, width.toFloat()),
            y.coerceIn(0f, height.toFloat())
        )
    }

    fun percentOfSmallerDimension(percent: Float): Float {
        val smallerDimension = minOf(width, height)
        return smallerDimension * percent
    }
}