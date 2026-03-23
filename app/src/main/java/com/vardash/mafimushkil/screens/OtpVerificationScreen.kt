package com.vardash.mafimushkil.screens

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.Routes
import com.vardash.mafimushkil.auth.AuthState
import com.vardash.mafimushkil.auth.AuthViewModel
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreen(
    navController: NavController,
    phoneNumber: String,
    isUpdate: Boolean = false,
    authViewModel: AuthViewModel = viewModel()
) {
    val view = LocalView.current
    
    // Correctly unwrap Activity from View context to bypass MafiMushkilTheme wrappers
    val activity = remember(view) {
        var currentContext = view.context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) return@remember currentContext
            currentContext = currentContext.baseContext
        }
        null
    }

    val authState by authViewModel.authState.collectAsState()
    var otpCode by remember { mutableStateOf("") }
    var countdown by remember { mutableIntStateOf(59) }
    var canResend by remember { mutableStateOf(false) }
    var showSuccessSheet by remember { mutableStateOf(false) }

    // React to auth state
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> showSuccessSheet = true
            else -> {}
        }
    }

    // Countdown timer
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000L)
            countdown--
        }
        canResend = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        // ── Back button (Fixed at top) ─────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color(0xFF282828),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // ── Centered Content ──────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Lime green circle with chat icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCCFD04)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Chat,
                    contentDescription = null,
                    tint = Color(0xFF282828),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            // Title
            Text(
                text = stringResource(R.string.otp_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                textAlign = TextAlign.Center,
                lineHeight = 30.sp,
                fontFamily = Questv1FontFamily
            )

            Spacer(Modifier.height(12.dp))

            // Subtitle
            Text(
                text = stringResource(R.string.otp_subtitle, phoneNumber),
                fontSize = 14.sp,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center,
                lineHeight = 21.sp,
                fontFamily = Questv1FontFamily
            )

            Spacer(Modifier.height(36.dp))

            // OTP code input
            TextField(
                value = otpCode,
                onValueChange = { if (it.length <= 6) otpCode = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
                placeholder = {
                    Text(
                        text = stringResource(R.string.otp_placeholder),
                        color = Color(0xFFAAAAAA),
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontFamily = Questv1FontFamily
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontFamily = Questv1FontFamily),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = Color(0xFF282828)
                )
            )

            Spacer(Modifier.height(20.dp))

            // Verify button
            Button(
                onClick = { activity?.let { authViewModel.verifyOtp(otpCode, it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF282828),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFAAAAAA),
                    disabledContentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp),
                enabled = otpCode.isNotEmpty() && authState !is AuthState.Loading
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(
                        color = Color(0xFFCCFD04),
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 4.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.verify),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Questv1FontFamily
                    )
                }
            }

            // Error display
            if (authState is AuthState.Error) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = (authState as AuthState.Error).message,
                    color = Color.Red,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontFamily = Questv1FontFamily
                )
            }

            Spacer(Modifier.height(16.dp))

            // Countdown / Resend
            if (canResend) {
                TextButton(onClick = {
                    activity?.let { authViewModel.resendOtp(phoneNumber, it) }
                    countdown = 59
                    canResend = false
                }) {
                    Text(
                        text = stringResource(R.string.resend_code),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFCCFD04),
                        fontFamily = Questv1FontFamily
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.resend_timer, countdown),
                    fontSize = 14.sp,
                    color = Color(0xFFAAAAAA),
                    textAlign = TextAlign.Center,
                    fontFamily = Questv1FontFamily
                )
            }
            
            Spacer(Modifier.height(48.dp))
        }
    }

    if (showSuccessSheet) {
        VerificationSuccessSheet(
            title = if (isUpdate) stringResource(R.string.phone_updated) else stringResource(R.string.verification_successful),
            onGoHome = {
                showSuccessSheet = false
                if (isUpdate) {
                    navController.navigate(Routes.MyProfile) {
                        popUpTo(Routes.MyProfile) { inclusive = true }
                    }
                } else {
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationSuccessSheet(
    title: String,
    onGoHome: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = { /* do nothing — user must tap button */ },
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        scrimColor = Color.Black.copy(alpha = 0.5f),
        windowInsets = WindowInsets(0),
        tonalElevation = 0.dp
    ) {
        MafiMushkilTheme {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 28.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Green circle with checkmark
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Title
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    textAlign = TextAlign.Center,
                    fontFamily = Questv1FontFamily
                )

                Spacer(Modifier.height(28.dp))

                // Go to Home button
                Button(
                    onClick = onGoHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF282828),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ok),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = Questv1FontFamily
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, locale = "ar")
@Composable
fun OtpVerificationScreenPreview() {
    MafiMushkilTheme {
        OtpVerificationScreen(navController = rememberNavController(), phoneNumber = "+213123456789")
    }
}
