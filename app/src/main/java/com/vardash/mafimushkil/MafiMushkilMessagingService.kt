package com.vardash.mafimushkil

import android.content.Intent
import android.app.PendingIntent
import android.net.Uri
import android.os.Build
import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
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
        val badgeColor = notificationColorForStatus(status)

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
            .setLargeIcon(createStatusBadgeBitmap(badgeColor))
            .setColor(badgeColor)
            .setColorized(true)
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

    private fun notificationColorForStatus(status: String): Int {
        return when (status.lowercase()) {
            "accepted" -> android.graphics.Color.parseColor("#FF9800")
            "confirmed" -> android.graphics.Color.parseColor("#7C3AED")
            "assigned" -> android.graphics.Color.parseColor("#2196F3")
            "in_progress" -> android.graphics.Color.parseColor("#2196F3")
            "completed" -> android.graphics.Color.parseColor("#4CAF50")
            "cancelled" -> android.graphics.Color.parseColor("#F44336")
            else -> android.graphics.Color.parseColor("#282828")
        }
    }

    private fun createStatusBadgeBitmap(color: Int): Bitmap {
        val size = (64 * resources.displayMetrics.density).toInt().coerceAtLeast(64)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawRoundRect(rect, size * 0.18f, size * 0.18f, paint)
        return bitmap
    }
}
