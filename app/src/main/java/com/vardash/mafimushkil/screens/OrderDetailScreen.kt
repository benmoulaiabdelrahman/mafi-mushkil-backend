package com.vardash.mafimushkil.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.Routes
import com.vardash.mafimushkil.auth.OrderState
import com.vardash.mafimushkil.auth.OrderViewModel
import com.vardash.mafimushkil.auth.ProfileViewModel
import com.vardash.mafimushkil.auth.UserProfile
import com.vardash.mafimushkil.models.BookedService
import com.vardash.mafimushkil.models.Order
import com.vardash.mafimushkil.models.Payment
import com.vardash.mafimushkil.models.SelectedCategory
import com.vardash.mafimushkil.models.Worker
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun OrderDetailScreen(
    orderId: String,
    navController: NavController,
    orderViewModel: OrderViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val order by orderViewModel.selectedOrder.collectAsState()
    val orderState by orderViewModel.orderState.collectAsState()
    val userProfile by profileViewModel.userProfile.collectAsState()

    LaunchedEffect(orderId) {
        orderViewModel.observeOrder(orderId)
        profileViewModel.loadUserProfile(context)
    }

    DisposableEffect(Unit) {
        onDispose { orderViewModel.clearObservedOrder() }
    }

    order?.let { currentOrder ->
        OrderDetailContent(
            navController = navController,
            order = currentOrder,
            orderState = orderState,
            userProfile = userProfile,
            onBack = { navController.popBackStack() }
        )
    } ?: run {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.Black)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailContent(
    navController: NavController,
    order: Order,
    orderState: OrderState,
    userProfile: UserProfile,
    onBack: () -> Unit
) {
    val normalizedStatus = order.status.lowercase()
    var showFullDetailsDialog by remember { mutableStateOf(false) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var showWorkersSheet by remember { mutableStateOf(false) }
    val view = LocalView.current
    val subtotal = order.bookedServices.sumOf { it.price * it.quantity }
    val baseTotal = when {
        order.totalPrice > 0.0 -> order.totalPrice
        subtotal > 0.0 -> subtotal + order.tax - order.discount
        else -> 0.0
    }.coerceAtLeast(0.0)
    val displayTotal = if (order.totalPrice > 0.0) order.totalPrice else baseTotal

    ApplyOrderDetailBars(
        mode = when {
            selectedImageUrl != null -> OrderDetailBarsMode.ImagePreview
            showWorkersSheet -> OrderDetailBarsMode.Sheet
            else -> OrderDetailBarsMode.Normal
        },
        view = view
    )

    Scaffold(
        containerColor = Color(0xFFF7F8FA),
        topBar = {
            Surface(
                color = Color.White,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                    Text(
                        text = stringResource(R.string.order_detail_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        fontFamily = Questv1FontFamily
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Address Section ───────────────────────────
            DetailSection {
                Text(
                    text = stringResource(R.string.order_detail_address),
                    color = Color(0xFF888888),
                    fontSize = 14.sp,
                    fontFamily = Questv1FontFamily
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = order.address,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1A1A1A),
                    lineHeight = 22.sp,
                    fontFamily = Questv1FontFamily
                )
            }

            // ── Services Section ──────────────────────────
            DetailSection {
                Text(
                    text = stringResource(R.string.order_detail_service_type),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1A1A1A),
                    fontFamily = Questv1FontFamily
                )
                Spacer(Modifier.height(16.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    items(order.categories) { cat ->
                        ServiceItem(name = cat.name, iconRes = getCategoryIcon(cat.iconName))
                    }
                }
            }

            // ── Date Section ──────────────────────────────
            DetailSection {
                Text(
                    text = stringResource(R.string.order_detail_date),
                    color = Color(0xFF888888),
                    fontSize = 14.sp,
                    fontFamily = Questv1FontFamily
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatDisplayDate(Date(order.createdAt)),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1A1A1A),
                    fontFamily = Questv1FontFamily
                )
            }

            // ── Details Section ───────────────────────────
            DetailSection {
                Text(
                    text = stringResource(R.string.order_detail_details),
                    color = Color(0xFF888888),
                    fontSize = 14.sp,
                    fontFamily = Questv1FontFamily
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = order.details.ifBlank { stringResource(R.string.no_additional_details) },
                    fontSize = 14.sp,
                    color = Color(0xFF1A1A1A),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp,
                    fontFamily = Questv1FontFamily
                )
                Text(
                    text = stringResource(R.string.order_detail_show_more),
                    color = Color(0xFF888888),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showFullDetailsDialog = true }
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    fontFamily = Questv1FontFamily
                )
            }

            // ── Photos Section ────────────────────────────
            if (order.photoUrls.isNotEmpty()) {
                DetailSection {
                    Text(
                        text = stringResource(R.string.order_detail_attachments),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1A1A1A),
                        fontFamily = Questv1FontFamily
                    )
                    Spacer(Modifier.height(16.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(order.photoUrls) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedImageUrl = url },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            // ── Booked Services Section ───────────────────
            if (order.bookedServices.isNotEmpty()) {
                DetailSection {
                    Text(
                        text = stringResource(R.string.booked_services_title),
                        color = Color(0xFF888888),
                        fontSize = 14.sp,
                        fontFamily = Questv1FontFamily
                    )
                    Spacer(Modifier.height(12.dp))

                    order.bookedServices.forEach { service ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (service.quantity > 1) {
                                    "${getLocalizedCategoryName(service.name)} ${stringResource(R.string.quantity_prefix)}${service.quantity}"
                                } else {
                                    getLocalizedCategoryName(service.name)
                                },
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1A1A1A),
                                fontFamily = Questv1FontFamily
                            )
                            CurrencyAmount(amountText = formatPriceValue(service.price * service.quantity))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))
                    if (order.discount > 0.0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.booked_services_discount),
                                color = Color(0xFF888888),
                                fontFamily = Questv1FontFamily
                            )
                            CurrencyAmount(amountText = formatPriceValue(order.discount), prefix = "-")
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.booked_services_total),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color(0xFF1A1A1A),
                            fontFamily = Questv1FontFamily
                        )
                        CurrencyAmount(
                            amountText = formatPriceValue(displayTotal),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            // ── Workers Section ───────────────────────────
            if (order.displayWorkers.isNotEmpty()) {
                ClickableSection(
                    title = stringResource(R.string.workers_and_assistants_title)
                ) {
                    showWorkersSheet = true
                }
            }

            // ── Payment Section ──────────────────────────
            val clearedAmount = order.payments.filter { it.isCleared() }.sumOf { it.amount }
            val remainingAmount = (displayTotal - clearedAmount).coerceAtLeast(0.0)
            if (normalizedStatus == "in_progress" && (remainingAmount > 0.0 || order.payments.isNotEmpty())) {
                ClickableSection(
                    title = stringResource(R.string.order_detail_payments)
                ) {
                    navController.navigate(Routes.payments(order.orderId))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showWorkersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWorkersSheet = false },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFDDDDDD)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                @Suppress("DEPRECATION")
                Text(
                    text = stringResource(R.string.order_detail_workers_list),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    fontFamily = Questv1FontFamily
                )
                Spacer(Modifier.height(24.dp))
                order.displayWorkers.forEachIndexed { index, worker ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (worker.photoUrl.isNotEmpty()) {
                            AsyncImage(
                                model = worker.photoUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF5F5F5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFCCCCCC))
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = worker.name,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A1A),
                                fontFamily = Questv1FontFamily
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = worker.services.ifBlank { stringResource(R.string.order_detail_service_not_defined) },
                                color = Color(0xFF888888),
                                fontSize = 13.sp,
                                fontFamily = Questv1FontFamily
                            )
                        }
                    }
                    if (index != order.displayWorkers.lastIndex) {
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { showWorkersSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282828)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.close),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Questv1FontFamily
                    )
                }
            }
        }
    }

    if (showFullDetailsDialog) {
        Dialog(
            onDismissRequest = { showFullDetailsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFCCFF00))
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        @Suppress("DEPRECATION")
                        Text(
                            text = stringResource(R.string.order_detail_details),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.Black,
                            modifier = Modifier.align(Alignment.CenterStart),
                            fontFamily = Questv1FontFamily
                        )
                        IconButton(
                            onClick = { showFullDetailsDialog = false },
                            modifier = Modifier.align(Alignment.CenterEnd).size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Color.Black)
                        }
                    }
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = order.details,
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            color = Color(0xFF1A1A1A),
                            fontFamily = Questv1FontFamily
                        )
                    }
                }
            }
        }
    }

    if (selectedImageUrl != null) {
        Dialog(
            onDismissRequest = { selectedImageUrl = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = true
            )
        ) {
            val dialogView = LocalView.current
            val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window

            DisposableEffect(dialogWindow) {
                if (dialogWindow != null) {
                    dialogWindow.attributes = dialogWindow.attributes.apply {
                        dimAmount = 0f
                    }
                    dialogWindow.statusBarColor = Color.White.toArgb()
                    dialogWindow.navigationBarColor = Color.White.toArgb()
                    WindowCompat.getInsetsController(dialogWindow, dialogView).apply {
                        isAppearanceLightStatusBars = true
                        isAppearanceLightNavigationBars = true
                    }
                }

                onDispose { }
            }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .navigationBarsPadding()
                ) {
                    Scaffold(
                        contentWindowInsets = WindowInsets(0),
                        topBar = {
                        Surface(color = Color.White) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { selectedImageUrl = null }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.back)
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.order_detail_title),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                    fontFamily = Questv1FontFamily
                                )
                            }
                        }
                    },
                    containerColor = Color.White
                ) { p ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(p)
                            .padding(bottom = 8.dp)
                            .background(Color(0xFFF7F8FA))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(16.dp)
                        ) {
                            AsyncImage(
                                model = selectedImageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(order.photoUrls) { url ->
                                val isSelected = url == selectedImageUrl
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) Color.Black else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedImageUrl = url },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class OrderDetailBarsMode {
    Normal,
    Sheet,
    ImagePreview
}

@Composable
private fun ApplyOrderDetailBars(
    mode: OrderDetailBarsMode,
    view: android.view.View
) {
    if (view.isInEditMode) return

    SideEffect {
        val window = (view.context as Activity).window
        val white = Color.White.toArgb()
        val transparent = Color.Transparent.toArgb()

        when (mode) {
            OrderDetailBarsMode.Normal, OrderDetailBarsMode.ImagePreview -> {
                window.statusBarColor = transparent
                window.navigationBarColor = transparent
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
            }
            OrderDetailBarsMode.Sheet -> {
                window.statusBarColor = white
                window.navigationBarColor = white
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
            }
        }
    }
}

@Composable
fun ClickableSection(title: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = Color(0xFF888888),
                fontSize = 14.sp,
                fontFamily = Questv1FontFamily
            )
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFFBBBBBB),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun DetailSection(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(Color.White)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun StatusBadge(status: String) {
    val badgeColor = when (status.lowercase()) {
        "pending"     -> Color(0xFFFF9800)
        "confirmed"   -> Color(0xFF4CAF50)
        "assigned"    -> Color(0xFF2196F3)
        "accepted"    -> Color(0xFFE91E63)
        "in_progress" -> Color(0xFF9C27B0)
        "completed"   -> Color(0xFF4CAF50)
        "cancelled"   -> Color(0xFFF44336)
        else          -> Color(0xFF888888)
    }
    Surface(
        shape = RoundedCornerShape(50.dp),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
        color = Color.Transparent
    ) {
        Text(
            text = getLocalizedStatus(status),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = badgeColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            fontFamily = Questv1FontFamily
        )
    }
}

@Composable
fun ServiceItem(name: String, iconRes: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .border(
                width = 1.dp,
                color = Color(0xFFEEEEEE),
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                Color.White,
                RoundedCornerShape(8.dp)
            )
            .padding(vertical = 12.dp)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = name,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = getLocalizedCategoryName(name),
            fontSize = 11.sp,
            color = Color(0xFF1A1A1A),
            fontFamily = Questv1FontFamily,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun Payment.isCleared(): Boolean =
    status.lowercase() in setOf("paid", "cleared", "completed")

@Composable
fun getStatusDescriptionLocalized(status: String): String = when (status.lowercase()) {
    "pending" -> stringResource(R.string.desc_pending)
    "accepted" -> stringResource(R.string.desc_accepted)
    "confirmed" -> stringResource(R.string.desc_confirmed)
    "assigned" -> stringResource(R.string.desc_assigned)
    "in_progress" -> stringResource(R.string.desc_in_progress)
    "cancelled" -> stringResource(R.string.desc_cancelled)
    "completed" -> stringResource(R.string.desc_completed)
    else -> stringResource(R.string.desc_default)
}

@Composable
private fun CurrencyAmount(
    amountText: String,
    fontWeight: FontWeight = FontWeight.Bold,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    prefix: String = ""
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (prefix.isNotBlank()) {
                Text(
                    text = prefix,
                    fontWeight = fontWeight,
                    fontSize = fontSize,
                    color = Color(0xFF1A1A1A),
                    fontFamily = Questv1FontFamily
                )
                Spacer(Modifier.width(2.dp))
            }
            Text(
                text = "دج",
                fontWeight = fontWeight,
                fontSize = fontSize,
                color = Color(0xFF1A1A1A),
                fontFamily = Questv1FontFamily
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = amountText,
                fontWeight = fontWeight,
                fontSize = fontSize,
                color = Color(0xFF1A1A1A),
                fontFamily = Questv1FontFamily
            )
        }
    }
}

private fun formatPriceValue(amount: Double): String = if (amount % 1.0 == 0.0) String.format(Locale.US, "%.0f", amount) else String.format(Locale.US, "%.2f", amount)

private fun formatDisplayDate(date: Date): String {
    val raw = try {
        SimpleDateFormat("dd MMMM hh:mm a", Locale.forLanguageTag("ar-u-nu-latn")).format(date)
    } catch (e: Exception) {
        SimpleDateFormat("dd MMMM hh:mm a", Locale.forLanguageTag("ar-u-nu-latn")).format(date)
    }
    return raw
        .replace(Regex("\\sص$"), " صباحا")
        .replace(Regex("\\sم$"), " مساءا")
}

@Preview(showBackground = true, showSystemUi = true, locale = "ar")
@Composable
fun OrderDetailScreenPreview() {
    MafiMushkilTheme {
        OrderDetailContent(
            navController = rememberNavController(),
            order = Order(
                orderId = "preview_id",
                categories = listOf(
                    SelectedCategory("1", "Cleaning", "cleaning"),
                    SelectedCategory("2", "Electrician", "electrician")
                ),
                status = "assigned",
                address = "123 Main St, Dubai",
                details = "أحتاج إلى تنظيف عميق لشقتي المكونة من غرفتي نوم وصالة. كما أحتاج إلى إصلاح مفتاح إضاءة في المطبخ.",
                photoUrls = listOf("https://via.placeholder.com/150"),
                createdAt = System.currentTimeMillis(),
                bookedServices = listOf(
                    BookedService(name = "Cleaning", price = 100.0),
                    BookedService(name = "Repairing", price = 50.0)
                ),
                assignedWorkers = listOf(
                    Worker(name = "Shahid Iqbal", services = "Plumbing", photoUrl = ""),
                    Worker(name = "Ehsan Ullah", services = "Plumbing", photoUrl = "")
                ),
                totalPrice = 150.0
            ),
            orderState = OrderState.Idle,
            userProfile = UserProfile(),
            onBack = {}
        )
    }
}
