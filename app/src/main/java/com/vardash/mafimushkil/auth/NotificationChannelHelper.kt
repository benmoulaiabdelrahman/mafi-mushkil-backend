package com.vardash.mafimushkil.auth

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannelHelper {
    const val ORDER_UPDATES_CHANNEL_ID = "mafi_mushkil_order_updates_v2"

    fun ensureOrderUpdatesChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            ORDER_UPDATES_CHANNEL_ID,
            "Order Updates",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for order status updates"
            enableLights(true)
            lightColor = android.graphics.Color.WHITE
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 250L, 250L, 250L)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
