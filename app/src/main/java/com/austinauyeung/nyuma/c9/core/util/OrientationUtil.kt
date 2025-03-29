package com.austinauyeung.nyuma.c9.core.util

import android.view.KeyEvent
import android.view.Surface

object OrientationUtil {
    enum class Orientation {
        PORTRAIT,
        LANDSCAPE_RIGHT,
        PORTRAIT_UPSIDE_DOWN,
        LANDSCAPE_LEFT
    }

    fun getOrientationFromRotation(rotation: Int): Orientation {
        return when (rotation) {
            Surface.ROTATION_0 -> Orientation.PORTRAIT
            Surface.ROTATION_90 -> Orientation.LANDSCAPE_LEFT
            Surface.ROTATION_180 -> Orientation.PORTRAIT_UPSIDE_DOWN
            Surface.ROTATION_270 -> Orientation.LANDSCAPE_RIGHT
            else -> Orientation.PORTRAIT
        }
    }

    fun mapDPadKey(keyCode: Int, orientation: Orientation): Int {
        if (!isDpadDirection(keyCode)) return keyCode

        return when (orientation) {
            Orientation.PORTRAIT -> keyCode
            Orientation.LANDSCAPE_LEFT -> when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> KeyEvent.KEYCODE_DPAD_LEFT
                KeyEvent.KEYCODE_DPAD_RIGHT -> KeyEvent.KEYCODE_DPAD_UP
                KeyEvent.KEYCODE_DPAD_DOWN -> KeyEvent.KEYCODE_DPAD_RIGHT
                KeyEvent.KEYCODE_DPAD_LEFT -> KeyEvent.KEYCODE_DPAD_DOWN
                else -> keyCode
            }
            Orientation.PORTRAIT_UPSIDE_DOWN -> when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> KeyEvent.KEYCODE_DPAD_DOWN
                KeyEvent.KEYCODE_DPAD_RIGHT -> KeyEvent.KEYCODE_DPAD_LEFT
                KeyEvent.KEYCODE_DPAD_DOWN -> KeyEvent.KEYCODE_DPAD_UP
                KeyEvent.KEYCODE_DPAD_LEFT -> KeyEvent.KEYCODE_DPAD_RIGHT
                else -> keyCode
            }
            Orientation.LANDSCAPE_RIGHT -> when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> KeyEvent.KEYCODE_DPAD_RIGHT
                KeyEvent.KEYCODE_DPAD_RIGHT -> KeyEvent.KEYCODE_DPAD_DOWN
                KeyEvent.KEYCODE_DPAD_DOWN -> KeyEvent.KEYCODE_DPAD_LEFT
                KeyEvent.KEYCODE_DPAD_LEFT -> KeyEvent.KEYCODE_DPAD_UP
                else -> keyCode
            }
        }
    }

    fun mapNumberKey(keyCode: Int, orientation: Orientation): Int {
        if (!isNumberKey(keyCode)) return keyCode

        return when (orientation) {
            Orientation.PORTRAIT -> keyCode
            Orientation.LANDSCAPE_LEFT -> when (keyCode) {
                KeyEvent.KEYCODE_1 -> KeyEvent.KEYCODE_7
                KeyEvent.KEYCODE_2 -> KeyEvent.KEYCODE_4
                KeyEvent.KEYCODE_3 -> KeyEvent.KEYCODE_1
                KeyEvent.KEYCODE_4 -> KeyEvent.KEYCODE_8
                KeyEvent.KEYCODE_5 -> KeyEvent.KEYCODE_5
                KeyEvent.KEYCODE_6 -> KeyEvent.KEYCODE_2
                KeyEvent.KEYCODE_7 -> KeyEvent.KEYCODE_9
                KeyEvent.KEYCODE_8 -> KeyEvent.KEYCODE_6
                KeyEvent.KEYCODE_9 -> KeyEvent.KEYCODE_3
                else -> keyCode
            }
            Orientation.PORTRAIT_UPSIDE_DOWN -> when (keyCode) {
                KeyEvent.KEYCODE_1 -> KeyEvent.KEYCODE_9
                KeyEvent.KEYCODE_2 -> KeyEvent.KEYCODE_8
                KeyEvent.KEYCODE_3 -> KeyEvent.KEYCODE_7
                KeyEvent.KEYCODE_4 -> KeyEvent.KEYCODE_6
                KeyEvent.KEYCODE_5 -> KeyEvent.KEYCODE_5
                KeyEvent.KEYCODE_6 -> KeyEvent.KEYCODE_4
                KeyEvent.KEYCODE_7 -> KeyEvent.KEYCODE_3
                KeyEvent.KEYCODE_8 -> KeyEvent.KEYCODE_2
                KeyEvent.KEYCODE_9 -> KeyEvent.KEYCODE_1
                else -> keyCode
            }
            Orientation.LANDSCAPE_RIGHT -> when (keyCode) {
                KeyEvent.KEYCODE_1 -> KeyEvent.KEYCODE_3
                KeyEvent.KEYCODE_2 -> KeyEvent.KEYCODE_6
                KeyEvent.KEYCODE_3 -> KeyEvent.KEYCODE_9
                KeyEvent.KEYCODE_4 -> KeyEvent.KEYCODE_2
                KeyEvent.KEYCODE_5 -> KeyEvent.KEYCODE_5
                KeyEvent.KEYCODE_6 -> KeyEvent.KEYCODE_8
                KeyEvent.KEYCODE_7 -> KeyEvent.KEYCODE_1
                KeyEvent.KEYCODE_8 -> KeyEvent.KEYCODE_4
                KeyEvent.KEYCODE_9 -> KeyEvent.KEYCODE_7
                else -> keyCode
            }
        }
    }

    fun getRotatedGridNumbers(orientation: Orientation): Array<Array<Int>> {
        return when (orientation) {
            Orientation.PORTRAIT -> arrayOf(
                arrayOf(1, 2, 3),
                arrayOf(4, 5, 6),
                arrayOf(7, 8, 9)
            )
            Orientation.LANDSCAPE_RIGHT -> arrayOf(
                arrayOf(7, 4, 1),
                arrayOf(8, 5, 2),
                arrayOf(9, 6, 3)
            )
            Orientation.PORTRAIT_UPSIDE_DOWN -> arrayOf(
                arrayOf(9, 8, 7),
                arrayOf(6, 5, 4),
                arrayOf(3, 2, 1)
            )
            Orientation.LANDSCAPE_LEFT -> arrayOf(
                arrayOf(3, 6, 9),
                arrayOf(2, 5, 8),
                arrayOf(1, 4, 7)
            )
        }
    }

    fun isDpadDirection(keyCode: Int): Boolean {
        return keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT
    }

    fun isNumberKey(keyCode: Int): Boolean {
        return keyCode in KeyEvent.KEYCODE_1..KeyEvent.KEYCODE_9
    }

    fun getOrientationName(orientation: Orientation): String {
        return when (orientation) {
            Orientation.PORTRAIT -> "Portrait"
            Orientation.LANDSCAPE_RIGHT -> "Landscape Right"
            Orientation.PORTRAIT_UPSIDE_DOWN -> "Portrait Upside Down"
            Orientation.LANDSCAPE_LEFT -> "Landscape Left"
        }
    }
}