package com.vardash.mafimushkil.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.Routes
import com.vardash.mafimushkil.auth.OrderViewModel
import com.vardash.mafimushkil.models.Order
import com.vardash.mafimushkil.models.toEpochMillis
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily

// ── Data model ───────────────────────────────────────────
enum class NotificationType {
    ORDER_ACCEPTED,
    ORDER_CONFIRMED,
    ORDER_ASSIGNED,
    ORDER_COMPLETED,
    ORDER_CANCELLED,
    ANNOUNCEMENT
}

data class AppNotification(
    val id: String,
    val orderId: String,
    val type: NotificationType,
    val title: String,       // e.g. "Order"
    val boldWord: String,    // e.g. "Accepted" — shown bold inline
    val message: String,
    val timeAgo: String,
    val isRead: Boolean = false,
    val sortKey: Long = 0L,
    val targetTab: Int = 0
)

// ── Helpers ──────────────────────────────────────────────
fun notificationColor(type: NotificationType): Color = when (type) {
    NotificationType.ORDER_ACCEPTED  -> Color(0xFFFF9800)
    NotificationType.ORDER_CONFIRMED -> Color(0xFF7C3AED)
    NotificationType.ORDER_ASSIGNED  -> Color(0xFF2196F3)
    NotificationType.ORDER_COMPLETED -> Color(0xFF4CAF50)
    NotificationType.ORDER_CANCELLED -> Color(0xFFF44336)
    NotificationType.ANNOUNCEMENT    -> Color(0xFF282828)
}

fun notificationIcon(type: NotificationType): ImageVector = when (type) {
    NotificationType.ORDER_ACCEPTED  -> Icons.Outlined.CheckCircle
    NotificationType.ORDER_CONFIRMED -> Icons.Outlined.VerifiedUser
    NotificationType.ORDER_ASSIGNED  -> Icons.Outlined.Engineering
    NotificationType.ORDER_COMPLETED -> Icons.Outlined.CheckCircle
    NotificationType.ORDER_CANCELLED -> Icons.Outlined.Cancel
    NotificationType.ANNOUNCEMENT    -> Icons.Outlined.Notifications
}

private fun notificationBadgeLabel(type: NotificationType): String = when (type) {
    NotificationType.ORDER_ACCEPTED -> "مقبول"
    NotificationType.ORDER_CONFIRMED -> "مؤكد"
    NotificationType.ORDER_ASSIGNED -> "تم التعيين"
    NotificationType.ORDER_COMPLETED -> "مكتمل"
    NotificationType.ORDER_CANCELLED -> "ملغي"
    NotificationType.ANNOUNCEMENT -> "إشعار"
}

@Composable
private fun NotificationTypeBadge(type: NotificationType) {
    val badgeColor = notificationColor(type)
    Surface(
        shape = RoundedCornerShape(50.dp),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
        color = Color.Transparent
    ) {
        Text(
            text = notificationBadgeLabel(type),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = badgeColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            fontFamily = Questv1FontFamily
        )
    }
}

private fun notificationTypeForStatus(status: String): NotificationType? = when (status.lowercase()) {
    "accepted" -> NotificationType.ORDER_ACCEPTED
    "confirmed" -> NotificationType.ORDER_CONFIRMED
    "assigned" -> NotificationType.ORDER_ASSIGNED
    "in_progress" -> NotificationType.ORDER_ASSIGNED
    "completed" -> NotificationType.ORDER_COMPLETED
    "cancelled" -> NotificationType.ORDER_CANCELLED
    else -> null
}

private fun notificationBoldWordForStatus(status: String): String = when (status.lowercase()) {
    "accepted" -> "تم قبوله"
    "confirmed" -> "تم تأكيده"
    "assigned" -> "تم تعيين العمال"
    "in_progress" -> "قيد التنفيذ"
    "completed" -> "اكتمل الطلب"
    "cancelled" -> "تم إلغاؤه"
    else -> status
}

private fun notificationMessageForStatus(status: String): String = when (status.lowercase()) {
    "accepted" -> "تم قبول طلبك. يرجى مراجعة التفاصيل وتأكيدها."
    "confirmed" -> "تم تأكيد الطلب. سيتم تعيين العمال قريبًا."
    "assigned" -> "تم تعيين العمال لطلبك."
    "in_progress" -> "الطلب قيد التنفيذ الآن."
    "completed" -> "تم إكمال طلبك. شكراً لك!"
    "cancelled" -> "تم إلغاء طلبك."
    else -> "تم تحديث الطلب."
}

private fun notificationTargetTabForStatus(status: String): Int = when (status.lowercase()) {
    "completed", "cancelled" -> 1
    else -> 0
}

@Composable
private fun buildNotifications(orders: List<Order>): List<AppNotification> {
    return orders.mapNotNull { order ->
        val type = notificationTypeForStatus(order.status) ?: return@mapNotNull null
        val timestamp = order.updatedAt.takeIf { it.toEpochMillis() != 0L } ?: order.createdAt
        AppNotification(
            id = order.orderId.ifBlank { "${order.status}_${timestamp.toEpochMillis()}" },
            orderId = order.orderId,
            type = type,
            title = "تحديث الطلب",
            boldWord = notificationBoldWordForStatus(order.status),
            message = notificationMessageForStatus(order.status),
            timeAgo = formatTimeAgoLocalized(timestamp),
            sortKey = timestamp.toEpochMillis(),
            targetTab = notificationTargetTabForStatus(order.status)
        )
    }.sortedByDescending { it.sortKey }
}

// ── Notification card ────────────────────────────────────
@Composable
fun NotificationCard(
    notification: AppNotification,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Colored icon box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(notificationColor(notification.type)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = notificationIcon(notification.type),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Middle: title + message
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = buildAnnotatedString {
                            append(notification.title + " ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(notification.boldWord)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        color = Color(0xFF1A1A1A),
                        fontFamily = Questv1FontFamily
                    )
                    Spacer(Modifier.width(8.dp))
                    NotificationTypeBadge(notification.type)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = notification.message,
                    fontSize = 12.sp,
                    color = Color(0xFF888888),
                    lineHeight = 17.sp,
                    fontFamily = Questv1FontFamily
                )
            }

            Spacer(Modifier.width(8.dp))

            // Time with clock icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = Color(0xFFAAAAAA),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = notification.timeAgo,
                    fontSize = 11.sp,
                    color = Color(0xFFAAAAAA),
                    fontFamily = Questv1FontFamily
                )
            }
        }
    }
    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
}

// ── Main screen ──────────────────────────────────────────
@Composable
fun NotificationsScreen(
    navController: NavController,
    orderViewModel: OrderViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val pendingOrders by orderViewModel.pendingOrders.collectAsState()
    val completedOrders by orderViewModel.completedOrders.collectAsState()
    val notifications = buildNotifications(pendingOrders + completedOrders)
    var hasMarkedSeen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        orderViewModel.loadUserOrders()
    }

    LaunchedEffect(notifications.firstOrNull()?.sortKey, notifications.size) {
        if (!hasMarkedSeen && notifications.isNotEmpty()) {
            orderViewModel.markNotificationsSeen()
            hasMarkedSeen = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            // Wrap NavigationBar in a Box that extends white background behind system nav buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White) // white fills behind system buttons
            ) {
                AppBottomBar(navController = navController, selectedIndex = 3)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F8FA)) // Matched with Home and Order screens
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            // ── Wrap white top bar in Box that fills behind status bar ──────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White) // white fills behind status bar
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Text(
                        text = stringResource(R.string.nav_notifications),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        fontFamily = Questv1FontFamily
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

            // ── Body: list or empty state ─────────────────────
            if (notifications.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF7F8FA)), // Matched
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.notification),
                            contentDescription = stringResource(R.string.notifications_empty_title),
                            modifier = Modifier.size(220.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.notifications_empty_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A),
                            fontFamily = Questv1FontFamily
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.notifications_empty_desc),
                            fontSize = 14.sp,
                            color = Color(0xFFAAAAAA),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            fontFamily = Questv1FontFamily
                        )
                    }
                }
            } else {
                // Notifications list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF7F8FA)) // Matched
                ) {
                    items(notifications, key = { it.id }) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClick = {
                                orderViewModel.markNotificationsSeen()
                                hasMarkedSeen = true
                                navController.navigate(
                                    Routes.orders(
                                        tab = notification.targetTab,
                                        focusOrderId = notification.orderId
                                    )
                                ) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun NotificationsScreenPreview() {
    MafiMushkilTheme {
        NotificationsScreen(rememberNavController())
    }
}
