package com.austinauyeung.nyuma.c9.gesture.api

import com.austinauyeung.nyuma.c9.common.domain.ScrollDirection

/**
 * Strategy interface for gesture implementations.
 */
interface GestureStrategy {
    suspend fun performScroll(
        direction: ScrollDirection,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float
    ): Boolean

    suspend fun performZoom(
        isZoomIn: Boolean,
        startX1: Float, startY1: Float,
        startX2: Float, startY2: Float,
        endX1: Float, endY1: Float,
        endX2: Float, endY2: Float
    ): Boolean

    suspend fun startTap(x: Float, y: Float): Boolean

    suspend fun dragTap(fromX: Float, fromY: Float, toX: Float, toY: Float): Boolean

    suspend fun endTap(finalX: Float, finalY: Float): Boolean

    fun cancelTap(): Boolean
}