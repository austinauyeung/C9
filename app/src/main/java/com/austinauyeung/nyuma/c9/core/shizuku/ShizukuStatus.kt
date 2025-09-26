package com.austinauyeung.nyuma.c9.core.shizuku

enum class ShizukuStatus {
    UNKNOWN,                // Initial state
    NOT_AVAILABLE,          // Shizuku not available
    PERMISSION_REQUIRED,    // Shizuku available but needs permission
    READY,                  // Shizuku ready
    ERROR,                  // Error during initialization
}
