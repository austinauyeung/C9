package com.austinauyeung.nyuma.c9.common.domain

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import com.austinauyeung.nyuma.c9.core.logs.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Handles device orientation changes.
 */
class OrientationHandler(private val context: Context) {
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _screenDimensions = MutableStateFlow(getPhysicalDimensions(context))
    val screenDimensions: StateFlow<ScreenDimensions> = _screenDimensions.asStateFlow()

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}

        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) {
                mainHandler.post {
                    updateScreenDimensions()
                }
            }
        }
    }

    init {
        displayManager.registerDisplayListener(displayListener, mainHandler)
        updateScreenDimensions()
    }

    private fun updateScreenDimensions() {
        try {
            val dimensions = getPhysicalDimensions(context)
            _screenDimensions.value = dimensions

            Logger.i("Updated screen dimensions to: ${dimensions.width} x ${dimensions.height}")
        } catch (e: Exception) {
            Logger.e("Error updating screen dimensions", e)
        }
    }

    @Suppress("Deprecation")
    private fun getPhysicalDimensions(context: Context): ScreenDimensions {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            ScreenDimensions(bounds.width(), bounds.height())
        } else {
            val display = windowManager.defaultDisplay
            val metrics = DisplayMetrics()
            display.getRealMetrics(metrics)
            ScreenDimensions(metrics.widthPixels, metrics.heightPixels)
        }
    }

    fun cleanup() {
        displayManager.unregisterDisplayListener(displayListener)
    }
}