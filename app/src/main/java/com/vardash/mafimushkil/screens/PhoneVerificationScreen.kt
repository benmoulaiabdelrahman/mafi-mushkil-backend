package com.vardash.mafimushkil.screens

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
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

@Composable
fun PhoneVerificationScreen(
    navController: NavController,
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
    var phoneNumber by remember { mutableStateOf("") }
    val countryFlag = "🇩🇿"
    val countryCode = "+213"

    // React to auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.CodeSent -> {
                val fullPhone = "$countryCode$phoneNumber"
                if (isUpdate) {
                    navController.navigate(Routes.updateOtp(fullPhone))
                } else {
                    navController.navigate(Routes.otpVerification(fullPhone))
                }
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        // ── Close button top-right ─────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            IconButton(
                onClick = {
                    if (isUpdate) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Splash) { inclusive = true }
                        }
                    }
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
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
            verticalArrangement = Arrangement.Center
        ) {
            // Lime green circle with phone icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCCFD04))
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhoneAndroid,
                    contentDescription = null,
                    tint = Color(0xFF282828),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            // Title
            Text(
                text = if (isUpdate) stringResource(R.string.phone_verification_update_title)
                       else stringResource(R.string.phone_verification_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                modifier = Modifier.fillMaxWidth(),
                fontFamily = Questv1FontFamily
            )

            Spacer(Modifier.height(32.dp))

            // Phone Number label
            Text(
                text = stringResource(R.string.phone_number_label),
                fontSize = 13.sp,
                color = Color(0xFF888888),
                modifier = Modifier.fillMaxWidth(),
                fontFamily = Questv1FontFamily
            )

            Spacer(Modifier.height(8.dp))

            // Country code + phone input row - ALWAYS LTR
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Country code box (LEFT)
                    Box(
                        modifier = Modifier
                            .width(88.dp)
                            .height(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = countryFlag, fontSize = 18.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = countryCode,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF282828),
                                fontFamily = Questv1FontFamily
                            )
                        }
                    }

                    // Phone number input (RIGHT)
                    TextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        placeholder = {
                            Text(
                                text = "321456987",
                                color = Color(0xFFAAAAAA),
                                fontSize = 14.sp,
                                fontFamily = Questv1FontFamily
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone
                        ),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedContainerColor = Color(0xFFF5F5F5),
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            cursorColor = Color(0xFF282828)
                        ),
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            fontFamily = Questv1FontFamily,
                            textDirection = TextDirection.Ltr
                        )
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Verify button
            Button(
                onClick = {
                    val fullPhone = "$countryCode$phoneNumber"
                    activity?.let { authViewModel.sendOtp(fullPhone, it) }
                },
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
                enabled = phoneNumber.isNotEmpty() && authState !is AuthState.Loading
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(
                        color = Color(0xFFCCFD04),
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 4.dp
                    )
                } else {
                    Text(
                        text = if (isUpdate) stringResource(R.string.send_code) else stringResource(R.string.verify),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = Questv1FontFamily
                    )
                }
            }

            // Show error if any
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
            
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, locale = "ar")
@Composable
fun PhoneVerificationScreenPreview() {
    MafiMushkilTheme {
        PhoneVerificationScreen(navController = rememberNavController())
    }
}
