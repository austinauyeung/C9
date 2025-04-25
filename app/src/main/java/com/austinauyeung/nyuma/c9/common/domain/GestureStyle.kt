package com.austinauyeung.nyuma.c9.common.domain

/**
 * Represents scroll and zoom style.
 */
enum class GestureStyle {
    FIXED,      // Fixed distance, may work better with media
    FIXED_2,    // Fixed distance, may work better in browsers
    INERTIA,    // Momentum-based
}
