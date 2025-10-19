package com.austinauyeung.nyuma.c9.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.austinauyeung.nyuma.c9.R
import com.austinauyeung.nyuma.c9.core.control.ModeCoordinator
import com.austinauyeung.nyuma.c9.core.logs.Logger
import com.austinauyeung.nyuma.c9.settings.ui.SettingsActivity

/**
 * Manages notifications for active cursor modes.
 */
class NotificationManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "cursor_active_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active Cursor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when a cursor mode is active"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(mode: ModeCoordinator.OverlayMode) {
        try {
            val (title, text, icon) = when (mode) {
                ModeCoordinator.OverlayMode.GRID -> Triple(
                    "Grid Cursor Active",
                    "Tap to open settings",
                    R.drawable.ic_blur_on
                )
                ModeCoordinator.OverlayMode.CURSOR -> Triple(
                    "Standard Cursor Active",
                    "Tap to open settings",
                    R.drawable.ic_blur_on
                )
                ModeCoordinator.OverlayMode.AUTOHIDDEN -> Triple(
                    "Cursor Autohidden",
                    "Tap to open settings",
                    R.drawable.ic_blur_off
                )
                ModeCoordinator.OverlayMode.OFF -> {
                    hideNotification()
                    return
                }
            }

            val settingsIntent = Intent(context, SettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(icon)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setShowWhen(false)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
            Logger.d("Cursor notification shown for mode: $mode")

        } catch (e: Exception) {
            Logger.e("Error showing cursor notification", e)
        }
    }

    fun hideNotification() {
        try {
            notificationManager.cancel(NOTIFICATION_ID)
            Logger.d("Cursor notification hidden")
        } catch (e: Exception) {
            Logger.e("Error hiding cursor notification", e)
        }
    }

    fun cleanup() {
        hideNotification()
    }
}