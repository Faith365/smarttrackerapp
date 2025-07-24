plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.smarttracker.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.smarttracker.app"
        minSdk = 25
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        viewBinding = true // ⛳️ You need to add this line!
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation (libs.androidx.navigation.compose)
    implementation (libs.androidx.material.icons.extended)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    // Biometrics Authentication library dependencies
    implementation (libs.androidx.biometric)


    // 🔥Firebase BoM for version management
    implementation(platform(libs.firebase.bom))

    // 🔥Firebase Authentication for easy Kotlin support(you can add others like Firestore later)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)


    // FIDO2 (WebAuthn API for Android)
    implementation(libs.fido)

    implementation(libs.credentials) // The Credential Manager API

    implementation(libs.credentials.play.services.auth)

    implementation(libs.androidx.core.ktx.v1120)
    implementation(libs.androidx.lifecycle.runtime.ktx.v262)
    implementation(libs.androidx.activity.compose.v180)
    implementation(libs.ui)
    implementation(libs.material3)

    // Coil for image loading
    implementation(libs.coil.kt.coil.compose)

    // Google Maps SDK for Android
    implementation(libs.play.services.maps)

    // Google Maps Compose library
    implementation(libs.maps.compose)


    implementation(libs.play.services.location)


    implementation(libs.gson)


    implementation(libs.kotlinx.coroutines.play.services)


    implementation(libs.maps.utils)

    implementation(libs.accompanist.permissions)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// ✅ Apply Google Services plugin OUTSIDE the dependencies block
apply(plugin = "com.google.gms.google-services")
