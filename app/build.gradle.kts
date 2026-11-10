plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Release APKs come out unsigned and are signed by hand with the Numio key,
// so the key never leaves RR's own computer. GitHub only makes test builds.
android {
    namespace = "com.rr.numio.control"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.rr.numio.control"
        // 29 (Android 10): Settings panels (Wi-Fi, internet, NFC) and the
        // lock screen / screenshot accessibility actions all exist from here. See README.
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        // Debug builds install next to the real app as "Control Debug"
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            // R8: shrinks unused library code and starts faster
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    // F-Droid: no Google-encrypted dependency blob in the APK
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Settings (theme, handle side, tile order), kept on the phone
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
