plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")   // NEW: for @Serializable NavKeys
}

android {
    namespace = "com.example.expensetracker"
    compileSdk = 36                                    // CHANGED: Navigation 3 needs 36

    defaultConfig {
        applicationId = "com.example.expensetracker"
        minSdk = 26          // java.time (LocalDate, YearMonth) needs API 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // REMOVED: kotlinOptions { } (it's an error in Kotlin 2.2, see the kotlin { } block below)

    buildFeatures {
        compose = true
    }
}

// NEW: replaces kotlinOptions
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.12.00")   // CHANGED
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.12.0")             // CHANGED
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    // NEW: Lab 5
    implementation("androidx.navigation3:navigation3-runtime:1.2.1")
    implementation("androidx.navigation3:navigation3-ui:1.2.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}