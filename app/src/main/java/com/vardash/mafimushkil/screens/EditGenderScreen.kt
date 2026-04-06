package com.vardash.mafimushkil.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.vardash.mafimushkil.auth.UserProfile
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGenderScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel? = if (androidx.compose.ui.platform.LocalInspectionMode.current) null else viewModel()
) {
    val context = LocalContext.current
    val profileState by (profileViewModel?.profileState?.collectAsState() ?: remember { mutableStateOf(ProfileState.Idle) })
    val userProfile by (profileViewModel?.userProfile?.collectAsState() ?: remember { mutableStateOf(UserProfile()) })
    
    var selectedGender by remember(userProfile.gender) { mutableStateOf(userProfile.gender) }
    var showSuccessSheet by remember { mutableStateOf(false) }
    var showNoInternetSheet by remember { mutableStateOf(false) }

    val isSaveEnabled = selectedGender.isNotBlank() && 
            selectedGender != userProfile.gender &&
            profileState !is ProfileState.Loading

    LaunchedEffect(profileState) {
        when (profileState) {
            is ProfileState.Success -> {
                showSuccessSheet = true
                profileViewModel?.resetState()
            }
            is ProfileState.Error -> {
                val errorMsg = (profileState as ProfileState.Error).message
                if (errorMsg.contains("network error", ignoreCase = true)) {
                    showNoInternetSheet = true
                }
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
                        text = stringResource(R.string.edit_gender_title),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        fontFamily = Questv1FontFamily
                    )
                }
                Button(
                    onClick = { profileViewModel?.updateGender(selectedGender, context) },
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
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFFCCFD04),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.save),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = Questv1FontFamily
                        )
                    }
                }
            }
        }

        // ── Body — Radio options ──────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Spacer(Modifier.height(24.dp))

            // Male option
            GenderOption(
                label = stringResource(R.string.edit_gender_male),
                selected = selectedGender == "Male",
                onClick = { selectedGender = "Male" }
            )

            Spacer(Modifier.height(8.dp))

            // Female option
            GenderOption(
                label = stringResource(R.string.edit_gender_female),
                selected = selectedGender == "Female",
                onClick = { selectedGender = "Female" }
            )
        }
    }

    // ── Success bottom sheet ──────────────────────────────
    if (showSuccessSheet) {
        ModalBottomSheet(
            onDismissRequest = { 
                showSuccessSheet = false
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
                // Cyan circle with person icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF29B6F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.AccessibilityNew,
                        contentDescription = stringResource(R.string.edit_gender_title),
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.edit_gender_success),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    textAlign = TextAlign.Center,
                    fontFamily = Questv1FontFamily
                )

                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = {
                        showSuccessSheet = false
                        navController.popBackStack()
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

    // ── No Internet Sheet ─────────────────────────────────
    if (showNoInternetSheet) {
        NoInternetSheet(
            onDismiss = { showNoInternetSheet = false },
            onTryAgain = {
                showNoInternetSheet = false
                profileViewModel?.updateGender(selectedGender, context)
            }
        )
    }
}

@Composable
fun GenderOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null, // Row handles the click
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFF282828),
                unselectedColor = Color(0xFFAAAAAA)
            )
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 16.sp,
            color = Color(0xFF888888),
            fontFamily = Questv1FontFamily
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, locale = "ar")
@Composable
fun EditGenderScreenPreview() {
    MafiMushkilTheme {
        EditGenderScreen(navController = rememberNavController())
    }
}
