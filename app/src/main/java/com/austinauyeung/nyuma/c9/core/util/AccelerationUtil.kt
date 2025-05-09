package com.austinauyeung.nyuma.c9.core.util

object AccelerationUtil {
    fun cubicBezier(t: Float): Float {
        val p0 = 0f
        val p1 = 0.2f
        val p2 = 0.8f
        val p3 = 1f

        val u = 1 - t
        val tt = t * t
        val uu = u * u
        val uuu = uu * u
        val ttt = tt * t

        return uuu * p0 +
                3 * uu * t * p1 +
                3 * u * tt * p2 +
                ttt * p3
    }

    fun normalizeValue(value: Long, startValue: Long, endValue: Long): Float {
        return when {
            value <= startValue -> 0f
            value >= endValue -> 1f
            else -> (value - startValue) * 1f / (endValue - startValue)
        }
    }
}