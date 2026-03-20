package com.vardash.mafimushkil.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.location.LocationManager
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseOnMapScreen(
    navController: NavController,
    onLocationSelected: (Double, Double, String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val layoutDirection = LocalLayoutDirection.current
    var searchQuery by remember { mutableStateOf("") }
    var showDropdown by remember { mutableStateOf(false) }
    // Default location: Laghouat, Algeria
    var centerLat by remember { mutableStateOf(33.8016) }
    var centerLon by remember { mutableStateOf(2.8656) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var locationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    var searchResults by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var isSelectingFromResult by remember { mutableStateOf(false) }
    
    // To store the reverse geocoded address without showing it in the search bar
    var currentAddress by remember { mutableStateOf("") }
    
    // State for Location Disabled Sheet
    var showLocationDisabledSheet by remember { mutableStateOf(false) }

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    // 1. Search Logic using Nominatim
    LaunchedEffect(searchQuery) {
        if (searchQuery.length > 2 && !isSelectingFromResult) {
            delay(700) 
            isSearching = true
            try {
                val results = withContext(Dispatchers.IO) {
                    val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
                    val urlString = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=10&accept-language=ar"
                    
                    val url = URL(urlString)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.setRequestProperty("User-Agent", "MafiMushkilApp-Android/1.1")
                    connection.connectTimeout = 8000
                    connection.readTimeout = 8000
                    
                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val response = connection.inputStream.bufferedReader().readText()
                        val jsonArray = JSONArray(response)
                        List(jsonArray.length()) { i -> 
                            val item = jsonArray.getJSONObject(i)
                            val obj = JSONObject()
                            obj.put("display_name", item.optString("display_name", ""))
                            obj.put("lat", item.optDouble("lat", 0.0))
                            obj.put("lon", item.optDouble("lon", 0.0))
                            obj
                        }
                    } else {
                        null
                    }
                }
                if (results != null) {
                    searchResults = results
                    showDropdown = results.isNotEmpty()
                } else {
                    searchResults = emptyList()
                    showDropdown = false
                }
            } catch (e: Exception) {
                Log.e("ChooseOnMap", "Search error", e)
                searchResults = emptyList()
                showDropdown = false
            } finally {
                isSearching = false
            }
        } else {
            if (!isSelectingFromResult) {
                searchResults = emptyList()
                showDropdown = false
            }
        }
    }

    // 2. Reverse Geocoding Logic (Background update only)
    LaunchedEffect(centerLat, centerLon) {
        delay(1000) 
        try {
            val address = withContext(Dispatchers.IO) {
                val urlString = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$centerLat&lon=$centerLon&accept-language=ar"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", "MafiMushkilApp-Android/1.1")
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    JSONObject(response).optString("display_name", "")
                } else {
                    ""
                }
            }
            if (address.isNotEmpty()) {
                currentAddress = address
            }
        } catch (e: Exception) { 
            Log.e("ChooseOnMap", "Reverse geocode error", e)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            if (!isLocationEnabled(context)) {
                showLocationDisabledSheet = true
            } else {
                locationOverlay?.enableMyLocation()
            }
        } else {
            Toast.makeText(context, context.getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!isPreview) {
            Configuration.getInstance().userAgentValue = "MafiMushkilApp-Android/1.1"
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── OSM Map with Gray Filter ──────────────────────────────
        if (isPreview) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Map is not available in Preview", color = Color.Gray)
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        controller.setCenter(GeoPoint(centerLat, centerLon))

                        val matrix = ColorMatrix()
                        matrix.setSaturation(0f)
                        val array = matrix.array
                        val brightnessOffset = 45f
                        array[4] = brightnessOffset
                        array[9] = brightnessOffset
                        array[14] = brightnessOffset
                        val filter = ColorMatrixColorFilter(matrix)
                        overlayManager.tilesOverlay.setColorFilter(filter)

                        overlays.add(object : Overlay() {
                            override fun onTouchEvent(e: MotionEvent, mapView: MapView): Boolean {
                                if (e.action == MotionEvent.ACTION_MOVE) {
                                    isSelectingFromResult = false 
                                }
                                if (e.action == MotionEvent.ACTION_UP) {
                                    val center = mapView.mapCenter
                                    centerLat = center.latitude
                                    centerLon = center.longitude
                                }
                                return false
                            }
                        })

                        val overlay = object : MyLocationNewOverlay(GpsMyLocationProvider(ctx), this) {
                            override fun onResume() {
                                val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
                                canvasDrawCircle(bitmap)
                                super.onResume()
                            }
                            
                            private fun canvasDrawCircle(bitmap: Bitmap) {
                                val canvas = Canvas(bitmap)
                                val paint = Paint().apply {
                                    color = android.graphics.Color.parseColor("#4285F4")
                                    isAntiAlias = true
                                }
                                canvas.drawCircle(16f, 16f, 16f, paint)
                                setPersonIcon(bitmap)
                                setDirectionIcon(bitmap)
                            }
                        }
                        
                        val fineLocation = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                        val coarseLocation = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (fineLocation == PackageManager.PERMISSION_GRANTED || coarseLocation == PackageManager.PERMISSION_GRANTED) {
                            overlay.enableMyLocation()
                        }
                        locationOverlay = overlay
                        overlays.add(overlay)
                        
                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    view.controller.setCenter(GeoPoint(centerLat, centerLon))
                }
            )
        }

        // ── Custom Marker in Center (Matching UI) ────────────────
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFF282828), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp)
                )
            }
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(22.dp)
                    .background(Color(0xFF282828))
            )
        }

        // ── Search & Buttons Overlay ──────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Bar Pill
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (layoutDirection == LayoutDirection.Rtl) Icons.Default.Search else Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFBBBBBB),
                            modifier = Modifier.size(22.dp)
                        )
                        
                        TextField(
                            value = searchQuery,
                            onValueChange = { 
                                searchQuery = it
                                if (it.isEmpty()) isSelectingFromResult = false
                            },
                            placeholder = { Text(stringResource(R.string.search_location), color = Color(0xFFBBBBBB), fontFamily = Questv1FontFamily) },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 15.sp, fontFamily = Questv1FontFamily)
                        )
                        
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF2196F3))
                        }
                    }

                    // Dropdown Results
                    if (showDropdown && searchResults.isNotEmpty()) {
                        HorizontalDivider(color = Color(0xFFF5F5F5))
                        LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                            items(searchResults) { result ->
                                val name = result.getString("display_name")
                                val lat = result.getDouble("lat")
                                val lon = result.getDouble("lon")
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            isSelectingFromResult = true
                                            searchQuery = name
                                            centerLat = lat
                                            centerLon = lon
                                            showDropdown = false
                                        }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.LocationOn, null, tint = Color(0xFFBBBBBB), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(name, fontSize = 14.sp, color = Color(0xFF444444), maxLines = 1, fontFamily = Questv1FontFamily)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // Separate Close Button
            Surface(
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.Close, null, tint = Color.Black, modifier = Modifier.size(24.dp))
                }
            }
        }

        // ── Bottom Actions ────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.End
        ) {
            // My Location Button
            Surface(
                onClick = {
                    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    if (fine == PackageManager.PERMISSION_GRANTED) {
                        if (!isLocationEnabled(context)) {
                            showLocationDisabledSheet = true
                        } else {
                            locationOverlay?.let { overlay ->
                                overlay.enableMyLocation()
                                overlay.myLocation?.let { lastLoc ->
                                    centerLat = lastLoc.latitude
                                    centerLon = lastLoc.longitude
                                    mapView?.controller?.animateTo(lastLoc)
                                }
                            }
                        }
                    } else {
                        permissionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    }
                },
                modifier = Modifier.size(58.dp),
                shape = CircleShape,
                color = Color(0xFF2D2D2D),
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.MyLocation, null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            // Done Button
            Button(
                onClick = {
                    val finalAddress = if (searchQuery.isNotEmpty()) searchQuery 
                                     else if (currentAddress.isNotEmpty()) currentAddress
                                     else "$centerLat, $centerLon"
                    onLocationSelected(centerLat, centerLon, finalAddress)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF282828),
                    contentColor = Color.White
                )
            ) {
                @Suppress("DEPRECATION")
                Text(
                    text = stringResource(R.string.choose_map_done), 
                    fontSize = 17.sp, 
                    fontWeight = FontWeight.Bold,
                    fontFamily = Questv1FontFamily,
                    color = Color.White
                )
            }
        }

        // ── Location Disabled Bottom Sheet ───────────────────────
        if (showLocationDisabledSheet) {
            ModalBottomSheet(
                onDismissRequest = { showLocationDisabledSheet = false },
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
                            .background(Color(0xFF2196F3).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.MyLocation,
                            contentDescription = null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.location_disabled_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        textAlign = TextAlign.Center,
                        fontFamily = Questv1FontFamily
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.location_disabled_desc),
                        fontSize = 14.sp,
                        color = Color(0xFF888888),
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp,
                        fontFamily = Questv1FontFamily
                    )
                    Spacer(Modifier.height(28.dp))
                    Button(
                        onClick = {
                            showLocationDisabledSheet = false
                            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF282828),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Settings, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            @Suppress("DEPRECATION")
                            Text(
                                text = stringResource(R.string.location_enable_settings), 
                                fontSize = 16.sp, 
                                fontWeight = FontWeight.Bold,
                                fontFamily = Questv1FontFamily
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { showLocationDisabledSheet = false }) {
                        @Suppress("DEPRECATION")
                        Text(
                            text = stringResource(R.string.menu_cancel), 
                            color = Color(0xFF888888), 
                            fontWeight = FontWeight.Medium,
                            fontFamily = Questv1FontFamily
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, locale = "ar")
@Composable
fun ChooseOnMapScreenPreview() {
    MafiMushkilTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ChooseOnMapScreen(rememberNavController())
        }
    }
}
