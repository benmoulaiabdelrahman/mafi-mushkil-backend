@file:OptIn(ExperimentalFoundationApi::class)
package com.vardash.mafimushkil.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily
import kotlinx.coroutines.launch

data class OnboardingPage(val resId: Int, val titleRes: Int, val descRes: Int)

@Composable
fun OnboardingScreen(navController: NavController) {
    val pages = listOf(
        OnboardingPage(R.drawable.onboarding1, R.string.onboarding_title1, R.string.onboarding_desc1),
        OnboardingPage(R.drawable.onboarding2, R.string.onboarding_title2, R.string.onboarding_desc2),
        OnboardingPage(R.drawable.onboarding3, R.string.onboarding_title3, R.string.onboarding_desc3)
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    var showSecuritySheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { index ->
            val page = pages[index]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = page.resId),
                        contentDescription = stringResource(page.titleRes),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                
                Text(
                    text = stringResource(page.titleRes),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF33353C),
                    textAlign = TextAlign.Center,
                    fontFamily = Questv1FontFamily
                )
                
                Spacer(Modifier.height(12.dp))
                
                Text(
                    text = stringResource(page.descRes),
                    fontSize = 16.sp,
                    color = Primary.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    fontFamily = Questv1FontFamily
                )
                
                Spacer(Modifier.height(48.dp))
            }
        }

        // Dot indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { i ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(if (i == pagerState.currentPage) 20.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (i == pagerState.currentPage) Primary
                            else Color(0xFFD9D9D9)
                        )
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Skip / Next buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { 
                showSecuritySheet = true
            }) {
                Text(
                    text = stringResource(R.string.skip), 
                    fontSize = 17.sp, 
                    color = Primary,
                    fontFamily = Questv1FontFamily
                )
            }

            Button(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        showSecuritySheet = true
                    }
                },
                modifier = Modifier.size(width = 108.dp, height = 40.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = Accent
                )
            ) {
                Text(
                    text = if (pagerState.currentPage == pages.size - 1) stringResource(R.string.finish) else stringResource(R.string.next),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Accent,
                    fontFamily = Questv1FontFamily
                )
            }
        }
    }

    if (showSecuritySheet) {
        SecurityVerificationSheet(
            onVerify = {
                showSecuritySheet = false
                navController.navigate("phone_verification")
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, locale = "ar")
@Composable
fun OnboardingScreenPreview() {
    MafiMushkilTheme {
        OnboardingScreen(navController = rememberNavController())
    }
}
