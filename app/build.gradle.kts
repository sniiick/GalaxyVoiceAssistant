plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.voiceapp3"
    compileSdk = 32
    buildToolsVersion = "30.0.3"
    ndkVersion = "21.4.7075529"

    defaultConfig {
        applicationId = "com.example.voiceapp3"
        minSdk = 28
        targetSdk = 32
        versionCode = 76
        versionName = "7.1.2024_1203_1849-p145"
    }

    buildFeatures {
        compose = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    useLibrary("android.car")
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packagingOptions {
        jniLibs.useLegacyPackaging = true
    }
}

dependencies {
    implementation("androidx.core:core:1.7.0")
    implementation("androidx.media:media:1.6.0")

    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("net.java.dev.jna:jna:5.14.0@aar")
    implementation("com.alphacephei:vosk-android:0.3.47") {
        exclude(group = "net.java.dev.jna", module = "jna")
    }
    implementation("com.airbnb.android:lottie:6.0.0")
    implementation("com.microsoft.onnxruntime:onnxruntime-android:latest.release")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation(files("libs/sherpa-onnx-v1.12.9-java8.jar"))
}
