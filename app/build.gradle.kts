import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

fun readLocalProperty(name: String): String {
    return localProperties.getProperty(name, "").replace("\"", "\\\"")
}

android {
    namespace = "com.vardash.mafimushkil"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.vardash.mafimushkil"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "CHARGILY_SECRET_KEY", "\"${readLocalProperty("chargily.secret_key")}\"")
        buildConfigField("String", "CHARGILY_PUBLIC_KEY", "\"${readLocalProperty("chargily.public_key")}\"")
        buildConfigField("String", "CHARGILY_WEBHOOK_URL", "\"${readLocalProperty("chargily.webhook_url")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        noCompress += listOf("png")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.splashscreen)

    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.0")

    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("androidx.browser:browser:1.8.0")

    debugImplementation(libs.androidx.compose.ui.tooling)
    
    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:34.10.0"))

    // Firebase libraries
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-dynamic-links:22.1.0")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging-ktx:23.3.0")
    implementation("com.google.firebase:firebase-storage")

    // Coroutines support for Firebase
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // ViewModel & Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Cloudinary
    implementation("com.cloudinary:cloudinary-android:3.1.2")
}
