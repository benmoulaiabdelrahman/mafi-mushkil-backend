package com.vardash.mafimushkil.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.Context
import android.net.Uri
import android.content.pm.PackageManager
import android.provider.Settings
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.Routes
import com.vardash.mafimushkil.auth.OrderViewModel
import com.vardash.mafimushkil.auth.ProfileViewModel
import com.vardash.mafimushkil.models.Order
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceOrderDetailScreen(
    navController: NavController,
    orderId: String,
    orderViewModel: OrderViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val selectedOrder by orderViewModel.selectedOrder.collectAsState()
    val serviceOrders by orderViewModel.serviceAllOrders.collectAsState()

    LaunchedEffect(orderId) {
        orderViewModel.observeOrder(orderId)
        orderViewModel.loadServiceOrders(context)
    }

    DisposableEffect(orderId) {
        onDispose { orderViewModel.clearObservedOrder() }
    }

    val order = selectedOrder ?: serviceOrders.find { it.orderId == orderId }
    var customerPhone by remember { mutableStateOf("") }
    var showContactSheet by remember { mutableStateOf(false) }

    LaunchedEffect(order?.userId) {
        val userId = order?.userId?.trim().orEmpty()
        if (userId.isNotBlank()) {
            customerPhone = runCatching {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .get()
                    .await()
                    .let { if (it.exists()) it.getString("phone").orEmpty() else "" }
            }.getOrDefault("")
        } else {
            customerPhone = ""
        }
    }

    if (order == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.Black)
        }
        return
    }

    Scaffold(
        containerColor = Color(0xFFF7F8FA),
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF7F8FA))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                        Text(
                            text = stringResource(R.string.order_detail_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            fontFamily = Questv1FontFamily
                        )
                        IconButton(
                            onClick = { showContactSheet = true },
                            modifier = Modifier
                                .border(1.2.dp, Color.Black, CircleShape)
                                .size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Phone,
                                contentDescription = stringResource(R.string.order_detail_contact),
                                modifier = Modifier.size(18.dp),
                                tint = Color.Black
                            )
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF5F5F5))
                }
            }

            if (showContactSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showContactSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = Color.White,
                    dragHandle = null,
                    scrimColor = Color.Black.copy(alpha = 0.5f),
                    windowInsets = WindowInsets(0),
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 40.dp, top = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .background(Color(0xFFFFD54F), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Call,
                                contentDescription = null,
                                modifier = Modifier.size(45.dp),
                                tint = Color.Black
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.order_detail_call_client_title),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A),
                            fontFamily = Questv1FontFamily
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (customerPhone.isBlank()) {
                                stringResource(R.string.order_detail_call_client_unavailable)
                            } else {
                                stringResource(R.string.order_detail_call_client_desc)
                            },
                            fontSize = 15.sp,
                            color = Color(0xFF888888),
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            fontFamily = Questv1FontFamily
                        )
                        Spacer(Modifier.height(8.dp))
                        if (customerPhone.isNotBlank()) {
                            CompositionLocalProvider(LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                                Text(
                                    text = customerPhone,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1A1A),
                                    fontFamily = Questv1FontFamily
                                )
                            }
                        }
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = {
                                customerPhone.takeIf { it.isNotBlank() }?.let { phone ->
                                    runCatching {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:${phone.trim()}")
                                            if (context !is Activity) {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                        }
                                        context.startActivity(intent)
                                        showContactSheet = false
                                    }
                                }
                            },
                            enabled = customerPhone.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282828)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.order_detail_call_client_button),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                fontFamily = Questv1FontFamily
                            )
                        }
                        Spacer(Modifier.height(48.dp))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                DetailSection {
                    Text(
                        text = stringResource(R.string.order_detail_address),
                        color = Color(0xFF888888),
                        fontSize = 14.sp,
                        fontFamily = Questv1FontFamily
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = order.address,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        fontFamily = Questv1FontFamily
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { navController.navigate(Routes.serviceOrderMap(orderId)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF282828),
                            contentColor = Color.White
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.LocationOn,
                                null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.services_locate_on_map),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = Questv1FontFamily
                            )
                        }
                    }
                }

                DetailSection {
                    Text(
                        text = stringResource(R.string.order_detail_details),
                        color = Color(0xFF888888),
                        fontSize = 14.sp,
                        fontFamily = Questv1FontFamily
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = order.details.ifBlank { stringResource(R.string.services_no_description) },
                        fontSize = 14.sp,
                        color = Color(0xFF1A1A1A),
                        lineHeight = 20.sp,
                        fontFamily = Questv1FontFamily
                    )
                }

                if (order.photoUrls.isNotEmpty()) {
                    DetailSection {
                        Text(
                            text = stringResource(R.string.order_detail_attachments),
                            color = Color(0xFF888888),
                            fontSize = 14.sp,
                            fontFamily = Questv1FontFamily
                        )
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(order.photoUrls) { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(88.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0xFFEAEAEA), RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceOrderMapScreen(
    navController: NavController,
    orderId: String,
    orderViewModel: OrderViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectedOrder by orderViewModel.selectedOrder.collectAsState()
    val serviceOrders by orderViewModel.serviceAllOrders.collectAsState()

    var showLocationDisabledSheet by remember { mutableStateOf(false) }
    var showNoInternetSheet by remember { mutableStateOf(false) }
    var orderAddressPoint by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var servicePoint by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var currentPoint by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var distanceMeters by remember { mutableStateOf<Double?>(null) }
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var locationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var locationPermissionGranted by remember { mutableStateOf(false) }

    val order = selectedOrder ?: serviceOrders.find { it.orderId == orderId }
    val isLocationEnabled = {
        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }

    LaunchedEffect(orderId) {
        orderViewModel.observeOrder(orderId)
        orderViewModel.loadServiceOrders(context)
    }

    DisposableEffect(orderId) {
        onDispose { orderViewModel.clearObservedOrder() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionGranted = permissions.values.any { it }
        if (locationPermissionGranted) {
            if (!isLocationEnabled()) {
                showLocationDisabledSheet = true
            } else {
                locationOverlay?.enableMyLocation()
            }
        }
    }

    fun ensureLocationPermission() {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        locationPermissionGranted = fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
        if (!locationPermissionGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else if (isLocationEnabled()) {
            locationOverlay?.enableMyLocation()
        }
    }

    suspend fun geocodeAddress(address: String): Pair<Double, Double>? {
        return try {
            withContext(Dispatchers.IO) {
                val urlString = "https://nominatim.openstreetmap.org/search?q=${Uri.encode(address)}&format=json&limit=1&accept-language=ar"
                val connection = URL(urlString).openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", "MafiMushkilApp-Android/1.1")
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val first = JSONArray(response).optJSONObject(0)
                    if (first == null) return@withContext null
                    first.optString("lat").toDoubleOrNull()?.let { lat ->
                        first.optString("lon").toDoubleOrNull()?.let { lon ->
                            lat to lon
                        }
                    }
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    LaunchedEffect(order?.address) {
        val address = order?.address?.trim().orEmpty()
        if (address.isNotBlank()) {
            orderAddressPoint = geocodeAddress(address)
            if (servicePoint == null) {
                servicePoint = orderAddressPoint
            }
            if (orderAddressPoint == null) {
                showNoInternetSheet = true
            }
        }
    }

    LaunchedEffect(locationOverlay) {
        while (true) {
            val loc = locationOverlay?.myLocation
            if (loc != null) {
                currentPoint = loc.latitude to loc.longitude
                mapView?.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude))
                break
            }
            delay(1000)
        }
    }

    LaunchedEffect(servicePoint, currentPoint) {
        if (servicePoint != null && currentPoint != null) {
            val route = fetchRoadRoute(
                start = currentPoint!!,
                end = servicePoint!!
            )
            if (route != null) {
                routePoints = route.points
                distanceMeters = route.distanceMeters
            } else {
                routePoints = emptyList()
                val results = FloatArray(1)
                android.location.Location.distanceBetween(
                    currentPoint!!.first,
                    currentPoint!!.second,
                    servicePoint!!.first,
                    servicePoint!!.second,
                    results
                )
                distanceMeters = results.firstOrNull()?.toDouble()
            }
        }
    }

    LaunchedEffect(Unit) {
        ensureLocationPermission()
        if (!isLocationEnabled()) {
            showLocationDisabledSheet = true
        }
        Configuration.getInstance().userAgentValue = "MafiMushkilApp-Android/1.1"
    }

    Scaffold(
        containerColor = Color(0xFFF7F8FA),
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                order == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.Black)
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MapRouteView(
                            order = order,
                            orderAddressPoint = orderAddressPoint,
                            servicePoint = servicePoint,
                            currentPoint = currentPoint,
                            routePoints = routePoints,
                            onServicePointMoved = { movedPoint ->
                                servicePoint = movedPoint
                            },
                            mapViewHolder = { mapView = it },
                            locationOverlayHolder = { locationOverlay = it }
                        )

                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .statusBarsPadding()
                                .padding(16.dp)
                                .size(44.dp),
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 6.dp
                        ) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                    tint = Color.Black
                                )
                            }
                        }

                    }
                }
            }
        }
    }

    if (showLocationDisabledSheet) {
        LocationDisabledSheet(
            onDismiss = { showLocationDisabledSheet = false },
            onOpenSettings = {
                showLocationDisabledSheet = false
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        )
    }

    if (showNoInternetSheet) {
        NoInternetSheet(
            onDismiss = { showNoInternetSheet = false },
            onTryAgain = {
                showNoInternetSheet = false
                scope.launch {
                    orderAddressPoint = order?.address?.let { geocodeAddress(it) }
                }
            }
        )
    }
}

@Composable
private fun MapRouteView(
    order: Order,
    orderAddressPoint: Pair<Double, Double>?,
    servicePoint: Pair<Double, Double>?,
    currentPoint: Pair<Double, Double>?,
    routePoints: List<GeoPoint>,
    onServicePointMoved: (Pair<Double, Double>) -> Unit,
    mapViewHolder: (MapView?) -> Unit,
    locationOverlayHolder: (MyLocationNewOverlay?) -> Unit
) {
    var serviceMarker by remember { mutableStateOf<Marker?>(null) }
    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                setBuiltInZoomControls(false)
                controller.setZoom(14.0)
                val matrix = ColorMatrix()
                matrix.setSaturation(0f)
                val filter = ColorMatrixColorFilter(matrix)
                overlayManager.tilesOverlay.setColorFilter(filter)
                if (currentPoint != null) {
                    controller.setCenter(GeoPoint(currentPoint.first, currentPoint.second))
                }
                val marker = Marker(this).apply {
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    isDraggable = true
                    icon = createServiceMarkerDrawable(ctx)
                    title = "service_location"
                    servicePoint?.let {
                        position = GeoPoint(it.first, it.second)
                    }
                    setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                        override fun onMarkerDragStart(marker: Marker?) = Unit

                        override fun onMarkerDrag(marker: Marker?) = Unit

                        override fun onMarkerDragEnd(marker: Marker?) {
                            marker?.position?.let { pos ->
                                onServicePointMoved(pos.latitude to pos.longitude)
                            }
                        }
                    })
                }
                serviceMarker = marker
                overlays.add(marker)
                val overlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                locationOverlayHolder(overlay)
                overlays.add(overlay)
                mapViewHolder(this)
                val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
                if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
                    overlay.enableMyLocation()
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { mapView ->
            mapViewHolder(mapView)

            if (routePoints.isNotEmpty()) {
                mapView.overlays.removeAll { it is Polyline }
                val polyline = Polyline().apply {
                    setPoints(routePoints)
                    outlinePaint.color = android.graphics.Color.parseColor("#E8524A")
                    outlinePaint.strokeWidth = 8f
                }
                mapView.overlays.add(polyline)
                mapView.invalidate()
            }

            serviceMarker?.let { marker ->
                val target = servicePoint ?: orderAddressPoint
                if (target != null) {
                    val targetPoint = GeoPoint(target.first, target.second)
                    if (marker.position != targetPoint) {
                        marker.position = targetPoint
                    }
                }
            }
        }
    )
}

private fun createServiceMarkerDrawable(context: android.content.Context): BitmapDrawable {
    val sizePx = (56 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
    val source = BitmapFactory.decodeResource(context.resources, R.drawable.app_logo)
    val scaled = Bitmap.createScaledBitmap(source, sizePx, sizePx, true)
    return BitmapDrawable(context.resources, scaled)
}

private data class RoutePath(
    val points: List<GeoPoint>,
    val distanceMeters: Double
)

private suspend fun fetchRoadRoute(
    start: Pair<Double, Double>,
    end: Pair<Double, Double>
): RoutePath? {
    return try {
        withContext(Dispatchers.IO) {
            val urlString = buildString {
                append("https://router.project-osrm.org/route/v1/driving/")
                append("${start.second},${start.first};${end.second},${end.first}")
                append("?overview=full&geometries=geojson&steps=false&alternatives=false")
            }
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "MafiMushkilApp-Android/1.1")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null

            val response = connection.inputStream.bufferedReader().readText()
            val root = JSONObject(response)
            if (root.optString("code") != "Ok") return@withContext null

            val route = root.optJSONArray("routes")?.optJSONObject(0) ?: return@withContext null
            val geometry = route.optJSONObject("geometry") ?: return@withContext null
            val coords = geometry.optJSONArray("coordinates") ?: return@withContext null
            val points = buildList {
                for (i in 0 until coords.length()) {
                    val coord = coords.optJSONArray(i) ?: continue
                    val lon = coord.optDouble(0, Double.NaN)
                    val lat = coord.optDouble(1, Double.NaN)
                    if (!lat.isNaN() && !lon.isNaN()) {
                        add(GeoPoint(lat, lon))
                    }
                }
            }
            if (points.isEmpty()) return@withContext null
            RoutePath(
                points = points,
                distanceMeters = route.optDouble("distance", 0.0)
            )
        }
    } catch (e: Exception) {
        Log.e("ServiceOrderDetail", "Failed to fetch road route", e)
        null
    }
}

private fun formatDistance(distanceMeters: Double): String {
    return if (distanceMeters >= 1000) {
        String.format(java.util.Locale.US, "%.1f km", distanceMeters / 1000.0)
    } else {
        String.format(java.util.Locale.US, "%.0f m", distanceMeters)
    }
}
