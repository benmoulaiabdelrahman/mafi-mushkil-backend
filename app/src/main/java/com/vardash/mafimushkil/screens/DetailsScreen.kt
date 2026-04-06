package com.vardash.mafimushkil.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily

@Composable
fun DetailsScreen(
    navController: NavController,
    initialDetails: String = "",
    onDetailsSaved: (String) -> Unit = {}
) {
    var detailsText by remember { mutableStateOf(initialDetails) }
    val hasContent = detailsText.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA)) // Matched
    ) {
        // ── Top bar ────────────────────────────────────────────────
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
                Text(
                    text = stringResource(R.string.details_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    modifier = Modifier.weight(1f),
                    fontFamily = Questv1FontFamily
                )
                Button(
                    onClick = {
                        onDetailsSaved(detailsText)
                        navController.previousBackStackEntry?.savedStateHandle?.set("details", detailsText)
                        navController.popBackStack()
                    },
                    enabled = hasContent,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasContent) Color(0xFF1A1A1A) else Color(0xFFAAAAAA),
                        disabledContainerColor = Color(0xFFAAAAAA)
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.save),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontFamily = Questv1FontFamily
                    )
                }
            }
        }

        // ── Body ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.add_details),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                fontFamily = Questv1FontFamily
            )

            Spacer(Modifier.height(12.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                TextField(
                    value = detailsText,
                    onValueChange = { detailsText = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.details_placeholder),
                            color = Color(0xFFAAAAAA),
                            fontSize = 14.sp,
                            fontFamily = Questv1FontFamily
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFF282828)
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, locale = "ar")
@Composable
fun DetailsScreenPreview() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MafiMushkilTheme {
            DetailsScreen(
                navController = rememberNavController(),
                initialDetails = "أحتاج إلى تنظيف شامل للمنزل، مع التركيز بشكل خاص على المطبخ والحمامات. يرجى إحضار جميع أدوات التنظيف اللازمة."
            )
        }
    }
}
