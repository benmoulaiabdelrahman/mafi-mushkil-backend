package com.vardash.mafimushkil.screens

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.vardash.mafimushkil.R
import com.vardash.mafimushkil.auth.ProfileState
import com.vardash.mafimushkil.auth.ProfileViewModel
import com.vardash.mafimushkil.auth.UserProfile
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme
import com.vardash.mafimushkil.ui.theme.Questv1FontFamily
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPhotoScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel? = if (LocalInspectionMode.current) null else viewModel(),
    forceShowSuccess: Boolean = false
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val density = LocalDensity.current

    val profileState by (profileViewModel?.profileState?.collectAsState() ?: remember { mutableStateOf(ProfileState.Idle) })
    val userProfile by (profileViewModel?.userProfile?.collectAsState() ?: remember { mutableStateOf(UserProfile()) })

    // Use rememberSaveable to persist selection across activity destruction
    var selectedImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedImageUri = remember(selectedImageUriString) {
        selectedImageUriString?.let { Uri.parse(it) }
    }
    
    var showSuccessSheet by remember { mutableStateOf(forceShowSuccess) }
    var showNoInternetSheet by remember { mutableStateOf(false) }
    
    // Transformation states
    var userScale by rememberSaveable { mutableFloatStateOf(1f) }
    var offsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable { mutableFloatStateOf(0f) }
    var imageAspectRatio by remember { mutableStateOf<Float?>(null) }
    var isInitialSyncDone by rememberSaveable { mutableStateOf(false) }

    val boxSize = 280.dp
    val boxSizePx = with(density) { boxSize.toPx() }

    // Camera related states
    var tempUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val tempUri = remember(tempUriString) {
        if (tempUriString != null) Uri.parse(tempUriString) else Uri.EMPTY
    }
    var cameraTrigger by rememberSaveable { mutableIntStateOf(0) }

    // Initialize tempUri if needed
    LaunchedEffect(Unit) {
        if (tempUriString == null && !isPreview) {
            try {
                val tempFile = File.createTempFile("edit_photo_", ".jpg", context.cacheDir)
                tempUriString = FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile).toString()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // minScale is the scale required to make the image cover the box
    val minScale = remember(imageAspectRatio) {
        val aspect = imageAspectRatio ?: 1f
        if (aspect > 1f) aspect else 1f / aspect
    }

    // Logic to calculate max offsets to keep the image covering the box
    fun calculateMaxOffsets(s: Float, aspect: Float): Pair<Float, Float> {
        val fittedWidth = if (aspect > 1f) boxSizePx else boxSizePx * aspect
        val fittedHeight = if (aspect > 1f) boxSizePx / aspect else boxSizePx
        
        val ms = if (aspect > 1f) aspect else 1f / aspect
        val actualScale = s * ms
        val finalWidth = fittedWidth * actualScale
        val finalHeight = fittedHeight * actualScale
        
        val maxOffsetX = (finalWidth - boxSizePx).coerceAtLeast(0f) / 2f
        val maxOffsetY = (finalHeight - boxSizePx).coerceAtLeast(0f) / 2f
        return maxOffsetX to maxOffsetY
    }

    // 1. Sync from userProfile on initial load or profile update
    LaunchedEffect(userProfile) {
        if (selectedImageUri == null && profileState !is ProfileState.Loading && profileState !is ProfileState.Success && !isInitialSyncDone) {
            userScale = userProfile.photoScale.coerceAtLeast(1f)
            offsetX = userProfile.photoOffsetX * density.density
            offsetY = userProfile.photoOffsetY * density.density
            isInitialSyncDone = true
        }
    }

    // 2. Reset transformations when a new image is selected or captured
    LaunchedEffect(selectedImageUriString, cameraTrigger) {
        if (selectedImageUriString != null) {
            userScale = 1f
            offsetX = 0f
            offsetY = 0f
            imageAspectRatio = null
            isInitialSyncDone = true
        }
    }

    // 3. Re-coerce offsets when scale or aspect ratio changes
    LaunchedEffect(userScale, imageAspectRatio, isInitialSyncDone) {
        val aspect = imageAspectRatio ?: return@LaunchedEffect
        if (selectedImageUri == null && !isInitialSyncDone) return@LaunchedEffect
        
        val (maxOffsetX, maxOffsetY) = calculateMaxOffsets(userScale, aspect)
        offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
        offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUriString = uri.toString()
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUriString = tempUriString
            cameraTrigger++
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (tempUri != Uri.EMPTY) {
                cameraLauncher.launch(tempUri)
            }
        } else {
            Toast.makeText(context, context.getString(R.string.camera_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    fun performUpload() {
        val uriToUpload = selectedImageUri ?: Uri.parse(userProfile.profilePhoto)
        if (uriToUpload != Uri.EMPTY) {
            profileViewModel?.uploadProfilePhoto(
                uriToUpload, 
                context, 
                userScale, 
                offsetX / density.density, 
                offsetY / density.density
            )
        }
    }

    LaunchedEffect(profileState) {
        when (profileState) {
            is ProfileState.Success -> {
                showSuccessSheet = true
                profileViewModel?.resetState()
            }
            is ProfileState.Error -> {
                val errorMsg = (profileState as ProfileState.Error).message.lowercase()
                if (errorMsg.contains("network error") || 
                    errorMsg.contains("timeout") || 
                    errorMsg.contains("unreachable") ||
                    errorMsg.contains("connect")) {
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
                        text = stringResource(R.string.edit_photo_title),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        fontFamily = Questv1FontFamily
                    )
                }
                Button(
                    onClick = { performUpload() },
                    enabled = (selectedImageUri != null || (userProfile.profilePhoto.isNotEmpty() && (userScale != userProfile.photoScale || (offsetX / density.density) != userProfile.photoOffsetX || (offsetY / density.density) != userProfile.photoOffsetY))) 
                        && profileState !is ProfileState.Loading,
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
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFFCCFD04), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.save), fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = Questv1FontFamily)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(boxSize)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE0E0E0))
                    .pointerInput(imageAspectRatio, minScale) {
                        val aspect = imageAspectRatio ?: return@pointerInput
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (userScale * zoom).coerceIn(1f, 5f)
                            val (maxOffsetX, maxOffsetY) = calculateMaxOffsets(newScale, aspect)
                            
                            userScale = newScale
                            offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                            offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val imageModel = selectedImageUri ?: userProfile.profilePhoto.ifEmpty { null }
                
                if (imageModel != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageModel)
                            .apply {
                                if (imageModel == tempUri || (imageModel is String && imageModel == tempUriString)) {
                                    memoryCachePolicy(CachePolicy.DISABLED)
                                    diskCachePolicy(CachePolicy.DISABLED)
                                    setParameter("trigger", cameraTrigger)
                                }
                            }
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.profile_photo),
                        onSuccess = { state ->
                            imageAspectRatio = state.painter.intrinsicSize.width / state.painter.intrinsicSize.height
                        },
                        onError = {
                            // If it fails, we'll see the gray background or the Person icon
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = userScale * minScale,
                                scaleY = userScale * minScale,
                                translationX = offsetX,
                                translationY = offsetY
                            ),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = stringResource(R.string.profile_photo),
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(100.dp)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.05f))
                )
            }

            Spacer(Modifier.height(12.dp))
            
            Text(
                text = stringResource(R.string.edit_photo_guideline),
                fontSize = 13.sp,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                fontFamily = Questv1FontFamily
            )

            Spacer(Modifier.height(32.dp))

            Card(
                onClick = { galleryLauncher.launch("image/*") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF5)),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Image,
                        contentDescription = stringResource(R.string.edit_photo_gallery),
                        tint = Color(0xFF282828),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.edit_photo_gallery),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF282828),
                        fontFamily = Questv1FontFamily
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF5)),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.CameraAlt,
                        contentDescription = stringResource(R.string.edit_photo_camera),
                        tint = Color(0xFF282828),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.edit_photo_camera),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF282828),
                        fontFamily = Questv1FontFamily
                    )
                }
            }
        }
    }

    if (showSuccessSheet) {
        ModalBottomSheet(
            onDismissRequest = { 
                showSuccessSheet = false
                selectedImageUriString = null // Reset so it loads from updated profile
                if (!isPreview) navController.popBackStack()
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
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.AccountBox,
                        contentDescription = stringResource(R.string.edit_photo_success),
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.edit_photo_success),
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
                        selectedImageUriString = null
                        if (!isPreview) navController.popBackStack()
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
                    @Suppress("DEPRECATION")
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
            onTryAgain = { performUpload() }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, locale = "ar")
@Composable
fun EditPhotoScreenPreview() {
    MafiMushkilTheme {
        EditPhotoScreen(navController = rememberNavController())
    }
}
