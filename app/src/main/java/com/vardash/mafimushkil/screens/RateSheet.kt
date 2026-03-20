package com.vardash.mafimushkil.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var selectedStars by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

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
            if (!submitted) {
                // Lime green circle with star icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFCCFD04)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFF282828),
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                @Suppress("DEPRECATION")
                Text(
                    text = stringResource(R.string.rate_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    textAlign = TextAlign.Center,
                    fontFamily = Questv1FontFamily
                )

                Spacer(Modifier.height(10.dp))

                @Suppress("DEPRECATION")
                Text(
                    text = stringResource(R.string.rate_subtitle),
                    fontSize = 14.sp,
                    color = Color(0xFF888888),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    fontFamily = Questv1FontFamily
                )

                Spacer(Modifier.height(24.dp))

                // 5 stars row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = if (star <= selectedStars) Icons.Filled.Star
                                          else Icons.Outlined.StarOutline,
                            contentDescription = "Star $star",
                            tint = if (star <= selectedStars) Color(0xFFCCFD04)
                                   else Color(0xFFDDDDDD),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable { selectedStars = star }
                        )
                    }
                }

                // Optional feedback field — only show after stars selected
                if (selectedStars > 0) {
                    Spacer(Modifier.height(20.dp))
                    TextField(
                        value = feedback,
                        onValueChange = { feedback = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)),
                        placeholder = {
                            @Suppress("DEPRECATION")
                            Text(stringResource(R.string.rate_placeholder), color = Color(0xFFAAAAAA), fontSize = 14.sp, fontFamily = Questv1FontFamily)
                        },
                        textStyle = TextStyle(fontFamily = Questv1FontFamily),
                        minLines = 3,
                        maxLines = 5,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedContainerColor = Color(0xFFF5F5F5),
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            cursorColor = Color(0xFF282828)
                        )
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Submit button
                Button(
                    onClick = {
                        // If 5 stars — open Play Store
                        if (selectedStars == 5) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=com.vardash.mafimushkil")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=com.vardash.mafimushkil")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        }
                        submitted = true
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = selectedStars > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF282828),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFAAAAAA),
                        disabledContentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    @Suppress("DEPRECATION")
                    Text(stringResource(R.string.rate_submit), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = Questv1FontFamily)
                }
            } else {
                // Success state
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                @Suppress("DEPRECATION")
                Text(
                    text = stringResource(R.string.rate_thanks),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    textAlign = TextAlign.Center,
                    fontFamily = Questv1FontFamily
                )

                Spacer(Modifier.height(10.dp))

                @Suppress("DEPRECATION")
                Text(
                    text = stringResource(R.string.rate_success_desc),
                    fontSize = 14.sp,
                    color = Color(0xFF888888),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    fontFamily = Questv1FontFamily
                )

                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = onDismiss,
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
}

@Preview(showBackground = true, locale = "ar")
@Composable
fun RateSheetArabicPreview() {
    MafiMushkilTheme {
        // We use a Box with fillMaxSize to provide a container for the ModalBottomSheet
        Box(Modifier.fillMaxSize()) {
            RateSheet(onDismiss = {})
        }
    }
}
