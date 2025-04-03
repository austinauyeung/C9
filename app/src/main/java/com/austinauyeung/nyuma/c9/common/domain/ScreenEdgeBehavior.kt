package com.austinauyeung.nyuma.c9.common.domain

enum class ScreenEdgeBehavior {
    NONE,           // Stop cursor at the edge
    WRAP_AROUND,    // Wrap cursor to opposite edge
    AUTO_SCROLL     // Start slow scrolling in direction of edge
}

enum class ScreenEdge {
    NONE,
    TOP,
    BOTTOM,
    LEFT,
    RIGHT
}