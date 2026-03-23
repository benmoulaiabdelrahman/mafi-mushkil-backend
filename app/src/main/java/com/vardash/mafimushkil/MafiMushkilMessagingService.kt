package com.vardash.mafimushkil

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.os.Build
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
            .setLargeIcon(createStatusBadgeBitmap(status))
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

    private fun createStatusBadgeBitmap(status: String): Bitmap {
        val size = (64 * resources.displayMetrics.density).toInt().coerceAtLeast(64)
        val iconSize = (30 * resources.displayMetrics.density).toInt().coerceAtLeast(30)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = notificationColorForStatus(status)
            style = Paint.Style.FILL
        }
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawRoundRect(rect, size * 0.18f, size * 0.18f, badgePaint)

        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }

        val paths = when (status.lowercase()) {
            "accepted", "completed" -> listOf(buildCheckCircleOutlinePath())
            "confirmed" -> listOf(buildVerifiedUserPath())
            "assigned", "in_progress" -> buildEngineeringPaths()
            "cancelled" -> listOf(buildCancelPath())
            else -> listOf(buildNotificationOutlinePath())
        }

        val left = (size - iconSize) / 2f
        val top = (size - iconSize) / 2f
        canvas.save()
        canvas.translate(left, top)
        canvas.scale(iconSize / 24f, iconSize / 24f)
        paths.forEach { canvas.drawPath(it, iconPaint) }
        canvas.restore()

        return bitmap
    }

    private fun buildCheckCircleOutlinePath(): Path = AndroidPathBuilder().apply {
        moveTo(12.0f, 2.0f)
        curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
        reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
        reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
        reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
        close()
        moveTo(12.0f, 20.0f)
        curveToRelative(-4.41f, 0.0f, -8.0f, -3.59f, -8.0f, -8.0f)
        reflectiveCurveToRelative(3.59f, -8.0f, 8.0f, -8.0f)
        reflectiveCurveToRelative(8.0f, 3.59f, 8.0f, 8.0f)
        reflectiveCurveToRelative(-3.59f, 8.0f, -8.0f, 8.0f)
        close()
        moveTo(16.59f, 7.58f)
        lineTo(10.0f, 14.17f)
        lineToRelative(-2.59f, -2.58f)
        lineTo(6.0f, 13.0f)
        lineToRelative(4.0f, 4.0f)
        lineToRelative(8.0f, -8.0f)
        close()
    }.path

    private fun buildVerifiedUserPath(): Path = AndroidPathBuilder().apply {
        moveTo(12.0f, 1.0f)
        lineTo(3.0f, 5.0f)
        verticalLineToRelative(6.0f)
        curveToRelative(0.0f, 5.55f, 3.84f, 10.74f, 9.0f, 12.0f)
        curveToRelative(5.16f, -1.26f, 9.0f, -6.45f, 9.0f, -12.0f)
        lineTo(21.0f, 5.0f)
        lineToRelative(-9.0f, -4.0f)
        close()
        moveTo(19.0f, 11.0f)
        curveToRelative(0.0f, 4.52f, -2.98f, 8.69f, -7.0f, 9.93f)
        curveToRelative(-4.02f, -1.24f, -7.0f, -5.41f, -7.0f, -9.93f)
        lineTo(5.0f, 6.3f)
        lineToRelative(7.0f, -3.11f)
        lineToRelative(7.0f, 3.11f)
        lineTo(19.0f, 11.0f)
        close()
        moveTo(7.41f, 11.59f)
        lineTo(6.0f, 13.0f)
        lineToRelative(4.0f, 4.0f)
        lineToRelative(8.0f, -8.0f)
        lineToRelative(-1.41f, -1.42f)
        lineTo(10.0f, 14.17f)
        close()
    }.path

    private fun buildCancelPath(): Path = AndroidPathBuilder().apply {
        moveTo(12.0f, 2.0f)
        curveTo(6.47f, 2.0f, 2.0f, 6.47f, 2.0f, 12.0f)
        reflectiveCurveToRelative(4.47f, 10.0f, 10.0f, 10.0f)
        reflectiveCurveToRelative(10.0f, -4.47f, 10.0f, -10.0f)
        reflectiveCurveTo(17.53f, 2.0f, 12.0f, 2.0f)
        close()
        moveTo(12.0f, 20.0f)
        curveToRelative(-4.41f, 0.0f, -8.0f, -3.59f, -8.0f, -8.0f)
        reflectiveCurveToRelative(3.59f, -8.0f, 8.0f, -8.0f)
        reflectiveCurveToRelative(8.0f, 3.59f, 8.0f, 8.0f)
        reflectiveCurveToRelative(-3.59f, 8.0f, -8.0f, 8.0f)
        close()
        moveTo(15.59f, 7.0f)
        lineTo(12.0f, 10.59f)
        lineTo(8.41f, 7.0f)
        lineTo(7.0f, 8.41f)
        lineTo(10.59f, 12.0f)
        lineTo(7.0f, 15.59f)
        lineTo(8.41f, 17.0f)
        lineTo(12.0f, 13.41f)
        lineTo(15.59f, 17.0f)
        lineTo(17.0f, 15.59f)
        lineTo(13.41f, 12.0f)
        lineTo(17.0f, 8.41f)
        close()
    }.path

    private fun buildNotificationOutlinePath(): Path = AndroidPathBuilder().apply {
        moveTo(12.0f, 2.0f)
        curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
        reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
        reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
        reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
        close()
    }.path

    private fun buildEngineeringPaths(): List<Path> {
        val first = AndroidPathBuilder().apply {
            moveTo(9.0f, 15.0f)
            curveToRelative(-2.67f, 0.0f, -8.0f, 1.34f, -8.0f, 4.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(16.0f)
            verticalLineToRelative(-2.0f)
            curveTo(17.0f, 16.34f, 11.67f, 15.0f, 9.0f, 15.0f)
            close()
            moveTo(3.0f, 19.0f)
            curveToRelative(0.22f, -0.72f, 3.31f, -2.0f, 6.0f, -2.0f)
            curveToRelative(2.7f, 0.0f, 5.8f, 1.29f, 6.0f, 2.0f)
            horizontalLineTo(3.0f)
            close()
        }.path

        val second = AndroidPathBuilder().apply {
            moveTo(4.74f, 9.0f)
            horizontalLineTo(5.0f)
            curveToRelative(0.0f, 2.21f, 1.79f, 4.0f, 4.0f, 4.0f)
            reflectiveCurveToRelative(4.0f, -1.79f, 4.0f, -4.0f)
            horizontalLineToRelative(0.26f)
            curveToRelative(0.27f, 0.0f, 0.49f, -0.22f, 0.49f, -0.49f)
            verticalLineTo(8.49f)
            curveToRelative(0.0f, -0.27f, -0.22f, -0.49f, -0.49f, -0.49f)
            horizontalLineTo(13.0f)
            curveToRelative(0.0f, -1.48f, -0.81f, -2.75f, -2.0f, -3.45f)
            verticalLineTo(5.5f)
            curveTo(11.0f, 5.78f, 10.78f, 6.0f, 10.5f, 6.0f)
            reflectiveCurveTo(10.0f, 5.78f, 10.0f, 5.5f)
            verticalLineTo(4.14f)
            curveTo(9.68f, 4.06f, 9.35f, 4.0f, 9.0f, 4.0f)
            reflectiveCurveTo(8.32f, 4.06f, 8.0f, 4.14f)
            verticalLineTo(5.5f)
            curveTo(8.0f, 5.78f, 7.78f, 6.0f, 7.5f, 6.0f)
            reflectiveCurveTo(7.0f, 5.78f, 7.0f, 5.5f)
            verticalLineTo(4.55f)
            curveTo(5.81f, 5.25f, 5.0f, 6.52f, 5.0f, 8.0f)
            horizontalLineTo(4.74f)
            curveTo(4.47f, 8.0f, 4.25f, 8.22f, 4.25f, 8.49f)
            verticalLineToRelative(0.03f)
            curveTo(4.25f, 8.78f, 4.47f, 9.0f, 4.74f, 9.0f)
            close()
            moveTo(11.0f, 9.0f)
            curveToRelative(0.0f, 1.1f, -0.9f, 2.0f, -2.0f, 2.0f)
            reflectiveCurveToRelative(-2.0f, -0.9f, -2.0f, -2.0f)
            horizontalLineTo(11.0f)
            close()
        }.path

        val third = AndroidPathBuilder().apply {
            moveTo(21.98f, 6.23f)
            lineToRelative(0.93f, -0.83f)
            lineToRelative(-0.75f, -1.3f)
            lineToRelative(-1.19f, 0.39f)
            curveToRelative(-0.14f, -0.11f, -0.3f, -0.2f, -0.47f, -0.27f)
            lineTo(20.25f, 3.0f)
            horizontalLineToRelative(-1.5f)
            lineTo(18.5f, 4.22f)
            curveToRelative(-0.17f, 0.07f, -0.33f, 0.16f, -0.48f, 0.27f)
            lineTo(16.84f, 4.1f)
            lineToRelative(-0.75f, 1.3f)
            lineToRelative(0.93f, 0.83f)
            curveTo(17.0f, 6.4f, 17.0f, 6.58f, 17.02f, 6.75f)
            lineToRelative(-0.93f, 0.85f)
            lineToRelative(0.75f, 1.3f)
            lineToRelative(1.2f, -0.38f)
            curveToRelative(0.13f, 0.1f, 0.28f, 0.18f, 0.43f, 0.25f)
            lineTo(18.75f, 10.0f)
            horizontalLineToRelative(1.5f)
            lineToRelative(0.27f, -1.22f)
            curveToRelative(0.16f, -0.07f, 0.3f, -0.15f, 0.44f, -0.25f)
            lineToRelative(1.19f, 0.38f)
            lineToRelative(0.75f, -1.3f)
            lineToRelative(-0.93f, -0.85f)
            curveTo(22.0f, 6.57f, 21.99f, 6.4f, 21.98f, 6.23f)
            close()
            moveTo(19.5f, 7.75f)
            curveToRelative(-0.69f, 0.0f, -1.25f, -0.56f, -1.25f, -1.25f)
            reflectiveCurveToRelative(0.56f, -1.25f, 1.25f, -1.25f)
            reflectiveCurveToRelative(1.25f, 0.56f, 1.25f, 1.25f)
            reflectiveCurveTo(20.19f, 7.75f, 19.5f, 7.75f)
            close()
        }.path

        val fourth = AndroidPathBuilder().apply {
            moveTo(19.4f, 10.79f)
            lineToRelative(-0.85f, 0.28f)
            curveToRelative(-0.1f, -0.08f, -0.21f, -0.14f, -0.33f, -0.19f)
            lineTo(18.04f, 10.0f)
            horizontalLineToRelative(-1.07f)
            lineToRelative(-0.18f, 0.87f)
            curveToRelative(-0.12f, 0.05f, -0.24f, 0.12f, -0.34f, 0.19f)
            lineToRelative(-0.84f, -0.28f)
            lineToRelative(-0.54f, 0.93f)
            lineToRelative(0.66f, 0.59f)
            curveToRelative(-0.01f, 0.13f, -0.01f, 0.25f, 0.0f, 0.37f)
            lineToRelative(-0.66f, 0.61f)
            lineToRelative(0.54f, 0.93f)
            lineToRelative(0.86f, -0.27f)
            curveToRelative(0.1f, 0.07f, 0.2f, 0.13f, 0.31f, 0.18f)
            lineTo(16.96f, 15.0f)
            horizontalLineToRelative(1.07f)
            lineToRelative(0.19f, -0.87f)
            curveToRelative(0.11f, -0.05f, 0.22f, -0.11f, 0.32f, -0.18f)
            lineToRelative(0.85f, 0.27f)
            lineToRelative(0.54f, -0.93f)
            lineToRelative(-0.66f, -0.61f)
            curveToRelative(0.01f, -0.13f, 0.01f, -0.25f, 0.0f, -0.37f)
            lineToRelative(0.66f, -0.59f)
            lineTo(19.4f, 10.79f)
            close()
            moveTo(17.5f, 13.39f)
            curveToRelative(-0.49f, 0.0f, -0.89f, -0.4f, -0.89f, -0.89f)
            curveToRelative(0.0f, -0.49f, 0.4f, -0.89f, 0.89f, -0.89f)
            reflectiveCurveToRelative(0.89f, 0.4f, 0.89f, 0.89f)
            curveTo(18.39f, 12.99f, 17.99f, 13.39f, 17.5f, 13.39f)
            close()
        }.path

        return listOf(first, second, third, fourth)
    }

    private class AndroidPathBuilder {
        private val internalPath = Path()
        val path: Path
            get() = internalPath
        private var currentX = 0f
        private var currentY = 0f
        private var lastCtrlX = 0f
        private var lastCtrlY = 0f
        private var hasCubicControl = false

        fun moveTo(x: Float, y: Float) {
            internalPath.moveTo(x, y)
            currentX = x
            currentY = y
            hasCubicControl = false
        }

        fun lineTo(x: Float, y: Float) {
            internalPath.lineTo(x, y)
            currentX = x
            currentY = y
            hasCubicControl = false
        }

        fun lineToRelative(dx: Float, dy: Float) {
            lineTo(currentX + dx, currentY + dy)
        }

        fun horizontalLineTo(x: Float) {
            lineTo(x, currentY)
        }

        fun horizontalLineToRelative(dx: Float) {
            lineTo(currentX + dx, currentY)
        }

        fun verticalLineTo(y: Float) {
            lineTo(currentX, y)
        }

        fun verticalLineToRelative(dy: Float) {
            lineTo(currentX, currentY + dy)
        }

        fun curveTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
            internalPath.cubicTo(x1, y1, x2, y2, x3, y3)
            lastCtrlX = x2
            lastCtrlY = y2
            currentX = x3
            currentY = y3
            hasCubicControl = true
        }

        fun curveToRelative(dx1: Float, dy1: Float, dx2: Float, dy2: Float, dx3: Float, dy3: Float) {
            curveTo(
                currentX + dx1,
                currentY + dy1,
                currentX + dx2,
                currentY + dy2,
                currentX + dx3,
                currentY + dy3
            )
        }

        fun reflectiveCurveTo(x2: Float, y2: Float, x3: Float, y3: Float) {
            val x1 = if (hasCubicControl) 2 * currentX - lastCtrlX else currentX
            val y1 = if (hasCubicControl) 2 * currentY - lastCtrlY else currentY
            curveTo(x1, y1, x2, y2, x3, y3)
        }

        fun reflectiveCurveToRelative(dx2: Float, dy2: Float, dx3: Float, dy3: Float) {
            val x1 = if (hasCubicControl) 2 * currentX - lastCtrlX else currentX
            val y1 = if (hasCubicControl) 2 * currentY - lastCtrlY else currentY
            curveTo(
                x1,
                y1,
                currentX + dx2,
                currentY + dy2,
                currentX + dx3,
                currentY + dy3
            )
        }

        fun close() {
            internalPath.close()
            hasCubicControl = false
        }
    }
}
