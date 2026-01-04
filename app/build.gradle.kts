plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.liquidglass.fluxhub"
    compileSdk {
        version = release(36)
    }
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "com.liquidglass.fluxhub"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += arrayOf(
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json",
                "kotlin/**",
                "META-INF/*.version",
                "META-INF/**/LICENSE.txt"
            )
        }
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xlambdas=class"
        )
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Compose
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.ripple)
    implementation(libs.androidx.compose.material.icons)
    
    // Lifecycle & Navigation
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    
    // Networking
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation(libs.kotlinx.serialization.json)
    
    // Liquid Glass Effect
    implementation(libs.capsule)
    implementation(project(":backdrop"))
}
