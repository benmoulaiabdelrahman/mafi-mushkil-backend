package com.vardash.mafimushkil.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.Routes
import com.vardash.mafimushkil.auth.AuthState
import com.vardash.mafimushkil.auth.AuthViewModel
import com.vardash.mafimushkil.ui.theme.Accent
import com.vardash.mafimushkil.ui.theme.Primary
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme

@Composable
fun SplashScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    skipAutoNavigation: Boolean = false
) {
    val context = LocalContext.current
    val view = LocalView.current
    val accentColorArgb = Accent.toArgb()

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = accentColorArgb
            window.navigationBarColor = accentColorArgb
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    LaunchedEffect(skipAutoNavigation) {
        authViewModel.checkAuthState(context)
        withFrameNanos { }
        if (skipAutoNavigation) return@LaunchedEffect

        if (authViewModel.authState.value is AuthState.Success) {
            navController.navigate(Routes.Home) {
                popUpTo(Routes.Splash) { inclusive = true }
            }
        } else {
            navController.navigate(Routes.Welcome) {
                popUpTo(Routes.Splash) { inclusive = true }
            }
        }
    }

    SplashScreenContent()
}

@Composable
fun SplashScreenContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Accent)
    ) {
        // Center content
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = stringResource(R.string.app_logo_desc),
            modifier = Modifier.size(96.dp),
            contentScale = ContentScale.Fit
        )

            Spacer(Modifier.height(4.dp))

            // Updated text to match the styling and spacing in the home screen
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Primary,
                fontFamily = Questv1FontFamily,
                lineHeight = 32.sp // Added line height to better control spacing between lines
            )
        }

        // Bottom branding
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.vardash),
                contentDescription = stringResource(R.string.brand_desc),
                modifier = Modifier.size(36.dp),
                contentScale = ContentScale.Fit
            )

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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    MafiMushkilTheme {
        SplashScreenContent()
    }
}

@Composable
fun LaunchSplashContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Accent),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.app_logo_reversed),
            contentDescription = stringResource(R.string.app_logo_desc),
            modifier = Modifier.size(144.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Launch Splash")
@Composable
fun LaunchSplashPreview() {
    MafiMushkilTheme {
        LaunchSplashContent()
    }
}
