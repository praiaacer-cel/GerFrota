// app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    // ❌ NÃO COLOQUE O kotlin-android AQUI (O Flutter já aplica automaticamente)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.gerfrota.lite"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gerfrota.lite"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        ndk { 
            abiFilters += listOf("arm64-v8a", "x86_64") 
        }

        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_STL=c++_shared", "-DGGML_NATIVE=OFF")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildFeatures {
        compose = true
    }

    // ⚠️ No AGP 9.x o "aaptOptions" foi substituído por "androidResources"
    androidResources {
        noCompress += listOf("gguf")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
}
