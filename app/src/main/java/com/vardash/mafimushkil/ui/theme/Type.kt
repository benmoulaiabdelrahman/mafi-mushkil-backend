package com.vardash.mafimushkil.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.vardash.mafimushkil.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val GlegooFontFamily = FontFamily(
    Font(googleFont = GoogleFont("Glegoo"), fontProvider = provider)
)

// Load Questv1 from res/font
// Note: Ensure the files are named questv1_regular.otf and questv1_bold.otf
val Questv1FontFamily = FontFamily(
    Font(R.font.questv1_regular, FontWeight.Normal),
    Font(R.font.questv1_bold, FontWeight.Bold)
)

// Set of Material typography styles to start with
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = Questv1FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp
    ),
    displayMedium = TextStyle(
        fontFamily = Questv1FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp
    ),
    displaySmall = TextStyle(
        fontFamily = Questv1FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = Questv1FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Questv1FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Questv1FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Questv1FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Questv1FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Questv1FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Questv1FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Questv1FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Questv1FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Questv1FontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Questv1FontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Questv1FontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    )
)
