package com.vardash.mafimushkil.screens

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.auth.ChargilyManager
import com.vardash.mafimushkil.auth.OrderViewModel
import com.vardash.mafimushkil.models.BookedService
import com.vardash.mafimushkil.models.Order
import com.vardash.mafimushkil.models.Payment
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
    navController: NavController,
    orderId: String,
    orderViewModel: OrderViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val selectedOrder by orderViewModel.selectedOrder.collectAsState()
    val pendingOrders by orderViewModel.pendingOrders.collectAsState()
    val completedOrders by orderViewModel.completedOrders.collectAsState()
    val isLoading by orderViewModel.isLoading.collectAsState()

    var isCreatingCheckout by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    
    var showNoInternetSheet by remember { mutableStateOf(false) }

    LaunchedEffect(orderId) {
        orderViewModel.observeOrder(orderId)
    }

    DisposableEffect(orderId) {
        onDispose { orderViewModel.clearObservedOrder() }
    }

    val order = selectedOrder ?: (pendingOrders + completedOrders).find { it.orderId == orderId }

    // ── Payment availability logic ──────────────────────────────────────────
    val normalizedStatus = order?.status?.lowercase()?.replace(' ', '_').orEmpty()
    val clearedPayments = order?.payments?.filter { it.isCleared() }.orEmpty()
    val unpaidPayments = order?.payments?.filter { !it.isCleared() }.orEmpty()
    val paidAmountSoFar = clearedPayments.sumOf { it.amount }

    val derivedServicesTotal = order?.bookedServices?.sumOf { it.price * it.quantity } ?: 0.0
    val serviceBasedTotal = order?.let {
        (derivedServicesTotal + it.tax - it.discount).coerceAtLeast(0.0)
    } ?: 0.0
    val derivedOrderTotal = if (order != null) {
        when {
            serviceBasedTotal > 0.0 -> serviceBasedTotal
            order.totalPrice > 0.0 -> order.totalPrice
            else -> order.payments.sumOf { it.amount }
        }
    } else 0.0
    val remainingAmount = (derivedOrderTotal - paidAmountSoFar).coerceAtLeast(0.0)

    LaunchedEffect(orderId, order) {
        if (order != null) {
            Log.d(
                "PaymentsScreen",
                buildString {
                    append("Order pricing breakdown for orderId=$orderId; ")
                    append("status=${order.status}; ")
                    append("bookedServices=${order.bookedServices.size}; ")
                    append("tax=${order.tax}; ")
                    append("discount=${order.discount}; ")
                    append("totalPrice=${order.totalPrice}; ")
                    append("derivedServicesTotal=$derivedServicesTotal; ")
                    append("serviceBasedTotal=$serviceBasedTotal; ")
                    append("derivedOrderTotal=$derivedOrderTotal; ")
                    append("paidAmountSoFar=$paidAmountSoFar; ")
                    append("remainingAmount=$remainingAmount")
                }
            )
        }
    }

    // The payment object we will use to create the Chargily checkout.
    val duePayment: Payment? = when {
        normalizedStatus != "in_progress" -> null
        remainingAmount > 0.0 -> {
            val template = unpaidPayments.firstOrNull() ?: Payment(
                id = "generated_active_payment",
                title = "الخدمات المحجوزة",
                amount = remainingAmount,
                status = "pending"
            )
            template.copy(amount = remainingAmount)
        }
        unpaidPayments.isNotEmpty() -> unpaidPayments.first()
        else -> null
    }
    // ────────────────────────────────────────────────────────────────────────

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    fun openUrl(url: String) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder().setShowTitle(true).build()
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
            customTabsIntent.launchUrl(context, Uri.parse(url))
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
            }
            context.startActivity(intent)
        }
    }

    fun launchCheckout(payment: Payment) {
        if (!isNetworkAvailable(context)) {
            showNoInternetSheet = true
            return
        }
        
        scope.launch {
            isCreatingCheckout = true
            localError = null
            try {
                val session = ChargilyManager.createCheckout(
                    orderId = orderId,
                    payment = payment,
                    userId = FirebaseAuth.getInstance().currentUser?.uid
                )
                if (session.checkoutUrl.isBlank()) throw IllegalStateException("Empty checkout URL")
                openUrl(session.checkoutUrl)
            } catch (e: Exception) {
                val errorMsg = e.message ?: ""
                if (errorMsg.contains("network") || errorMsg.contains("timeout")) {
                    showNoInternetSheet = true
                } else {
                    localError = "تعذر إنشاء رابط الدفع"
                    Toast.makeText(context, localError, Toast.LENGTH_LONG).show()
                }
            } finally {
                isCreatingCheckout = false
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                Surface(color = Color.White) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                        Text(
                            text = stringResource(R.string.order_detail_payments),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            fontFamily = Questv1FontFamily
                        )
                    }
                }
            },
            containerColor = Color.White
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF7F8FA))
            ) {
                when {
                    isLoading && order == null -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.Black
                        )
                    }
                    order != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // ── Booked services card ────────────────────────────
                            if (order.bookedServices.isNotEmpty()) {
                                Surface(
                                    color = Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = stringResource(R.string.order_detail_booked_services),
                                            color = Color(0xFF888888),
                                            fontSize = 14.sp,
                                            fontFamily = Questv1FontFamily
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        order.bookedServices.forEach { service ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = if (service.quantity > 1)
                                                        "${service.name} x${service.quantity}"
                                                    else service.name,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF1A1A1A),
                                                    fontFamily = Questv1FontFamily
                                                )
                                                CurrencyAmount(
                                                    amountText = formatPriceValue(service.price * service.quantity)
                                                )
                                            }
                                        }
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            color = Color(0xFFEEEEEE)
                                        )
                                        if (order.discount > 0.0) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    "الخصم",
                                                    color = Color(0xFF888888),
                                                    fontFamily = Questv1FontFamily
                                                )
                                                CurrencyAmount(
                                                    amountText = formatPriceValue(order.discount),
                                                    prefix = "-"
                                                )
                                            }
                                            Spacer(Modifier.height(8.dp))
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "الإجمالي:",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 18.sp,
                                                color = Color(0xFF1A1A1A),
                                                fontFamily = Questv1FontFamily
                                            )
                                            CurrencyAmount(
                                                amountText = formatPriceValue(derivedOrderTotal),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 18.sp
                                            )
                                        }
                                    }
                                }
                            }

                            if (!localError.isNullOrBlank()) {
                                Surface(
                                    color = Color.White,
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                                ) {
                                    Text(
                                        text = localError!!,
                                        color = Color(0xFFF44336),
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(16.dp),
                                        fontFamily = Questv1FontFamily
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Button(
                                onClick = { if (duePayment != null) launchCheckout(duePayment) },
                                enabled = duePayment != null && !isCreatingCheckout,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF282828),
                                    disabledContainerColor = Color(0xFFAAAAAA),
                                    contentColor = Color.White
                                )
                            ) {
                                if (isCreatingCheckout) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFFCCFD04))
                                } else {
                                    Text(text = "ادفع الآن", fontWeight = FontWeight.Bold, fontFamily = Questv1FontFamily)
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                    else -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.Black)
                        }
                    }
                }
            }
        }
    }
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
                Text(text = prefix, fontWeight = fontWeight, fontSize = fontSize, fontFamily = Questv1FontFamily)
                Spacer(Modifier.width(2.dp))
            }
            Text(text = "دج", fontWeight = fontWeight, fontSize = fontSize, fontFamily = Questv1FontFamily)
            Spacer(Modifier.width(4.dp))
            Text(text = amountText, fontWeight = fontWeight, fontSize = fontSize, fontFamily = Questv1FontFamily)
        }
    }
}

private fun formatPriceValue(amount: Double): String =
    if (amount % 1.0 == 0.0) String.format(Locale.US, "%.0f", amount)
    else String.format(Locale.US, "%.2f", amount)

private fun Payment.isCleared(): Boolean =
    status.lowercase() in setOf("paid", "cleared", "completed")

class MockOrderViewModel(val mockOrder: Order) : OrderViewModel() {
    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _selectedOrder = MutableStateFlow<Order?>(mockOrder)
    override val selectedOrder: StateFlow<Order?> = _selectedOrder.asStateFlow()

    override fun observeOrder(orderId: String) {}
    override fun clearObservedOrder() {}
}

@Preview(showBackground = true, showSystemUi = true, locale = "ar")
@Composable
fun PaymentsScreenPreview() {
    val mockOrder = Order(
        orderId = "mock_123",
        status = "in_progress",
        bookedServices = listOf(
            BookedService(name = "تنظيف مكيف", price = 2500.0, quantity = 2),
            BookedService(name = "إصلاح تسرب", price = 1500.0, quantity = 1)
        ),
        totalPrice = 6500.0,
        discount = 500.0,
        tax = 0.0
    )
    
    MafiMushkilTheme {
        PaymentsScreen(
            navController = rememberNavController(),
            orderId = "mock_123",
            orderViewModel = MockOrderViewModel(mockOrder)
        )
    }
}
