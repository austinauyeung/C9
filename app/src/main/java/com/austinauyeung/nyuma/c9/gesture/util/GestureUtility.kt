package com.austinauyeung.nyuma.c9.gesture.util

import com.austinauyeung.nyuma.c9.gesture.api.GestureManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object GestureUtility {
    fun launchContinuousGesture(
        backgroundScope: CoroutineScope,
        gestureManager: GestureManager,
        initialDelay: Long,
        condition: () -> Boolean,
        action: suspend () -> Unit
    ): Job {
        return backgroundScope.launch {
            delay(initialDelay)
            while (condition()) {
                gestureManager.isReady.collect { isReady ->
                    if (isReady) {
                        action()
                    }
                }
            }
        }
    }
}