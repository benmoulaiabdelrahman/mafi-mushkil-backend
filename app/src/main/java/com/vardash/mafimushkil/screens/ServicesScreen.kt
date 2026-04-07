package com.vardash.mafimushkil.screens
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.Routes
import com.vardash.mafimushkil.auth.OrderViewModel
import com.vardash.mafimushkil.auth.ProfileViewModel
import com.vardash.mafimushkil.models.Order
import com.vardash.mafimushkil.models.toEpochMillis
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun ServicesScreen(
    navController: NavController,
    orderViewModel: OrderViewModel = viewModel(),
    profileViewModel: ProfileViewModel? = if (LocalInspectionMode.current) null else viewModel()
) {
    val context = LocalContext.current
    val contextTabs = listOf(
        stringResource(R.string.services_tab_services),
        stringResource(R.string.services_tab_history),
        stringResource(R.string.services_tab_balance)
    )
    var selectedTab by remember { mutableIntStateOf(0) }

    val pendingOrders by orderViewModel.servicePendingOrders.collectAsState()
    val completedOrders by orderViewModel.serviceCompletedOrders.collectAsState()
    val allOrders by orderViewModel.serviceAllOrders.collectAsState()
    val isLoaded by orderViewModel.isServiceOrdersLoaded.collectAsState()
    val userProfile by (profileViewModel?.userProfile?.collectAsState() ?: remember { mutableStateOf(com.vardash.mafimushkil.auth.UserProfile()) })

    LaunchedEffect(Unit) {
        profileViewModel?.loadUserProfile(context)
        orderViewModel.loadServiceOrders(context)
    }

    val currentList = when (selectedTab) {
        0 -> pendingOrders
        1 -> completedOrders
        else -> emptyList()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            AppBottomBar(
                navController = navController,
                selectedRoute = Routes.Services,
                profileViewModel = profileViewModel
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F8FA))
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            Surface(color = Color.White) {
                Column {
                    ScreenHeaderTitle(
                        text = stringResource(R.string.services_title),
                        showDivider = false
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
                        contextTabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    @Suppress("DEPRECATION")
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

            when (selectedTab) {
                0, 1 -> {
                val shouldShowLoading = !isLoaded && currentList.isEmpty()
                if (shouldShowLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.Black)
                    }
                } else if (currentList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF7F8FA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 40.dp)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.order),
                                    contentDescription = stringResource(
                                        if (selectedTab == 0) R.string.services_empty_active_title
                                        else R.string.services_empty_history_title
                                    ),
                                    modifier = Modifier.size(200.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(Modifier.height(24.dp))
                                Text(
                                    text = if (selectedTab == 0)
                                        stringResource(R.string.services_empty_active_title)
                                    else stringResource(R.string.services_empty_history_title),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1A1A),
                                    fontFamily = Questv1FontFamily
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = if (selectedTab == 0)
                                        stringResource(R.string.services_empty_active_desc)
                                    else stringResource(R.string.services_empty_history_desc),
                                    fontSize = 14.sp,
                                    color = Color(0xFFAAAAAA),
                                    textAlign = TextAlign.Center,
                                    fontFamily = Questv1FontFamily
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(currentList) { order ->
                                ServiceOrderCard(
                                    order = order,
                                    onClick = {
                                        navController.navigate(Routes.serviceOrderDetail(order.orderId))
                                    }
                                )
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .background(Color(0xFFF7F8FA))
                                )
                            }
                        }
                    }
                }
                else -> {
                    ServiceBalanceScreen(
                        navController = navController,
                        totalEarnings = allOrders.filter { it.status.lowercase() == "completed" }.sumOf { it.totalPrice },
                        completedOrdersCount = allOrders.count { it.status.lowercase() == "completed" }
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceOrderCard(
    order: Order,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        @Suppress("DEPRECATION")
        Text(
            text = order.details.ifBlank { stringResource(R.string.services_no_description) },
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1A1A1A),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 21.sp,
            fontFamily = Questv1FontFamily
        )
        val timeAgo = formatTimeAgoLocalized(order.createdAt)
        if (timeAgo.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = Color(0xFFAAAAAA),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                @Suppress("DEPRECATION")
                Text(
                    text = timeAgo,
                    fontSize = 12.sp,
                    color = Color(0xFFAAAAAA),
                    fontFamily = Questv1FontFamily
                )
            }
        }
    }
    HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)
}

@Composable
private fun ServiceBalanceScreen(
    navController: NavController,
    totalEarnings: Double,
    completedOrdersCount: Int
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA)),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                @Suppress("DEPRECATION")
                Text(
                    text = stringResource(R.string.services_balance_title),
                    fontSize = 14.sp,
                    color = Color(0xFF888888),
                    fontFamily = Questv1FontFamily
                )
                Spacer(Modifier.height(8.dp))
                @Suppress("DEPRECATION")
                Text(
                    text = formatBalanceValue(totalEarnings),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1A1A1A),
                    fontFamily = Questv1FontFamily
                )
                Spacer(Modifier.height(6.dp))
                @Suppress("DEPRECATION")
                Text(
                    text = stringResource(R.string.services_balance_subtitle, completedOrdersCount),
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E8E),
                    fontFamily = Questv1FontFamily
                )
            }
        }
        item {
            HorizontalDivider(color = Color(0xFFF5F5F5))
        }
    }
}

private fun formatBalanceValue(amount: Double): String =
    if (amount % 1.0 == 0.0) String.format(Locale.US, "%.0f", amount)
    else String.format(Locale.US, "%.2f", amount)
