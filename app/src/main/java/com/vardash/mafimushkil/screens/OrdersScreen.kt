package com.vardash.mafimushkil.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.Routes
import com.vardash.mafimushkil.auth.OrderViewModel
import com.vardash.mafimushkil.models.Order
import com.vardash.mafimushkil.models.toEpochMillis
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily
import java.util.*
import java.util.concurrent.TimeUnit

fun statusColor(status: String): Color = when (status.lowercase()) {
    "pending"     -> Color(0xFFFF9800)
    "confirmed"   -> Color(0xFF4CAF50)
    "assigned"    -> Color(0xFF2196F3)
    "accepted"    -> Color(0xFFE91E63)
    "in_progress" -> Color(0xFF9C27B0)
    "completed"   -> Color(0xFF4CAF50)
    "cancelled"   -> Color(0xFFF44336)
    else          -> Color(0xFF888888)
}

@Composable
fun formatTimeAgoLocalized(timestamp: Any?): String {
    val timestampMillis = timestamp.toEpochMillis()
    val now = System.currentTimeMillis()
    val diff = now - timestampMillis
    
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    // Using Locale with Latin numerals extension for standard digits
    val localeLatn = Locale.forLanguageTag("ar-u-nu-latn")

    return when {
        minutes < 1 -> stringResource(R.string.orders_time_just_now)
        minutes < 60 -> String.format(localeLatn, stringResource(R.string.orders_time_mins_ago), minutes.toInt())
        hours < 24 -> String.format(localeLatn, stringResource(R.string.orders_time_hrs_ago), hours.toInt())
        days < 7 -> String.format(localeLatn, stringResource(R.string.orders_time_days_ago), days.toInt())
        else -> {
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", localeLatn)
            sdf.format(Date(timestampMillis))
        }
    }
}

@Composable
fun OrderCard(order: Order, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left: title + description
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = run {
                        val maxVisible = 3
                        val categoriesToDisplay = order.categories.take(maxVisible)
                        val namesString = categoriesToDisplay.joinToString(", ") { it.name }
                        val localizedNames = getLocalizedCategoryName(namesString)
                        val finalNames = if (order.categories.size > maxVisible) "$localizedNames..." else localizedNames
                        stringResource(R.string.orders_needed, finalNames)
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = Questv1FontFamily
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = order.details,
                    fontSize = 14.sp,
                    color = Color(0xFF888888),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp,
                    fontFamily = Questv1FontFamily
                )
            }

            // Right: status badge + time
            Column(horizontalAlignment = Alignment.End) {
                // Status badge — outlined pill
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    border = border(1.dp, statusColor(order.status).copy(alpha = 0.5f)),
                    color = Color.Transparent
                ) {
                    Text(
                        text = getLocalizedStatus(order.status),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor(order.status),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        fontFamily = Questv1FontFamily
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Time with clock icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFFAAAAAA),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = formatTimeAgoLocalized(order.createdAt),
                        fontSize = 12.sp,
                        color = Color(0xFFAAAAAA),
                        fontFamily = Questv1FontFamily
                    )
                }
            }
        }
    }
    HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)
}

private fun border(width: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)

@Composable
fun OrdersScreen(
    navController: NavController,
    orderViewModel: OrderViewModel = viewModel(),
    initialTab: Int = 0,
    focusOrderId: String = ""
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var openedFocusedOrderId by remember { mutableStateOf("") }
    val tabs = listOf(
        stringResource(R.string.orders_pending),
        stringResource(R.string.orders_history)
    )

    val pendingOrders by orderViewModel.pendingOrders.collectAsState()
    val completedOrders by orderViewModel.completedOrders.collectAsState()

    LaunchedEffect(Unit) {
        orderViewModel.loadUserOrders()
    }

    // If initialTab changes (e.g. from deep link), update selectedTab
    LaunchedEffect(initialTab) {
        selectedTab = initialTab
    }

    LaunchedEffect(focusOrderId, pendingOrders, completedOrders) {
        if (focusOrderId.isBlank() || openedFocusedOrderId == focusOrderId) return@LaunchedEffect

        val focusedOrder = (pendingOrders + completedOrders).firstOrNull { it.orderId == focusOrderId }
        if (focusedOrder != null) {
            openedFocusedOrderId = focusOrderId
            navController.navigate(Routes.orderDetail(focusOrderId)) {
                launchSingleTop = true
                popUpTo(Routes.Orders) {
                    inclusive = false
                }
            }
        }
    }

    val currentList = if (selectedTab == 0) pendingOrders else completedOrders

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            AppBottomBar(navController = navController, selectedIndex = 1)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F8FA))
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Text(
                        text = stringResource(R.string.orders_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        fontFamily = Questv1FontFamily
                    )
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.White,
                        contentColor = Color(0xFF1A1A1A),
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                height = 2.dp,
                                color = Color(0xFF1A1A1A)
                            )
                        },
                        divider = { HorizontalDivider(color = Color(0xFFEEEEEE)) }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontSize = 15.sp,
                                        fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (selectedTab == index) Color(0xFF1A1A1A) else Color(0xFFAAAAAA),
                                        fontFamily = Questv1FontFamily
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Body — list or empty state
            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.order),
                            contentDescription = stringResource(R.string.orders_empty_active_title),
                            modifier = Modifier.size(200.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = if (selectedTab == 0) 
                                stringResource(R.string.orders_empty_active_title) 
                            else stringResource(R.string.orders_empty_history_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A),
                            fontFamily = Questv1FontFamily
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (selectedTab == 0) 
                                stringResource(R.string.orders_empty_active_desc) 
                            else stringResource(R.string.orders_empty_history_desc),
                            fontSize = 14.sp,
                            color = Color(0xFFAAAAAA),
                            textAlign = TextAlign.Center,
                            fontFamily = Questv1FontFamily
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(currentList, key = { it.orderId }) { order ->
                        OrderCard(
                            order = order,
                            onClick = {
                                navController.navigate("order_detail/${order.orderId}")
                            }
                        )
                    }
                }
            }
        }
    }
}
