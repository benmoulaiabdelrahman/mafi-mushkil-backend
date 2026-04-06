package com.vardash.mafimushkil.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily

private fun isNetworkError(message: String): Boolean {
    val errorMsg = message.lowercase()
    return errorMsg.contains("network error") ||
        errorMsg.contains("timeout") ||
        errorMsg.contains("unreachable") ||
        errorMsg.contains("failed to connect") ||
        errorMsg.contains("host")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevokeRegistrationScreen(
    navController: NavController,
    target: String,
    profileViewModel: ProfileViewModel? = if (LocalInspectionMode.current) null else viewModel()
) {
    val context = LocalContext.current
    val profileState by (profileViewModel?.profileState?.collectAsState() ?: remember { mutableStateOf(ProfileState.Idle) })
    var showNoInternetSheet by remember { mutableStateOf(false) }

    val isCompany = target.lowercase().trim() == "company"
    val subtitle = if (isCompany) {
        stringResource(R.string.revoke_company_subtitle)
    } else {
        stringResource(R.string.revoke_worker_subtitle)
    }

    LaunchedEffect(Unit) {
        profileViewModel?.resetState()
    }

    LaunchedEffect(profileState) {
        when (profileState) {
            is ProfileState.Success -> {
                profileViewModel?.resetState()
                navController.popBackStack()
            }
            is ProfileState.Error -> {
                val errorMsg = (profileState as ProfileState.Error).message
                if (isNetworkError(errorMsg)) {
                    showNoInternetSheet = true
                }
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
                .size(40.dp),
            shape = CircleShape,
            color = Color(0xFFF4F4F4),
            tonalElevation = 0.dp
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color(0xFF1A1A1A)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8524A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = stringResource(R.string.revoke_goodbye_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1A1A1A),
                textAlign = TextAlign.Center,
                fontFamily = Questv1FontFamily
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = subtitle,
                fontSize = 15.sp,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                fontFamily = Questv1FontFamily
            )

            Spacer(Modifier.height(28.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFF8F9FD)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = stringResource(R.string.revoke_goodbye_note_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        fontFamily = Questv1FontFamily
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.revoke_goodbye_note),
                        fontSize = 13.sp,
                        color = Color(0xFF6F6F6F),
                        lineHeight = 20.sp,
                        fontFamily = Questv1FontFamily
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    if (isCompany) {
                        profileViewModel?.revokeCompanyRegistration(context)
                    } else {
                        profileViewModel?.revokeWorkerRegistration(context)
                    }
                },
                enabled = profileState !is ProfileState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE8524A),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFDD8A84),
                    disabledContentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                if (profileState is ProfileState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.revoke_button),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Questv1FontFamily
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            TextButton(onClick = { navController.popBackStack() }) {
                Text(
                    text = stringResource(R.string.revoke_cancel),
                    color = Color(0xFF8E8E8E),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Questv1FontFamily
                )
            }
        }

        if (showNoInternetSheet) {
            NoInternetSheet(
                onDismiss = { showNoInternetSheet = false },
                onTryAgain = {
                    showNoInternetSheet = false
                    if (isCompany) {
                        profileViewModel?.revokeCompanyRegistration(context)
                    } else {
                        profileViewModel?.revokeWorkerRegistration(context)
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RevokeRegistrationScreenPreview() {
    MafiMushkilTheme {
        RevokeRegistrationScreen(
            navController = rememberNavController(),
            target = "worker"
        )
    }
}
