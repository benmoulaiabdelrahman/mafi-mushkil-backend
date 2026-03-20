package com.vardash.mafimushkil.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.vardash.mafimushkil.R

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
fun AppBottomBar(navController: NavController, selectedIndex: Int) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        navItemsList.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick = {
                    if (selectedIndex != index) {
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
                    if (item.hasBadge) {
                        Box {
                            Icon(
                                if (selectedIndex == index) item.selectedIcon else item.unselectedIcon,
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
                            if (selectedIndex == index) item.selectedIcon else item.unselectedIcon,
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
                        fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal
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
    
    return items.joinToString(", ") { item ->
        val key = item.lowercase()
        when {
            key == "cleaning" -> catCleaning
            key == "electrician" -> catElectrician
            key == "plumber" || key == "repairing" -> catPlumber
            key == "carpenter" -> catCarpenter
            
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
        
        // Home Maintenance
        key == "painter" -> R.drawable.painter
        key == "mason" || key == "contractor" -> R.drawable.mason
        key == "roofing" || key == "waterproofing" -> R.drawable.roofing
        key.contains("ac") -> R.drawable.ac_repair
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
