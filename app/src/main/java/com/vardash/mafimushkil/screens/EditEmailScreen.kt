package com.vardash.mafimushkil.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.auth.ProfileState
import com.vardash.mafimushkil.auth.ProfileViewModel
import com.vardash.mafimushkil.auth.UserProfile
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEmailScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel? = if (LocalInspectionMode.current) null else viewModel()
) {
    val profileState by (profileViewModel?.profileState?.collectAsState() ?: remember { mutableStateOf(ProfileState.Idle) })
    val userProfile by (profileViewModel?.userProfile?.collectAsState() ?: remember { mutableStateOf(UserProfile()) })
    
    var email by remember(userProfile.email) { mutableStateOf(userProfile.email) }
    var showVerifySheet by remember { mutableStateOf(false) }
    var showResendState by remember { mutableStateOf(false) }

    val isSaveEnabled = email.isNotBlank() && 
            email != userProfile.email &&
            android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
            profileState !is ProfileState.Loading

    LaunchedEffect(profileState) {
        when (profileState) {
            is ProfileState.Success -> {
                showVerifySheet = true
                profileViewModel?.resetState()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEEF0F5))
    ) {
        // ── White top bar ─────────────────────────────────
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color(0xFF282828)
                        )
                    }
                    Text(
                        text = stringResource(R.string.edit_email_title),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                }
                Button(
                    onClick = { profileViewModel?.updateEmail(email) },
                    enabled = isSaveEnabled,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF282828),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFAAAAAA),
                        disabledContentColor = Color.White
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    if (profileState is ProfileState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.save), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // ── Body ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Spacer(Modifier.height(20.dp))

            // Email input with bottom border
            TextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.edit_email_placeholder),
                        color = Color(0xFFAAAAAA),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color(0xFFDDDDDD),
                    focusedIndicatorColor = Color(0xFF282828),
                    cursorColor = Color(0xFF282828)
                )
            )

            // Resend state — shown after user dismisses verify sheet
            if (showResendState) {
                Spacer(Modifier.height(40.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.edit_email_resend_hint),
                        fontSize = 14.sp,
                        color = Color(0xFF888888),
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            profileViewModel?.updateEmail(email)
                            showResendState = false
                        },
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
                        if (profileState is ProfileState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = stringResource(R.string.edit_email_resend_button),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Verify email sheet ────────────────────────────────
    if (showVerifySheet) {
        ModalBottomSheet(
            onDismissRequest = { 
                showVerifySheet = false
                showResendState = true
                navController.popBackStack()
            },
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
                // Blue circle with envelope icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2196F3)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Email,
                        contentDescription = stringResource(R.string.profile_email),
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.edit_email_verify_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.edit_email_verify_desc),
                    fontSize = 14.sp,
                    color = Color(0xFF888888),
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp
                )

                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = {
                        showVerifySheet = false
                        showResendState = true
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF282828),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text(stringResource(R.string.ok), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, locale = "ar")
@Composable
fun EditEmailScreenPreview() {
    MafiMushkilTheme {
        EditEmailScreen(navController = rememberNavController())
    }
}
