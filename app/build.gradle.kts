plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "dev.sudhanshu.calender"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.sudhanshu.calender"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
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
    implementation(libs.androidx.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.common)
    testImplementation(libs.junit)
    implementation(libs.accompanist.systemuicontroller)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.navigation.compose)

    // testing library
    testImplementation(libs.androidx.core.testing)

    // Mockito
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)

    // Coroutine testing
    testImplementation(libs.kotlinx.coroutines.test)

    // MockWebServer
    testImplementation(libs.mockwebserver)

    // retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.okhttp)

    // Kotlin Coroutines dependencies
    implementation(libs.kotlinx.coroutines.android) 

    // Hilt dependencies
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Lifecycle components
    implementation(libs.androidx.lifecycle.runtime.ktx.v282)

    // View model
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // google font
    implementation(libs.androidx.ui.text.google.fonts)

    //Google Credentials Manager
    implementation("androidx.credentials:credentials:1.2.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")
    //implementation("com.google.apis:google-api-services-calendar:v3-rev411-1.25.0")
    //implementation("com.google.http-client:google-http-client-android:1.43.0")
    //implementation("com.google.oauth-client:google-oauth-client-jetty:1.32.1")
    implementation("com.google.android.gms:play-services-auth:20.6.0")
    //implementation("androidx.activity:activity-ktx:1.8.2")
    //implementation("com.google.api-client:google-api-client-android:1.31.5")
    //implementation("com.google.api-client:google-api-client-gson:1.31.5")
    //implementation("com.squareup.okhttp3:okhttp:4.9.1")
    //implementation("org.json:json:20210307")
    //implementation("com.google.http-client:google-http-client-jackson2:1.42.3")
    //implementation("androidx.appcompat:appcompat:1.6.1")
}