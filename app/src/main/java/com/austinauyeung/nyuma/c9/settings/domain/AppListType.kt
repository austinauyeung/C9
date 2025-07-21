package com.austinauyeung.nyuma.c9.settings.domain

/**
 * Represents application list type for cursor auto-hide.
 */
enum class AppListType {
    ALLOW_LIST, // Auto-show for selected apps, auto-hide elsewhere
    DENY_LIST   // Auto-hide for selected apps, auto-show elsewhere
}
