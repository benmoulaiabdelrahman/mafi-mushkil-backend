package com.vardash.mafimushkil.screens

import android.content.Context
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
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.vardash.mafimushkil.models.ServiceCategoryOption
import com.vardash.mafimushkil.models.serviceCategoryOptions
import com.vardash.mafimushkil.ui.theme.Accent
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily

private fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
    return when {
        activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> true
        activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        else -> false
    }
}

private fun isNetworkError(message: String): Boolean {
    val errorMsg = message.lowercase()
    return errorMsg.contains("network error") ||
        errorMsg.contains("timeout") ||
        errorMsg.contains("unreachable") ||
        errorMsg.contains("failed to connect") ||
        errorMsg.contains("host")
}

private fun matchesServiceCategory(raw: String, option: ServiceCategoryOption, context: Context): Boolean {
    val key = raw.lowercase().trim()
    val localized = context.getString(option.labelResId).lowercase().trim()
    return key == option.category.id.lowercase().trim() ||
        key == option.category.name.lowercase().trim() ||
        key == option.category.iconName.lowercase().trim() ||
        key == localized
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyFormScreen(
    navController: NavController,
    applicationViewModel: ApplicationViewModel = viewModel()
) {
    val applicationState by applicationViewModel.applicationState.collectAsState()
    val context = LocalContext.current

    // Using rememberSaveable to ensure data survives navigation back/forth
    var companyName by rememberSaveable { mutableStateOf("") }
    var supervisorName by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    // sets/collections need a custom saver for rememberSaveable, or keep as simple string
    var selectedWorkerTypesString by rememberSaveable { mutableStateOf("") }
    
    val selectedWorkerTypes = remember(selectedWorkerTypesString) {
        if (selectedWorkerTypesString.isEmpty()) emptySet<String>() 
        else selectedWorkerTypesString.split("|").toSet()
    }

    var showWorkerTypeSheet by remember { mutableStateOf(false) }
    var showSuccessSheet by remember { mutableStateOf(false) }
    var showNoInternetSheet by remember { mutableStateOf(false) }

    // Listen for address from SelectLocationScreen
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val selectedAddress by savedStateHandle?.getStateFlow<String>("address", "")?.collectAsState() ?: remember { mutableStateOf("") }

    LaunchedEffect(selectedAddress) {
        if (selectedAddress.isNotEmpty()) {
            address = selectedAddress
            savedStateHandle?.remove<String>("address")
        }
    }

    val isFormValid = companyName.isNotBlank() &&
            supervisorName.isNotBlank() &&
            phoneNumber.isNotBlank() &&
            email.isNotBlank() &&
            address.isNotBlank() &&
            selectedWorkerTypes.isNotEmpty()

    val selectedWorkerTypesLabel = selectedWorkerTypes.joinToString(", ") { selectedName ->
        serviceCategoryOptions.firstOrNull { option -> option.category.name == selectedName }
            ?.let { option -> context.getString(option.labelResId) }
            ?: selectedName
    }

    LaunchedEffect(applicationState) {
        if (applicationState is ApplicationState.Success) {
            showSuccessSheet = true
            applicationViewModel.resetState()
        } else if (applicationState is ApplicationState.Error) {
            if (isNetworkError((applicationState as ApplicationState.Error).message)) {
                showNoInternetSheet = true
            }
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
                    text = stringResource(R.string.menu_register_company),
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
            CompanyFormField(label = stringResource(R.string.company_name)) {
                CompanyTextField(value = companyName, onValueChange = { companyName = it }, hint = stringResource(R.string.company_name_hint))
            }
            Spacer(Modifier.height(12.dp))
            CompanyFormField(label = stringResource(R.string.supervisor_name)) {
                CompanyTextField(value = supervisorName, onValueChange = { supervisorName = it }, hint = stringResource(R.string.supervisor_name_hint))
            }
            Spacer(Modifier.height(12.dp))
            CompanyFormField(label = stringResource(R.string.phone_number_label)) {
                CompanyTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    hint = stringResource(R.string.phone_number_hint),
                    keyboardType = KeyboardType.Phone
                )
            }
            Spacer(Modifier.height(12.dp))
            CompanyFormField(label = stringResource(R.string.profile_email)) {
                CompanyTextField(
                    value = email,
                    onValueChange = { email = it },
                    hint = stringResource(R.string.email_hint),
                    keyboardType = KeyboardType.Email
                )
            }
            Spacer(Modifier.height(12.dp))
            CompanyFormField(label = stringResource(R.string.worker_type)) {
                // Dropdown trigger
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White) // White card on gray background
                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                        .clickable { showWorkerTypeSheet = true }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedWorkerTypes.isEmpty()) stringResource(R.string.worker_type)
                            else selectedWorkerTypesLabel,
                            color = if (selectedWorkerTypes.isEmpty()) Color(0xFFAAAAAA) else Color(0xFF282828),
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
            CompanyFormField(label = stringResource(R.string.address)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White) // White card on gray background
                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                        .clickable { navController.navigate(Routes.SelectLocation) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (address.isEmpty()) stringResource(R.string.address_hint) else address,
                            color = if (address.isEmpty()) Color(0xFFAAAAAA) else Color(0xFF282828),
                            fontSize = 14.sp,
                            maxLines = 1,
                            fontFamily = Questv1FontFamily
                        )
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFAAAAAA),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    if (isNetworkAvailable(context)) {
                        applicationViewModel.submitCompanyApplication(
                            context = context,
                            companyName = companyName,
                            ownerName = supervisorName,
                            phone = phoneNumber,
                            email = email,
                            city = address,
                            serviceType = selectedWorkerTypes.joinToString(", "),
                            registrationNumber = "",
                            description = ""
                        )
                    } else {
                        showNoInternetSheet = true
                    }
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
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Accent)
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
                val errorMessage = (applicationState as ApplicationState.Error).message
                if (!isNetworkError(errorMessage)) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        fontFamily = Questv1FontFamily
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showWorkerTypeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWorkerTypeSheet = false },
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
                    items(serviceCategoryOptions) { type ->
                        val typeName = stringResource(type.labelResId)
                        val isSelected = selectedWorkerTypes.contains(type.category.name)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newSet = if (isSelected)
                                        selectedWorkerTypes - type.category.name
                                    else
                                        selectedWorkerTypes + type.category.name
                                    selectedWorkerTypesString = newSet.joinToString("|")
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(getCategoryIcon(type.category.iconName)),
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
                    onClick = { showWorkerTypeSheet = false },
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

    if (showSuccessSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSuccessSheet = false },
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
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }
                Spacer(Modifier.height(24.dp))
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
                    text = stringResource(R.string.company_registration_success_desc),
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

    if (showNoInternetSheet) {
        NoInternetSheet(
            onDismiss = { showNoInternetSheet = false },
            onTryAgain = {
                applicationViewModel.submitCompanyApplication(
                    context = context,
                    companyName = companyName,
                    ownerName = supervisorName,
                    phone = phoneNumber,
                    email = email,
                    city = address,
                    serviceType = selectedWorkerTypes.joinToString(", "),
                    registrationNumber = "",
                    description = ""
                )
            }
        )
    }
}

@Composable
fun CompanyFormField(label: String, content: @Composable () -> Unit) {
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
fun CompanyTextField(
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
        shape = RoundedCornerShape(8.dp)
    )
}

@Preview(showBackground = true, showSystemUi = true, locale = "ar")
@Composable
fun CompanyFormScreenArabicPreview() {
    MafiMushkilTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            CompanyFormScreen(navController = rememberNavController())
        }
    }
}
