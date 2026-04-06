package com.vardash.mafimushkil

import androidx.compose.animation.core.tween
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.net.Uri
import androidx.core.view.WindowCompat
import com.vardash.mafimushkil.auth.ApplicationViewModel
import com.vardash.mafimushkil.auth.AuthViewModel
import com.vardash.mafimushkil.auth.OrderViewModel
import com.vardash.mafimushkil.auth.ProfileViewModel
import com.vardash.mafimushkil.screens.*
import kotlinx.coroutines.delay

@Composable
fun AppNavigation(
    startDestination: String = Routes.Splash,
    paymentReturnRequest: PaymentReturnRequest? = null,
    notificationOpenRequest: OrdersTabRequest? = null,
    onPaymentReturnConsumed: () -> Unit = {},
    onNotificationOpenConsumed: () -> Unit = {},
    onNavControllerReady: (NavController) -> Unit = {}
) {
    val navController = rememberNavController()
    val view = LocalView.current
    var homeContentReady by remember { mutableStateOf(false) }
    val currentRoute = navController.currentBackStackEntryAsState().value
        ?.destination
        ?.route
        ?.substringBefore("?")
    val activeRoute = currentRoute ?: startDestination

    // Shared ViewModels
    val authViewModel: AuthViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val orderViewModel: OrderViewModel = viewModel()
    val applicationViewModel: ApplicationViewModel = viewModel()

    val bottomBarRoutes = listOf(Routes.Home, Routes.Services, Routes.Orders, Routes.Promotions, Routes.Notifications)

    LaunchedEffect(navController) {
        onNavControllerReady(navController)
    }

    LaunchedEffect(profileViewModel) {
        profileViewModel.loadUserProfile()
    }

    LaunchedEffect(activeRoute) {
        if (activeRoute != Routes.Home) {
            homeContentReady = false
        }
    }

    SideEffect {
        val statusBarColor = when {
            activeRoute == Routes.Splash -> Color(0xFFCCFD04)
            activeRoute in bottomBarRoutes || activeRoute == Routes.Categories || activeRoute == Routes.ChooseOnMap ||
            activeRoute == Routes.MyProfile || activeRoute == Routes.ContactUs || activeRoute == Routes.BecomeWorker ||
            activeRoute == Routes.Services || activeRoute == Routes.WorkerForm || activeRoute == Routes.RegisterCompany || activeRoute == Routes.RevokeRegistration ||
            activeRoute.startsWith("${Routes.ServiceOrderDetail}/") || activeRoute.startsWith("${Routes.ServiceOrderMap}/") ||
            activeRoute.startsWith("${Routes.RevokeRegistration}/") || activeRoute == Routes.CompanyForm || activeRoute == Routes.PlaceOrder -> Color.Transparent
            else -> Color.White
        }
        val navigationBarColor = when {
            activeRoute == Routes.Splash -> Color(0xFFCCFD04)
            activeRoute == Routes.PlaceOrder || activeRoute == Routes.Details || activeRoute == Routes.Categories ||
            activeRoute == Routes.SelectLocation || activeRoute == Routes.ChooseOnMap || activeRoute == Routes.MyProfile ||
            activeRoute == Routes.ContactUs || activeRoute == Routes.BecomeWorker || activeRoute == Routes.Services || activeRoute == Routes.WorkerForm ||
            activeRoute == Routes.RegisterCompany || activeRoute == Routes.RevokeRegistration ||
            activeRoute.startsWith("${Routes.ServiceOrderDetail}/") || activeRoute.startsWith("${Routes.ServiceOrderMap}/") ||
            activeRoute.startsWith("${Routes.RevokeRegistration}/") || activeRoute == Routes.CompanyForm -> Color.Transparent
            activeRoute == Routes.Home -> Color.White
            else -> Color.White
        }
        if (!view.isInEditMode) {
            (view.context as? android.app.Activity)?.window?.let { window ->
                window.statusBarColor = statusBarColor.toArgb()
                window.navigationBarColor = navigationBarColor.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
            }
        }
    }

    fun isBottomBarTransition(initial: NavBackStackEntry?, target: NavBackStackEntry?): Boolean {
        val initialRoute = initial?.destination?.route?.split("?")?.get(0)
        val targetRoute = target?.destination?.route?.split("?")?.get(0)
        return initialRoute in bottomBarRoutes && targetRoute in bottomBarRoutes
    }

    fun isHomeCardTransition(initial: NavBackStackEntry?, target: NavBackStackEntry?): Boolean {
        val initialRoute = initial?.destination?.route?.split("?")?.get(0)
        val targetRoute = target?.destination?.route?.split("?")?.get(0)
        return (initialRoute == Routes.Home && targetRoute in setOf(Routes.PlaceOrder, Routes.Categories)) ||
            (targetRoute == Routes.Home && initialRoute in setOf(Routes.PlaceOrder, Routes.Categories))
    }

    LaunchedEffect(paymentReturnRequest) {
        val request = paymentReturnRequest ?: return@LaunchedEffect
        navController.navigate(
            "payment_return/${request.status}?orderId=${Uri.encode(request.orderId)}"
        ) {
            launchSingleTop = true
        }
        onPaymentReturnConsumed()
    }

    LaunchedEffect(notificationOpenRequest) {
        val request = notificationOpenRequest ?: return@LaunchedEffect
        navController.navigate(Routes.orders(request.tab)) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = true
            }
            launchSingleTop = true
        }
        if (request.focusOrderId.isNotBlank()) {
            navController.navigate(Routes.orderDetail(request.focusOrderId)) {
                launchSingleTop = true
            }
        }
        onNotificationOpenConsumed()
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            if (isHomeCardTransition(initialState, targetState)) {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(500)
                )
            } else if (isBottomBarTransition(initialState, targetState)) {
                EnterTransition.None
            } else {
                slideInHorizontally(
                    initialOffsetX = { it }, // slides in from RIGHT
                    animationSpec = tween(500)
                )
            }
        },
        exitTransition = {
            if (isHomeCardTransition(initialState, targetState)) {
                slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(500)
                )
            } else if (isBottomBarTransition(initialState, targetState)) {
                ExitTransition.None
            } else {
                slideOutHorizontally(
                    targetOffsetX = { -it }, // exits to LEFT
                    animationSpec = tween(500)
                )
            }
        },
        popEnterTransition = {
            if (isHomeCardTransition(initialState, targetState)) {
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(500)
                )
            } else if (isBottomBarTransition(initialState, targetState)) {
                EnterTransition.None
            } else {
                slideInHorizontally(
                    initialOffsetX = { -it }, // back: slides in from LEFT
                    animationSpec = tween(500)
                )
            }
        },
        popExitTransition = {
            if (isHomeCardTransition(initialState, targetState)) {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(500)
                )
            } else if (isBottomBarTransition(initialState, targetState)) {
                ExitTransition.None
            } else {
                slideOutHorizontally(
                    targetOffsetX = { it }, // back: exits to RIGHT
                    animationSpec = tween(500)
                )
            }
        }
    ) {
        composable(Routes.Splash)      {
            SplashScreen(
                navController,
                authViewModel = authViewModel,
                skipAutoNavigation = paymentReturnRequest != null || notificationOpenRequest != null
            )
        }
        composable(Routes.Welcome)     { WelcomeScreen(navController) }
        composable(Routes.Onboarding)  { OnboardingScreen(navController) }
        composable(Routes.Home)        {
            HomeScreen(
                navController,
                authViewModel = authViewModel,
                orderViewModel = orderViewModel,
                profileViewModel = profileViewModel,
                onContentReady = { homeContentReady = true }
            )
        }
        composable(Routes.Services)    { ServicesScreen(navController, orderViewModel, profileViewModel) }
        
        composable(
            route = "${Routes.Categories}?mode={mode}",
            arguments = listOf(navArgument("mode") { defaultValue = "fresh" })
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "fresh"
            CategoriesScreen(navController, isAdding = mode == "add", orderViewModel = orderViewModel)
        }

        composable(
            route = "${Routes.PlaceOrder}?categoryId={categoryId}&categoryName={categoryName}&iconName={iconName}",
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("categoryName") { type = NavType.StringType },
                navArgument("iconName") { type = NavType.StringType; defaultValue = "cleaning" }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
            val iconName = backStackEntry.arguments?.getString("iconName") ?: "cleaning"
            PlaceOrderScreen(navController, categoryId, categoryName, iconName, orderViewModel)
        }

        composable(Routes.SelectLocation) { SelectLocationScreen(navController) }
        composable(Routes.ChooseOnMap) {
            ChooseOnMapScreen(
                navController = navController,
                onLocationSelected = { lat, lon, address ->
                    val finalAddress = address.ifEmpty { "$lat, $lon" }
                    val previousBackStackEntry = navController.previousBackStackEntry
                    
                    if (previousBackStackEntry?.destination?.route == Routes.SelectLocation) {
                        previousBackStackEntry.savedStateHandle.set("selected_location", finalAddress)
                    } else {
                        previousBackStackEntry?.savedStateHandle?.set("address", finalAddress)
                    }
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = "${Routes.Details}?initialDetails={initialDetails}",
            arguments = listOf(navArgument("initialDetails") { defaultValue = "" })
        ) { backStackEntry ->
            val initialDetails = backStackEntry.arguments?.getString("initialDetails") ?: ""
            DetailsScreen(navController = navController, initialDetails = initialDetails)
        }
        composable(Routes.AddPhoto) {
            AddPhotoScreen(navController = navController)
        }
        composable(Routes.OrderConfirmed) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Order Confirmed!")
            }
        }

        composable(
            route = "${Routes.Orders}?tab={tab}&focusOrderId={focusOrderId}",
            arguments = listOf(
                navArgument("tab") { defaultValue = "0" },
                navArgument("focusOrderId") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val tabStr = backStackEntry.arguments?.getString("tab") ?: "0"
            val initialTab = tabStr.toIntOrNull() ?: 0
            
            OrdersScreen(
                navController = navController,
                orderViewModel = orderViewModel,
                profileViewModel = profileViewModel,
                initialTab = initialTab
            )
        }

        composable(
            route = "payment_return/{status}?orderId={orderId}",
            arguments = listOf(
                navArgument("status") { type = NavType.StringType },
                navArgument("orderId") { type = NavType.StringType }
            ),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "mafimushkil://payment/{status}?orderId={orderId}"
                }
            )
        ) { backStackEntry ->
            val status = backStackEntry.arguments?.getString("status").orEmpty()
            val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
            PaymentReturnScreen(
                navController = navController,
                orderViewModel = orderViewModel,
                status = status,
                orderId = orderId
            )
        }

        composable(
            route = "${Routes.OrderDetail}/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "mafimushkil://order/{orderId}"
                }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderDetailScreen(navController, orderId, orderViewModel, profileViewModel)
        }
        composable(
            route = "${Routes.Payments}/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            PaymentsScreen(navController, orderId, orderViewModel)
        }
        composable(Routes.Promotions)  { PromotionsScreen(navController, profileViewModel) }
        composable(Routes.Notifications) { NotificationsScreen(navController, orderViewModel, profileViewModel) }
        composable(Routes.PhoneVerification) { PhoneVerificationScreen(navController, authViewModel = authViewModel) }
        composable(
            route = "${Routes.OtpVerification}/{phoneNumber}",
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            OtpVerificationScreen(
                navController = navController,
                phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: "",
                authViewModel = authViewModel
            )
        }
        composable(
            route = Routes.MyProfile,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it }, // slide in from RIGHT
                    animationSpec = tween(500)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it }, // exit to RIGHT
                    animationSpec = tween(500)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it }, // back: slide in from LEFT
                    animationSpec = tween(500)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it }, // back: exit to LEFT
                    animationSpec = tween(500)
                )
            }
        ) { MyProfileScreen(navController, profileViewModel) }
        composable(Routes.EditPhoto) { EditPhotoScreen(navController, profileViewModel) }
        composable(Routes.EditName) { EditNameScreen(navController, profileViewModel) }
        composable(Routes.EditEmail) { EditEmailScreen(navController, profileViewModel) }
        composable(Routes.UpdatePhone) {
            PhoneVerificationScreen(navController, isUpdate = true, authViewModel = authViewModel)
        }
        composable(
            route = "${Routes.UpdateOtp}/{phoneNumber}",
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            OtpVerificationScreen(
                navController = navController,
                phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: "",
                isUpdate = true,
                authViewModel = authViewModel
            )
        }
        composable("edit_gender") { EditGenderScreen(navController, profileViewModel) }
        composable(
            route = Routes.ContactUs,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it }, // slide in from RIGHT
                    animationSpec = tween(500)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it }, // exit to RIGHT
                    animationSpec = tween(500)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it }, // back: slide in from LEFT
                    animationSpec = tween(500)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it }, // back: exit to LEFT
                    animationSpec = tween(500)
                )
            }
        ) { ContactUsScreen(navController) }
        composable(Routes.BecomeWorker) { BecomeWorkerScreen(navController) }
        composable(Routes.WorkerForm)   { WorkerFormScreen(navController, applicationViewModel) }
        composable(Routes.RegisterCompany) { RegisterCompanyScreen(navController) }
        composable(Routes.CompanyForm)     { CompanyFormScreen(navController, applicationViewModel) }
        composable(
            route = "${Routes.ServiceOrderDetail}/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            ServiceOrderDetailScreen(
            navController = navController,
            orderId = backStackEntry.arguments?.getString("orderId") ?: "",
            orderViewModel = orderViewModel,
            profileViewModel = profileViewModel
        )
        }
        composable(
            route = "${Routes.ServiceOrderMap}/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            ServiceOrderMapScreen(
                navController = navController,
                orderId = backStackEntry.arguments?.getString("orderId") ?: "",
                orderViewModel = orderViewModel
            )
        }
        composable(
            route = "${Routes.RevokeRegistration}/{target}",
            arguments = listOf(navArgument("target") { type = NavType.StringType })
        ) { backStackEntry ->
            RevokeRegistrationScreen(
                navController = navController,
                target = backStackEntry.arguments?.getString("target") ?: "worker",
                profileViewModel = profileViewModel
            )
        }
    }
}

@Composable
private fun PaymentReturnScreen(
    navController: NavController,
    orderViewModel: OrderViewModel,
    status: String,
    orderId: String
) {
    suspend fun waitForNavHostReady() {
        repeat(10) {
            val state = navController.currentBackStackEntry?.lifecycle?.currentState
            if (state?.isAtLeast(Lifecycle.State.CREATED) == true) return
            delay(50)
        }
    }

    LaunchedEffect(status, orderId) {
        waitForNavHostReady()
        delay(300)
        val currentEntry = navController.currentBackStackEntry
        if (currentEntry?.destination == null) return@LaunchedEffect
        
        when (status.lowercase()) {
            "success" -> {
                orderViewModel.markPaymentCompleted(orderId = orderId, paymentId = "")
                navController.navigate(Routes.orders(1)) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
            "failure" -> {
                navController.navigate(Routes.payments(orderId)) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
            else -> {
                navController.navigate(Routes.orders(1)) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA)) // Match app background color
    )
}
