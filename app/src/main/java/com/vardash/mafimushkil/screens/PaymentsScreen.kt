package com.vardash.mafimushkil.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.auth.ChargilyManager
import com.vardash.mafimushkil.auth.OrderViewModel
import com.vardash.mafimushkil.models.Order
import com.vardash.mafimushkil.models.Payment
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PaymentsScreen(
    navController: NavController,
    orderId: String,
    orderViewModel: OrderViewModel = viewModel()
) {
    val pendingOrders by orderViewModel.pendingOrders.collectAsState()
    val completedOrders by orderViewModel.completedOrders.collectAsState()
    val selectedOrder by orderViewModel.selectedOrder.collectAsState()
    val error by orderViewModel.error.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var checkoutUrl by remember { mutableStateOf<String?>(null) }
    var checkoutReference by remember { mutableStateOf("") }
    var checkoutPaymentId by remember { mutableStateOf("") }
    var isCreatingCheckout by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(orderId) {
        orderViewModel.observeOrder(orderId)
    }

    DisposableEffect(orderId) {
        onDispose {
            orderViewModel.clearObservedOrder()
        }
    }

    val order = selectedOrder ?: (pendingOrders + completedOrders).find { it.orderId == orderId }

    val activePayment = order?.payments?.firstOrNull { it.isPending() }
    val remainingPayments = order?.payments?.filter { it.isPending() && it.id != activePayment?.id }.orEmpty()
    val clearedPayments = order?.payments?.filter { it.isCleared() }.orEmpty()

    fun launchCheckout(payment: Payment) {
        scope.launch {
            isCreatingCheckout = true
            localError = null
            try {
                val session = ChargilyManager.createCheckout(
                    orderId = orderId,
                    payment = payment,
                    customerId = FirebaseAuth.getInstance().currentUser?.uid
                )
                checkoutReference = session.reference
                checkoutPaymentId = payment.id
                checkoutUrl = session.checkoutUrl
            } catch (e: Exception) {
                localError = e.message ?: "Unable to create checkout"
            } finally {
                isCreatingCheckout = false
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF7F8FA))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        Text(
                            text = "Payments",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            fontFamily = Questv1FontFamily
                        )
                    }
                    HorizontalDivider(color = Color(0xFFF5F5F5))
                }
            }

            if (order == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.Black)
                }
            } else {
                val amountPaid = clearedPayments.sumOf { it.amount }
                val totalAmount = if (order.totalPrice > 0.0) order.totalPrice else order.payments.sumOf { it.amount }
                val remainingAmount = (totalAmount - amountPaid).coerceAtLeast(0.0)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    DetailSection {
                        SectionHeader("Active payment")
                        Spacer(Modifier.height(12.dp))
                        if (activePayment == null) {
                            EmptyPaymentText("There is no active payment right now.")
                        } else {
                            ActivePaymentCard(
                                payment = activePayment,
                                isLoading = isCreatingCheckout,
                                onPayNow = { launchCheckout(activePayment) }
                            )
                        }
                    }

                    DetailSection {
                        SectionHeader("Remaining payments")
                        Spacer(Modifier.height(12.dp))
                        if (remainingPayments.isEmpty()) {
                            EmptyPaymentText("No remaining payments.")
                        } else {
                            remainingPayments.forEachIndexed { index, payment ->
                                ExpandablePaymentRow(
                                    payment = payment,
                                    showStatus = false
                                )
                                if (index != remainingPayments.lastIndex) {
                                    HorizontalDivider(color = Color(0xFFF1F1F1))
                                }
                            }
                        }
                    }

                    DetailSection {
                        SectionHeader("Cleared payments")
                        Spacer(Modifier.height(12.dp))
                        if (clearedPayments.isEmpty()) {
                            EmptyPaymentText("No cleared payments yet.")
                        } else {
                            clearedPayments.forEachIndexed { index, payment ->
                                ClearedPaymentRow(payment = payment)
                                if (index != clearedPayments.lastIndex) {
                                    HorizontalDivider(color = Color(0xFFF1F1F1))
                                }
                            }
                        }
                    }

                    DetailSection {
                        SectionHeader("Payments summary")
                        Spacer(Modifier.height(12.dp))
                        SummaryRow(label = "Total Amount", value = formatDzd(totalAmount), bold = false)
                        Spacer(Modifier.height(10.dp))
                        SummaryRow(label = "Amount paid", value = formatDzd(amountPaid), bold = false)
                        Spacer(Modifier.height(10.dp))
                        SummaryRow(label = "Remaining Amount", value = formatDzd(remainingAmount), bold = true)
                    }

                    val resolvedError = localError ?: error
                    if (!resolvedError.isNullOrBlank()) {
                        DetailSection {
                            Text(
                                text = resolvedError,
                                color = Color(0xFFF44336),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontFamily = Questv1FontFamily
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (checkoutUrl != null && order != null) {
        PaymentCheckoutDialog(
            checkoutUrl = checkoutUrl!!,
            onClose = { checkoutUrl = null },
            onPaymentSuccess = {
                orderViewModel.markPaymentCompleted(
                    orderId = order.orderId,
                    paymentId = checkoutPaymentId,
                    checkoutUrl = checkoutUrl!!,
                    reference = checkoutReference
                )
                checkoutUrl = null
            },
            onPaymentFailure = {
                checkoutUrl = null
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF888888),
        fontSize = 14.sp,
        fontFamily = Questv1FontFamily
    )
}

@Composable
private fun EmptyPaymentText(text: String) {
    Text(
        text = text,
        color = Color(0xFF888888),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFamily = Questv1FontFamily
    )
}

@Composable
private fun ActivePaymentCard(
    payment: Payment,
    isLoading: Boolean,
    onPayNow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            text = payment.title.ifBlank { "Material payment" },
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF1A1A1A),
            fontFamily = Questv1FontFamily
        )
        Spacer(Modifier.height(16.dp))
        PaymentInfoRow(label = "Amount", value = formatDzd(payment.amount))
        Spacer(Modifier.height(8.dp))
        PaymentInfoRow(label = "Due Date", value = formatDate(payment.dueDate))
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onPayNow,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF54B65F)),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "PAY NOW",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Questv1FontFamily
                )
            }
        }
    }
}

@Composable
private fun PaymentInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color(0xFF888888),
            fontSize = 14.sp,
            fontFamily = Questv1FontFamily
        )
        Text(
            text = value,
            color = Color(0xFF1A1A1A),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Questv1FontFamily
        )
    }
}

@Composable
private fun ExpandablePaymentRow(
    payment: Payment,
    showStatus: Boolean
) {
    var expanded by remember(payment.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payment.title.ifBlank { "Payment Amount" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1A1A1A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = Questv1FontFamily
                )
                if (showStatus) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = payment.status.replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp,
                        color = Color(0xFF888888),
                        fontFamily = Questv1FontFamily
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = formatDzd(payment.amount),
                color = Color(0xFF1A1A1A),
                fontWeight = FontWeight.Bold,
                fontFamily = Questv1FontFamily
            )
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFFBBBBBB),
                modifier = Modifier.size(14.dp)
            )
        }
        if (expanded) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Due Date: ${formatDate(payment.dueDate)}",
                color = Color(0xFF888888),
                fontSize = 13.sp,
                fontFamily = Questv1FontFamily
            )
        }
    }
}

@Composable
private fun ClearedPaymentRow(payment: Payment) {
    var expanded by remember(payment.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F3F3)),
                    contentAlignment = Alignment.Center
                ) {
                    if (payment.method.lowercase().contains("cash")) {
                        Icon(
                            imageVector = Icons.Filled.AttachMoney,
                            contentDescription = null,
                            tint = Color(0xFF1A1A1A)
                        )
                    } else if (payment.method.lowercase().contains("cib") || payment.method.lowercase().contains("edahabia")) {
                        Image(
                            painter = painterResource(R.drawable.wallet),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Payments,
                            contentDescription = null,
                            tint = Color(0xFF1A1A1A)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Amount paid",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1A1A1A),
                        fontFamily = Questv1FontFamily
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formatDzd(payment.amount),
                        fontSize = 13.sp,
                        color = Color(0xFF888888),
                        fontFamily = Questv1FontFamily
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFFBBBBBB),
                modifier = Modifier.size(14.dp)
            )
        }
        if (expanded) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Paid on ${formatDate(payment.paidDate)}",
                color = Color(0xFF888888),
                fontSize = 13.sp,
                fontFamily = Questv1FontFamily
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, bold: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color(0xFF1A1A1A),
            fontSize = 15.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontFamily = Questv1FontFamily
        )
        Text(
            text = value,
            color = Color(0xFF1A1A1A),
            fontSize = 15.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontFamily = Questv1FontFamily
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PaymentCheckoutDialog(
    checkoutUrl: String,
    onClose: () -> Unit,
    onPaymentSuccess: () -> Unit,
    onPaymentFailure: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
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
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        Text(
                            text = "Payments",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            fontFamily = Questv1FontFamily
                        )
                    }
                }
            },
            containerColor = Color.White
        ) { paddingValues ->
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                handleRedirect(url, onPaymentSuccess, onPaymentFailure)
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val targetUrl = request?.url?.toString()
                                return handleRedirect(targetUrl, onPaymentSuccess, onPaymentFailure)
                            }
                        }
                        loadUrl(checkoutUrl)
                    }
                }
            )
        }
    }
}

private fun handleRedirect(
    url: String?,
    onPaymentSuccess: () -> Unit,
    onPaymentFailure: () -> Unit
): Boolean {
    if (url.isNullOrBlank()) return false
    return when {
        url.startsWith("https://mafimushkil.app/payments/success") -> {
            onPaymentSuccess()
            true
        }
        url.startsWith("https://mafimushkil.app/payments/failure") -> {
            onPaymentFailure()
            true
        }
        else -> false
    }
}

private fun Payment.isPending(): Boolean = status.lowercase() in setOf("pending", "active", "unpaid", "due")

private fun Payment.isCleared(): Boolean = status.lowercase() in setOf("paid", "cleared", "completed")

private fun formatDzd(amount: Double): String = "${String.format(Locale.US, "%.2f", amount)} DZD"

private fun formatDate(timeMillis: Long): String {
    if (timeMillis <= 0L) return "N/A"
    return SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("ar-u-nu-latn")).format(Date(timeMillis))
}
