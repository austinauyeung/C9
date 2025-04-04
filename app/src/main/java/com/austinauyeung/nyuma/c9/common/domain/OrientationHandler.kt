package com.austinauyeung.nyuma.c9.common.domain

import android.content.Context
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowInsets
import android.view.WindowManager
import com.austinauyeung.nyuma.c9.core.logs.Logger
import com.austinauyeung.nyuma.c9.core.util.OrientationUtil
import com.austinauyeung.nyuma.c9.settings.domain.OverlaySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Handles device orientation changes.
 */
class OrientationHandler(
    private val context: Context,
    private val settingsFlow: StateFlow<OverlaySettings>
) {
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _screenDimensions = MutableStateFlow(getPhysicalDimensions())
    val screenDimensions: StateFlow<ScreenDimensions> = _screenDimensions.asStateFlow()

    private val _currentOrientation = MutableStateFlow(getOrientation())
    val currentOrientation: StateFlow<OrientationUtil.Orientation> = _currentOrientation.asStateFlow()

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}

        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) {
                mainHandler.post {
                    updateScreenInfo()
                }
            }
        }
    }

    init {
        displayManager.registerDisplayListener(displayListener, mainHandler)

        settingsFlow.onEach {
            updateScreenInfo()
        }.launchIn(CoroutineScope(Dispatchers.Main + SupervisorJob()))

        updateScreenInfo()
    }

    private fun updateScreenInfo() {
        try {
            val settings = settingsFlow.value
            val dimensions = if (settings.usePhysicalSize) getPhysicalDimensions() else getUsableDimensions()
            _screenDimensions.value = dimensions

            val orientation = getOrientation()
            _currentOrientation.value = orientation

            Logger.i("Updated screen dimensions to: ${dimensions.width} x ${dimensions.height}")
        } catch (e: Exception) {
            Logger.e("Error updating screen dimensions", e)
        }
    }

    @Suppress("Deprecation")
    private fun getPhysicalDimensions(): ScreenDimensions {
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

    fun getSystemInsets(): Rect {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val insets = windowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            Rect(insets.left, insets.top, insets.right, insets.bottom)
        } else {
            Rect(0, 0, 0, 0)
        }
    }

    @Suppress("Deprecation")
    private fun getUsableDimensions(): ScreenDimensions {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val insets = windowMetrics.windowInsets
                .getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars() or
                            WindowInsets.Type.displayCutout()
                )
            val insetsWidth = insets.left + insets.right
            val insetsHeight = insets.top + insets.bottom

            val bounds = windowMetrics.bounds
            ScreenDimensions(
                bounds.width() - insetsWidth,
                bounds.height() - insetsHeight
            )
        } else {
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)
            ScreenDimensions(
                metrics.widthPixels,
                metrics.heightPixels
            )
        }
    }

    private fun getOrientation(): OrientationUtil.Orientation {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("Deprecation")
        val defaultRotation = windowManager.defaultDisplay.rotation

        val rotation = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) context.display.rotation else defaultRotation
        }.getOrDefault(defaultRotation)

        return OrientationUtil.getOrientationFromRotation(rotation)
    }

    fun getCurrentScreenDimensions(): ScreenDimensions {
        return _screenDimensions.value
    }

    fun getCurrentOrientation(): OrientationUtil.Orientation {
        return _currentOrientation.value
    }

    fun cleanup() {
        displayManager.unregisterDisplayListener(displayListener)
    }
}