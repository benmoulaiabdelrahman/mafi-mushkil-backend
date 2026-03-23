package com.vardash.mafimushkil.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.Routes
import com.vardash.mafimushkil.auth.ApplicationState
import com.vardash.mafimushkil.auth.ApplicationViewModel
import com.vardash.mafimushkil.auth.ProfileViewModel
import com.vardash.mafimushkil.auth.UserProfile
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily

// Data class and list moved here to be shared if needed, 
// using names that won't conflict or cause unresolved references
data class WorkerWorkType(
    val labelRes: Int,
    val iconRes: Int
)

val workerWorkTypesList = listOf(
    WorkerWorkType(R.string.cat_cleaning, R.drawable.cleaning),
    WorkerWorkType(R.string.cat_electrician, R.drawable.electrician),
    WorkerWorkType(R.string.cat_plumber, R.drawable.repairing),
    WorkerWorkType(R.string.cat_carpenter, R.drawable.carpenter),
    WorkerWorkType(R.string.cat_painter, R.drawable.painter),
    WorkerWorkType(R.string.cat_mason, R.drawable.mason),
    WorkerWorkType(R.string.cat_roofing, R.drawable.roofing),
    WorkerWorkType(R.string.cat_ac_repair, R.drawable.ac_repair),
    WorkerWorkType(R.string.cat_glazier, R.drawable.glazier),
    WorkerWorkType(R.string.cat_cook, R.drawable.cook),
    WorkerWorkType(R.string.cat_babysitter, R.drawable.babysitter),
    WorkerWorkType(R.string.cat_nurse, R.drawable.nurse),
    WorkerWorkType(R.string.cat_car_wash, R.drawable.car_wash),
    WorkerWorkType(R.string.cat_moving, R.drawable.moving),
    WorkerWorkType(R.string.cat_gardener, R.drawable.gardener),
    WorkerWorkType(R.string.cat_car_repair, R.drawable.mechanic),
    WorkerWorkType(R.string.cat_delivery, R.drawable.delivery),
    WorkerWorkType(R.string.cat_errands, R.drawable.errands)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerFormScreen(
    navController: NavController,
    applicationViewModel: ApplicationViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    val applicationState by applicationViewModel.applicationState.collectAsState()
    val userProfile by profileViewModel.userProfile.collectAsState()

    WorkerFormContent(
        navController = navController,
        applicationState = applicationState,
        userProfile = userProfile,
        onLoadProfile = { profileViewModel.loadUserProfile() },
        onSubmit = { fullName, phone, email, services, experience ->
            applicationViewModel.submitWorkerApplication(
                fullName = fullName,
                phone = phone,
                email = email,
                services = services,
                experience = experience,
                bio = ""
            )
        },
        onResetState = { applicationViewModel.resetState() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerFormContent(
    navController: NavController,
    applicationState: ApplicationState,
    userProfile: UserProfile,
    onLoadProfile: () -> Unit,
    onSubmit: (String, String, String, String, String) -> Unit,
    onResetState: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var yearsExperience by remember { mutableStateOf("") }
    var selectedWorkTypes by remember { mutableStateOf(setOf<String>()) }
    var showWorkTypeSheet by remember { mutableStateOf(false) }
    var showSuccessSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onLoadProfile()
    }

    LaunchedEffect(userProfile.name, userProfile.phone, userProfile.email, userProfile.workerExperience, userProfile.workerServices) {
        if (fullName.isBlank() && userProfile.name.isNotBlank()) fullName = userProfile.name
        if (phoneNumber.isBlank() && userProfile.phone.isNotBlank()) phoneNumber = userProfile.phone
        if (email.isBlank() && userProfile.email.isNotBlank()) email = userProfile.email
        if (yearsExperience.isBlank() && userProfile.workerExperience.isNotBlank()) yearsExperience = userProfile.workerExperience
        if (selectedWorkTypes.isEmpty() && userProfile.workerServices.isNotBlank()) {
            selectedWorkTypes = userProfile.workerServices
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet()
        }
    }

    val resolvedFullName = fullName.ifBlank { userProfile.name }
    val resolvedPhoneNumber = phoneNumber.ifBlank { userProfile.phone }
    val resolvedEmail = email.ifBlank { userProfile.email }
    val isFormValid = resolvedFullName.isNotBlank() &&
        resolvedPhoneNumber.isNotBlank() &&
        resolvedEmail.isNotBlank() &&
        yearsExperience.isNotBlank() &&
        selectedWorkTypes.isNotEmpty()

    LaunchedEffect(applicationState) {
        if (applicationState is ApplicationState.Success) {
            showSuccessSheet = true
            onResetState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FD)) // Gray background from home
    ) {
        // Top bar
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color(0xFF282828))
                }
                Text(
                    text = stringResource(R.string.menu_become_worker),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    fontFamily = Questv1FontFamily
                )
            }
        }

        // Form
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            if (userProfile.name.isBlank()) {
                WorkerFormField(label = stringResource(R.string.full_name)) {
                    WorkerTextField(value = fullName, onValueChange = { fullName = it }, hint = stringResource(R.string.full_name))
                }
                Spacer(Modifier.height(12.dp))
            }
            if (userProfile.phone.isBlank()) {
                WorkerFormField(label = stringResource(R.string.phone_number_label)) {
                    WorkerTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        hint = stringResource(R.string.phone_number_hint),
                        keyboardType = KeyboardType.Phone
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            if (userProfile.email.isBlank()) {
                WorkerFormField(label = stringResource(R.string.profile_email)) {
                    WorkerTextField(
                        value = email,
                        onValueChange = { email = it },
                        hint = stringResource(R.string.email_hint),
                        keyboardType = KeyboardType.Email
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(12.dp))
            WorkerFormField(label = stringResource(R.string.work_type)) {
                // Dropdown trigger
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White) // White card on gray background
                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                        .clickable { showWorkTypeSheet = true }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedWorkTypes.isEmpty()) stringResource(R.string.work_type_placeholder)
                                   else selectedWorkTypes.joinToString(", "),
                            color = if (selectedWorkTypes.isEmpty()) Color(0xFFAAAAAA) else Color(0xFF282828),
                            fontSize = 14.sp,
                            maxLines = 1,
                            fontFamily = Questv1FontFamily
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFFAAAAAA)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            WorkerFormField(label = stringResource(R.string.years_of_experience)) {
                WorkerTextField(
                    value = yearsExperience,
                    onValueChange = { yearsExperience = it },
                    hint = stringResource(R.string.years_of_experience_hint),
                    keyboardType = KeyboardType.Number
                )
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    onSubmit(
                        resolvedFullName,
                        resolvedPhoneNumber,
                        resolvedEmail,
                        selectedWorkTypes.joinToString(", "),
                        yearsExperience
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = isFormValid && applicationState !is ApplicationState.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF282828),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFAAAAAA),
                    disabledContentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                if (applicationState is ApplicationState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text(
                        text = stringResource(R.string.submit),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Questv1FontFamily
                    )
                }
            }

            if (applicationState is ApplicationState.Error) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = (applicationState as ApplicationState.Error).message,
                    color = Color.Red,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontFamily = Questv1FontFamily
                )
            }
        }
    }

    // Work type selection sheet
    if (showWorkTypeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWorkTypeSheet = false },
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
                    .heightIn(max = 560.dp)
                    .padding(horizontal = 24.dp)
                    .padding(top = 28.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.select_work_type),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontFamily = Questv1FontFamily
                )
                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(workerWorkTypesList) { workType ->
                        val typeName = stringResource(workType.labelRes)
                        val isSelected = selectedWorkTypes.contains(typeName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedWorkTypes = if (isSelected)
                                        selectedWorkTypes - typeName
                                    else
                                        selectedWorkTypes + typeName
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(workType.iconRes),
                                    contentDescription = typeName,
                                    modifier = Modifier.size(36.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = typeName,
                                    fontSize = 15.sp,
                                    color = Color(0xFF282828),
                                    fontFamily = Questv1FontFamily
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Color(0xFF282828) else Color.Transparent
                                    )
                                    .border(
                                        1.5.dp,
                                        if (isSelected) Color(0xFF282828) else Color(0xFFCCCCCC),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { showWorkTypeSheet = false },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
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

    // Success sheet
    if (showSuccessSheet) {
        ModalBottomSheet(
            onDismissRequest = { /* must tap OK */ },
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
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(44.dp))
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
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Home) { inclusive = false }
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

@Composable
fun WorkerFormField(label: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF282828),
            fontWeight = FontWeight.Medium,
            fontFamily = Questv1FontFamily
        )
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
fun WorkerTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp)),
        placeholder = { Text(hint, color = Color(0xFFAAAAAA), fontSize = 14.sp, fontFamily = Questv1FontFamily) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = TextStyle(fontFamily = Questv1FontFamily),
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            cursorColor = Color(0xFF282828)
        ),
        // Add border to white textfield on gray background
        shape = RoundedCornerShape(8.dp)
    )
}

@Preview(showBackground = true, showSystemUi = true, locale = "ar")
@Composable
fun WorkerFormScreenArabicPreview() {
    MafiMushkilTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            WorkerFormContent(
                navController = rememberNavController(),
                applicationState = ApplicationState.Idle,
                userProfile = UserProfile(),
                onLoadProfile = {},
                onSubmit = { _, _, _, _, _ -> },
                onResetState = {}
            )
        }
    }
}
