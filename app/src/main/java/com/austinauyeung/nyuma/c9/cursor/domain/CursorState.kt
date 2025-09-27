package com.austinauyeung.nyuma.c9.cursor.domain

import androidx.compose.ui.geometry.Offset

/**
 * Represents the standard cursor's state.
 */
data class CursorState(
    val position: Offset,
    val isVisible: Boolean = true,
    val inScrollMode: Boolean = false,
    val isHoldActive: Boolean = false,
    val clickable: Boolean = false
)
