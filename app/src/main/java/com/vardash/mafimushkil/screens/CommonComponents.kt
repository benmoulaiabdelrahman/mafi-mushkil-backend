package com.vardash.mafimushkil.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.navigation.NavController
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.auth.NotificationBadgeStore
import com.vardash.mafimushkil.auth.ProfileViewModel
import com.vardash.mafimushkil.auth.SessionManager
import androidx.lifecycle.viewmodel.compose.viewModel

data class NavItem(
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val hasBadge: Boolean = false,
    val route: String
)

val navItemsList = listOf(
    NavItem(R.string.nav_home,          Icons.Filled.Home,          Icons.Outlined.Home,          route = "home"),
    NavItem(R.string.nav_orders,        Icons.Filled.ReceiptLong,   Icons.Outlined.ReceiptLong,   route = "orders"),
    NavItem(R.string.nav_promotions,    Icons.Filled.CardGiftcard,  Icons.Outlined.CardGiftcard,  route = "promotions"),
    NavItem(R.string.nav_notifications, Icons.Filled.Notifications, Icons.Outlined.NotificationsNone, hasBadge = false, route = "notifications")
)

@Composable
fun AppBottomBar(
    navController: NavController,
    selectedRoute: String,
    profileViewModel: ProfileViewModel? = null,
    containerColor: Color = Color.White
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    
    val resolvedProfileViewModel: ProfileViewModel? =
        profileViewModel ?: if (LocalInspectionMode.current) null else viewModel()
    
    val userProfile by (resolvedProfileViewModel?.userProfile?.collectAsState()
        ?: androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateOf(com.vardash.mafimushkil.auth.UserProfile())
        })

    // Compute visibility based on both cache AND live state
    val cachedWorkerState = sessionManager.getWorkerState()
    val cachedCompanyState = sessionManager.getCompanyState()
    
    val isWorker = userProfile.workerState.lowercase().trim() != "not" || 
                   cachedWorkerState.lowercase().trim() != "not"
    val isCompany = userProfile.companyState.lowercase().trim() != "not" || 
                    cachedCompanyState.lowercase().trim() != "not"
                    
    val showServicesTab = isWorker || isCompany

    val bottomBarItems = if (showServicesTab) {
        listOf(
            NavItem(R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, route = "home"),
            NavItem(R.string.nav_services, Icons.Filled.Engineering, Icons.Outlined.Engineering, route = com.vardash.mafimushkil.Routes.Services),
            NavItem(R.string.nav_orders, Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, route = "orders"),
            NavItem(R.string.nav_promotions, Icons.Filled.CardGiftcard, Icons.Outlined.CardGiftcard, route = "promotions"),
            NavItem(R.string.nav_notifications, Icons.Filled.Notifications, Icons.Outlined.NotificationsNone, hasBadge = false, route = "notifications")
        )
    } else {
        listOf(
            NavItem(R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, route = "home"),
            NavItem(R.string.nav_orders, Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, route = "orders"),
            NavItem(R.string.nav_promotions, Icons.Filled.CardGiftcard, Icons.Outlined.CardGiftcard, route = "promotions"),
            NavItem(R.string.nav_notifications, Icons.Filled.Notifications, Icons.Outlined.NotificationsNone, hasBadge = false, route = "notifications")
        )
    }

    LaunchedEffect(Unit) {
        resolvedProfileViewModel?.loadUserProfile(context)
    }

    val unreadNotificationCount = NotificationBadgeStore.unreadCount.collectAsState().value
    NavigationBar(
        containerColor = containerColor,
        tonalElevation = 0.dp
    ) {
        bottomBarItems.forEach { item ->
            val isSelected = selectedRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navController.navigate(item.route) {
                            popUpTo("home") {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = item.route != "home"
                        }
                    }
                },
                icon = {
                    if (item.route == "notifications" && unreadNotificationCount > 0) {
                        Box {
                            Icon(
                                if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = stringResource(item.labelResId),
                                modifier = Modifier.size(24.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                                    .background(Color.Red, CircleShape)
                            )
                        }
                    } else {
                        Icon(
                            if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = stringResource(item.labelResId),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = {
                    @Suppress("DEPRECATION")
                    Text(
                        text = stringResource(item.labelResId),
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF282828),
                    selectedTextColor = Color(0xFF282828),
                    unselectedIconColor = Color(0xFFAAAAAA),
                    unselectedTextColor = Color(0xFFAAAAAA),
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

/**
 * Shared helper to translate category names (even comma-separated ones) to localized strings.
 */
@Composable
fun getLocalizedCategoryName(name: String): String {
    if (name.isEmpty()) return ""
    
    val items = name.split(",").map { it.trim() }
    
    val catCleaning = stringResource(R.string.cat_cleaning)
    val catElectrician = stringResource(R.string.cat_electrician)
    val catPlumber = stringResource(R.string.cat_plumber)
    val catCarpenter = stringResource(R.string.cat_carpenter)
    val catPestControl = stringResource(R.string.cat_pest_control)
    val catLocksmith = stringResource(R.string.cat_locksmith)
    val catApplianceRepair = stringResource(R.string.cat_appliance_repair)
    val catFlooringTiling = stringResource(R.string.cat_flooring_tiling)
    val catInteriorDesign = stringResource(R.string.cat_interior_design)
    val catWeldingIronwork = stringResource(R.string.cat_welding_ironwork)
    val catSatelliteTvInstallation = stringResource(R.string.cat_satellite_tv_installation)
    val catBarberHaircutAtHome = stringResource(R.string.cat_barber_haircut_at_home)
    val catBeautyMakeup = stringResource(R.string.cat_beauty_makeup)
    val catPersonalTrainer = stringResource(R.string.cat_personal_trainer)
    val catPhotographerVideographer = stringResource(R.string.cat_photographer_videographer)
    val catTutoringPrivateTeacher = stringResource(R.string.cat_tutoring_private_teacher)
    val catItTechSupport = stringResource(R.string.cat_it_tech_support)
    val catVeterinarian = stringResource(R.string.cat_veterinarian)
    
    return items.joinToString(", ") { item ->
        val key = item.lowercase()
        when {
            key == "cleaning" -> catCleaning
            key == "electrician" -> catElectrician
            key == "plumber" || key == "repairing" -> catPlumber
            key == "carpenter" -> catCarpenter
            key == "pest_control" || key.contains("pest") -> catPestControl
            key == "locksmith" -> catLocksmith
            key == "appliance_repair" || (key.contains("appliance") && key.contains("repair")) -> catApplianceRepair
            key == "flooring" || key.contains("tiling") -> catFlooringTiling
            key == "interior_design" || (key.contains("interior") && key.contains("design")) -> catInteriorDesign
            key == "welding" || key.contains("ironwork") || key.contains("weld") -> catWeldingIronwork
            key.contains("satellite") || key.contains("tv installation") || key == "satelite_dish" -> catSatelliteTvInstallation
            key == "barber" || key.contains("haircut") -> catBarberHaircutAtHome
            key.contains("beauty") || key.contains("makeup") -> catBeautyMakeup
            key.contains("trainer") || (key.contains("personal") && key.contains("trainer")) -> catPersonalTrainer
            key.contains("photographer") || key.contains("videographer") -> catPhotographerVideographer
            key.contains("tutor") || key.contains("teacher") -> catTutoringPrivateTeacher
            key == "it_support" || (key.contains("tech") && key.contains("support")) -> catItTechSupport
            key.contains("veter") -> catVeterinarian
            
            // Home Maintenance
            key == "painter" -> "دهان"
            key == "mason" || key == "contractor" -> "بناء / مقاول"
            key == "roofing" || key == "waterproofing" -> "سقف / عزل"
            key.contains("ac") || key.contains("air condition") -> "تكييف وتبريد"
            key.contains("glazier") || key.contains("glass") -> "زجاج"
            
            // Household
            key == "cook" || key == "chef" -> "طباخ"
            key == "babysitter" || key == "nanny" -> "مربية"
            key.contains("nurse") -> "ممرض منزلي"
            key.contains("car wash") || key.contains("car_wash") -> "غسيل سيارات"
            key.contains("moving") || key.contains("furniture") -> "نقل اثاث"
            
            // Outdoor
            key == "gardener" -> "بستاني"
            key.contains("mechanic") || (key.contains("car") && key.contains("repair")) -> "إصلاح سيارات"
            
            // Delivery & Errands
            key == "delivery" -> "توصيل"
            key.contains("errands") -> "قضاء حوائج"

            else -> item
        }
    }
}

@Composable
fun ScreenHeaderTitle(
    text: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            @Suppress("DEPRECATION")
            Text(
                text = text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Questv1FontFamily,
                textAlign = TextAlign.Center
            )
        }
        if (showDivider) {
            HorizontalDivider(color = Color(0xFFF5F5F5))
        }
    }
}

@Composable
fun getLocalizedStatus(status: String): String {
    return when (status.lowercase().trim()) {
        "pending" -> stringResource(R.string.status_pending)
        "confirmed" -> stringResource(R.string.status_confirmed)
        "assigned" -> stringResource(R.string.status_assigned)
        "accepted" -> stringResource(R.string.status_accepted)
        "in_progress" -> stringResource(R.string.status_in_progress)
        "completed" -> stringResource(R.string.status_completed)
        "cancelled" -> stringResource(R.string.status_cancelled)
        else -> status.replaceFirstChar { it.uppercase() }
    }
}

// SHARED helper to map icon names to drawable resources
fun getCategoryIcon(iconName: String): Int {
    val key = iconName.lowercase().trim()
    return when {
        key == "cleaning" -> R.drawable.cleaning
        key == "repairing" || key == "plumber" -> R.drawable.repairing
        key == "electrician" -> R.drawable.electrician
        key == "carpenter" -> R.drawable.carpenter
        key == "pest_control" || key == "cockroach" || key.contains("pest") -> R.drawable.cockroach
        key == "locksmith" -> R.drawable.locksmith
        key == "appliance_repair" || key == "appliance" || (key.contains("appliance") && key.contains("repair")) -> R.drawable.appliance
        key == "flooring" || key.contains("tiling") -> R.drawable.flooring
        key == "interior_design" || (key.contains("interior") && key.contains("design")) -> R.drawable.interior_design
        key == "welding" || key.contains("ironwork") || key.contains("weld") -> R.drawable.ironworker
        key.contains("satellite") || key.contains("tv installation") || key == "satelite_dish" -> R.drawable.satelite_dish
        key == "barber" || key.contains("haircut") -> R.drawable.barber
        key.contains("beauty") || key.contains("makeup") -> R.drawable.make_up
        key.contains("trainer") || (key.contains("personal") && key.contains("trainer")) -> R.drawable.trainer
        key.contains("photographer") || key.contains("videographer") -> R.drawable.photographer
        key.contains("tutor") || key.contains("teacher") -> R.drawable.teacher
        key == "it_support" || (key.contains("tech") && key.contains("support")) -> R.drawable.it_support
        key.contains("veter") -> R.drawable.veterinary
        
        // Home Maintenance
        key == "painter" -> R.drawable.painter
        key == "mason" || key == "contractor" -> R.drawable.mason
        key == "roofing" || key == "waterproofing" -> R.drawable.roofing
        key == "ac_repair" || key.contains("air condition") || key.contains("air_condition") -> R.drawable.ac_repair
        key.contains("glazier") || key.contains("glass") -> R.drawable.glazier
        
        // Household
        key == "cook" || key == "chef" -> R.drawable.cook
        key == "babysitter" || key == "nanny" -> R.drawable.babysitter
        key.contains("nurse") -> R.drawable.nurse
        key.contains("car_wash") || key.contains("car wash") -> R.drawable.car_wash
        key.contains("moving") -> R.drawable.moving
        
        // Outdoor
        key == "gardener" -> R.drawable.gardener
        key.contains("mechanic") -> R.drawable.mechanic
        
        // Delivery & Errands
        key == "delivery" -> R.drawable.delivery
        key.contains("errands") -> R.drawable.errands
        
        key == "more" -> R.drawable.more
        else -> R.drawable.repairing // Default to plumber icon for unrecognized
    }
}
