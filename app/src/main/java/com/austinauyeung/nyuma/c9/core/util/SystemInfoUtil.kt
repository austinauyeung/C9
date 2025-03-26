package com.austinauyeung.nyuma.c9.core.util

import android.content.Context
import android.os.Build

object SystemInfoUtil {
    fun getDeviceInfo(context: Context): String {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val density = displayMetrics.density

        return buildString {
            append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
            append("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
            append("Screen: $screenWidth x $screenHeight pixels ")
            append("(density $density)")
        }
    }
}