package com.vardash.mafimushkil.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.Routes
import com.vardash.mafimushkil.auth.AuthViewModel
import com.vardash.mafimushkil.auth.OrderViewModel
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    orderViewModel: OrderViewModel = viewModel()
) {
    val focusManager = LocalFocusManager.current
    var isMenuVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val categories by orderViewModel.categories.collectAsState()
    val isLoading by orderViewModel.isLoading.collectAsState()
    val error by orderViewModel.error.collectAsState()

    val filteredCategories = remember(searchQuery, categories) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            categories
        } else {
            categories.filter {
                categorySearchLabel(it.name).contains(query, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(Unit) {
        orderViewModel.loadCategories()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        Scaffold(
            bottomBar = {
                AppBottomBar(navController = navController, selectedIndex = 0)
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
                    .background(Color(0xFFF8F9FD))
            ) {
                // ── Header Section ──────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color(0xFFCCFD04))
                            .statusBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.app_logo),
                                    contentDescription = stringResource(R.string.app_logo_desc),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.mafi_mushkil),
                                    color = Color(0xFF282828),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 28.sp,
                                    fontFamily = Questv1FontFamily
                                )
                            }
                            IconButton(onClick = { isMenuVisible = true }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = stringResource(R.string.menu_desc),
                                    tint = Color(0xFF282828),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(56.dp)
                            .align(Alignment.BottomCenter)
                            .offset(y = 28.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFFAAAAAA),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    @Suppress("DEPRECATION")
                                    Text(
                                        text = stringResource(R.string.home_search_placeholder),
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 16.sp,
                                        fontFamily = Questv1FontFamily
                                    )
                                }
                                androidx.compose.foundation.text.BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = Color.Black,
                                        fontFamily = Questv1FontFamily
                                    ),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(44.dp))

                @Suppress("DEPRECATION")
                Text(
                    text = stringResource(R.string.home_services_title),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color.Black,
                    fontFamily = Questv1FontFamily
                )

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.Black)
                    }
                } else if (error != null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: $error", color = Color.Red, fontFamily = Questv1FontFamily)
                    }
                } else {
                    // Limit to 3 rows (8 items + "More" card = 9 items total)
                    val displayList = filteredCategories.take(8)
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .weight(1f),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        userScrollEnabled = true 
                    ) {
                        items(displayList) { category ->
                            CategoryCard(
                                name = getLocalizedCategoryName(category.name),
                                iconRes = getCategoryIcon(category.iconName),
                                onClick = {
                                    focusManager.clearFocus()
                                    // ✅ Encoding parameters to prevent navigation crashes
                                    val encId = Uri.encode(category.id)
                                    val encName = Uri.encode(category.name)
                                    val encIcon = Uri.encode(category.iconName)
                                    navController.navigate("place_order?categoryId=$encId&categoryName=$encName&iconName=$encIcon")
                                }
                            )
                        }
                        
                        if (searchQuery.isEmpty()) {
                            item {
                                CategoryCard(
                                    name = stringResource(R.string.home_more),
                                    iconRes = R.drawable.more,
                                    isMore = true,
                                    onClick = { 
                                        focusManager.clearFocus()
                                    navController.navigate(Routes.categories())
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        MenuDrawer(
            isVisible = isMenuVisible,
            onClose = { isMenuVisible = false },
            navController = navController,
            authViewModel = authViewModel
        )
    }
}

private fun categorySearchLabel(name: String): String {
    val key = name.trim().lowercase()
    return when {
        key == "cleaning" -> "تنظيف"
        key == "electrician" -> "كهربائي"
        key == "plumber" || key == "repairing" -> "سباك"
        key == "carpenter" -> "نجار"
        key == "painter" -> "دهان"
        key == "mason" || key == "contractor" -> "بناء / مقاول"
        key == "roofing" || key == "waterproofing" -> "سقف / عزل"
        key.contains("ac") || key.contains("air condition") -> "تكييف وتبريد"
        key.contains("glazier") || key.contains("glass") -> "زجاج"
        key == "cook" || key == "chef" -> "طباخ"
        key == "babysitter" || key == "nanny" -> "مربية"
        key.contains("nurse") -> "ممرض منزلي"
        key.contains("car wash") || key.contains("car_wash") -> "غسيل سيارات"
        key.contains("moving") || key.contains("furniture") -> "نقل اثاث"
        key == "gardener" -> "بستاني"
        key.contains("mechanic") || (key.contains("car") && key.contains("repair")) -> "إصلاح سيارات"
        key.contains("delivery") -> "توصيل"
        key.contains("errand") -> "قضاء حوائج"
        else -> name
    }
}

@Composable
fun CategoryCard(name: String, iconRes: Int, isMore: Boolean = false, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = name,
                modifier = Modifier.size(if (isMore) 26.dp else 42.dp)
            )
            Spacer(Modifier.height(10.dp))
            @Suppress("DEPRECATION")
            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontFamily = Questv1FontFamily,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, locale = "ar")
@Composable
fun HomeScreenPreview() {
    MafiMushkilTheme {
        HomeScreen(rememberNavController())
    }
}
