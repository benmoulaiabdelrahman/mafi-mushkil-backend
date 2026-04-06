package com.vardash.mafimushkil.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme
import kotlinx.coroutines.delay

const val CONTACT_PHONE = "+213 347 096 35"
const val CONTACT_EMAIL = "contact@mafimushkil.services"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactUsScreen(navController: NavController) {
    val context = LocalContext.current
    var showPhoneSheet by remember { mutableStateOf(false) }
    var showEmailSheet by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var lastToastMessage by remember { mutableStateOf("") }

    val phoneCopiedMsg = stringResource(R.string.contact_toast_phone_copied)
    val emailCopiedMsg = stringResource(R.string.contact_toast_email_copied)

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            lastToastMessage = toastMessage!!
            delay(2000)
            toastMessage = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // ── Back button ───────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 8.dp, top = 4.dp)
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color(0xFF282828)
                    )
                }
            }

            // ── Center content ────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                @Suppress("DEPRECATION")
                Text(
                    text = stringResource(R.string.contact_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    fontFamily = Questv1FontFamily
                )

                Spacer(Modifier.height(8.dp))

                @Suppress("DEPRECATION")
                Text(
                    text = stringResource(R.string.contact_subtitle),
                    fontSize = 14.sp,
                    color = Color(0xFF888888),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    fontFamily = Questv1FontFamily
                )

                Spacer(Modifier.height(40.dp))

                // Phone contact
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showPhoneSheet = true }
                        .padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                color = Color(0xFFCCFD04),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Phone,
                            contentDescription = stringResource(R.string.profile_contact),
                            tint = Color(0xFF282828),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    
                    // Force Phone number to be LTR
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        @Suppress("DEPRECATION")
                        Text(
                            text = CONTACT_PHONE,
                            fontSize = 14.sp,
                            color = Color(0xFF888888),
                            fontFamily = Questv1FontFamily,
                            style = TextStyle(textDirection = TextDirection.Ltr)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Email contact
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showEmailSheet = true }
                        .padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                color = Color(0xFFCCFD04),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Email,
                            contentDescription = stringResource(R.string.profile_email),
                            tint = Color(0xFF282828),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    @Suppress("DEPRECATION")
                    Text(
                        text = CONTACT_EMAIL,
                        fontSize = 14.sp,
                        color = Color(0xFF888888),
                        fontFamily = Questv1FontFamily
                    )
                }
            }

            // ── Bottom branding ───────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Get Connected label
                @Suppress("DEPRECATION")
                Text(
                    text = stringResource(R.string.contact_get_connected),
                    fontSize = 13.sp,
                    color = Color(0xFF888888),
                    fontFamily = Questv1FontFamily
                )

                Spacer(Modifier.height(14.dp))

                // Social icons row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SocialIcon(painterResource(R.drawable.ic_linkedin), "LinkedIn")
                    SocialIcon(painterResource(R.drawable.ic_facebook), "Facebook")
                    SocialIcon(painterResource(R.drawable.ic_twitter), "Twitter")
                    SocialIcon(painterResource(R.drawable.ic_instagram), "Instagram")
                    SocialIcon(painterResource(R.drawable.ic_whatsapp), "WhatsApp")
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(24.dp))

                // Vardash branding (matching SplashScreen)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.vardash),
                        contentDescription = stringResource(R.string.brand_desc),
                        modifier = Modifier.size(36.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.width(4.dp))
                    @Suppress("DEPRECATION")
                    Text(
                        text = stringResource(R.string.brand_name),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Questv1FontFamily,
                        color = Color(0xFF282828),
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // ── Custom Toast ──────────────────────────────────
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        ) {
            Surface(
                color = Color.Black,
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 4.dp
            ) {
                @Suppress("DEPRECATION")
                Text(
                    text = lastToastMessage,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    fontSize = 14.sp,
                    fontFamily = Questv1FontFamily
                )
            }
        }

        // ── Phone action sheet ────────────────────────────────
        if (showPhoneSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPhoneSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                        .padding(horizontal = 16.dp)
                        .padding(top = 28.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 32.dp)
                ) {
                    ContactActionRow(
                        icon = Icons.Outlined.ContentCopy,
                        label = stringResource(R.string.contact_copy_phone),
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("phone", CONTACT_PHONE))
                            toastMessage = phoneCopiedMsg
                            showPhoneSheet = false
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    ContactActionRow(
                        icon = Icons.Outlined.Phone,
                        label = stringResource(R.string.contact_call_us),
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, "tel:$CONTACT_PHONE".toUri()).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                            showPhoneSheet = false
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    ContactActionRow(
                        icon = Icons.AutoMirrored.Outlined.Chat,
                        label = stringResource(R.string.contact_open_whatsapp),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW,
                                "https://wa.me/${CONTACT_PHONE.replace(" ", "").replace("+", "")}".toUri()).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                            showPhoneSheet = false
                        }
                    )
                }
            }
        }

        // ── Email action sheet ────────────────────────────────
        if (showEmailSheet) {
            ModalBottomSheet(
                onDismissRequest = { showEmailSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                        .padding(horizontal = 16.dp)
                        .padding(top = 28.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 32.dp)
                ) {
                    ContactActionRow(
                        icon = Icons.Outlined.ContentCopy,
                        label = stringResource(R.string.contact_copy_email),
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("email", CONTACT_EMAIL))
                            toastMessage = emailCopiedMsg
                            showEmailSheet = false
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    ContactActionRow(
                        icon = Icons.Outlined.AlternateEmail,
                        label = stringResource(R.string.contact_send_email),
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:$CONTACT_EMAIL".toUri()
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                            showEmailSheet = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SocialIcon(painter: Painter, label: String) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(Color(0xFF282828), RoundedCornerShape(4.dp))
            .clickable { /* TODO: open social link */ },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun ContactActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF282828),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(16.dp))
            @Suppress("DEPRECATION")
            Text(
                text = label,
                fontSize = 15.sp,
                color = Color(0xFF282828),
                fontFamily = Questv1FontFamily
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, locale = "ar")
@Composable
fun ContactUsScreenPreview() {
    MafiMushkilTheme {
        ContactUsScreen(navController = rememberNavController())
    }
}
