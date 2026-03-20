package com.vardash.mafimushkil.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.auth.AuthState
import com.vardash.mafimushkil.auth.AuthViewModel
import com.vardash.mafimushkil.ui.theme.Accent
import com.vardash.mafimushkil.ui.theme.Primary
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.checkAuthState(context)
        delay(2500)
        
        if (authState is AuthState.Success) {
            navController.navigate("home") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("welcome") {
                popUpTo("splash") { inclusive = true }
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
                modifier = Modifier.size(120.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(16.dp))

            // Updated text to match the styling and spacing in the home screen
            Text(
                text = stringResource(R.string.mafi_mushkil),
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
                painter = painterResource(R.drawable.pixel_io_technologies),
                contentDescription = stringResource(R.string.pixel_io_desc),
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
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    MafiMushkilTheme {
        SplashScreenContent()
    }
}
