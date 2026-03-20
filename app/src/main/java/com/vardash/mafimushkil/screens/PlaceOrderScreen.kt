package com.vardash.mafimushkil.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.auth.OrderState
import com.vardash.mafimushkil.auth.OrderViewModel
import com.vardash.mafimushkil.models.SelectedCategory
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmOrderSheet(
    orderState: OrderState,
    onPlaceOrder: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
                    .background(Color(0xFFF5C242)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = stringResource(R.string.confirm_order_sheet_title),
                    tint = Color(0xFF282828),
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.confirm_order_sheet_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                textAlign = TextAlign.Center,
                fontFamily = Questv1FontFamily
            )
            Spacer(Modifier.height(12.dp))
            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.confirm_order_sheet_desc),
                fontSize = 14.sp,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center,
                lineHeight = 21.sp,
                fontFamily = Questv1FontFamily
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onPlaceOrder,
                enabled = orderState !is OrderState.Loading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF282828),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                if (orderState is OrderState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    @Suppress("DEPRECATION")
                    Text(
                        text = stringResource(R.string.confirm_order_sheet_button),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = Questv1FontFamily
                    )
                }
            }
        }
    }
}

@Composable
fun PlaceOrderScreen(
    navController: NavController,
    categoryId: String = "",
    categoryName: String = "",
    iconName: String = "",
    orderViewModel: OrderViewModel = viewModel()
) {
    val context = LocalContext.current
    val orderState by orderViewModel.orderState.collectAsState()

    val selectedCategories = orderViewModel.pendingCategories
    val photos = orderViewModel.pendingPhotos

    // ✅ Reset form only when starting from main menus
    LaunchedEffect(categoryId) {
        if (categoryId.isNotEmpty() && categoryId != "{categoryId}") {
            orderViewModel.addInitialCategory(categoryId, categoryName, iconName)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { orderViewModel.addPhoto(it) }
    }

    val isFormFilled = selectedCategories.isNotEmpty() &&
            orderViewModel.pendingAddress.isNotEmpty() &&
            orderViewModel.pendingDetails.isNotEmpty()

    var showConfirmSheet by remember { mutableStateOf(false) }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle

    // ✅ FIXED: Individual stable keys to handle multiple additions flawlessly
    val resId by savedStateHandle?.getStateFlow<String?>("added_id", null)?.collectAsState() ?: remember { mutableStateOf(null) }
    val resName by savedStateHandle?.getStateFlow<String?>("added_name", null)?.collectAsState() ?: remember { mutableStateOf(null) }
    val resIcon by savedStateHandle?.getStateFlow<String?>("added_icon", null)?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(resId, resName, resIcon) {
        if (resId != null && resName != null && resIcon != null) {
            orderViewModel.addCategory(resId!!, resName!!, resIcon!!)
            // Clear values to avoid re-triggering on rotation/recomposition
            savedStateHandle?.set("added_id", null)
            savedStateHandle?.set("added_name", null)
            savedStateHandle?.set("added_icon", null)
        }
    }

    val addressResult by savedStateHandle
        ?.getStateFlow("address", "")
        ?.collectAsState()
        ?: remember { mutableStateOf("") }

    LaunchedEffect(addressResult) {
        if (addressResult.isNotEmpty()) {
            orderViewModel.pendingAddress = addressResult
        }
    }

    val detailsResult: String? by savedStateHandle
        ?.getStateFlow("details", "")
        ?.collectAsState()
        ?: remember { mutableStateOf<String?>(null) }

    LaunchedEffect(detailsResult) {
        val details = detailsResult
        if (!details.isNullOrEmpty()) {
            orderViewModel.pendingDetails = details
            savedStateHandle?.set("details", null) // Clear to prevent re-triggering
        }
    }

    LaunchedEffect(orderState) {
        if (orderState is OrderState.Success) {
            showConfirmSheet = false
            orderViewModel.clearPendingOrder()
            orderViewModel.resetState()
            navController.navigate("orders") {
                popUpTo("home") { inclusive = false }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FD))
    ) {
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
                IconButton(onClick = {
                    orderViewModel.clearPendingOrder()
                    navController.popBackStack()
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color(0xFF282828)
                    )
                }
                @Suppress("DEPRECATION")
                Text(
                    text = stringResource(R.string.place_order_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    fontFamily = Questv1FontFamily
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            Spacer(Modifier.height(20.dp))

            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.place_order_category),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                fontFamily = Questv1FontFamily
            )
            Spacer(Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(selectedCategories, key = { it.id }) { cat ->
                    Box(modifier = Modifier.size(115.dp)) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Image(
                                    painter = painterResource(id = getCategoryIcon(cat.iconName)),
                                    contentDescription = cat.name,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(6.dp))
                                @Suppress("DEPRECATION")
                                Text(
                                    text = getLocalizedCategoryName(cat.name),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF282828),
                                    textAlign = TextAlign.Center,
                                    fontFamily = Questv1FontFamily
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F1F1))
                                .clickable { orderViewModel.removeCategory(cat.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color(0xFF888888),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                item {
                    Box(
                        modifier = Modifier
                            .size(115.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(BorderStroke(1.dp, Color(0xFFEEEEEE)), RoundedCornerShape(12.dp))
                            .clickable {
                                navController.navigate("categories?mode=add")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add More",
                            tint = Color(0xFF282828)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.place_order_details),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                fontFamily = Questv1FontFamily
            )
            Spacer(Modifier.height(12.dp))

            Card(
                onClick = { navController.navigate("details?initialDetails=${Uri.encode(orderViewModel.pendingDetails)}") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Assignment,
                        contentDescription = null,
                        tint = Color(0xFF282828),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        @Suppress("DEPRECATION")
                        Text(
                            text = if (orderViewModel.pendingDetails.isEmpty())
                                stringResource(R.string.place_order_add_details)
                            else
                                orderViewModel.pendingDetails,
                            fontSize = 14.sp,
                            color = if (orderViewModel.pendingDetails.isEmpty()) Color(0xFF888888) else Color(0xFF282828),
                            fontFamily = Questv1FontFamily
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.place_order_address),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                fontFamily = Questv1FontFamily
            )
            Spacer(Modifier.height(12.dp))

            Card(
                onClick = { navController.navigate("select_location") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF282828),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        @Suppress("DEPRECATION")
                        Text(
                            text = if (orderViewModel.pendingAddress.isEmpty())
                                stringResource(R.string.place_order_add_address)
                            else
                                orderViewModel.pendingAddress,
                            fontSize = 14.sp,
                            color = if (orderViewModel.pendingAddress.isEmpty()) Color(0xFF888888) else Color(0xFF282828),
                            fontFamily = Questv1FontFamily
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.place_order_photo),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                fontFamily = Questv1FontFamily
            )
            Spacer(Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(photos) { uri ->
                    Box(modifier = Modifier.size(90.dp)) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, Color(0xFFEEEEEE)), RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F1F1))
                                .clickable { orderViewModel.removePhoto(photos.indexOf(uri)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color(0xFF888888),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
                item {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(BorderStroke(1.dp, Color(0xFFEEEEEE)), RoundedCornerShape(12.dp))
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Photo",
                            tint = Color(0xFF282828)
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            @Suppress("DEPRECATION")
            Button(
                onClick = { showConfirmSheet = true },
                enabled = isFormFilled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF282828),
                    disabledContainerColor = Color(0xFFE0E0E0),
                    contentColor = Color.White,
                    disabledContentColor = Color(0xFF888888)
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text(
                    text = stringResource(R.string.confirm_order_sheet_button),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Questv1FontFamily
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showConfirmSheet) {
        ConfirmOrderSheet(
            orderState = orderState,
            onPlaceOrder = {
                orderViewModel.placeOrder(
                    context,
                    selectedCategories,
                    orderViewModel.pendingAddress,
                    orderViewModel.pendingDetails,
                    photos
                )
            },
            onDismiss = { showConfirmSheet = false }
        )
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
fun PlaceOrderScreenPreview() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val navController = rememberNavController()
        PlaceOrderScreen(
            navController = navController,
            categoryId = "1",
            categoryName = "Cleaning",
            iconName = "cleaning"
        )
    }
}
