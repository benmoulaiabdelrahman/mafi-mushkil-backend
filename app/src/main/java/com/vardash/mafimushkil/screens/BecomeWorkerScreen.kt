package com.vardash.mafimushkil.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.navigation.compose.rememberNavController
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BecomeWorkerScreen(navController: NavController) {
    // Check if we should show the success sheet (passed back from the form)
    val showSuccess = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.get<Boolean>("showSuccess") ?: false
    
    var showSuccessSheet by remember { mutableStateOf(showSuccess) }

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            showSuccessSheet = true
            navController.currentBackStackEntry?.savedStateHandle?.remove<Boolean>("showSuccess")
        }
    }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp
    val imageHeight = maxOf((screenHeight * 0.40f).toInt(), 250).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Worker photo top section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.worker_photo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Bottom content section (scrollable)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding()
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.become_worker_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1A1A1A),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 26.sp,
                    fontFamily = Questv1FontFamily
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.become_worker_subtitle),
                    fontSize = 14.sp,
                    color = Color(0xFF888888),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontFamily = Questv1FontFamily
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Feature rows with icons
                WorkerFeatureRow(
                    iconRes = R.drawable.clock,
                    title = stringResource(R.string.worker_feature1_title),
                    description = stringResource(R.string.worker_feature1_desc)
                )
                Spacer(modifier = Modifier.height(16.dp))
                WorkerFeatureRow(
                    iconRes = R.drawable.certificate,
                    title = stringResource(R.string.worker_feature2_title),
                    description = stringResource(R.string.worker_feature2_desc)
                )
                Spacer(modifier = Modifier.height(16.dp))
                WorkerFeatureRow(
                    iconRes = R.drawable.wallet,
                    title = stringResource(R.string.worker_feature3_title),
                    description = stringResource(R.string.worker_feature3_desc)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Register Now button
                Button(
                    onClick = { navController.navigate("worker_form") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF262626),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text(
                        text = stringResource(R.string.register_now),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Questv1FontFamily
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.need_help),
                    fontSize = 14.sp,
                    color = Color(0xFF888888),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("contact_us") },
                    fontFamily = Questv1FontFamily
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Close button overlay
        Surface(
            modifier = Modifier
                .align(if (LocalLayoutDirection.current == LayoutDirection.Rtl) Alignment.TopStart else Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .size(36.dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Success bottom sheet
        if (showSuccessSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSuccessSheet = false },
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
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.registration_submitted),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        textAlign = TextAlign.Center,
                        fontFamily = Questv1FontFamily
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.worker_registration_success_desc),
                        fontSize = 14.sp,
                        color = Color(0xFF888888),
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp,
                        fontFamily = Questv1FontFamily
                    )
                    Spacer(Modifier.height(28.dp))
                    Button(
                        onClick = {
                            showSuccessSheet = false
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = false }
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
                        Text(
                            text = stringResource(R.string.ok),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Questv1FontFamily
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkerFeatureRow(iconRes: Int, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = title,
            modifier = Modifier.size(32.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                fontFamily = Questv1FontFamily
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = Color(0xFF888888),
                lineHeight = 18.sp,
                fontFamily = Questv1FontFamily
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, locale = "ar")
@Composable
fun BecomeWorkerScreenArabicPreview() {
    MafiMushkilTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            BecomeWorkerScreen(navController = rememberNavController())
        }
    }
}
