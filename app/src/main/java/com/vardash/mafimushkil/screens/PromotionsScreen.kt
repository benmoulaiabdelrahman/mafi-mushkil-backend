package com.vardash.mafimushkil.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.Routes
import com.vardash.mafimushkil.auth.ProfileViewModel
import com.vardash.mafimushkil.auth.PromotionViewModel
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily

enum class PromotionStatus { ACTIVE, EXPIRED }

data class Promotion(
    val id: String,
    val title: String,
    val description: String,
    val status: PromotionStatus,
    val dateRange: String? = null  // only shown for ACTIVE promotions
)

fun promotionStatusColor(status: PromotionStatus): Color = when (status) {
    PromotionStatus.ACTIVE  -> Color(0xFF4CAF50)
    PromotionStatus.EXPIRED -> Color(0xFFF44336)
}

@Composable
fun promotionStatusLabel(status: PromotionStatus): String = when (status) {
    PromotionStatus.ACTIVE  -> stringResource(R.string.promotions_status_active)
    PromotionStatus.EXPIRED -> stringResource(R.string.promotions_status_expired)
}

@Composable
fun PromotionCard(promotion: Promotion) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left: title + description
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = promotion.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    fontFamily = Questv1FontFamily
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = promotion.description,
                    fontSize = 13.sp,
                    color = Color(0xFF888888),
                    lineHeight = 18.sp,
                    fontFamily = Questv1FontFamily
                )
            }

            // Right: status badge + date range
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Outlined pill badge
                Box(
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = promotionStatusColor(promotion.status),
                            shape = RoundedCornerShape(50.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = promotionStatusLabel(promotion.status),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = promotionStatusColor(promotion.status),
                        fontFamily = Questv1FontFamily
                    )
                }

                // Date range — only shown for ACTIVE
                if (promotion.status == PromotionStatus.ACTIVE &&
                    promotion.dateRange != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = promotion.dateRange,
                        fontSize = 12.sp,
                        color = Color(0xFFAAAAAA),
                        fontFamily = Questv1FontFamily
                    )
                }
            }
        }
    }
    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
}

@Composable
fun PromotionsScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel = viewModel(),
    promotionViewModel: PromotionViewModel = viewModel()
) {
    val context = LocalContext.current
    val promotions by promotionViewModel.promotions.collectAsState()
    val isLoading by promotionViewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        promotionViewModel.loadPromotions(context)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
            bottomBar = {
                // Wrap NavigationBar in a Box that extends white background behind system nav buttons
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White) // white fills behind system buttons
                ) {
                    AppBottomBar(
                        navController = navController,
                        selectedRoute = Routes.Promotions,
                        profileViewModel = profileViewModel
                    )
                }
            }
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F8FA)) // Matched with Home and Order screens
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            Surface(color = Color.White) {
                ScreenHeaderTitle(
                    text = stringResource(R.string.nav_promotions)
                )
            }

            if (isLoading && promotions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.Black)
                }
            } else if (promotions.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF7F8FA)), // Matched
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.promotion),
                            contentDescription = stringResource(R.string.promotions_empty_title),
                            modifier = Modifier.size(200.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.height(24.dp))
                        @Suppress("DEPRECATION")
                        Text(
                            text = stringResource(R.string.promotions_empty_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A),
                            fontFamily = Questv1FontFamily
                        )
                        Spacer(Modifier.height(8.dp))
                        @Suppress("DEPRECATION")
                        Text(
                            text = stringResource(R.string.promotions_empty_desc),
                            fontSize = 14.sp,
                            color = Color(0xFFAAAAAA),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            fontFamily = Questv1FontFamily
                        )
                    }
                }
            } else {
                // Promotions list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF7F8FA)) // Matched
                ) {
                    items(promotions) { promotion ->
                        PromotionCard(promotion = promotion)
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(Color(0xFFF7F8FA)) // Matched
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, locale = "ar")
@Composable
fun PromotionsScreenPreview() {
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        MafiMushkilTheme {
            PromotionsScreen(rememberNavController())
        }
    }
}
