package com.vardash.mafimushkil.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import java.util.Locale

private val AppColorScheme = lightColorScheme(
    primary       = Primary,
    background    = Background,
    surface       = Surface,
    onPrimary     = Accent,
    onBackground  = Primary,
    onSurface     = Primary,
)

@Composable
fun MafiMushkilTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    
    // Force Arabic Locale configuration
    val locale = Locale("ar")
    val configuration = Configuration(context.resources.configuration).apply {
        setLocale(locale)
        setLayoutDirection(locale)
    }

    // Android Studio Layout Editor does not support createConfigurationContext.
    // We skip it in edit mode to avoid fidelity warnings.
    val localizedContext = if (view.isInEditMode) {
        context
    } else {
        context.createConfigurationContext(configuration)
    }

    val activity = context.findActivity()

    if (!view.isInEditMode) {
        Locale.setDefault(locale)
        SideEffect {
            if (activity != null) {
                val window = activity.window
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            }
        }
    }

    // By providing localizedContext and configuration here, we force stringResource
    // to use the "ar" resources even if the Preview environment defaults to English.
    // We also must explicitly re-provide Owners that are normally derived from LocalContext,
    // because localizedContext (a ConfigurationContext) doesn't implement them.
    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides (if (view.isInEditMode) LocalConfiguration.current else configuration),
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        val registryOwner = activity as? ActivityResultRegistryOwner
        val backOwner = activity as? androidx.activity.OnBackPressedDispatcherOwner
        
        if (registryOwner != null && backOwner != null) {
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides registryOwner,
                LocalOnBackPressedDispatcherOwner provides backOwner
            ) {
                MaterialTheme(
                    colorScheme = AppColorScheme,
                    typography  = Typography,
                    content     = content
                )
            }
        } else {
            MaterialTheme(
                colorScheme = AppColorScheme,
                typography  = Typography,
                content     = content
            )
        }
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
