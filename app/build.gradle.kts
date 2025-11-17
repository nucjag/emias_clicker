plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.autoclicker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.autoclicker"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    dependencies {
        // AppCompat для AppCompatActivity
        implementation("androidx.appcompat:appcompat:1.7.0")

        // Jetpack Compose BOM для управления версиями
        implementation(platform("androidx.compose:compose-bom:2024.10.00"))
        implementation("androidx.compose.ui:ui")
        implementation("androidx.compose.ui:ui-graphics")
        implementation("androidx.compose.ui:ui-tooling-preview")
        implementation("androidx.compose.material3:material3")

        // Для интеграции Compose с Activity
        implementation("androidx.activity:activity-compose:1.9.2")

        // Другие стандартные зависимости Android (если нужны)
        implementation("androidx.core:core-ktx:1.13.1")
        implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

        // Тестирование Compose (опционально)
        debugImplementation("androidx.compose.ui:ui-tooling")
        debugImplementation("androidx.compose.ui:ui-test-manifest")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // Add this block to enable Compose
    buildFeatures {
        compose = true
    }

    // Move the kotlinCompilerExtensionVersion into this new block
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}
