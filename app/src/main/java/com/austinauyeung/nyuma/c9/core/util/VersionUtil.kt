package com.austinauyeung.nyuma.c9.core.util

import android.os.Build

object VersionUtil {
    fun isAndroid11(): Boolean {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.R
    }
}
