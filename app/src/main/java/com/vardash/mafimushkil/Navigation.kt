package com.vardash.mafimushkil

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vardash.mafimushkil.auth.ApplicationViewModel
import com.vardash.mafimushkil.auth.AuthViewModel
import com.vardash.mafimushkil.auth.OrderViewModel
import com.vardash.mafimushkil.auth.ProfileViewModel
import com.vardash.mafimushkil.screens.*

@Composable
fun AppNavigation(startDestination: String = "splash") {
    val navController = rememberNavController()

    // Shared ViewModels
    val authViewModel: AuthViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val orderViewModel: OrderViewModel = viewModel()
    val applicationViewModel: ApplicationViewModel = viewModel()

    val bottomBarRoutes = listOf("home", "orders", "promotions", "notifications")

    fun isBottomBarTransition(initial: NavBackStackEntry?, target: NavBackStackEntry?): Boolean {
        val initialRoute = initial?.destination?.route
        val targetRoute = target?.destination?.route
        return initialRoute in bottomBarRoutes && targetRoute in bottomBarRoutes
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            if (isBottomBarTransition(initialState, targetState)) {
                fadeIn(animationSpec = tween(200))
            } else {
                slideInHorizontally(
                    initialOffsetX = { it }, // slides in from RIGHT
                    animationSpec = tween(300)
                )
            }
        },
        exitTransition = {
            if (isBottomBarTransition(initialState, targetState)) {
                fadeOut(animationSpec = tween(200))
            } else {
                slideOutHorizontally(
                    targetOffsetX = { -it }, // exits to LEFT
                    animationSpec = tween(300)
                )
            }
        },
        popEnterTransition = {
            if (isBottomBarTransition(initialState, targetState)) {
                fadeIn(animationSpec = tween(200))
            } else {
                slideInHorizontally(
                    initialOffsetX = { -it }, // back: slides in from LEFT
                    animationSpec = tween(300)
                )
            }
        },
        popExitTransition = {
            if (isBottomBarTransition(initialState, targetState)) {
                fadeOut(animationSpec = tween(200))
            } else {
                slideOutHorizontally(
                    targetOffsetX = { it }, // back: exits to RIGHT
                    animationSpec = tween(300)
                )
            }
        }
    ) {
        composable("splash")      { SplashScreen(navController, authViewModel = authViewModel) }
        composable("welcome")     { WelcomeScreen(navController) }
        composable("onboarding")  { OnboardingScreen(navController) }
        composable("home")        { HomeScreen(navController, authViewModel = authViewModel, orderViewModel = orderViewModel) }
        
        composable(
            route = "categories?mode={mode}",
            arguments = listOf(navArgument("mode") { defaultValue = "fresh" })
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "fresh"
            CategoriesScreen(navController, isAdding = mode == "add", orderViewModel = orderViewModel)
        }

        composable(
            route = "place_order?categoryId={categoryId}&categoryName={categoryName}&iconName={iconName}",
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

        composable("select_location") { SelectLocationScreen(navController) }
        composable("choose_on_map") {
            ChooseOnMapScreen(
                navController = navController,
                onLocationSelected = { lat, lon, address ->
                    val finalAddress = address.ifEmpty { "$lat, $lon" }
                    val previousBackStackEntry = navController.previousBackStackEntry
                    
                    if (previousBackStackEntry?.destination?.route == "select_location") {
                        previousBackStackEntry.savedStateHandle.set("selected_location", finalAddress)
                    } else {
                        previousBackStackEntry?.savedStateHandle?.set("address", finalAddress)
                    }
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = "details?initialDetails={initialDetails}",
            arguments = listOf(navArgument("initialDetails") { defaultValue = "" })
        ) { backStackEntry ->
            val initialDetails = backStackEntry.arguments?.getString("initialDetails") ?: ""
            DetailsScreen(navController = navController, initialDetails = initialDetails)
        }
        composable("add_photo") {
            AddPhotoScreen(navController = navController)
        }
        composable("order_confirmed") {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Order Confirmed!")
            }
        }
        composable("orders")      { OrdersScreen(navController, orderViewModel) }
        composable(
            route = "order_detail/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderDetailScreen(navController, orderId, orderViewModel)
        }
        composable(
            route = "payments/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            PaymentsScreen(navController, orderId, orderViewModel)
        }
        composable("promotions")  { PromotionsScreen(navController) }
        composable("notifications") { NotificationsScreen(navController) }
        composable("phone_verification") { PhoneVerificationScreen(navController, authViewModel = authViewModel) }
        composable(
            route = "otp_verification/{phoneNumber}",
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            OtpVerificationScreen(
                navController = navController,
                phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: "",
                authViewModel = authViewModel
            )
        }
        composable(
            route = "my_profile",
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it }, // slide in from LEFT
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it }, // exit to RIGHT
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it }, // back: slide in from RIGHT
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it }, // back: exit to LEFT
                    animationSpec = tween(300)
                )
            }
        ) { MyProfileScreen(navController, profileViewModel) }
        composable("edit_photo") { EditPhotoScreen(navController, profileViewModel) }
        composable("edit_name") { EditNameScreen(navController, profileViewModel) }
        composable("edit_email") { EditEmailScreen(navController, profileViewModel) }
        composable("update_phone") {
            PhoneVerificationScreen(navController, isUpdate = true, authViewModel = authViewModel)
        }
        composable(
            route = "update_otp/{phoneNumber}",
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
            route = "contact_us",
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it }, // slide in from LEFT
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it }, // exit to RIGHT
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it }, // back: slide in from RIGHT
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it }, // back: exit to LEFT
                    animationSpec = tween(300)
                )
            }
        ) { ContactUsScreen(navController) }
        composable("become_worker") { BecomeWorkerScreen(navController) }
        composable("worker_form")   { WorkerFormScreen(navController, applicationViewModel) }
        composable("register_company") { RegisterCompanyScreen(navController) }
        composable("company_form")     { CompanyFormScreen(navController, applicationViewModel) }
    }
}
