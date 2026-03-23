package com.vardash.mafimushkil.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.Routes
import com.vardash.mafimushkil.auth.ProfileState
import com.vardash.mafimushkil.auth.ProfileViewModel
import com.vardash.mafimushkil.auth.UserProfile
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel? = if (LocalInspectionMode.current) null else viewModel()
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    // Safely collect state from ViewModel or provide default for Preview
    val userProfile by (profileViewModel?.userProfile?.collectAsState() ?: remember { mutableStateOf(UserProfile()) })
    val profileState by (profileViewModel?.profileState?.collectAsState() ?: remember { mutableStateOf(ProfileState.Idle) })

    var showEmailVerifiedSheet by remember { mutableStateOf(false) }
    var showUpdatePhoneSheet by remember { mutableStateOf(false) }

    // Load profile when screen opens
    LaunchedEffect(Unit) {
        profileViewModel?.loadUserProfile()
    }

    // Check email verification when returning to screen
    LaunchedEffect(Unit) {
        profileViewModel?.checkEmailVerification()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEEF0F5))
    ) {
        // ── White top bar ─────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color(0xFF282828)
                    )
                }
                @Suppress("DEPRECATION")
                Text(
                    text = stringResource(R.string.profile_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    fontFamily = Questv1FontFamily
                )
            }
        }

        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

        // ── Profile rows ──────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            // Profile photo row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(Routes.EditPhoto) }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                @Suppress("DEPRECATION")
                Text(
                    text = stringResource(R.string.profile_photo),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    modifier = Modifier.padding(end = 16.dp),
                    fontFamily = Questv1FontFamily
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE0E0E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userProfile.profilePhoto.isNotEmpty()) {
                            // Use the URL as a key for remember to reset aspect ratio when URL changes
                            var avatarAspectRatio by remember(userProfile.profilePhoto) { mutableFloatStateOf(1f) }
                            
                            // Proportional scaling logic
                            val minScale = if (avatarAspectRatio > 1f) avatarAspectRatio else 1f / avatarAspectRatio
                            
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(userProfile.profilePhoto)
                                    .size(Size.ORIGINAL) // Force full resolution for crisp zoom
                                    .crossfade(true)
                                    .build(),
                                contentDescription = stringResource(R.string.profile_photo),
                                onSuccess = { state ->
                                    avatarAspectRatio = state.painter.intrinsicSize.width / state.painter.intrinsicSize.height
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = userProfile.photoScale * minScale,
                                        scaleY = userProfile.photoScale * minScale,
                                        // Offsets were saved in DP relative to 280dp editor box.
                                        // Multiply by density to get pixels, then scale by proportional size (56/280 = 0.2)
                                        translationX = userProfile.photoOffsetX * density.density * (56f / 280f),
                                        translationY = userProfile.photoOffsetY * density.density * (56f / 280f)
                                    ),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = stringResource(R.string.profile_photo),
                                tint = Color(0xFF9E9E9E),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFFCCCCCC)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(horizontal = 20.dp))

            // Name row
            ProfileRow(
                label = stringResource(R.string.profile_name),
                value = userProfile.name.ifEmpty { stringResource(R.string.profile_not_set) },
                onClick = { navController.navigate(Routes.EditName) }
            )

            HorizontalDivider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(horizontal = 20.dp))

            // Email row - Optimized for long emails with ellipsis
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navController.navigate(Routes.EditEmail)
                    }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    @Suppress("DEPRECATION")
                    Text(
                        text = stringResource(R.string.profile_email),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        fontFamily = Questv1FontFamily
                    )
                    if (userProfile.isEmailVerified) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = stringResource(R.string.profile_verified_desc),
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Spacer(Modifier.width(16.dp))

                @Suppress("DEPRECATION")
                Text(
                    text = userProfile.email.ifEmpty { stringResource(R.string.profile_not_set) },
                    fontSize = 14.sp,
                    color = Color(0xFF888888),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.5f), // Give email value more priority space
                    fontFamily = Questv1FontFamily
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFFCCCCCC)
                )
            }

            HorizontalDivider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(horizontal = 20.dp))

            // Contact row
            ProfileRow(
                label = stringResource(R.string.profile_contact),
                value = userProfile.phone.ifEmpty { stringResource(R.string.profile_not_set) },
                onClick = { 
                    showUpdatePhoneSheet = true 
                },
                isPhone = true
            )

            HorizontalDivider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(horizontal = 20.dp))

            // Gender row
            ProfileRow(
                label = stringResource(R.string.profile_gender),
                value = when(userProfile.gender) {
                    "Male" -> stringResource(R.string.edit_gender_male)
                    "Female" -> stringResource(R.string.edit_gender_female)
                    else -> userProfile.gender.ifEmpty { stringResource(R.string.profile_not_set) }
                },
                onClick = { navController.navigate("edit_gender") }
            )
        }
    }

    // ── Email verification result sheet ───────────────────
    if (showEmailVerifiedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEmailVerifiedSheet = false },
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
                    .padding(horizontal = 24.dp)
                    .padding(top = 28.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circle — green if verified, red if not
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            if (userProfile.isEmailVerified) Color(0xFF4CAF50)
                            else Color(0xFFF44336)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (userProfile.isEmailVerified) Icons.Outlined.CheckCircle
                        else Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                @Suppress("DEPRECATION")
                Text(
                    text = if (userProfile.isEmailVerified) stringResource(R.string.profile_email_verified)
                    else stringResource(R.string.profile_email_not_verified),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    textAlign = TextAlign.Center,
                    fontFamily = Questv1FontFamily
                )

                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = {
                        showEmailVerifiedSheet = false
                        if (!userProfile.isEmailVerified) {
                            navController.navigate(Routes.EditEmail)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF282828),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    @Suppress("DEPRECATION")
                    Text(stringResource(R.string.ok), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = Questv1FontFamily)
                }
            }
        }
    }

    if (showUpdatePhoneSheet) {
        UpdatePhoneSheet(
            onUpdate = {
                showUpdatePhoneSheet = false
                navController.navigate(Routes.UpdatePhone)
            },
            onDismiss = { showUpdatePhoneSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatePhoneSheet(
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp)
                .navigationBarsPadding()
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Purple circle with phone icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7C6FD4)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Phone,
                    contentDescription = stringResource(R.string.profile_contact),
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.profile_update_phone_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                textAlign = TextAlign.Center,
                fontFamily = Questv1FontFamily
            )

            Spacer(Modifier.height(12.dp))

            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.profile_update_phone_desc),
                fontSize = 14.sp,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center,
                lineHeight = 21.sp,
                fontFamily = Questv1FontFamily
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onUpdate,
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
                    text = stringResource(R.string.profile_update_button),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Questv1FontFamily
                )
            }
        }
    }
}

@Composable
fun ProfileRow(
    label: String,
    value: String?,
    onClick: () -> Unit,
    isPhone: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        @Suppress("DEPRECATION")
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
            modifier = Modifier.weight(1f), // Ensure label takes available space
            fontFamily = Questv1FontFamily
        )
        
        Spacer(Modifier.width(16.dp))

        Row(
            modifier = Modifier.weight(1.5f), // Give value more space
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            if (value != null) {
                @Suppress("DEPRECATION")
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (isPhone) LayoutDirection.Ltr else LocalLayoutDirection.current
                ) {
                    Text(
                        text = value,
                        fontSize = 14.sp,
                        color = Color(0xFF888888),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        fontFamily = Questv1FontFamily,
                        style = if (isPhone) TextStyle(textDirection = TextDirection.Ltr) else LocalTextStyle.current
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFCCCCCC)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, locale = "ar")
@Composable
fun MyProfileScreenPreview() {
    MafiMushkilTheme {
        MyProfileScreen(navController = rememberNavController())
    }
}
