package com.vardash.mafimushkil.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.ui.theme.Accent
import com.vardash.mafimushkil.ui.theme.Primary
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme

@Composable
fun WelcomeScreen(navController: NavController) {
    // Layout direction and Locale are now handled globally in MafiMushkilTheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        Text(
            text = stringResource(R.string.welcome_title),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Primary,
            textAlign = TextAlign.Center,
            lineHeight = 42.sp,
            fontFamily = Questv1FontFamily
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.welcome_subtitle),
            fontSize = 14.sp,
            color = Primary.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            fontFamily = Questv1FontFamily
        )

        // Worker image
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.worker),
                contentDescription = stringResource(R.string.worker_image_desc),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Button(
            onClick = { navController.navigate("onboarding") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 38.dp)
                .height(52.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = Accent
            )
        ) {
            Text(
                text = stringResource(R.string.get_started),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Accent,
                fontFamily = Questv1FontFamily
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true, locale = "ar")
@Composable
fun WelcomeScreenPreview() {
    MafiMushkilTheme {
        WelcomeScreen(navController = rememberNavController())
    }
}
