package com.vardash.mafimushkil

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.vardash.mafimushkil.auth.CloudinaryManager
import com.vardash.mafimushkil.auth.FcmTokenManager
import com.vardash.mafimushkil.auth.NotificationChannelHelper
import com.vardash.mafimushkil.auth.OrderViewModel
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val notificationsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    private var paymentReturnRequest by mutableStateOf<PaymentReturnRequest?>(null)
    private var notificationOpenRequest by mutableStateOf<OrdersTabRequest?>(null)
    private var navController: NavController? = null

    override fun attachBaseContext(newBase: Context) {
        val locale = Locale("ar")
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
        firestore.firestoreSettings = settings

        CloudinaryManager.init(this)
        NotificationChannelHelper.ensureOrderUpdatesChannel(this)

        val orderViewModel = ViewModelProvider(this)[OrderViewModel::class.java]
        orderViewModel.seedCategoriesIfEmpty()

        CoroutineScope(Dispatchers.IO).launch {
            FcmTokenManager.fetchAndStoreToken()
        }

        FirebaseAnalytics.getInstance(this)
        handleIncomingIntent(intent)
        requestNotificationsPermissionOnce()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        setContent {
            MafiMushkilTheme {
                AppNavigation(
                    startDestination = Routes.Splash,
                    paymentReturnRequest = paymentReturnRequest,
                    notificationOpenRequest = notificationOpenRequest,
                    onPaymentReturnConsumed = { paymentReturnRequest = null },
                    onNotificationOpenConsumed = { notificationOpenRequest = null },
                    onNavControllerReady = { navController = it }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
        if (paymentReturnRequest != null || notificationOpenRequest != null) {
            return
        }
        navController?.handleDeepLink(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val data = intent?.data
        paymentReturnRequest = data?.toPaymentReturnRequest()
        notificationOpenRequest = intent?.toOrderNotificationRequest()

        if (paymentReturnRequest != null || notificationOpenRequest != null || intent == null || data == null || !data.isFirebaseDynamicLinkHost()) {
            return
        }

        FirebaseDynamicLinks.getInstance()
            .getDynamicLink(intent)
            .addOnSuccessListener(this) { pendingDynamicLinkData ->
                val deepLink = pendingDynamicLinkData?.link ?: return@addOnSuccessListener
                paymentReturnRequest = deepLink.toPaymentReturnRequest()
            }
            .addOnFailureListener(this) { error ->
                android.util.Log.e(
                    "MainActivity",
                    "Failed to resolve Firebase Dynamic Link: ${error.message}",
                    error
                )
            }
    }

    private fun requestNotificationsPermissionOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val permissionState = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        )

        if (permissionState != PackageManager.PERMISSION_GRANTED) {
            notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

data class PaymentReturnRequest(
    val status: String,
    val orderId: String
)

data class OrdersTabRequest(
    val tab: Int = 0,
    val focusOrderId: String = ""
)

private fun Uri.toPaymentReturnRequest(): PaymentReturnRequest? {
    if (scheme != "mafimushkil" || host != "payment") return null
    val status = pathSegments.firstOrNull().orEmpty().lowercase()
    val orderId = getQueryParameter("orderId").orEmpty()
    if (status !in setOf("success", "failure") || orderId.isBlank()) return null
    return PaymentReturnRequest(status = status, orderId = orderId)
}

private fun Intent.toOrderNotificationRequest(): OrdersTabRequest? {
    data?.toOrderNotificationRequest()?.let { return it }
    val openOrdersTab = getBooleanExtra("open_orders_tab", false) ||
        getStringExtra("open_orders_tab")?.equals("true", ignoreCase = true) == true
    return if (openOrdersTab) {
        OrdersTabRequest(
            tab = getIntExtra("tab", getStringExtra("tab")?.toIntOrNull() ?: 0),
            focusOrderId = getStringExtra("focusOrderId").orEmpty()
        )
    } else {
        null
    }
}

private fun Uri.toOrderNotificationRequest(): OrdersTabRequest? {
    if (scheme != "mafimushkil" || host != "orders") return null
    return OrdersTabRequest(
        tab = getQueryParameter("tab")?.toIntOrNull() ?: 0,
        focusOrderId = getQueryParameter("focusOrderId").orEmpty()
    )
}

private fun Uri.isFirebaseDynamicLinkHost(): Boolean {
    return scheme == "https" && host == "mafimushkil.page.link"
}
