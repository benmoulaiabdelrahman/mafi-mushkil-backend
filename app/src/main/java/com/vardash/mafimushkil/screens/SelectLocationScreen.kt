package com.vardash.mafimushkil.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.Routes
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import java.net.SocketTimeoutException

@Composable
fun SelectLocationScreen(
    navController: NavController,
    onAddressSelected: (String) -> Unit = {}
) {
    var addressQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    
    var showNoInternetSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val currentSavedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val selectedLocationFromMap by currentSavedStateHandle
        ?.getStateFlow<String?>("selected_location", null)
        ?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(selectedLocationFromMap) {
        selectedLocationFromMap?.let { address ->
            if (address.isNotEmpty()) {
                // Return the address to the previous screen (PlaceOrderScreen)
                navController.previousBackStackEntry?.savedStateHandle?.set("address", address)
                // Clean up current savedStateHandle
                currentSavedStateHandle?.set("selected_location", null)
                navController.popBackStack()
            }
        }
    }

    suspend fun performSearch(query: String) {
        if (query.length <= 2) {
            searchResults = emptyList()
            return
        }
        isSearching = true
        searchError = null
        try {
            val results = withContext(Dispatchers.IO) {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                // Using Nominatim API - more reliable for multiple languages including Arabic
                val urlString = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=10&accept-language=ar"
                
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", "MafiMushkilApp/1.0")
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonArray = JSONArray(response)
                    List(jsonArray.length()) { i -> 
                        jsonArray.getJSONObject(i).optString("display_name", "")
                    }.filter { it.isNotEmpty() }
                } else {
                    null
                }
            }
            if (results != null) {
                searchResults = results
            } else {
                searchResults = emptyList()
            }
        } catch (e: Exception) {
            Log.e("SelectLocation", "Search error", e)
            if (e is UnknownHostException || e is SocketTimeoutException || e.message?.contains("timeout") == true) {
                showNoInternetSheet = true
            }
            searchResults = emptyList()
        } finally {
            isSearching = false
        }
    }

    LaunchedEffect(addressQuery) {
        if (addressQuery.length > 2) {
            delay(700) 
            performSearch(addressQuery)
        } else {
            searchResults = emptyList()
            searchError = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
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
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color(0xFF282828)
                    )
                }
                Text(
                    text = stringResource(R.string.select_location_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF888888),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                TextField(
                    value = addressQuery,
                    onValueChange = { addressQuery = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.select_location_placeholder),
                            color = Color(0xFFAAAAAA),
                            fontSize = 15.sp
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedIndicatorColor = Color(0xFFDDDDDD),
                        focusedIndicatorColor = Color(0xFF282828),
                        cursorColor = Color(0xFF282828)
                    )
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        navController.navigate(Routes.ChooseOnMap)
                    }
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Map,
                    contentDescription = stringResource(R.string.select_location_choose_map),
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.select_location_choose_map),
                    fontSize = 15.sp,
                    color = Color(0xFF2196F3),
                    fontWeight = FontWeight.Medium
                )
            }

            if (isSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = Color(0xFF2196F3))
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (searchResults.isEmpty() && addressQuery.length > 2 && !isSearching) {
                    item {
                        Text(
                            text = stringResource(R.string.select_location_no_results),
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    }
                }
                
                itemsIndexed(searchResults) { index, result ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.previousBackStackEntry?.savedStateHandle?.set("address", result)
                                    navController.popBackStack()
                                }
                                .padding(vertical = 14.dp, horizontal = 16.dp)
                        ) {
                            Text(text = result, fontSize = 14.sp, color = Color(0xFF1A1A1A))
                        }
                    }
                }
            }
        }
    }
    
    if (showNoInternetSheet) {
        NoInternetSheet(
            onDismiss = { showNoInternetSheet = false },
            onTryAgain = {
                scope.launch { performSearch(addressQuery) }
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, locale = "ar")
@Composable
fun SelectLocationScreenPreview() {
    MafiMushkilTheme {
        SelectLocationScreen(navController = rememberNavController())
    }
}
