package com.vardash.mafimushkil

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.Manifest
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.vardash.mafimushkil.auth.FcmTokenManager
import com.vardash.mafimushkil.auth.NotificationChannelHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MafiMushkilMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            FcmTokenManager.saveTokenForCurrentUser(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        NotificationChannelHelper.ensureOrderUpdatesChannel(this)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "MafiMushkil"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "You have a new update."
        val orderId = remoteMessage.data["orderId"].orEmpty()
        val status = remoteMessage.data["status"].orEmpty()

        val launchIntent = Intent(Intent.ACTION_VIEW).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            data = Uri.parse(buildOrdersDeepLink(orderId, status))
            setPackage(packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            orderId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NotificationChannelHelper.ORDER_UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (canPostNotifications()) {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
        }
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun buildOrdersDeepLink(orderId: String, status: String): String {
        val tab = if (status.lowercase() in setOf("completed", "cancelled")) 1 else 0
        return if (orderId.isBlank()) {
            "mafimushkil://orders?tab=$tab"
        } else {
            "mafimushkil://orders?tab=$tab&focusOrderId=${Uri.encode(orderId)}"
        }
    }
}
