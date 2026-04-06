package com.vardash.mafimushkil

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.vardash.mafimushkil.auth.AuthState
import com.vardash.mafimushkil.auth.AuthViewModel
import com.vardash.mafimushkil.screens.SplashScreenContent
import com.vardash.mafimushkil.ui.theme.Accent
import com.vardash.mafimushkil.ui.theme.MafiMushkilTheme

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Accent.toArgb()
        window.navigationBarColor = Accent.toArgb()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        authViewModel.checkAuthState(this)

        setContent {
            MafiMushkilTheme {
                val view = LocalView.current
                SideEffect {
                    val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
                    window.statusBarColor = Accent.toArgb()
                    window.navigationBarColor = Accent.toArgb()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                    }
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = true
                        isAppearanceLightNavigationBars = true
                    }
                }

                LaunchedEffect(Unit) {
                    withFrameNanos { }
                    val destination = if (authViewModel.authState.value is AuthState.Success) {
                        Routes.Home
                    } else {
                        Routes.Welcome
                    }
                    startActivity(
                        Intent(this@SplashActivity, MainActivity::class.java)
                            .putExtra(EXTRA_START_DESTINATION, destination)
                    )
                    finish()
                }

                SplashScreenContent()
            }
        }
    }
}
