package com.vardash.mafimushkil.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
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
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.auth.OrderViewModel
import com.vardash.mafimushkil.models.Category
import com.vardash.mafimushkil.models.defaultServiceCategories
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily

@Composable
fun CategoriesScreen(
    navController: NavController,
    isAdding: Boolean = false,
    orderViewModel: OrderViewModel = viewModel()
) {
    val context = LocalContext.current
    val categories by orderViewModel.categories.collectAsState()
    val isLoading by orderViewModel.isLoading.collectAsState()
    val isInspectionMode = LocalInspectionMode.current

    LaunchedEffect(Unit) {
        if (!isInspectionMode) {
            orderViewModel.loadCategories(context)
        }
    }

    CategoriesScreenContent(
        navController = navController,
        isAdding = isAdding,
        categories = categories,
        isLoading = isLoading,
        onCategoryClick = { category ->
            if (isAdding) {
                // ✅ Mode: ADDING - use individual stable strings to return result
                navController.previousBackStackEntry?.savedStateHandle?.let { handle ->
                    handle["added_id"] = category.id
                    handle["added_name"] = category.name
                    handle["added_icon"] = category.iconName
                }
                navController.popBackStack()
            } else {
                // ✅ Mode: STARTING FRESH - navigate with encoding
                val encId = Uri.encode(category.id)
                val encName = Uri.encode(category.name)
                val encIcon = Uri.encode(category.iconName)
                navController.navigate("place_order?categoryId=$encId&categoryName=$encName&iconName=$encIcon")
            }
        }
    )
}

@Composable
fun CategoriesScreenContent(
    navController: NavController,
    isAdding: Boolean,
    categories: List<Category>,
    isLoading: Boolean = false,
    onCategoryClick: (Category) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FD)) 
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color(0xFF282828),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.categories_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    fontFamily = Questv1FontFamily
                )
            }
        }

        if (isLoading && categories.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.Black)
            }
        } else {
            // ── Categories grid ───────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(categories) { category ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onCategoryClick(category) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = getCategoryIcon(category.iconName)),
                                contentDescription = category.name,
                                modifier = Modifier.size(42.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(Modifier.height(8.dp))
                            @Suppress("DEPRECATION")
                            Text(
                                text = getLocalizedCategoryName(category.name),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF282828),
                                textAlign = TextAlign.Center,
                                fontFamily = Questv1FontFamily,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, locale = "ar")
@Composable
fun CategoriesScreenPreview() {
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        MafiMushkilTheme {
            CategoriesScreenContent(
                navController = rememberNavController(),
                isAdding = false,
                categories = defaultServiceCategories,
                onCategoryClick = {}
            )
        }
    }
}
