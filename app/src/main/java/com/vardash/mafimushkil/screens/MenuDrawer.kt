package com.vardash.mafimushkil.screens

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.vardash.mafimushkil.Routes
import androidx.navigation.compose.rememberNavController
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.auth.AuthViewModel
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily

fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun MenuDrawer(
    isVisible: Boolean,
    onClose: () -> Unit,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    var showRateSheet by remember { mutableStateOf(false) }
    var showLogoutSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClose() }
            )
        }

        // Drawer Content
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(initialOffsetX = { if (layoutDirection == LayoutDirection.Rtl) it else -it }),
            exit = slideOutHorizontally(targetOffsetX = { if (layoutDirection == LayoutDirection.Rtl) it else -it })
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp),
                color = Color(0xFFF8F9FD), // Very light gray/blue background
                shape = RoundedCornerShape(
                    topStart = 0.dp,
                    bottomStart = 0.dp,
                    topEnd = 24.dp, 
                    bottomEnd = 24.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        @Suppress("DEPRECATION")
                        Text(
                            text = stringResource(R.string.app_name),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D2D2D),
                            fontFamily = Questv1FontFamily
                        )
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
                        }
                    }

                    // Spacer/Divider line
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF1F4F9)))

                    // Menu Items
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Group 1
                        Column(modifier = Modifier.background(Color.White)) {
                            MenuListItem(
                                icon = Icons.Outlined.Person,
                                label = stringResource(R.string.menu_profile),
                                onClick = { onClose(); navController.navigate(Routes.MyProfile) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 60.dp), color = Color(0xFFF1F4F9))
                            MenuListItem(
                                icon = Icons.Outlined.ContactSupport,
                                label = stringResource(R.string.menu_contact),
                                onClick = { onClose(); navController.navigate(Routes.ContactUs) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Group 2
                        Column(modifier = Modifier.background(Color.White)) {
                            MenuListItem(
                                icon = Icons.Outlined.Engineering, 
                                label = stringResource(R.string.menu_become_worker),
                                onClick = { onClose(); navController.navigate(Routes.BecomeWorker) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 60.dp), color = Color(0xFFF1F4F9))
                            MenuListItem(
                                icon = Icons.Outlined.Domain, 
                                label = stringResource(R.string.menu_register_company),
                                onClick = { onClose(); navController.navigate(Routes.RegisterCompany) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Group 3
                        Column(modifier = Modifier.background(Color.White)) {
                            MenuListItem(
                                icon = Icons.Outlined.Share,
                                label = stringResource(R.string.menu_share),
                                onClick = {
                                    onClose()
                                    try {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.menu_share_text))
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, null).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(shareIntent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 60.dp), color = Color(0xFFF1F4F9))
                            MenuListItem(
                                icon = Icons.Outlined.StarOutline,
                                label = stringResource(R.string.menu_rate),
                                onClick = { onClose(); showRateSheet = true }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 60.dp), color = Color(0xFFF1F4F9))
                            MenuListItem(
                                icon = Icons.AutoMirrored.Outlined.Logout,
                                label = stringResource(R.string.menu_logout),
                                onClick = { onClose(); showLogoutSheet = true }
                            )
                        }
                    }

                    // Footer with Pixel io Technologies branding
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pixel io Technologies (matching ContactUs style)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.pixel_io_technologies),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(Modifier.width(6.dp))
                            Column {
                                @Suppress("DEPRECATION")
                                Text(
                                    text = stringResource(R.string.pixel_io),
                                    fontSize = 9.9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Questv1FontFamily,
                                    color = Color(0xFF282828),
                                    lineHeight = 11.sp
                                )
                                @Suppress("DEPRECATION")
                                Text(
                                    text = stringResource(R.string.technologies),
                                    fontSize = 9.9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Questv1FontFamily,
                                    color = Color(0xFF282828),
                                    lineHeight = 11.sp
                                )
                            }
                        }

                        // Version Number
                        @Suppress("DEPRECATION")
                        Text(
                            text = stringResource(R.string.menu_version, "1.0"),
                            fontSize = 13.sp,
                            color = Color(0xFF8E8E8E),
                            fontFamily = Questv1FontFamily
                        )
                    }
                }
            }
        }

        // Rate Sheet
        if (showRateSheet) {
            RateSheet(onDismiss = { showRateSheet = false })
        }

        // Logout Bottom Sheet
        if (showLogoutSheet) {
            LogoutSheet(
                onDismiss = { showLogoutSheet = false },
                onConfirm = {
                    showLogoutSheet = false
                    authViewModel.logout(context)
                    navController.navigate(Routes.Welcome) {
                        popUpTo(0)
                    }
                }
            )
        }
    }
}

@Composable
fun MenuListItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFBDBDBD),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            @Suppress("DEPRECATION")
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF4A4A4A),
                modifier = Modifier.weight(1f),
                fontFamily = Questv1FontFamily
            )
            // Use standard KeyboardArrowLeft (not AutoMirrored) to ensure it always faces left '<'
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = null,
                tint = Color(0xFFE0E0E0),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogoutSheet(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        scrimColor = Color.Black.copy(alpha = 0.5f),
        windowInsets = WindowInsets(0),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 36.dp)
                .navigationBarsPadding()
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Red circle with logout icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8524A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.menu_logout_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                textAlign = TextAlign.Center,
                fontFamily = Questv1FontFamily
            )

            Spacer(Modifier.height(12.dp))

            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.menu_logout_confirm),
                fontSize = 15.sp,
                color = Color(0xFF8E8E8E),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                fontFamily = Questv1FontFamily
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF282828),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                @Suppress("DEPRECATION")
                Text(
                    stringResource(R.string.menu_logout_yes),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Questv1FontFamily
                )
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = onDismiss) {
                @Suppress("DEPRECATION")
                Text(
                    stringResource(R.string.menu_cancel),
                    color = Color(0xFFE8524A),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Questv1FontFamily
                )
            }
        }
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
fun MenuDrawerPreview() {
    MafiMushkilTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            MenuDrawer(
                isVisible = true,
                onClose = {},
                navController = rememberNavController(),
                authViewModel = AuthViewModel()
            )
        }
    }
}
